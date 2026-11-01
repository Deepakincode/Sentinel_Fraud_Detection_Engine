package com.sentinel.service;

import com.sentinel.dao.TransactionDAO;
import com.sentinel.model.Transaction;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * RULE-BASED risk scoring (NOT machine learning).
 * We check every incoming transaction against a set of hand-written rules.
 * Each rule that matches adds points to a risk score (0-100).
 * If the total score crosses a threshold, the transaction gets flagged.
 *
 * This is the "brain" of the fraud engine.
 */
public class RiskScoringService {

    private static final double HIGH_AMOUNT_THRESHOLD = 50000.0;   // Rule 1
    private static final double FLAG_THRESHOLD = 50.0;             // score >= this => flagged
    private static final Duration IMPOSSIBLE_TRAVEL_WINDOW = Duration.ofMinutes(5);

    private final TransactionDAO transactionDAO;

    // Keeps recent transactions per account in memory so we can check
    // "impossible travel" and "rapid-fire transactions" rules quickly,
    // without hitting the database for every single check.
    private final java.util.Map<String, List<Transaction>> recentByAccount = new java.util.concurrent.ConcurrentHashMap<>();

    public RiskScoringService(TransactionDAO transactionDAO) {
        this.transactionDAO = transactionDAO;
    }

    /**
     * Evaluates ONE transaction and returns it with riskScore / flagged / flagReason filled in.
     * This method is called concurrently by multiple worker threads (see TransactionProcessor),
     * so it must be thread-safe.
     */
    public Transaction evaluate(Transaction txn) throws Exception {
        double score = 0;
        StringBuilder reasons = new StringBuilder();

        // Rule 1: unusually large amount
        if (txn.getAmount() >= HIGH_AMOUNT_THRESHOLD) {
            score += 55;
            reasons.append("High amount; ");
        }

        // Rule 2: "impossible travel" - same account, different location, too soon after last txn
        List<Transaction> history = recentByAccount.computeIfAbsent(
                txn.getAccountId(), k -> new CopyOnWriteArrayList<>());

        for (Transaction prev : history) {
            boolean differentLocation = !prev.getLocation().equalsIgnoreCase(txn.getLocation());
            boolean tooSoon = Duration.between(prev.getTimestamp(), txn.getTimestamp())
                    .abs().compareTo(IMPOSSIBLE_TRAVEL_WINDOW) < 0;
            if (differentLocation && tooSoon) {
                score += 50;
                reasons.append("Impossible travel (")
                        .append(prev.getLocation()).append(" -> ").append(txn.getLocation())
                        .append(" within ").append(IMPOSSIBLE_TRAVEL_WINDOW.toMinutes()).append("m); ");
                break;
            }
        }

        // Rule 3: rapid-fire transactions (3 or more within the last minute on same account,
        // counting the current transaction itself as one of the three)
        long priorInLastMinute = history.stream()
                .filter(p -> Duration.between(p.getTimestamp(), txn.getTimestamp()).abs().toMinutes() < 1)
                .count();
        if (priorInLastMinute + 1 >= 3) {
            score += 55;
            reasons.append("Rapid-fire transactions (>=3 within 1 min); ");
        }

        history.add(txn);

        txn.setRiskScore(Math.min(score, 100));
        txn.setFlagged(score >= FLAG_THRESHOLD);
        txn.setFlagReason(reasons.length() == 0 ? "No issues detected" : reasons.toString().trim());

        // Persist the result (via DAO -> keeps DB access isolated from this business logic)
        transactionDAO.updateRiskResult(txn);

        return txn;
    }
}
