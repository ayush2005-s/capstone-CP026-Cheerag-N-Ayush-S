package com.nutriplanner.web.style;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds the site's stylesheet from Java. Rules are written as a selector followed by property/value pairs, for
 * example {@code rule(".btn", "display", "inline-flex", "gap", "8px")}.
 */
public final class StyleSheet {

    private final StringBuilder out = new StringBuilder(64 * 1024);
    private final String indent;

    public StyleSheet() {
        this("");
    }

    private StyleSheet(String indent) {
        this.indent = indent;
    }

    public StyleSheet rule(String selector, String... declarations) {
        if (declarations.length % 2 != 0) throw new IllegalArgumentException("Unpaired declaration in " + selector);
        out.append(indent).append(selector).append('{');
        for (int i = 0; i < declarations.length; i += 2) {
            out.append(declarations[i]).append(':').append(declarations[i + 1]);
            if (i + 2 < declarations.length) out.append(';');
        }
        out.append("}\n");
        return this;
    }

    /** Custom properties (design tokens) on a selector. */
    public StyleSheet tokens(String selector, Map<String, String> tokens) {
        out.append(indent).append(selector).append('{');
        boolean first = true;
        for (Map.Entry<String, String> t : tokens.entrySet()) {
            if (!first) out.append(';');
            out.append("--").append(t.getKey()).append(':').append(t.getValue());
            first = false;
        }
        out.append("}\n");
        return this;
    }

    public StyleSheet media(String query, Consumer<StyleSheet> body) {
        StyleSheet inner = new StyleSheet(indent + "  ");
        body.accept(inner);
        out.append(indent).append("@media ").append(query).append("{\n").append(inner.out).append(indent).append("}\n");
        return this;
    }

    public StyleSheet keyframes(String name, Map<String, String[]> frames) {
        out.append("@keyframes ").append(name).append('{');
        for (Map.Entry<String, String[]> f : frames.entrySet()) {
            out.append(f.getKey()).append('{');
            String[] d = f.getValue();
            for (int i = 0; i < d.length; i += 2) {
                out.append(d[i]).append(':').append(d[i + 1]);
                if (i + 2 < d.length) out.append(';');
            }
            out.append('}');
        }
        out.append("}\n");
        return this;
    }

    public StyleSheet fontFace(String family, String url, String format, String weightRange, String stretchRange) {
        out.append("@font-face{font-family:\"").append(family).append("\";src:url(").append(url).append(") format(\"")
                .append(format).append("\");font-weight:").append(weightRange).append(";font-style:normal;font-display:swap");
        if (stretchRange != null) out.append(";font-stretch:").append(stretchRange);
        out.append("}\n");
        return this;
    }

    /** {@code var(--name)}. */
    public static String v(String token) {
        return "var(--" + token + ")";
    }

    public static Map<String, String> map(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
