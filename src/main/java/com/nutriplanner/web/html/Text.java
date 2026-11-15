package com.nutriplanner.web.html;

/** Plain text, always escaped. */
public final class Text implements Node {

    private final String value;

    public Text(String value) {
        this.value = value == null ? "" : value;
    }

    @Override
    public void render(StringBuilder out) {
        Escape.text(value, out);
    }
}
