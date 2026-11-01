package com.sentinel.dao;

import com.sentinel.db.ConnectionPool;
import com.sentinel.model.Role;
import com.sentinel.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Production DAO: reads users + their Role from the MySQL "users" table.
 * (For running a quick offline demo without a real database, see
 * com.sentinel.dao.InMemoryUserDAO instead.)
 */
public class UserDAOImpl implements UserDAO {

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT id, username, role FROM users WHERE username = ?";
        try {
            Connection conn = ConnectionPool.getInstance().borrowConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new User(rs.getInt("id"), rs.getString("username"),
                                Role.valueOf(rs.getString("role")));
                    }
                }
            } finally {
                ConnectionPool.getInstance().releaseConnection(conn);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user: " + username, e);
        }
        return null;
    }
}
