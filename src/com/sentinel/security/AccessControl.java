package com.sentinel.security;

import com.sentinel.exception.AccessDeniedException;
import com.sentinel.model.Role;
import com.sentinel.model.User;

/**
 * Role-Based Access Control (RBAC).
 * Each action in the system requires a MINIMUM role. This class checks the
 * calling user's role against that minimum before allowing the action.
 *
 * Hierarchy: EMPLOYEE < MANAGER < ADMIN
 *   EMPLOYEE -> can only VIEW transactions
 *   MANAGER  -> can VIEW + APPROVE/BLOCK flagged transactions
 *   ADMIN    -> can do everything, including VIEW_AUDIT_LOG
 */
public class AccessControl {

    public enum Action {
        VIEW_TRANSACTIONS(Role.EMPLOYEE),
        APPROVE_OR_BLOCK(Role.MANAGER),
        VIEW_AUDIT_LOG(Role.ADMIN);

        final Role minimumRole;
        Action(Role minimumRole) { this.minimumRole = minimumRole; }
    }

    public static void check(User user, Action action) {
        if (user == null) {
            throw new AccessDeniedException("No user logged in.");
        }
        if (user.getRole().ordinal() < action.minimumRole.ordinal()) {
            throw new AccessDeniedException(
                    user.getUsername() + " (" + user.getRole() + ") is not allowed to perform "
                            + action + ". Requires at least " + action.minimumRole + ".");
        }
    }
}
