package com.nutriplanner.web.ui;

import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Svg;

/**
 * Line icons on a 24×24 grid, drawn as vector shapes. Each icon is a list of shapes:
 * {@code p} path, {@code l} polyline, {@code c} circle (cx cy r), {@code r} rect (x y w h rx), {@code n} line.
 * Shapes are in the style of the Feather icon set (ISC licence).
 */
public enum Icon {
    DASHBOARD("r:3 3 7 9 2|r:14 3 7 5 2|r:14 12 7 9 2|r:3 16 7 5 2"),
    DIARY("p:M4 19.5A2.5 2.5 0 0 1 6.5 17H20|p:M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z|n:9 7 16 7|n:9 11 14 11"),
    CALENDAR("r:3 4 18 18 2|n:16 2 16 6|n:8 2 8 6|n:3 10 21 10"),
    PLAN("p:M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2|r:9 3 6 4 1|l:9 14 11 16 15 12"),
    APPLE("p:M12 7.5c-1.6-1.4-4.6-1.6-6.3.4-1.9 2.3-1.4 6.5.6 9.4 1.4 2 3 3.5 4.6 2.8.8-.3 1.4-.3 2.2 0 1.6.7 3.2-.8 4.6-2.8 2-2.9 2.5-7.1.6-9.4-1.7-2-4.7-1.8-6.3-.4z|p:M12 7.5c0-2.2 1-3.8 3-4.5"),
    TREND("l:23 6 13.5 15.5 8.5 10.5 1 18|l:17 6 23 6 23 12"),
    CALCULATOR("r:4 2 16 20 2|r:8 6 8 4 1|n:8 14 8.01 14|n:12 14 12.01 14|n:16 14 16.01 14|n:8 18 8.01 18|n:12 18 12.01 18|n:16 18 16.01 18"),
    USER("p:M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2|c:12 7 4"),
    USERS("p:M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2|c:9 7 4|p:M23 21v-2a4 4 0 0 0-3-3.87|p:M16 3.13a4 4 0 0 1 0 7.75"),
    SETTINGS("n:4 21 4 14|n:4 10 4 3|n:12 21 12 12|n:12 8 12 3|n:20 21 20 16|n:20 12 20 3|n:1 14 7 14|n:9 8 15 8|n:17 16 23 16"),
    SHIELD("p:M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"),
    LOGOUT("p:M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4|l:16 17 21 12 16 7|n:21 12 9 12"),
    PLUS("n:12 5 12 19|n:5 12 19 12"),
    MINUS("n:5 12 19 12"),
    TRASH("l:3 6 5 6 21 6|p:M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"),
    EDIT("p:M17 3a2.83 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"),
    CHECK("l:20 6 9 17 4 12"),
    CHECK_CIRCLE("p:M22 11.08V12a10 10 0 1 1-5.93-9.14|l:22 4 12 14.01 9 11.01"),
    CLOSE("n:18 6 6 18|n:6 6 18 18"),
    CHEVRON_LEFT("l:15 18 9 12 15 6"),
    CHEVRON_RIGHT("l:9 18 15 12 9 6"),
    ARROW_LEFT("n:19 12 5 12|l:12 19 5 12 12 5"),
    DROPLET("p:M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"),
    FLAME("p:M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.07-2.14-.22-4.05 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.15.43-2.29 1-3a2.5 2.5 0 0 0 2.5 2.5z"),
    TARGET("c:12 12 10|c:12 12 6|c:12 12 2"),
    REFRESH("l:23 4 23 10 17 10|l:1 20 1 14 7 14|p:M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"),
    SWAP("l:17 1 21 5 17 9|p:M3 11V9a4 4 0 0 1 4-4h14|l:7 23 3 19 7 15|p:M21 13v2a4 4 0 0 1-4 4H3"),
    COPY("r:9 9 13 13 2|p:M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"),
    ALERT("c:12 12 10|n:12 8 12 12|n:12 16 12.01 16"),
    INFO("c:12 12 10|n:12 16 12 12|n:12 8 12.01 8"),
    MENU("n:3 12 21 12|n:3 6 21 6|n:3 18 21 18"),
    LOCK("r:3 11 18 11 2|p:M7 11V7a5 5 0 0 1 10 0v4"),
    UNLOCK("r:3 11 18 11 2|p:M7 11V7a5 5 0 0 1 9.9-1"),
    SEARCH("c:11 11 8|n:21 21 16.65 16.65"),
    FILTER("p:M22 3H2l8 9.46V19l4 2v-8.54L22 3z"),
    ACTIVITY("l:22 12 18 12 15 21 9 3 6 12 2 12"),
    SCALE("r:3 3 18 18 5|p:M8 9a5.5 5.5 0 0 1 8 0l-2.5 3h-3z"),
    COFFEE("p:M18 8h1a4 4 0 0 1 0 8h-1|p:M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z|n:6 1 6 4|n:10 1 10 4|n:14 1 14 4"),
    SUN("c:12 12 5|n:12 1 12 3|n:12 21 12 23|n:4.22 4.22 5.64 5.64|n:18.36 18.36 19.78 19.78|n:1 12 3 12|n:21 12 23 12|n:4.22 19.78 5.64 18.36|n:18.36 5.64 19.78 4.22"),
    MOON("p:M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"),
    LEAF("p:M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10z|p:M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"),
    SPARK("p:M12 3l1.9 5.8L20 10l-6.1 1.2L12 17l-1.9-5.8L4 10l6.1-1.2z"),
    CLOCK("c:12 12 10|l:12 6 12 12 16 14"),
    LIST("n:8 6 21 6|n:8 12 21 12|n:8 18 21 18|n:3 6 3.01 6|n:3 12 3.01 12|n:3 18 3.01 18"),
    EYE_OFF("p:M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24|n:1 1 23 23"),
    EYE("p:M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z|c:12 12 3"),
    KEY("p:M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.78 7.78 5.5 5.5 0 0 1 7.78-7.78zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"),
    HEART("p:M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z");

    private final String spec;

    Icon(String spec) {
        this.spec = spec;
    }

    public Element svg() {
        return svg("icon");
    }

    public Element svg(String cls) {
        Element root = new Element("svg").attr("xmlns", "http://www.w3.org/2000/svg").attr("viewBox", "0 0 24 24")
                .attr("aria-hidden", "true").attr("focusable", "false").cls(cls);
        for (String shape : spec.split("\\|")) {
            String kind = shape.substring(0, shape.indexOf(':'));
            String data = shape.substring(shape.indexOf(':') + 1);
            String[] n = data.split(" ");
            switch (kind) {
                case "p":
                    root.add(Svg.path(data, null));
                    break;
                case "l":
                    root.add(new Element("polyline").attr("points", data));
                    break;
                case "c":
                    root.add(new Element("circle").attr("cx", n[0]).attr("cy", n[1]).attr("r", n[2]));
                    break;
                case "r":
                    root.add(new Element("rect").attr("x", n[0]).attr("y", n[1]).attr("width", n[2]).attr("height", n[3]).attr("rx", n[4]));
                    break;
                case "n":
                    root.add(new Element("line").attr("x1", n[0]).attr("y1", n[1]).attr("x2", n[2]).attr("y2", n[3]));
                    break;
                default:
                    throw new IllegalStateException("Unknown icon shape " + kind);
            }
        }
        return root;
    }
}
