package com.nutriplanner.web.ui;

import com.nutriplanner.domain.FoodLogEntry;
import com.nutriplanner.domain.PortionUnit;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Number and date formatting used across pages. */
public final class Fmt {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MEDIUM = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private Fmt() {
    }

    /** Whole number with thousands separators. */
    public static String kcal(double v) {
        return String.format(Locale.ENGLISH, "%,d", Math.round(v));
    }

    public static String grams(double v) {
        return amount(v) + " g";
    }

    /** One decimal below 10, whole numbers above. */
    public static String amount(double v) {
        if (Math.abs(v) < 10 && Math.abs(v - Math.rint(v)) > 0.05) return String.format(Locale.ENGLISH, "%.1f", v);
        return String.format(Locale.ENGLISH, "%,d", Math.round(v));
    }

    public static String one(double v) {
        return String.format(Locale.ENGLISH, "%.1f", v);
    }

    public static String signed(double v) {
        return (v > 0.05 ? "+" : v < -0.05 ? "−" : "") + String.format(Locale.ENGLISH, "%.1f", Math.abs(v));
    }

    public static String signedKcal(double v) {
        return (v > 0 ? "+" : v < 0 ? "−" : "") + kcal(Math.abs(v));
    }

    /** "1 serving", "1.5 servings", "½ serving". */
    public static String servings(double v) {
        String n = v == 0.5 ? "½" : v == 0.25 ? "¼" : v == 0.75 ? "¾" : trim(v);
        return n + (v == 1 || v < 1 ? " serving" : " servings");
    }

    public static String trim(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return Long.toString(Math.round(v));
        return String.format(Locale.ENGLISH, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /** "1 bowl (150 g)", or just "100 g" when the serving is already described in grams. */
    public static String serving(com.nutriplanner.domain.Food f) {
        String g = grams(f.getServingGrams());
        return f.getServingLabel().equalsIgnoreCase(g) ? g : f.getServingLabel() + " (" + g + ")";
    }

    public static String quantity(FoodLogEntry e) {
        if (e.getUnit() == PortionUnit.GRAM) return grams(e.getGrams());
        return servings(e.getQuantity()) + " (" + grams(e.getGrams()) + ")";
    }

    public static String date(LocalDate d) {
        return d == null ? "" : SHORT.format(d);
    }

    public static String longDate(LocalDate d) {
        return d == null ? "" : LONG.format(d);
    }

    public static String mediumDate(LocalDate d) {
        return d == null ? "" : MEDIUM.format(d);
    }

    public static String dayMonth(LocalDate d) {
        return d == null ? "" : DAY_MONTH.format(d);
    }

    public static String weekday(LocalDate d) {
        return WEEKDAY.format(d);
    }

    public static String relative(LocalDate d, LocalDate today) {
        if (d.equals(today)) return "Today";
        if (d.equals(today.minusDays(1))) return "Yesterday";
        if (d.equals(today.plusDays(1))) return "Tomorrow";
        return date(d);
    }

    public static String dateTime(Instant i, ZoneId zone) {
        return i == null ? "Never" : DATE_TIME.format(i.atZone(zone));
    }

    public static String percent(double fraction) {
        return Math.round(fraction * 100) + "%";
    }

    /** Clamped 0–100 integer for progress widths. */
    public static int pct(double value, double max) {
        if (max <= 0) return 0;
        return (int) Math.max(0, Math.min(100, Math.round(value / max * 100)));
    }
}
