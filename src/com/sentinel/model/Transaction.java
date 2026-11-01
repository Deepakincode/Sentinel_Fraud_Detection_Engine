package com.sentinel.model;

import java.time.LocalDateTime;

/**
 * Represents a single financial transaction coming into the system.
 * riskScore and flagged get filled in AFTER the RiskScoringService evaluates it.
 */
public class Transaction {
    private final int id;
    private final String accountId;
    private final double amount;
    private final String location;
    private final LocalDateTime timestamp;

    private double riskScore;      // 0.0 (safe) to 100.0 (very suspicious)
    private boolean flagged;
    private String flagReason;

    public Transaction(int id, String accountId, double amount, String location, LocalDateTime timestamp) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.location = location;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getAccountId() { return accountId; }
    public double getAmount() { return amount; }
    public String getLocation() { return location; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }

    @Override
    public String toString() {
        return String.format("Txn#%d [acct=%s, amount=%.2f, loc=%s, time=%s, risk=%.1f, flagged=%s]",
                id, accountId, amount, location, timestamp, riskScore, flagged);
    }
}
