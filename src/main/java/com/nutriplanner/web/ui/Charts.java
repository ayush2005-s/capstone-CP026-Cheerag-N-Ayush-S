package com.nutriplanner.web.ui;

import com.nutriplanner.domain.WeightEntry;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Svg;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Charts drawn as vector graphics on the server. Colours come from the stylesheet so they follow the theme. */
public final class Charts {

    private Charts() {
    }

    // -------------------------------------------------------------------- plate

    /**
     * The day as a plate: a ring around the rim fills with each meal's calories, in order, against the day's target.
     * Anything over the target shows as a red band on the outer edge.
     */
    public static Element plate(double[] mealKcal, double target, String value, String label, String subLabel) {
        double size = 300, c = size / 2;
        double total = 0;
        for (double k : mealKcal) total += k;
        Element svg = Svg.root(size, size, "plate").attr("role", "img")
                .attr("aria-label", String.format(Locale.ENGLISH, "%s %s. %.0f of %.0f kcal.", value, label, total, target));
        svg.add(Svg.circle(c, c, 146, "pl-rim"));
        svg.add(Svg.circle(c, c, 101, "pl-well"));
        svg.add(Svg.circle(c, c, 124, "pl-track").attr("stroke-width", 28));
        double scale = Math.max(target, total);
        double angle = 0;
        List<Double> boundaries = new ArrayList<>();
        for (int i = 0; i < mealKcal.length; i++) {
            if (mealKcal[i] <= 0 || scale <= 0) continue;
            double sweep = mealKcal[i] / scale * 360;
            if (sweep < 0.5) continue;
            svg.add(Svg.path(Svg.arc(c, c, 124, angle, angle + sweep), "pl-arc pl-m" + (i + 1)).attr("stroke-width", 28));
            angle += sweep;
            boundaries.add(angle);
        }
        if (total > 0) boundaries.add(0, 0.0);
        for (double b : boundaries) {
            double rad = Math.toRadians(b - 90);
            svg.add(Svg.line(c + 109 * Math.cos(rad), c + 109 * Math.sin(rad), c + 139 * Math.cos(rad), c + 139 * Math.sin(rad), "pl-gap"));
        }
        if (total > target && target > 0) {
            double start = target / scale * 360;
            svg.add(Svg.path(Svg.arc(c, c, 143, start, 360), "pl-arc pl-over").attr("stroke-width", 6));
        }
        svg.add(Svg.text(c, c + 12, value, "pl-value").attr("text-anchor", "middle").attr("font-size", 50));
        svg.add(Svg.text(c, c + 38, label, "pl-label").attr("text-anchor", "middle").attr("font-size", 15));
        if (subLabel != null) {
            svg.add(Svg.text(c, c - 34, subLabel, total > target ? "pl-over-label" : "pl-label")
                    .attr("text-anchor", "middle").attr("font-size", 13));
        }
        return svg;
    }

    // -------------------------------------------------------------------- donut

    /** Share of energy from protein, carbohydrate and fat. */
    public static Element macroDonut(double protein, double carbs, double fat, String centerValue, String centerLabel) {
        double size = 160, c = size / 2, r = 62;
        double p = protein * 4, cb = carbs * 4, f = fat * 9, total = p + cb + f;
        Element svg = Svg.root(size, size, "donut chart").attr("role", "img").attr("aria-label",
                total <= 0 ? "No energy" : String.format(Locale.ENGLISH, "Energy: %.0f%% protein, %.0f%% carbohydrate, %.0f%% fat",
                        p / total * 100, cb / total * 100, f / total * 100));
        svg.add(Svg.circle(c, c, r, "dn-track").attr("fill", "none").attr("stroke-width", 18));
        if (total > 0) {
            double angle = 0;
            double[] parts = {p, cb, f};
            String[] cls = {"dn-protein", "dn-carbs", "dn-fat"};
            for (int i = 0; i < 3; i++) {
                double sweep = parts[i] / total * 360;
                if (sweep < 0.5) continue;
                svg.add(Svg.path(Svg.arc(c, c, r, angle, angle + sweep - (sweep > 4 ? 1.5 : 0)), cls[i])
                        .attr("fill", "none").attr("stroke-width", 18));
                angle += sweep;
            }
        }
        svg.add(Svg.text(c, c + 6, centerValue, "dn-value").attr("text-anchor", "middle").attr("font-size", 26));
        svg.add(Svg.text(c, c + 26, centerLabel, "dn-label").attr("text-anchor", "middle").attr("font-size", 12));
        return svg;
    }

