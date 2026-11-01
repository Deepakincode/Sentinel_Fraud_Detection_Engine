package com.sentinel.model;

/**
 * Roles used for Role-Based Access Control (RBAC).
 * EMPLOYEE  -> can only VIEW transactions
 * MANAGER   -> can VIEW and APPROVE/BLOCK flagged transactions
 * ADMIN     -> full access, including viewing audit logs and managing users
 */
public enum Role {
    EMPLOYEE,
    MANAGER,
    ADMIN
}
