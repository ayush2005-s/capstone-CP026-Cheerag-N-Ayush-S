package com.nutriplanner.domain;

public enum AuditType implements Labeled {
    REGISTERED("Account created"),
    LOGIN_SUCCESS("Logged in"),
    LOGIN_FAILURE("Failed login"),
    ACCOUNT_LOCKED("Account locked"),
    LOGOUT("Logged out"),
    PASSWORD_CHANGED("Password changed"),
    ACCOUNT_DELETED("Account deleted"),
    ADMIN_USER_UPDATED("User updated by admin"),
    ADMIN_PASSWORD_RESET("Password reset by admin"),
    ADMIN_USER_DELETED("User deleted by admin"),
    ADMIN_FOOD_CREATED("Food created"),
    ADMIN_FOOD_UPDATED("Food updated"),
    ADMIN_FOOD_DELETED("Food deleted");

    private final String label;

    AuditType(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    public boolean isSecurityEvent() {
        return this == LOGIN_FAILURE || this == ACCOUNT_LOCKED;
    }
}
