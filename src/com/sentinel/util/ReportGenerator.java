package com.sentinel.util;

import com.sentinel.model.Transaction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Writes a plain-text "suspicious activity report" file listing every
 * transaction that got flagged, sorted by risk score. This is the
 * file-based reporting mentioned in the project description -- something a
 * manager could open and review without touching the database directly.
 */
public class ReportGenerator {

    public static void writeFlaggedReport(String filePath, List<Transaction> flagged) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath, false))) {
            out.println("SENTINEL FRAUD DETECTION - SUSPICIOUS ACTIVITY REPORT");
            out.println("Generated: " + LocalDateTime.now());
            out.println("Total flagged transactions: " + flagged.size());
            out.println("--------------------------------------------------------");
            for (Transaction t : flagged) {
                out.printf("Txn #%d | Account: %s | Amount: %.2f | Location: %s%n",
                        t.getId(), t.getAccountId(), t.getAmount(), t.getLocation());
                out.printf("  Risk Score: %.1f/100 | Reason: %s%n", t.getRiskScore(), t.getFlagReason());
                out.println();
            }
        } catch (IOException e) {
            System.err.println("Failed to write report: " + e.getMessage());
        }
    }
}
