package com.sentinel.dao;

import com.sentinel.model.Role;
import com.sentinel.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight in-memory stand-in for UserDAOImpl.
 * Used ONLY so the project can be run/demoed instantly without setting up
 * a real MySQL server (e.g. during a live viva). The real implementation
 * (UserDAOImpl) uses JDBC + MySQL exactly the same way TransactionDAOImpl does.
 */
public class InMemoryUserDAO implements UserDAO {

    private final Map<String, User> users = new HashMap<>();

    public InMemoryUserDAO() {
        users.put("alice", new User(1, "alice", Role.EMPLOYEE));
        users.put("bob", new User(2, "bob", Role.MANAGER));
        users.put("carol", new User(3, "carol", Role.ADMIN));
    }

    @Override
    public User findByUsername(String username) {
        return users.get(username);
    }
}
