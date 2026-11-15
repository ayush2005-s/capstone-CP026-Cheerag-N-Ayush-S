package com.nutriplanner.service;

import com.nutriplanner.domain.AccountStatus;
import com.nutriplanner.domain.AuditType;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.Role;
import com.nutriplanner.domain.User;
import com.nutriplanner.repository.Repositories;
import com.nutriplanner.security.PasswordHasher;
import com.nutriplanner.security.PasswordPolicy;
import com.nutriplanner.storage.DuplicateKeyException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Registration, login with brute-force lockout, and password changes. */
public final class AuthService {

    public static final int MAX_FAILED_LOGINS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_.]{2,23}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final Set<String> RESERVED = Set.of("admin", "administrator", "root", "system", "support",
            "nutriplanner", "moderator", "staff");

    private final Repositories repos;
    private final PasswordHasher hasher;
    private final AuditService audit;
    private final Clock clock;
    private final String dummyHash;

    public AuthService(Repositories repos, PasswordHasher hasher, AuditService audit, Clock clock) {
        this.repos = repos;
        this.hasher = hasher;
        this.audit = audit;
        this.clock = clock;
        this.dummyHash = hasher.hash("timing-equaliser-" + System.nanoTime());
    }

    // ---------------------------------------------------------------- register

    public record Registration(String fullName, String username, String email, String password,
                               String confirmPassword, boolean acceptedTerms) {
    }

    public User register(Registration form, String ip) {
        String fullName = clean(form.fullName());
        String username = clean(form.username());
        String email = clean(form.email()).toLowerCase(Locale.ROOT);

        ValidationException.Errors errors = new ValidationException.Errors();
        validateFullName(fullName, errors);
        if (!USERNAME.matcher(username).matches()) {
            errors.add("username", "Use 3–24 letters, numbers, dots or underscores, starting with a letter.");
        } else if (RESERVED.contains(username.toLowerCase(Locale.ROOT))) {
            errors.add("username", "This username is reserved. Choose another one.");
        } else if (repos.users.findByUsername(username).isPresent()) {
            errors.add("username", "This username is taken. Choose another one.");
        }
        validateEmail(email, errors);
        if (!errors.has("email") && repos.users.findByEmail(email).isPresent()) {
            errors.add("email", "An account with this email already exists. Log in instead.");
        }
        PasswordPolicy.check(form.password(), username).ifPresent(msg -> errors.add("password", msg));
        if (form.password() != null && !form.password().equals(form.confirmPassword())) {
            errors.add("confirmPassword", "The passwords don't match.");
        }
        errors.check(form.acceptedTerms(), "acceptedTerms", "Confirm that you understand this before continuing.");
        errors.throwIfAny();

        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(hasher.hash(form.password()));
        user.setRole(Role.USER);
        user.setStatus(AccountStatus.ACTIVE);
        Instant now = clock.instant();
        user.setCreatedAt(now);
        user.setPasswordChangedAt(now);
        try {
            repos.db.write(() -> {
                repos.users.insert(user);
                repos.profiles.save(new Profile(user.getId()));
            });
        } catch (DuplicateKeyException e) {
            throw ValidationException.field(e.index().endsWith("email") ? "email" : "username",
                    "This value was just taken by another account. Try again.");
        }
        audit.record(AuditType.REGISTERED, user, "New member account", ip);
        return user;
    }

    /** Creates an account without the public sign-up checks (used for seeding and by administrators). */
    public User createAccount(String fullName, String username, String email, String password, Role role, boolean mustChange) {
        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setEmail(email.toLowerCase(Locale.ROOT));
        user.setPasswordHash(hasher.hash(password));
        user.setRole(role);
        user.setMustChangePassword(mustChange);
        Instant now = clock.instant();
        user.setCreatedAt(now);
        user.setPasswordChangedAt(now);
        repos.db.write(() -> {
            repos.users.insert(user);
            repos.profiles.save(new Profile(user.getId()));
        });
        return user;
    }

    // ------------------------------------------------------------------- login

    public enum Outcome { SUCCESS, INVALID, LOCKED, DISABLED }

    public record LoginResult(Outcome outcome, User user, Instant lockedUntil) {
        public boolean success() {
            return outcome == Outcome.SUCCESS;
        }

        public String message(Clock clock) {
            switch (outcome) {
                case LOCKED: {
                    long minutes = Math.max(1, Duration.between(clock.instant(), lockedUntil).toMinutes() + 1);
                    return "Too many failed attempts. Try again in " + minutes + (minutes == 1 ? " minute." : " minutes.");
                }
                case DISABLED:
                    return "This account has been disabled. Contact an administrator.";
                case INVALID:
                    return "Incorrect username or password.";
                default:
                    return "";
            }
        }
    }

    public LoginResult login(String login, String password, String ip) {
        Optional<User> found = repos.users.findByLogin(clean(login));
        if (found.isEmpty()) {
            hasher.verify(password == null ? "" : password, dummyHash);
            audit.record(AuditType.LOGIN_FAILURE, 0, clean(login), "Unknown account", ip);
            return new LoginResult(Outcome.INVALID, null, null);
        }
        User user = found.get();
        Instant now = clock.instant();
        if (user.isLocked(now)) {
            return new LoginResult(Outcome.LOCKED, user, user.getLockedUntil());
        }
        if (!hasher.verify(password == null ? "" : password, user.getPasswordHash())) {
            int failures = user.getFailedLogins() + 1;
            if (failures >= MAX_FAILED_LOGINS) {
                user.setFailedLogins(0);
                user.setLockedUntil(now.plus(LOCK_DURATION));
                repos.users.update(user);
                audit.record(AuditType.ACCOUNT_LOCKED, user, MAX_FAILED_LOGINS + " failed attempts", ip);
                return new LoginResult(Outcome.LOCKED, user, user.getLockedUntil());
            }
            user.setFailedLogins(failures);
            repos.users.update(user);
            audit.record(AuditType.LOGIN_FAILURE, user, "Wrong password (" + failures + " of " + MAX_FAILED_LOGINS + ")", ip);
            return new LoginResult(Outcome.INVALID, null, null);
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            audit.record(AuditType.LOGIN_FAILURE, user, "Account disabled", ip);
            return new LoginResult(Outcome.DISABLED, null, null);
        }
        user.setFailedLogins(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        if (hasher.needsRehash(user.getPasswordHash())) user.setPasswordHash(hasher.hash(password));
        repos.users.update(user);
        audit.record(AuditType.LOGIN_SUCCESS, user, "", ip);
        return new LoginResult(Outcome.SUCCESS, user, null);
    }

    public void recordLogout(User user, String ip) {
        if (user != null) audit.record(AuditType.LOGOUT, user, "", ip);
    }

    // ---------------------------------------------------------------- password

    public void changePassword(long userId, String current, String next, String confirm, String ip) {
        User user = repos.users.findById(userId).orElseThrow(() -> new NotFoundException("Account not found"));
        ValidationException.Errors errors = new ValidationException.Errors();
        if (!hasher.verify(current == null ? "" : current, user.getPasswordHash())) {
            errors.add("currentPassword", "Your current password is incorrect.");
        }
        PasswordPolicy.check(next, user.getUsername()).ifPresent(msg -> errors.add("newPassword", msg));
        if (!errors.has("newPassword") && next.equals(current)) {
            errors.add("newPassword", "Choose a password you haven't used for this account.");
        }
        if (next != null && !next.equals(confirm)) errors.add("confirmPassword", "The passwords don't match.");
        errors.throwIfAny();
        user.setPasswordHash(hasher.hash(next));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(clock.instant());
        repos.users.update(user);
        audit.record(AuditType.PASSWORD_CHANGED, user, "", ip);
    }

    public boolean verifyPassword(User user, String password) {
        return hasher.verify(password == null ? "" : password, user.getPasswordHash());
    }

    public String hashPassword(String password) {
        return hasher.hash(password);
    }

    // -------------------------------------------------------------- validation

    static void validateFullName(String fullName, ValidationException.Errors errors) {
        if (fullName.length() < 2 || fullName.length() > 60) {
            errors.add("fullName", "Enter your name (2–60 characters).");
        } else if (fullName.chars().anyMatch(Character::isISOControl)) {
            errors.add("fullName", "Your name contains characters that aren't allowed.");
        }
    }

    static void validateEmail(String email, ValidationException.Errors errors) {
        if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
            errors.add("email", "Enter a valid email address, like name@example.com.");
        }
    }

    static String clean(String s) {
        return s == null ? "" : s.strip();
    }
}
