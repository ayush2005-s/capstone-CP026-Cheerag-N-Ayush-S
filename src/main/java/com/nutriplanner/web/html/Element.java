package com.nutriplanner.web.html;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** A page element with attributes and children, built fluently from Java and rendered with escaping. */
public final class Element implements Node {

    private static final Set<String> VOID = Set.of("area", "base", "br", "col", "embed", "hr", "img", "input", "link",
            "meta", "source", "track", "wbr");
    private static final Pattern NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9:-]*");

    private final String tag;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<String> classes = new ArrayList<>();
    private final List<Node> children = new ArrayList<>();
    private final boolean selfClosing;

    public Element(String tag) {
        if (!NAME.matcher(tag).matches()) throw new IllegalArgumentException("Bad element name " + tag);
        this.tag = tag;
        this.selfClosing = VOID.contains(tag);
    }

    public String tag() {
        return tag;
    }

    public Element cls(String... names) {
        for (String n : names) {
            if (n == null || n.isBlank()) continue;
            for (String part : n.trim().split("\\s+")) {
                if (!classes.contains(part)) classes.add(part);
            }
        }
        return this;
    }

    public Element clsIf(boolean condition, String names) {
        return condition ? cls(names) : this;
    }

    public Element attr(String name, String value) {
        if (!NAME.matcher(name).matches()) throw new IllegalArgumentException("Bad attribute name " + name);
        if (value == null) attributes.remove(name);
        else attributes.put(name, value);
        return this;
    }

    public Element attr(String name, long value) {
        return attr(name, Long.toString(value));
    }

    public Element attr(String name, double value) {
        return attr(name, Svg.num(value));
    }

    /** A boolean attribute such as {@code required}. */
    public Element flag(String name, boolean on) {
        return on ? attr(name, "") : attr(name, (String) null);
    }

    public Element id(String id) {
        return attr("id", id);
    }

    public Element add(Node... nodes) {
        for (Node n : nodes) {
            if (n != null) children.add(n);
        }
        return this;
    }

    public Element add(List<? extends Node> nodes) {
        for (Node n : nodes) {
            if (n != null) children.add(n);
        }
        return this;
    }

    public Element text(String text) {
        if (text != null) children.add(new Text(text));
        return this;
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    @Override
    public void render(StringBuilder out) {
        out.append('<').append(tag);
        if (!classes.isEmpty()) {
            out.append(" class=\"");
            Escape.attribute(String.join(" ", classes), out);
            out.append('"');
        }
        for (Map.Entry<String, String> a : attributes.entrySet()) {
            out.append(' ').append(a.getKey());
            if (!a.getValue().isEmpty() || !isBooleanStyle(a.getKey())) {
                out.append("=\"");
                Escape.attribute(a.getValue(), out);
                out.append('"');
            }
        }
        if (selfClosing) {
            out.append('>');
            return;
        }
        out.append('>');
        for (Node child : children) child.render(out);
        out.append("</").append(tag).append('>');
    }

    private static boolean isBooleanStyle(String name) {
        switch (name) {
            case "required": case "checked": case "selected": case "disabled": case "readonly": case "hidden":
            case "autofocus": case "open": case "multiple": case "novalidate":
                return true;
            default:
                return false;
        }
    }
}
