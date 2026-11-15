package com.nutriplanner.web.html;

/** Output encoding. All text and attribute values pass through here, which is what keeps pages safe from XSS. */
public final class Escape {

    private Escape() {
    }

    public static void text(String s, StringBuilder out) {
        if (s == null) return;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                default:
                    if (c < 0x20 && c != '\n' && c != '\t' && c != '\r') break;
                    out.append(c);
            }
        }
    }

    public static void attribute(String s, StringBuilder out) {
        if (s == null) return;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default:
                    if (c < 0x20 && c != '\t') break;
                    out.append(c);
            }
        }
    }

    public static String text(String s) {
        StringBuilder sb = new StringBuilder();
        text(s, sb);
        return sb.toString();
    }
}
