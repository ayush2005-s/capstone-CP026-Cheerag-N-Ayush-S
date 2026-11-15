package com.nutriplanner.web.html;

import java.util.List;

/** Short factory methods for building element trees. */
public final class Html {

    private Html() {
    }

    public static Element el(String tag, String cls) {
        return new Element(tag).cls(cls);
    }

    public static Element div(String cls, Node... children) {
        return new Element("div").cls(cls).add(children);
    }

    public static Element div(String cls, String text) {
        return new Element("div").cls(cls).text(text);
    }

    public static Element section(String cls, Node... children) {
        return new Element("section").cls(cls).add(children);
    }

    public static Element span(String cls, String text) {
        return new Element("span").cls(cls).text(text);
    }

    public static Element span(String cls, Node... children) {
        return new Element("span").cls(cls).add(children);
    }

    public static Element p(String cls, String text) {
        return new Element("p").cls(cls).text(text);
    }

    public static Element p(String cls, Node... children) {
        return new Element("p").cls(cls).add(children);
    }

    public static Element h(int level, String cls, String text) {
        return new Element("h" + Math.max(1, Math.min(6, level))).cls(cls).text(text);
    }

    public static Element a(String href, String cls, String text) {
        return new Element("a").attr("href", href).cls(cls).text(text);
    }

    public static Element a(String href, String cls, Node... children) {
        return new Element("a").attr("href", href).cls(cls).add(children);
    }

    public static Element strong(String text) {
        return new Element("strong").text(text);
    }

    public static Element ul(String cls, List<? extends Node> items) {
        return new Element("ul").cls(cls).add(items);
    }

    public static Element li(String cls, Node... children) {
        return new Element("li").cls(cls).add(children);
    }

    public static Element li(String cls, String text) {
        return new Element("li").cls(cls).text(text);
    }

    public static Text text(String value) {
        return new Text(value);
    }

    public static Fragment fragment(Node... nodes) {
        return new Fragment().add(nodes);
    }

    public static Node when(boolean condition, Node node) {
        return condition ? node : Node.EMPTY;
    }
}
