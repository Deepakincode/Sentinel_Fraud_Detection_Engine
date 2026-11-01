package com.sentinel.model;

public class User {
    private final int id;
    private final String username;
    private final Role role;

    public User(int id, String username, Role role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }

    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}