    // --------------------------------------------------------------------- line

    /** Weight over time with the goal as a dashed line. */
    public static Element weightLine(List<WeightEntry> entries, Double target) {
        return weightLine(entries, target, false);
    }

    /** {@code compact} draws a smaller version for narrow side panels, with text that stays readable. */
    public static Element weightLine(List<WeightEntry> entries, Double target, boolean compact) {
        double w = compact ? 380 : 680, h = compact ? 210 : 260, left = compact ? 36 : 44, right = compact ? 12 : 18, top = 18, bottom = 30;
        Element svg = Svg.root(w, h, "chart").attr("role", "img");
        if (entries.isEmpty()) return svg;
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (WeightEntry e : entries) {
            min = Math.min(min, e.getWeightKg());
            max = Math.max(max, e.getWeightKg());
        }
        if (target != null) {
            min = Math.min(min, target);
            max = Math.max(max, target);
        }
        double[] ticks = niceTicks(min - 0.5, max + 0.5, compact ? 4 : 5);
        double y0 = ticks[0], y1 = ticks[ticks.length - 1];
        LocalDate first = entries.get(0).getDate();
        LocalDate last = entries.get(entries.size() - 1).getDate();
        long days = Math.max(1, ChronoUnit.DAYS.between(first, last));
        double plotW = w - left - right, plotH = h - top - bottom;

        svg.attr("aria-label", String.format(Locale.ENGLISH, "Weight from %s (%.1f kg) to %s (%.1f kg)", Fmt.mediumDate(first),
                entries.get(0).getWeightKg(), Fmt.mediumDate(last), entries.get(entries.size() - 1).getWeightKg()));
        for (double t : ticks) {
            double y = top + plotH - (t - y0) / (y1 - y0) * plotH;
            svg.add(Svg.line(left, y, w - right, y, "ch-grid"));
            svg.add(Svg.text(left - 8, y + 4, Fmt.trim(Math.round(t * 10) / 10.0), null).attr("text-anchor", "end"));
        }
        StringBuilder line = new StringBuilder();
        StringBuilder area = new StringBuilder();
        double lastX = left, lastY = 0, firstX = left;
        for (int i = 0; i < entries.size(); i++) {
            WeightEntry e = entries.get(i);
            double x = entries.size() == 1 ? left + plotW / 2 : left + ChronoUnit.DAYS.between(first, e.getDate()) / (double) days * plotW;
            double y = top + plotH - (e.getWeightKg() - y0) / (y1 - y0) * plotH;
            line.append(i == 0 ? "M" : " L").append(Svg.num(x)).append(' ').append(Svg.num(y));
            if (i == 0) firstX = x;
            lastX = x;
            lastY = y;
        }
        area.append(line).append(" L").append(Svg.num(lastX)).append(' ').append(Svg.num(top + plotH))
                .append(" L").append(Svg.num(firstX)).append(' ').append(Svg.num(top + plotH)).append(" Z");
        svg.add(Svg.path(area.toString(), "ch-area"));
        if (target != null) {
            double ty = top + plotH - (target - y0) / (y1 - y0) * plotH;
            svg.add(Svg.line(left, ty, w - right, ty, "ch-target"));
            svg.add(Svg.text(w - right, ty - 7, "Goal " + Fmt.one(target) + " kg", "ch-target-label").attr("text-anchor", "end"));
        }
        svg.add(Svg.path(line.toString(), "ch-line"));
        boolean dots = entries.size() <= (compact ? 16 : 45);
        for (int i = 0; i < entries.size(); i++) {
            WeightEntry e = entries.get(i);
            double x = entries.size() == 1 ? left + plotW / 2 : left + ChronoUnit.DAYS.between(first, e.getDate()) / (double) days * plotW;
            double y = top + plotH - (e.getWeightKg() - y0) / (y1 - y0) * plotH;
            boolean isLast = i == entries.size() - 1;
            if (dots || isLast) {
                Element dot = Svg.circle(x, y, isLast ? 5.5 : 3.2, isLast ? "ch-dot-last" : "ch-dot");
                dot.add(Svg.title(Fmt.mediumDate(e.getDate()) + ": " + Fmt.one(e.getWeightKg()) + " kg"));
                svg.add(dot);
            }
        }
        svg.add(Svg.text(left, h - 8, Fmt.dayMonth(first), null));
        if (days > 20 && !compact) {
            LocalDate mid = first.plusDays(days / 2);
            svg.add(Svg.text(left + plotW / 2, h - 8, Fmt.dayMonth(mid), null).attr("text-anchor", "middle"));
        }
        svg.add(Svg.text(w - right, h - 8, Fmt.dayMonth(last), null).attr("text-anchor", "end"));
        svg.add(Svg.text(Math.min(lastX, w - right - 30), lastY - 12, Fmt.one(entries.get(entries.size() - 1).getWeightKg()), "ch-value")
                .attr("text-anchor", "middle"));
        return svg;
    }

