package com.sentinel.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes a timestamped line to a log file for every important action
 * (who did what, when). If the system misbehaves later, this file is the
 * "security camera footage" you review to see exactly what happened.
 * Thread-safe (synchronized) because many worker threads log at once.
 */
public class AuditLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final String filePath;

    public AuditLogger(String filePath) {
        this.filePath = filePath;
    }

    public synchronized void log(String message) {
        String line = "[" + LocalDateTime.now().format(FMT) + "] " + message;
        System.out.println(line); // also print to console for the live demo
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath, true))) {
            out.println(line);
        } catch (IOException e) {
            // Deliberately don't crash the whole app just because logging failed;
            // print to stderr so the failure is at least visible.
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }
}
