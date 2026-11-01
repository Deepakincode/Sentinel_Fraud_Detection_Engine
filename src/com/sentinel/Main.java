package com.sentinel;

import com.sentinel.dao.InMemoryTransactionDAO;
import com.sentinel.dao.InMemoryUserDAO;
import com.sentinel.dao.TransactionDAO;
import com.sentinel.dao.UserDAO;
import com.sentinel.exception.AccessDeniedException;
import com.sentinel.model.Transaction;
import com.sentinel.model.User;
import com.sentinel.service.FraudDetectionService;
import com.sentinel.service.TransactionProcessor;
import com.sentinel.util.AuditLogger;
import com.sentinel.util.ReportGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo entry point. Simulates a batch of incoming transactions (some clean,
 * some clearly fraudulent), processes them concurrently, then shows the RBAC
 * rules and file-based reporting in action.
 *
 * NOTE: this demo uses the InMemory DAOs (no real MySQL server needed) so it
 * runs instantly. Swap InMemoryTransactionDAO/InMemoryUserDAO for
 * TransactionDAOImpl/UserDAOImpl to run against a real MySQL database
 * (see sql/schema.sql).
 */
public class Main {

    public static void main(String[] args) throws Exception {
        AuditLogger auditLogger = new AuditLogger("logs/audit.log");
        TransactionDAO transactionDAO = new InMemoryTransactionDAO();
        UserDAO userDAO = new InMemoryUserDAO();

        // 1. Simulate incoming transactions
        List<Transaction> incoming = buildSampleTransactions();

        // 2. Process them concurrently (multithreading via ExecutorService)
        TransactionProcessor processor = new TransactionProcessor(4, transactionDAO, auditLogger);
        List<Transaction> processed;
        try {
            processed = processor.processAll(incoming);
            for (Transaction t : processed) {
                transactionDAO.save(t); // persist final scored result
            }
        } finally {
            processor.shutdown();
        }

        System.out.println("\n=== ALL PROCESSED TRANSACTIONS ===");
        processed.forEach(System.out::println);

        // 3. RBAC demo: an employee tries to view flagged transactions (allowed)
        FraudDetectionService service = new FraudDetectionService(transactionDAO, auditLogger);
        User alice = userDAO.findByUsername("alice"); // EMPLOYEE
        User bob = userDAO.findByUsername("bob");     // MANAGER

        System.out.println("\n=== FLAGGED TRANSACTIONS (viewed by " + alice + ") ===");
        List<Transaction> flagged = service.viewFlagged(alice);
        flagged.forEach(System.out::println);

        // 4. RBAC demo: employee tries to BLOCK a transaction -> should be denied
        System.out.println("\n=== RBAC CHECK: employee tries to block a transaction ===");
        try {
            if (!flagged.isEmpty()) {
                service.approveOrBlock(alice, flagged.get(0), false);
            }
        } catch (AccessDeniedException e) {
            System.out.println("DENIED as expected: " + e.getMessage());
        }

        // 5. Manager blocks it -> allowed
        System.out.println("\n=== MANAGER blocks the flagged transaction ===");
        if (!flagged.isEmpty()) {
            service.approveOrBlock(bob, flagged.get(0), false);
        }

        // 6. File-based report of all flagged transactions
        ReportGenerator.writeFlaggedReport("reports/suspicious_activity_report.txt", flagged);
        System.out.println("\nReport written to reports/suspicious_activity_report.txt");
        System.out.println("Audit log written to logs/audit.log");
    }

    private static List<Transaction> buildSampleTransactions() {
        List<Transaction> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Normal transaction
        list.add(new Transaction(1, "ACC-001", 1200.00, "Mumbai", now));

        // High amount -> should be flagged
        list.add(new Transaction(2, "ACC-002", 75000.00, "Delhi", now.plusSeconds(5)));

        // Impossible travel: same account, two far-apart cities within 2 minutes
        list.add(new Transaction(3, "ACC-003", 500.00, "Mumbai", now));
        list.add(new Transaction(4, "ACC-003", 800.00, "London", now.plusMinutes(2)));

        // Rapid-fire: same account, 3 transactions within a minute
        list.add(new Transaction(5, "ACC-004", 100.00, "Hyderabad", now));
        list.add(new Transaction(6, "ACC-004", 150.00, "Hyderabad", now.plusSeconds(15)));
        list.add(new Transaction(7, "ACC-004", 90.00, "Hyderabad", now.plusSeconds(30)));

        // Normal small transaction
        list.add(new Transaction(8, "ACC-005", 45.50, "Chennai", now));

        return list;
    }
}