    // ---------------------------------------------------------------------- bars

    /** Calories per day against the target. */
    public static Element dailyBars(Map<LocalDate, Double> values, double target, LocalDate today) {
        double w = 680, h = 230, left = 40, right = 10, top = 22, bottom = 30;
        Element svg = Svg.root(w, h, "chart").attr("role", "img").attr("aria-label", "Calories per day compared with your target");
        double max = target * 1.25;
        for (double v : values.values()) max = Math.max(max, v * 1.08);
        double[] ticks = niceTicks(0, max, 4);
        double top0 = ticks[ticks.length - 1];
        double plotW = w - left - right, plotH = h - top - bottom;
        for (double t : ticks) {
            double y = top + plotH - t / top0 * plotH;
            svg.add(Svg.line(left, y, w - right, y, "ch-grid"));
            svg.add(Svg.text(left - 8, y + 4, t >= 1000 ? Fmt.trim(t / 1000) + "k" : Fmt.kcal(t), null).attr("text-anchor", "end"));
        }
        int n = values.size();
        double slot = plotW / Math.max(1, n);
        double barW = Math.min(42, slot * 0.62);
        int i = 0;
        for (Map.Entry<LocalDate, Double> e : values.entrySet()) {
            double v = e.getValue();
            double x = left + i * slot + (slot - barW) / 2;
            double bh = v <= 0 ? 4 : Math.max(4, v / top0 * plotH);
            String cls = v <= 0 ? "ch-bar none" : v > target * 1.05 ? "ch-bar over" : e.getKey().equals(today) ? "ch-bar today" : "ch-bar";
            Element bar = Svg.rect(x, top + plotH - bh, barW, bh, 5, cls);
            bar.add(Svg.title(Fmt.date(e.getKey()) + ": " + Fmt.kcal(v) + " kcal"));
            svg.add(bar);
            if (v > 0) svg.add(Svg.text(x + barW / 2, top + plotH - bh - 6, Fmt.kcal(v), "ch-value").attr("text-anchor", "middle"));
            String label = n <= 7 ? Fmt.weekday(e.getKey()) : e.getKey().getDayOfMonth() + "";
            svg.add(Svg.text(x + barW / 2, h - 9, label, null).attr("text-anchor", "middle"));
            i++;
        }
        double ty = top + plotH - target / top0 * plotH;
        svg.add(Svg.line(left, ty, w - right, ty, "ch-target"));
        svg.add(Svg.text(w - right, ty - 6, "Target " + Fmt.kcal(target), "ch-target-label").attr("text-anchor", "end"));
        return svg;
    }

