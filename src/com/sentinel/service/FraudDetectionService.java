package com.sentinel.service;

import com.sentinel.dao.TransactionDAO;
import com.sentinel.model.Transaction;
import com.sentinel.model.User;
import com.sentinel.security.AccessControl;
import com.sentinel.util.AuditLogger;

import java.util.List;

/**
 * Public-facing service layer. Every action here checks RBAC permissions
 * FIRST (via AccessControl.check) before touching data, and logs the action
 * via AuditLogger so there's always a record of who accessed what.
 */
public class FraudDetectionService {

    private final TransactionDAO transactionDAO;
    private final AuditLogger auditLogger;

    public FraudDetectionService(TransactionDAO transactionDAO, AuditLogger auditLogger) {
        this.transactionDAO = transactionDAO;
        this.auditLogger = auditLogger;
    }

    public List<Transaction> viewFlagged(User requester) throws Exception {
        AccessControl.check(requester, AccessControl.Action.VIEW_TRANSACTIONS);
        auditLogger.log(requester + " viewed flagged transactions.");
        return transactionDAO.findFlagged();
    }

    public void approveOrBlock(User requester, Transaction txn, boolean approve) {
        AccessControl.check(requester, AccessControl.Action.APPROVE_OR_BLOCK);
        String action = approve ? "APPROVED" : "BLOCKED";
        auditLogger.log(requester + " " + action + " " + txn);
    }
}
