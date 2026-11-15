package com.nutriplanner.security;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Password rules in the spirit of NIST SP 800-63B: a sensible minimum length, a mix of letters and digits, and a
 * block-list of passwords that attackers try first.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;
    public static final String HINT = "At least 8 characters, with letters and at least one number.";

    private static final Set<String> COMMON = Set.of(
            "password", "password1", "password12", "password123", "passw0rd", "12345678", "123456789", "1234567890",
            "qwerty123", "qwertyuiop", "abc12345", "abcd1234", "iloveyou1", "welcome1", "welcome123", "admin123",
            "letmein1", "monkey123", "dragon123", "football1", "baseball1", "sunshine1", "princess1", "trustno1",
            "1q2w3e4r", "1qaz2wsx", "zaq12wsx", "qwer1234", "asdf1234", "india123", "nutriplanner1", "changeme1");

    private PasswordPolicy() {
    }

    /** Returns the reason a password is not acceptable, or empty if it is. */
    public static Optional<String> check(String password, String username) {
        if (password == null || password.length() < MIN_LENGTH) {
            return Optional.of("Use at least " + MIN_LENGTH + " characters.");
        }
        if (password.length() > MAX_LENGTH) return Optional.of("Use at most " + MAX_LENGTH + " characters.");
        boolean letter = false, digit = false;
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isLetter(ch)) letter = true;
            if (Character.isDigit(ch)) digit = true;
        }
        if (!letter || !digit) return Optional.of("Include both letters and at least one number.");
        String lower = password.toLowerCase(Locale.ROOT);
        if (COMMON.contains(lower)) return Optional.of("This password is too common. Choose something less predictable.");
        if (username != null && username.length() >= 3 && lower.contains(username.toLowerCase(Locale.ROOT))) {
            return Optional.of("Your password must not contain your username.");
        }
        return Optional.empty();
    }
}