    /** Small bars for admin activity counts. */
    public static Element countBars(Map<LocalDate, Integer> values) {
        double w = 420, h = 110, bottom = 20;
        Element svg = Svg.root(w, h, "chart").attr("role", "img").attr("aria-label", "Daily counts for the last two weeks");
        int max = 1;
        for (int v : values.values()) max = Math.max(max, v);
        double slot = w / Math.max(1, values.size());
        int i = 0;
        for (Map.Entry<LocalDate, Integer> e : values.entrySet()) {
            double bh = e.getValue() == 0 ? 3 : Math.max(3, e.getValue() / (double) max * (h - bottom - 14));
            double x = i * slot + slot * 0.18;
            Element bar = Svg.rect(x, h - bottom - bh, slot * 0.64, bh, 3, e.getValue() == 0 ? "ch-bar none" : "ch-bar");
            bar.add(Svg.title(Fmt.date(e.getKey()) + ": " + e.getValue()));
            svg.add(bar);
            if (i % 2 == 1 || values.size() <= 7) {
                svg.add(Svg.text(x + slot * 0.32, h - 5, Integer.toString(e.getKey().getDayOfMonth()), null).attr("text-anchor", "middle"));
            }
            i++;
        }
        return svg;
    }

    // --------------------------------------------------------------------- gauge

    /** A horizontal BMI scale from 15 to 40 with the WHO bands and a marker. */
    public static Element bmiGauge(double bmi) {
        double w = 560, h = 74, left = 10, right = 10, barY = 24, barH = 14;
        double from = 15, to = 40;
        double plotW = w - left - right;
        Element svg = Svg.root(w, h, "gauge chart").attr("role", "img").attr("aria-label", "BMI " + Fmt.one(bmi) + " on the WHO scale");
        double[][] bands = {{15, 18.5}, {18.5, 25}, {25, 30}, {30, 40}};
        String[] cls = {"gauge-under", "gauge-healthy", "gauge-over", "gauge-obese"};
        for (int i = 0; i < bands.length; i++) {
            double x0 = left + (bands[i][0] - from) / (to - from) * plotW;
            double x1 = left + (bands[i][1] - from) / (to - from) * plotW;
            svg.add(Svg.rect(x0 + (i == 0 ? 0 : 1.5), barY, x1 - x0 - (i == 0 ? 1.5 : 3), barH, 4, cls[i]));
        }
        for (double mark : new double[]{18.5, 25, 30}) {
            double x = left + (mark - from) / (to - from) * plotW;
            svg.add(Svg.text(x, barY + barH + 18, Fmt.trim(mark), null).attr("text-anchor", "middle"));
        }
        double clamped = Math.max(from, Math.min(to, bmi));
        double mx = left + (clamped - from) / (to - from) * plotW;
        svg.add(Svg.path("M" + Svg.num(mx - 7) + " " + Svg.num(barY - 12) + " L" + Svg.num(mx + 7) + " " + Svg.num(barY - 12)
                + " L" + Svg.num(mx) + " " + Svg.num(barY - 2) + " Z", "gauge-marker"));
        svg.add(Svg.rect(mx - 1.5, barY - 2, 3, barH + 4, 1, "gauge-marker"));
        return svg;
    }

    // ------------------------------------------------------------------ helpers

    /** Evenly spaced, round tick values covering [lo, hi]. */
    static double[] niceTicks(double lo, double hi, int count) {
        if (hi <= lo) hi = lo + 1;
        double rawStep = (hi - lo) / Math.max(1, count - 1);
        double mag = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double norm = rawStep / mag;
        double step = (norm < 1.5 ? 1 : norm < 3 ? 2 : norm < 7 ? 5 : 10) * mag;
        double start = Math.floor(lo / step) * step;
        double end = Math.ceil(hi / step) * step;
        int n = (int) Math.round((end - start) / step) + 1;
        double[] ticks = new double[Math.max(2, n)];
        for (int i = 0; i < ticks.length; i++) ticks[i] = start + i * step;
        return ticks;
    }
}
