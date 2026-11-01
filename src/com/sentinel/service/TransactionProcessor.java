package com.sentinel.service;

import com.sentinel.dao.TransactionDAO;
import com.sentinel.exception.FraudDetectionException;
import com.sentinel.model.Transaction;
import com.sentinel.util.AuditLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Processes MANY transactions AT THE SAME TIME instead of one-by-one.
 *
 * WHY: if 1000 transactions arrive, checking them one after another (single
 * thread) is slow. ExecutorService gives us a fixed pool of worker threads;
 * each transaction is submitted as a task and whichever thread is free picks
 * it up next -- like having several cashiers instead of one.
 */
public class TransactionProcessor {

    private final ExecutorService executor;
    private final RiskScoringService riskScoringService;
    private final TransactionDAO transactionDAO;
    private final AuditLogger auditLogger;

    public TransactionProcessor(int threadPoolSize, TransactionDAO transactionDAO, AuditLogger auditLogger) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        this.transactionDAO = transactionDAO;
        this.riskScoringService = new RiskScoringService(transactionDAO);
        this.auditLogger = auditLogger;
    }

    /**
     * Submits all transactions to the thread pool and waits for every one of
     * them to finish before returning. Returns the list of processed
     * (scored) transactions.
     */
    public List<Transaction> processAll(List<Transaction> incoming) throws FraudDetectionException {
        // IMPORTANT: risk rules like "impossible travel" and "rapid-fire" depend on
        // looking at a PREVIOUS transaction for the SAME account. If two transactions
        // from the same account ran on different threads at the same time, one worker
        // could evaluate its transaction before the other has been recorded -- a race
        // condition that causes flags to be missed.
        //
        // Fix: transactions for the SAME account are chained to run one-after-another
        // (in order), while transactions for DIFFERENT accounts still run fully in
        // parallel across the thread pool. This keeps correctness without giving up
        // concurrency.
        Map<String, CompletableFuture<Transaction>> lastByAccount = new HashMap<>();
        List<CompletableFuture<Transaction>> allFutures = new java.util.ArrayList<>();

        for (Transaction txn : incoming) {
            String account = txn.getAccountId();
            CompletableFuture<Transaction> previous = lastByAccount.get(account);

            CompletableFuture<Transaction> current = (previous == null)
                    ? CompletableFuture.supplyAsync(() -> evaluateAndLog(txn), executor)
                    : previous.thenApplyAsync(ignored -> evaluateAndLog(txn), executor);

            lastByAccount.put(account, current);
            allFutures.add(current);
        }

        try {
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            throw new FraudDetectionException("A transaction failed to process", e.getCause());
        }

        List<Transaction> processed = new java.util.ArrayList<>();
        for (CompletableFuture<Transaction> f : allFutures) {
            processed.add(f.join());
        }
        return processed;
    }

    /** Runs risk scoring for one transaction and writes an audit log line. Wraps checked exceptions so it can be used inside CompletableFuture. */
    private Transaction evaluateAndLog(Transaction txn) {
        try {
            Transaction result = riskScoringService.evaluate(txn);
            auditLogger.log(Thread.currentThread().getName() + " processed " + result);
            return result;
        } catch (Exception e) {
            auditLogger.log("ERROR processing txn#" + txn.getId() + ": " + e.getMessage());
            throw new CompletionException(e);
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
