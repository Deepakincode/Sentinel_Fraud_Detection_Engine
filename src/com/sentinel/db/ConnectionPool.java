package com.sentinel.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A small hand-rolled JDBC connection pool.
 *
 * WHY: opening a brand-new DB connection for every query is slow (TCP handshake +
 * auth every time). Instead we open a fixed number of connections ONCE at startup,
 * hand them out when a thread needs one (borrowConnection), and put them back when
 * done (releaseConnection) so they can be reused.
 *
 * This is a simplified version of what production pools like HikariCP do internally.
 */
public class ConnectionPool {

    private static final String URL = "jdbc:mysql://localhost:3306/sentinel_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "your_password_here";
    private static final int POOL_SIZE = 8;

    private static ConnectionPool instance;
    private final BlockingQueue<Connection> pool = new ArrayBlockingQueue<>(POOL_SIZE);

    private ConnectionPool() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found on classpath", e);
        }
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.add(DriverManager.getConnection(URL, USER, PASSWORD));
        }
    }

    public static synchronized ConnectionPool getInstance() throws SQLException {
        if (instance == null) {
            instance = new ConnectionPool();
        }
        return instance;
    }

    /** Borrow a connection from the pool (blocks if all are currently in use). */
    public Connection borrowConnection() throws SQLException {
        try {
            return pool.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a free connection", e);
        }
    }

    /** Return a connection back to the pool so another thread can reuse it. */
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            pool.offer(connection);
        }
    }

    public void shutdown() throws SQLException {
        for (Connection c : pool) {
            c.close();
        }
    }
}
