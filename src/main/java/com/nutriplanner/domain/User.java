package com.nutriplanner.domain;

import com.nutriplanner.storage.Doc;
import com.nutriplanner.storage.EntityMapper;

import java.time.Instant;

/** A registered account. Passwords are stored only as salted PBKDF2 hashes. */
public final class User {

    public static final EntityMapper<User> MAPPER = EntityMapper.of(User::getId, User::setId, User::toDoc, User::fromDoc);

    private long id;
    private String username;
    private String email;
    private String fullName;
    private String passwordHash;
    private Role role = Role.USER;
    private AccountStatus status = AccountStatus.ACTIVE;
    private int failedLogins;
    private Instant lockedUntil;
    private boolean mustChangePassword;
    private Instant createdAt;
    private Instant lastLoginAt;
    private Instant passwordChangedAt;

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public String firstName() {
        if (fullName == null || fullName.isBlank()) return username;
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    public String initials() {
        String source = fullName == null || fullName.isBlank() ? username : fullName.trim();
        String[] parts = source.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && sb.length() < 2) sb.appendCodePoint(Character.toUpperCase(part.codePointAt(0)));
        }
        return sb.toString();
    }

    public Doc toDoc() {
        return new Doc()
                .put("username", username)
                .put("email", email)
                .put("fullName", fullName)
                .put("passwordHash", passwordHash)
                .put("role", role)
                .put("status", status)
                .put("failedLogins", failedLogins)
                .put("lockedUntil", lockedUntil)
                .put("mustChangePassword", mustChangePassword)
                .put("createdAt", createdAt)
                .put("lastLoginAt", lastLoginAt)
                .put("passwordChangedAt", passwordChangedAt);
    }

    public static User fromDoc(long id, Doc d) {
        User u = new User();
        u.id = id;
        u.username = d.getString("username");
        u.email = d.getString("email");
        u.fullName = d.getString("fullName", "");
        u.passwordHash = d.getString("passwordHash");
        u.role = d.getEnum("role", Role.class, Role.USER);
        u.status = d.getEnum("status", AccountStatus.class, AccountStatus.ACTIVE);
        u.failedLogins = d.getInt("failedLogins", 0);
        u.lockedUntil = d.getInstant("lockedUntil");
        u.mustChangePassword = d.getBool("mustChangePassword", false);
        u.createdAt = d.getInstant("createdAt");
        u.lastLoginAt = d.getInstant("lastLoginAt");
        u.passwordChangedAt = d.getInstant("passwordChangedAt");
        return u;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public int getFailedLogins() { return failedLogins; }
    public void setFailedLogins(int failedLogins) { this.failedLogins = failedLogins; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(Instant passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
}
