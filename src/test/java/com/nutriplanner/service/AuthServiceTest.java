package com.nutriplanner.service;

import com.nutriplanner.domain.AccountStatus;
import com.nutriplanner.domain.User;
import com.nutriplanner.security.PasswordHasher;
import com.nutriplanner.testkit.Test;
import com.nutriplanner.testkit.TestServices;

import static com.nutriplanner.testkit.Assert.assertEquals;
import static com.nutriplanner.testkit.Assert.assertFalse;
import static com.nutriplanner.testkit.Assert.assertThrows;
import static com.nutriplanner.testkit.Assert.assertTrue;

public class AuthServiceTest extends TestServices {

    private AuthService.Registration form(String username, String email, String password) {
        return new AuthService.Registration("Meera Iyer", username, email, password, password, true);
    }

    @Test
    void registersAndLogsIn() {
        User u = services.auth.register(form("meera", "Meera@Example.com", "Turmeric42"), "127.0.0.1");
        assertEquals("meera@example.com", u.getEmail(), "emails are stored in lower case");
        assertTrue(u.getPasswordHash().startsWith("pbkdf2-sha256$"), "password is hashed");
        assertTrue(services.profiles.profile(u.getId()) != null, "a profile is created");
        assertTrue(services.auth.login("meera", "Turmeric42", "ip").success(), "login with username");
        assertTrue(services.auth.login("MEERA@example.com", "Turmeric42", "ip").success(), "login with email");
        assertFalse(services.auth.login("meera", "turmeric42", "ip").success(), "passwords are case sensitive");
    }

    @Test
    void rejectsInvalidRegistrations() {
        services.auth.register(form("meera", "meera@example.com", "Turmeric42"), "ip");
        ValidationException dup = assertThrows(ValidationException.class,
                () -> services.auth.register(form("Meera", "other@example.com", "Turmeric42"), "ip"));
        assertTrue(dup.fieldErrors().containsKey("username"), "duplicate username");
        ValidationException email = assertThrows(ValidationException.class,
                () -> services.auth.register(form("meera2", "MEERA@example.com", "Turmeric42"), "ip"));
        assertTrue(email.fieldErrors().containsKey("email"), "duplicate email");
        ValidationException weak = assertThrows(ValidationException.class,
                () -> services.auth.register(form("sunil", "sunil@example.com", "password1"), "ip"));
        assertTrue(weak.fieldErrors().containsKey("password"), "common password");
        ValidationException noDigit = assertThrows(ValidationException.class,
                () -> services.auth.register(form("sunil", "sunil@example.com", "onlyletters"), "ip"));
        assertTrue(noDigit.fieldErrors().containsKey("password"), "needs a number");
        ValidationException mismatch = assertThrows(ValidationException.class, () -> services.auth.register(
                new AuthService.Registration("Sunil", "sunil", "sunil@example.com", "Turmeric42", "Turmeric43", true), "ip"));
        assertTrue(mismatch.fieldErrors().containsKey("confirmPassword"), "confirmation must match");
        ValidationException reserved = assertThrows(ValidationException.class,
                () -> services.auth.register(form("Admin", "x@example.com", "Turmeric42"), "ip"));
        assertTrue(reserved.fieldErrors().containsKey("username"), "reserved username");
        ValidationException terms = assertThrows(ValidationException.class, () -> services.auth.register(
                new AuthService.Registration("Sunil", "sunil", "sunil@example.com", "Turmeric42", "Turmeric42", false), "ip"));
        assertTrue(terms.fieldErrors().containsKey("acceptedTerms"), "must acknowledge the disclaimer");
    }

    @Test
    void locksAccountAfterRepeatedFailures() {
        services.auth.register(form("meera", "meera@example.com", "Turmeric42"), "ip");
        for (int i = 1; i < AuthService.MAX_FAILED_LOGINS; i++) {
            assertEquals(AuthService.Outcome.INVALID, services.auth.login("meera", "wrong" + i, "ip").outcome());
        }
        assertEquals(AuthService.Outcome.LOCKED, services.auth.login("meera", "wrong5", "ip").outcome());
        assertEquals(AuthService.Outcome.LOCKED, services.auth.login("meera", "Turmeric42", "ip").outcome(),
                "a locked account rejects even the right password");
        clock.advanceMinutes(16);
        assertTrue(services.auth.login("meera", "Turmeric42", "ip").success(), "lock expires after 15 minutes");
    }

    @Test
    void disabledAccountsCannotLogIn() {
        User u = services.auth.register(form("meera", "meera@example.com", "Turmeric42"), "ip");
        services.admin.setStatus(admin(), u.getId(), AccountStatus.DISABLED, "ip");
        assertEquals(AuthService.Outcome.DISABLED, services.auth.login("meera", "Turmeric42", "ip").outcome());
        assertEquals(AuthService.Outcome.INVALID, services.auth.login("meera", "nope", "ip").outcome(),
                "a wrong password doesn't reveal that the account is disabled");
    }

    @Test
    void changesPassword() {
        User u = services.auth.register(form("meera", "meera@example.com", "Turmeric42"), "ip");
        ValidationException wrong = assertThrows(ValidationException.class,
                () -> services.auth.changePassword(u.getId(), "bad", "Saffron2026", "Saffron2026", "ip"));
        assertTrue(wrong.fieldErrors().containsKey("currentPassword"), "current password checked");
        services.auth.changePassword(u.getId(), "Turmeric42", "Saffron2026", "Saffron2026", "ip");
        assertFalse(services.auth.login("meera", "Turmeric42", "ip").success(), "old password no longer works");
        assertTrue(services.auth.login("meera", "Saffron2026", "ip").success(), "new password works");
    }

    @Test
    void seededAdminMustChangePassword() {
        User admin = admin();
        assertTrue(admin.isAdmin(), "admin role");
        assertTrue(admin.isMustChangePassword(), "first login forces a password change");
        assertTrue(services.auth.login("admin", "Planner#Start1", "ip").success(), "initial password works");
    }

    @Test
    void hasherUpgradesWeakHashes() {
        PasswordHasher weak = new PasswordHasher(1000);
        PasswordHasher strong = new PasswordHasher(2000);
        String hash = weak.hash("Turmeric42");
        assertTrue(strong.verify("Turmeric42", hash), "old hashes still verify");
        assertTrue(strong.needsRehash(hash), "weaker hashes are flagged for upgrade");
        assertFalse(weak.verify("Turmeric43", hash), "wrong password");
        assertFalse(weak.verify("Turmeric42", "garbage"), "malformed hash");
    }
}
