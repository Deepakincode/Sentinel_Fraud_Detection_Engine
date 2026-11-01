package com.sentinel.dao;

import com.sentinel.db.ConnectionPool;
import com.sentinel.model.Transaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of TransactionDAO using plain JDBC.
 * Every method: borrow a connection from the pool -> run the query -> ALWAYS
 * release the connection back to the pool in a finally block (so the pool
 * never "leaks" connections even if a query throws an exception).
 */
public class TransactionDAOImpl implements TransactionDAO {

    @Override
    public void save(Transaction t) throws Exception {
        String sql = "INSERT INTO transactions (account_id, amount, location, txn_time, risk_score, flagged, flag_reason) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = ConnectionPool.getInstance().borrowConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getAccountId());
            ps.setDouble(2, t.getAmount());
            ps.setString(3, t.getLocation());
            ps.setTimestamp(4, Timestamp.valueOf(t.getTimestamp()));
            ps.setDouble(5, t.getRiskScore());
            ps.setBoolean(6, t.isFlagged());
            ps.setString(7, t.getFlagReason());
            ps.executeUpdate();
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public void updateRiskResult(Transaction t) throws Exception {
        String sql = "UPDATE transactions SET risk_score = ?, flagged = ?, flag_reason = ? WHERE id = ?";
        Connection conn = ConnectionPool.getInstance().borrowConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, t.getRiskScore());
            ps.setBoolean(2, t.isFlagged());
            ps.setString(3, t.getFlagReason());
            ps.setInt(4, t.getId());
            ps.executeUpdate();
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
    }

    @Override
    public List<Transaction> findByAccountId(String accountId) throws Exception {
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY txn_time DESC";
        Connection conn = ConnectionPool.getInstance().borrowConnection();
        List<Transaction> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return results;
    }

    @Override
    public List<Transaction> findFlagged() throws Exception {
        String sql = "SELECT * FROM transactions WHERE flagged = TRUE ORDER BY risk_score DESC";
        Connection conn = ConnectionPool.getInstance().borrowConnection();
        List<Transaction> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } finally {
            ConnectionPool.getInstance().releaseConnection(conn);
        }
        return results;
    }

    /** Helper: converts one row of the ResultSet into a Transaction object. */
    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction(
                rs.getInt("id"),
                rs.getString("account_id"),
                rs.getDouble("amount"),
                rs.getString("location"),
                rs.getTimestamp("txn_time").toLocalDateTime()
        );
        t.setRiskScore(rs.getDouble("risk_score"));
        t.setFlagged(rs.getBoolean("flagged"));
        t.setFlagReason(rs.getString("flag_reason"));
        return t;
    }
}
