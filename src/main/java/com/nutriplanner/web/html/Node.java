package com.nutriplanner.web.html;

/** Anything that can be written into a page. */
public interface Node {

    void render(StringBuilder out);

    default String render() {
        StringBuilder sb = new StringBuilder(4096);
        render(sb);
        return sb.toString();
    }

    /** Renders nothing. */
    Node EMPTY = out -> {
    };
}
