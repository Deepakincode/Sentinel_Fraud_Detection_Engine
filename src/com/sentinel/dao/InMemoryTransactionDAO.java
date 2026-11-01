package com.sentinel.dao;

import com.sentinel.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory stand-in for TransactionDAOImpl.
 * Same public interface (TransactionDAO) as the real JDBC/MySQL version, so
 * the rest of the app (services, RiskScoringService, Main) doesn't need to
 * change at all when you swap this out for the real database implementation.
 * Uses ConcurrentHashMap because multiple threads (ExecutorService workers)
 * write to it at the same time.
 */
public class InMemoryTransactionDAO implements TransactionDAO {

    private final Map<Integer, Transaction> store = new ConcurrentHashMap<>();

    @Override
    public void save(Transaction transaction) {
        store.put(transaction.getId(), transaction);
    }

    @Override
    public void updateRiskResult(Transaction transaction) {
        store.put(transaction.getId(), transaction);
    }

    @Override
    public List<Transaction> findByAccountId(String accountId) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : store.values()) {
            if (t.getAccountId().equals(accountId)) {
                result.add(t);
            }
        }
        return result;
    }

    @Override
    public List<Transaction> findFlagged() {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : store.values()) {
            if (t.isFlagged()) {
                result.add(t);
            }
        }
        return result;
    }
}
