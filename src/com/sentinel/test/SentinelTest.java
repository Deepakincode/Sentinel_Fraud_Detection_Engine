package com.sentinel.test;

import com.sentinel.dao.InMemoryTransactionDAO;
import com.sentinel.dao.InMemoryUserDAO;
import com.sentinel.dao.TransactionDAO;
import com.sentinel.exception.AccessDeniedException;
import com.sentinel.model.Role;
import com.sentinel.model.Transaction;
import com.sentinel.model.User;
import com.sentinel.security.AccessControl;
import com.sentinel.service.RiskScoringService;
import com.sentinel.service.TransactionProcessor;
import com.sentinel.util.AuditLogger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight test runner (no external test framework needed).
 * FUNCTIONAL TESTS  -> test ONE piece of logic in isolation (e.g. does a
 *                      high-amount transaction get flagged?)
 * INTEGRATION TESTS -> test multiple pieces working together (e.g. does the
 *                      whole pipeline: processor -> risk scoring -> DAO
 *                      -> RBAC actually work end-to-end?)
 *
 * Run with: java -cp out com.sentinel.test.SentinelTest
 */
public class SentinelTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        testHighAmountIsFlagged();
        testNormalAmountIsNotFlagged();
        testImpossibleTravelIsFlagged();
        testRbacDeniesEmployeeFromBlocking();
        testRbacAllowsManagerToBlock();
        testEndToEndConcurrentProcessing();

        System.out.println("\n===================================");
        System.out.println("RESULTS: " + passed + " passed, " + failed + " failed");
        System.out.println("===================================");
    }

    // ---------- FUNCTIONAL TESTS ----------

    private static void testHighAmountIsFlagged() throws Exception {
        TransactionDAO dao = new InMemoryTransactionDAO();
        RiskScoringService risk = new RiskScoringService(dao);
        Transaction txn = new Transaction(100, "ACC-X", 60000.00, "Delhi", LocalDateTime.now());

        Transaction result = risk.evaluate(txn);
        assertTrue("High amount should be flagged", result.isFlagged());
    }

    private static void testNormalAmountIsNotFlagged() throws Exception {
        TransactionDAO dao = new InMemoryTransactionDAO();
        RiskScoringService risk = new RiskScoringService(dao);
        Transaction txn = new Transaction(101, "ACC-Y", 500.00, "Mumbai", LocalDateTime.now());

        Transaction result = risk.evaluate(txn);
        assertTrue("Normal small transaction should NOT be flagged", !result.isFlagged());
    }

    private static void testImpossibleTravelIsFlagged() throws Exception {
        TransactionDAO dao = new InMemoryTransactionDAO();
        RiskScoringService risk = new RiskScoringService(dao);
        LocalDateTime now = LocalDateTime.now();

        risk.evaluate(new Transaction(102, "ACC-Z", 300.00, "Mumbai", now));
        Transaction second = risk.evaluate(new Transaction(103, "ACC-Z", 300.00, "London", now.plusMinutes(1)));

        assertTrue("Two far-apart cities within 5 min should be flagged", second.isFlagged());
    }

    // ---------- RBAC (SECURITY) TESTS ----------

    private static void testRbacDeniesEmployeeFromBlocking() {
        User employee = new InMemoryUserDAO().findByUsername("alice"); // EMPLOYEE
        boolean denied = false;
        try {
            AccessControl.check(employee, AccessControl.Action.APPROVE_OR_BLOCK);
        } catch (AccessDeniedException e) {
            denied = true;
        }
        assertTrue("Employee should be denied APPROVE_OR_BLOCK", denied);
    }

    private static void testRbacAllowsManagerToBlock() {
        User manager = new InMemoryUserDAO().findByUsername("bob"); // MANAGER
        boolean allowed = true;
        try {
            AccessControl.check(manager, AccessControl.Action.APPROVE_OR_BLOCK);
        } catch (AccessDeniedException e) {
            allowed = false;
        }
        assertTrue("Manager should be allowed APPROVE_OR_BLOCK", allowed);
    }

    // ---------- INTEGRATION TEST ----------

    private static void testEndToEndConcurrentProcessing() throws Exception {
        TransactionDAO dao = new InMemoryTransactionDAO();
        AuditLogger logger = new AuditLogger("logs/test_audit.log");
        TransactionProcessor processor = new TransactionProcessor(3, dao, logger);

        LocalDateTime now = LocalDateTime.now();
        List<Transaction> batch = List.of(
                new Transaction(200, "ACC-T1", 100.00, "Pune", now),
                new Transaction(201, "ACC-T2", 90000.00, "Delhi", now),
                new Transaction(202, "ACC-T3", 250.00, "Chennai", now)
        );

        List<Transaction> results = processor.processAll(batch);
        processor.shutdown();

        assertTrue("All 3 submitted transactions should be processed", results.size() == 3);
        long flaggedCount = results.stream().filter(Transaction::isFlagged).count();
        assertTrue("Exactly 1 of the 3 should be flagged (the high-amount one)", flaggedCount == 1);
    }

    // ---------- tiny assert helper ----------

    private static void assertTrue(String description, boolean condition) {
        if (condition) {
            System.out.println("[PASS] " + description);
            passed++;
        } else {
            System.out.println("[FAIL] " + description);
            failed++;
        }
    }
}
