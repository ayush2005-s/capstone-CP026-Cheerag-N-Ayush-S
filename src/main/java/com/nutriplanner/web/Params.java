package com.nutriplanner.web;

import com.nutriplanner.web.http.Request;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads typed values from a request and collects "this isn't a number" style errors per field. */
public final class Params {

    private final Request request;
    private final Map<String, String> errors = new LinkedHashMap<>();

    public Params(Request request) {
        this.request = request;
    }

    public String text(String name) {
        String v = request.param(name);
        return v == null ? "" : v.strip();
    }

    /** A decimal number, or null when empty. A comma is accepted as the decimal separator. */
    public Double decimal(String name) {
        String v = text(name).replace(',', '.');
        if (v.isEmpty()) return null;
        try {
            double d = Double.parseDouble(v);
            if (Double.isNaN(d) || Double.isInfinite(d)) throw new NumberFormatException();
            return d;
        } catch (NumberFormatException e) {
            errors.putIfAbsent(name, "Enter a number.");
            return null;
        }
    }

    public Integer integer(String name) {
        String v = text(name).replace(",", "");
        if (v.isEmpty()) return null;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            errors.putIfAbsent(name, "Enter a whole number.");
            return null;
        }
    }

    public LocalDate date(String name) {
        String v = text(name);
        if (v.isEmpty()) return null;
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            errors.putIfAbsent(name, "Enter a date like 2026-03-15.");
            return null;
        }
    }

    public LocalDate date(String name, LocalDate fallback) {
        LocalDate d = date(name);
        errors.remove(name);
        return d == null ? fallback : d;
    }

    public <E extends Enum<E>> E choice(String name, Class<E> type) {
        String v = text(name);
        if (v.isEmpty()) return null;
        try {
            return Enum.valueOf(type, v);
        } catch (IllegalArgumentException e) {
            errors.putIfAbsent(name, "Choose one of the options.");
            return null;
        }
    }

    public <E extends Enum<E>> EnumSet<E> choices(String name, Class<E> type) {
        EnumSet<E> set = EnumSet.noneOf(type);
        for (String v : request.params(name)) {
            try {
                set.add(Enum.valueOf(type, v));
            } catch (IllegalArgumentException ignored) {
                // unknown option from a tampered form: ignore it
            }
        }
        return set;
    }

    public int page() {
        try {
            return Math.max(1, Integer.parseInt(text("page")));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public boolean flag(String name) {
        String v = text(name);
        return v.equals("on") || v.equals("true") || v.equals("1") || v.equals("yes");
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public Map<String, String> errors() {
        return errors;
    }
}
