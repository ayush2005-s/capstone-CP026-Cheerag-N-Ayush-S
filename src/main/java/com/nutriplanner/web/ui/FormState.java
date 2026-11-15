package com.nutriplanner.web.ui;

import com.nutriplanner.web.http.Request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Values and errors of a form, so a rejected form is shown again with what the user typed and why it failed. */
public final class FormState {

    private final Map<String, List<String>> values = new LinkedHashMap<>();
    private final Map<String, String> errors = new LinkedHashMap<>();
    private String message;

    public static FormState empty() {
        return new FormState();
    }

    /** The submitted fields of a request (passwords and the CSRF token are never echoed back). */
    public static FormState from(Request request) {
        FormState s = new FormState();
        request.formFields().forEach((k, v) -> {
            if (!k.toLowerCase().contains("password") && !k.startsWith("_")) s.values.put(k, new ArrayList<>(v));
        });
        return s;
    }

    public FormState set(String name, Object value) {
        if (value == null) {
            values.remove(name);
        } else if (value instanceof Collection<?>) {
            List<String> list = new ArrayList<>();
            for (Object o : (Collection<?>) value) list.add(o instanceof Enum<?> ? ((Enum<?>) o).name() : String.valueOf(o));
            values.put(name, list);
        } else if (value instanceof Enum<?>) {
            values.put(name, List.of(((Enum<?>) value).name()));
        } else if (value instanceof Double) {
            values.put(name, List.of(Fmt.trim((Double) value)));
        } else {
            values.put(name, List.of(String.valueOf(value)));
        }
        return this;
    }

    public FormState setIfAbsent(String name, Object value) {
        if (!values.containsKey(name)) set(name, value);
        return this;
    }

    public String value(String name) {
        List<String> v = values.get(name);
        return v == null || v.isEmpty() ? "" : v.get(0);
    }

    public List<String> values(String name) {
        return values.getOrDefault(name, List.of());
    }

    public boolean has(String name) {
        return values.containsKey(name);
    }

    public FormState errors(Map<String, String> fieldErrors) {
        errors.putAll(fieldErrors);
        return this;
    }

    public FormState error(String field, String message) {
        errors.put(field, message);
        return this;
    }

    public String error(String name) {
        return errors.get(name);
    }

    public boolean hasErrors() {
        return !errors.isEmpty() || message != null;
    }

    public FormState message(String message) {
        this.message = message;
        return this;
    }

    public String message() {
        return message;
    }
}
