package com.nutriplanner.web.html;

import java.util.Locale;

/** Builders for vector drawings (icons and charts), expressed as Java calls. */
public final class Svg {

    private Svg() {
    }

    public static Element root(double width, double height, String cls) {
        return new Element("svg").attr("xmlns", "http://www.w3.org/2000/svg")
                .attr("viewBox", "0 0 " + num(width) + " " + num(height)).cls(cls);
    }

    public static Element group(String cls) {
        return new Element("g").cls(cls);
    }

    public static Element circle(double cx, double cy, double r, String cls) {
        return new Element("circle").attr("cx", num(cx)).attr("cy", num(cy)).attr("r", num(r)).cls(cls);
    }

    public static Element rect(double x, double y, double w, double h, double radius, String cls) {
        Element e = new Element("rect").attr("x", num(x)).attr("y", num(y)).attr("width", num(Math.max(0, w)))
                .attr("height", num(Math.max(0, h))).cls(cls);
        if (radius > 0) e.attr("rx", num(radius));
        return e;
    }

    public static Element line(double x1, double y1, double x2, double y2, String cls) {
        return new Element("line").attr("x1", num(x1)).attr("y1", num(y1)).attr("x2", num(x2)).attr("y2", num(y2)).cls(cls);
    }

    public static Element path(String d, String cls) {
        return new Element("path").attr("d", d).cls(cls);
    }

    public static Element polyline(String points, String cls) {
        return new Element("polyline").attr("points", points).cls(cls);
    }

    public static Element text(double x, double y, String value, String cls) {
        return new Element("text").attr("x", num(x)).attr("y", num(y)).cls(cls).text(value);
    }

    public static Element title(String value) {
        return new Element("title").text(value);
    }

    /** SVG arc path from {@code startDeg} to {@code endDeg} (0° = 12 o'clock, clockwise). */
    public static String arc(double cx, double cy, double r, double startDeg, double endDeg) {
        double sweep = Math.max(0, Math.min(359.99, endDeg - startDeg));
        double a0 = Math.toRadians(startDeg - 90);
        double a1 = Math.toRadians(startDeg + sweep - 90);
        double x0 = cx + r * Math.cos(a0), y0 = cy + r * Math.sin(a0);
        double x1 = cx + r * Math.cos(a1), y1 = cy + r * Math.sin(a1);
        int large = sweep > 180 ? 1 : 0;
        return "M" + num(x0) + " " + num(y0) + " A" + num(r) + " " + num(r) + " 0 " + large + " 1 " + num(x1) + " " + num(y1);
    }

    public static String num(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return Long.toString(Math.round(v));
        return String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
