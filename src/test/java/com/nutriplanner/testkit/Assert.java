package com.nutriplanner.testkit;

import java.util.Collection;
import java.util.Objects;

/** Minimal assertion helpers. */
public final class Assert {

    private Assert() {
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError(message);
    }

    public static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertNear(double expected, double actual, double tolerance, String message) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(message + ": expected " + expected + " ± " + tolerance + " but was " + actual);
        }
    }

    public static void assertContains(String haystack, String needle) {
        if (haystack == null || !haystack.contains(needle)) {
            String shown = haystack == null ? "null" : haystack.length() > 400 ? haystack.substring(0, 400) + "…" : haystack;
            throw new AssertionError("Expected text to contain <" + needle + "> in: " + shown);
        }
    }

    public static void assertNotContains(String haystack, String needle) {
        if (haystack != null && haystack.contains(needle)) {
            throw new AssertionError("Expected text not to contain <" + needle + ">");
        }
    }

    public static void assertNotEmpty(Collection<?> c, String message) {
        if (c == null || c.isEmpty()) throw new AssertionError(message);
    }

    public static <T extends Throwable> T assertThrows(Class<T> type, ThrowingRunnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return type.cast(t);
            throw new AssertionError("Expected " + type.getSimpleName() + " but got " + t, t);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " to be thrown");
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
