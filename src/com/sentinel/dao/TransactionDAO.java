package com.sentinel.dao;

import com.sentinel.model.Transaction;
import java.util.List;

/**
 * DAO (Data Access Object) pattern: this interface defines WHAT database
 * operations are available for Transactions, without exposing HOW they are
 * implemented (raw SQL, connection handling, etc.) to the rest of the app.
 * The service layer only ever talks to this interface.
 */
public interface TransactionDAO {
    void save(Transaction transaction) throws Exception;
    void updateRiskResult(Transaction transaction) throws Exception;
    List<Transaction> findByAccountId(String accountId) throws Exception;
    List<Transaction> findFlagged() throws Exception;
}
