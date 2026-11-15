package com.nutriplanner.domain;

import java.time.LocalDate;
import java.util.Locale;

/** Builds sortable index keys. Ids are zero-padded so that string order equals numeric order. */
public final class Keys {
    private Keys() {
    }

    public static String id(long id) {
        return String.format(java.util.Locale.ROOT, "%019d", id);
    }

    public static String userDate(long userId, LocalDate date) {
        return id(userId) + "|" + date;
    }

    public static String userPrefix(long userId) {
        return id(userId) + "|";
    }

    public static String lower(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }
}
