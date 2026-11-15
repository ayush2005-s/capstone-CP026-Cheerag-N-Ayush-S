package com.nutriplanner.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Raised when user input is rejected. Carries one message per form field (keyed by the field name used in the
 * form) and/or a general message, so the page can show each error next to the field it belongs to.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }

    public ValidationException(Map<String, String> fieldErrors) {
        super(fieldErrors.isEmpty() ? "Check the highlighted fields." : fieldErrors.values().iterator().next());
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public static ValidationException field(String field, String message) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(field, message);
        return new ValidationException(m);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }

    /** Collects errors and throws them together. */
    public static final class Errors {
        private final Map<String, String> errors = new LinkedHashMap<>();

        public Errors add(String field, String message) {
            errors.putIfAbsent(field, message);
            return this;
        }

        public Errors check(boolean ok, String field, String message) {
            if (!ok) add(field, message);
            return this;
        }

        public boolean has(String field) {
            return errors.containsKey(field);
        }

        public boolean isEmpty() {
            return errors.isEmpty();
        }

        public void throwIfAny() {
            if (!errors.isEmpty()) throw new ValidationException(errors);
        }
    }
}
