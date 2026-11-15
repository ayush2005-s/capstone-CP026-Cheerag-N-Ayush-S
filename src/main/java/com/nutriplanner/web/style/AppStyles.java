package com.nutriplanner.web.style;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.nutriplanner.web.style.StyleSheet.v;

/** The complete stylesheet of the site, generated once at start-up and served from /assets/app.css. */
public final class AppStyles {

    private static final String CSS = build();
    private static final String HASH = hash(CSS);

    private AppStyles() {
    }

    public static String css() {
        return CSS;
    }

    /** Short content hash, used in the stylesheet URL so browsers can cache it forever. */
    public static String version() {
        return HASH;
    }

    private static String build() {
        StyleSheet s = new StyleSheet();
        fonts(s);
        tokens(s);
        base(s);
        shell(s);
        typography(s);
        panels(s);
        buttons(s);
        forms(s);
        tables(s);
        feedback(s);
        meters(s);
        meals(s);
        planner(s);
        charts(s);
        dashboard(s);
        foods(s);
        landing(s);
        auth(s);
        admin(s);
        utilities(s);
        responsive(s);
        return s.toString();
    }

    // ------------------------------------------------------------------- tokens

    private static void fonts(StyleSheet s) {
        s.fontFace("Inter", "/assets/fonts/InterVariable.woff2", "woff2", "100 900", null);
        s.fontFace("Bricolage Grotesque", "/assets/fonts/BricolageGrotesque.ttf", "truetype", "200 800", "75% 100%");
    }

    private static void tokens(StyleSheet s) {
        Map<String, String> shared = StyleSheet.map(
                "font-display", Theme.DISPLAY_FONT,
                "font-text", Theme.TEXT_FONT,
                "r-sm", "8px", "r-md", "12px", "r-lg", "18px", "r-xl", "26px",
                "meal-1", "#9FD3B8", "meal-2", "#5BAE86", "meal-3", "#2B7F5E", "meal-4", "#165140");
        s.tokens(":root", shared);
        s.tokens(":root,.theme-light", Theme.LIGHT);
        Map<String, String> darkMeals = StyleSheet.map("meal-1", "#2E5A47", "meal-2", "#3F8A69", "meal-3", "#5CB98F", "meal-4", "#8FDDB7");
        Map<String, String> dark = new LinkedHashMap<>(Theme.DARK);
        dark.putAll(darkMeals);
        s.tokens(".theme-dark", dark);
        s.media("(prefers-color-scheme: dark)", m -> m.tokens(".theme-system", dark));
        s.rule(".theme-dark", "color-scheme", "dark");
        s.media("(prefers-color-scheme: dark)", m -> m.rule(".theme-system", "color-scheme", "dark"));
    }

    private static void base(StyleSheet s) {
        s.rule("*,*::before,*::after", "box-sizing", "border-box");
        s.rule("html", "-webkit-text-size-adjust", "100%", "scroll-behavior", "smooth");
        s.rule("body", "margin", "0", "background", v("bg"), "color", v("ink"), "font-family", v("font-text"),
                "font-size", "15px", "line-height", "1.55", "font-feature-settings", "\"cv11\",\"ss03\"",
                "-webkit-font-smoothing", "antialiased", "text-rendering", "optimizeLegibility");
        s.rule("img,svg", "display", "block", "max-width", "100%");
        s.rule("a", "color", v("jade"), "text-decoration-thickness", "1px", "text-underline-offset", "3px");
        s.rule("a:hover", "color", v("jade-strong"));
        s.rule("h1,h2,h3,h4", "margin", "0", "font-family", v("font-display"), "font-weight", "650", "letter-spacing", "-0.01em",
                "line-height", "1.2", "color", v("ink"));
        s.rule("p", "margin", "0");
        s.rule("::selection", "background", v("turmeric-soft"), "color", v("ink"));
        s.rule(":focus-visible", "outline", "3px solid " + v("focus"), "outline-offset", "2px", "border-radius", "6px");
        s.rule(".skip", "position", "absolute", "left", "12px", "top", "-60px", "z-index", "100", "background", v("ink"),
                "color", v("surface"), "padding", "10px 14px", "border-radius", v("r-sm"));
        s.rule(".skip:focus", "top", "12px");
        s.rule(".sr-only", "position", "absolute", "width", "1px", "height", "1px", "overflow", "hidden", "clip", "rect(0 0 0 0)",
                "white-space", "nowrap");
        s.rule(".icon", "width", "20px", "height", "20px", "flex", "none", "fill", "none", "stroke", "currentColor",
                "stroke-width", "1.9", "stroke-linecap", "round", "stroke-linejoin", "round");
        s.rule(".icon-sm", "width", "16px", "height", "16px");
        s.rule(".icon-lg", "width", "28px", "height", "28px");
        s.media("(prefers-reduced-motion: reduce)", m -> m.rule("*,*::before,*::after", "animation", "none !important",
                "transition", "none !important", "scroll-behavior", "auto !important"));
    }

    // -------------------------------------------------------------------- shell

    private static void shell(StyleSheet s) {
        s.rule(".nav-toggle", "position", "absolute", "opacity", "0", "pointer-events", "none");
        s.rule(".shell", "display", "grid", "grid-template-columns", "252px minmax(0,1fr)", "min-height", "100vh");
        s.rule(".sidebar", "position", "sticky", "top", "0", "height", "100vh", "display", "flex", "flex-direction", "column",
                "gap", "18px", "padding", "22px 16px 18px", "background", v("surface"), "border-right", "1px solid " + v("line"),
                "overflow-y", "auto");
        s.rule(".brand", "display", "flex", "align-items", "center", "gap", "10px", "padding", "4px 10px 10px",
                "color", v("ink"), "text-decoration", "none");
        s.rule(".brand:hover", "color", v("ink"));
        s.rule(".brand-mark", "width", "34px", "height", "34px");
        s.rule(".brand-name", "font-family", v("font-display"), "font-weight", "700", "font-size", "19px", "letter-spacing", "-0.02em");
        s.rule(".nav", "display", "flex", "flex-direction", "column", "gap", "2px", "flex", "1");
        s.rule(".nav-label", "margin", "16px 10px 6px", "font-size", "12.5px", "font-weight", "600", "color", v("muted"));
        s.rule(".nav-link", "display", "flex", "align-items", "center", "gap", "12px", "padding", "9px 12px",
                "border-radius", v("r-md"), "color", v("ink-2"), "text-decoration", "none", "font-weight", "500",
                "font-size", "14.5px", "border", "0", "background", "none", "width", "100%", "font-family", "inherit",
                "cursor", "pointer", "text-align", "left", "transition", "background .15s, color .15s");
        s.rule(".nav-link .icon", "color", v("muted"));
        s.rule(".nav-link:hover", "background", v("surface-2"), "color", v("ink"));
        s.rule(".nav-link.active", "background", v("jade-soft"), "color", v("jade-strong"), "font-weight", "600");
        s.rule(".nav-link.active .icon", "color", v("jade"));
        s.rule(".sidebar-foot", "display", "flex", "flex-direction", "column", "gap", "6px", "padding-top", "14px",
                "border-top", "1px solid " + v("line"));
        s.rule(".user-chip", "display", "flex", "align-items", "center", "gap", "10px", "padding", "8px 10px",
                "border-radius", v("r-md"), "text-decoration", "none", "color", v("ink"));
        s.rule(".user-chip:hover", "background", v("surface-2"), "color", v("ink"));
        s.rule(".avatar", "display", "grid", "place-items", "center", "width", "34px", "height", "34px", "border-radius", "50%",
                "background", v("jade"), "color", v("on-jade"), "font-weight", "700", "font-size", "13px", "flex", "none");
        s.rule(".avatar-lg", "width", "56px", "height", "56px", "font-size", "19px");
        s.rule(".user-meta", "display", "flex", "flex-direction", "column", "min-width", "0", "line-height", "1.3");
        s.rule(".user-name", "font-weight", "600", "font-size", "14px", "white-space", "nowrap", "overflow", "hidden", "text-overflow", "ellipsis");
        s.rule(".user-role", "font-size", "12.5px", "color", v("muted"));
        s.rule(".logout", "margin", "0");
        s.rule(".main", "display", "flex", "flex-direction", "column", "min-width", "0");
        s.rule(".topbar", "display", "none");
        s.rule(".content", "flex", "1", "width", "100%", "max-width", "1240px", "padding", "34px 40px 48px");
        s.rule(".app-foot", "padding", "18px 40px 28px", "color", v("muted"), "font-size", "13px", "max-width", "1240px");
        s.rule(".scrim", "display", "none");
    }

    // --------------------------------------------------------------- typography

    private static void typography(StyleSheet s) {
        s.rule(".page-head", "display", "flex", "align-items", "flex-end", "justify-content", "space-between", "gap", "20px",
                "flex-wrap", "wrap", "margin-bottom", "26px");
        s.rule(".page-title", "font-size", "34px", "font-weight", "700", "letter-spacing", "-0.025em", "font-stretch", "92%");
        s.rule(".page-sub", "margin-top", "6px", "color", v("muted"), "font-size", "15.5px", "max-width", "62ch");
        s.rule(".page-actions", "display", "flex", "gap", "10px", "flex-wrap", "wrap", "align-items", "center");
        s.rule(".back-link", "display", "inline-flex", "align-items", "center", "gap", "6px", "margin-bottom", "10px",
                "font-size", "14px", "font-weight", "500", "text-decoration", "none", "color", v("muted"));
        s.rule(".back-link:hover", "color", v("ink"));
        s.rule(".num", "font-variant-numeric", "tabular-nums");
        s.rule(".display-num", "font-family", v("font-display"), "font-weight", "700", "letter-spacing", "-0.03em",
                "font-variant-numeric", "tabular-nums", "line-height", "1");
        s.rule(".muted", "color", v("muted"));
        s.rule(".small", "font-size", "13px");
        s.rule(".lead", "font-size", "17px", "color", v("ink-2"), "max-width", "60ch");
        s.rule(".prose p+p", "margin-top", "10px");
        s.rule(".prose", "max-width", "68ch", "color", v("ink-2"));
        s.rule(".h1", "font-size", "52px");
        s.rule(".h2", "font-size", "22px");
        s.rule(".site-main", "max-width", "1180px", "margin", "0 auto", "padding", "8px 28px 56px");
        s.rule(".h3", "font-size", "17px");
        s.rule(".unit", "font-family", v("font-text"), "font-weight", "500", "font-size", "14px", "color", v("muted"),
                "margin-left", "5px", "letter-spacing", "0");
        s.rule(".num-lg", "font-size", "38px");
        s.rule(".of", "color", v("muted"), "font-weight", "400");
        s.rule(".over", "color", v("chili"));
        s.rule(".good", "color", v("jade"));
    }

    // ------------------------------------------------------------------- panels

    private static void panels(StyleSheet s) {
        s.rule(".panel", "background", v("surface"), "border", "1px solid " + v("line"), "border-radius", v("r-lg"),
                "padding", "22px 24px", "min-width", "0");
        s.rule(".panel-head", "display", "flex", "align-items", "center", "justify-content", "space-between", "gap", "12px",
                "margin-bottom", "16px", "flex-wrap", "wrap");
        s.rule(".panel-title", "font-size", "18px", "font-weight", "650");
        s.rule(".panel-sub", "color", v("muted"), "font-size", "13.5px", "margin-top", "2px");
        s.rule(".panel-actions", "display", "flex", "gap", "8px", "align-items", "center", "flex-wrap", "wrap");
        s.rule(".panel.tinted", "background", v("surface-2"));
        s.rule(".panel.plain", "background", "transparent", "border", "0", "padding", "0");
        s.rule(".panel.danger", "border-color", v("chili-soft"));
        s.rule(".grid", "display", "grid", "gap", "20px", "align-items", "start");
        s.rule(".g-2", "grid-template-columns", "repeat(2,minmax(0,1fr))");
        s.rule(".g-3", "grid-template-columns", "repeat(3,minmax(0,1fr))");
        s.rule(".g-4", "grid-template-columns", "repeat(4,minmax(0,1fr))");
        s.rule(".g-main", "grid-template-columns", "minmax(0,1.7fr) minmax(0,1fr)");
        s.rule(".g-side", "grid-template-columns", "minmax(0,1fr) minmax(0,1.7fr)");
        s.rule(".span-2", "grid-column", "span 2");
        s.rule(".span-all", "grid-column", "1 / -1");
        s.rule(".stack", "display", "flex", "flex-direction", "column", "gap", "20px");
        s.rule(".stack-sm", "display", "flex", "flex-direction", "column", "gap", "10px");
        s.rule(".row", "display", "flex", "align-items", "center", "gap", "10px", "flex-wrap", "wrap");
        s.rule(".row-between", "display", "flex", "align-items", "center", "justify-content", "space-between", "gap", "12px", "flex-wrap", "wrap");
        s.rule(".divider", "height", "1px", "background", v("line"), "margin", "18px 0", "border", "0");
        s.rule(".kv", "display", "grid", "grid-template-columns", "minmax(120px,auto) 1fr", "gap", "10px 18px", "margin", "0");
        s.rule(".kv dt", "color", v("muted"), "font-size", "14px");
        s.rule(".kv dd", "margin", "0", "font-weight", "500");
        s.rule(".list-plain", "list-style", "none", "margin", "0", "padding", "0", "display", "flex", "flex-direction", "column", "gap", "10px");
        s.rule(".bullets", "margin", "0", "padding-left", "20px", "display", "flex", "flex-direction", "column", "gap", "6px", "color", v("ink-2"));
    }

    // ------------------------------------------------------------------ buttons

    private static void buttons(StyleSheet s) {
        s.rule(".btn", "display", "inline-flex", "align-items", "center", "justify-content", "center", "gap", "8px",
                "min-height", "40px", "padding", "8px 16px", "border-radius", v("r-md"), "border", "1px solid transparent",
                "font", "600 14.5px/1.2 " + v("font-text"), "text-decoration", "none", "cursor", "pointer", "white-space", "nowrap",
                "transition", "background .15s, border-color .15s, color .15s, transform .05s");
        s.rule(".btn:active", "transform", "translateY(1px)");
        s.rule(".btn-primary", "background", v("jade"), "color", v("on-jade"));
        s.rule(".btn-primary:hover", "background", v("jade-strong"), "color", v("on-jade"));
        s.rule(".btn-secondary", "background", v("surface"), "color", v("ink"), "border-color", v("line-strong"));
        s.rule(".btn-secondary:hover", "background", v("surface-2"), "color", v("ink"), "border-color", v("muted"));
        s.rule(".btn-ghost", "background", "transparent", "color", v("ink-2"));
        s.rule(".btn-ghost:hover", "background", v("sunken"), "color", v("ink"));
        s.rule(".btn-danger", "background", v("chili"), "color", "#fff");
        s.rule(".btn-danger:hover", "filter", "brightness(.92)", "color", "#fff");
        s.rule(".btn-danger-ghost", "background", "transparent", "color", v("chili"));
        s.rule(".btn-danger-ghost:hover", "background", v("chili-soft"), "color", v("chili"));
        s.rule(".btn-sm", "min-height", "32px", "padding", "5px 11px", "font-size", "13.5px", "border-radius", v("r-sm"));
        s.rule(".btn-lg", "min-height", "50px", "padding", "12px 22px", "font-size", "16px");
        s.rule(".btn-icon", "min-height", "34px", "width", "34px", "padding", "0", "border-radius", v("r-sm"));
        s.rule(".btn-block", "width", "100%");
        s.rule(".btn[disabled],.btn.disabled", "opacity", ".5", "cursor", "not-allowed", "pointer-events", "none");
        s.rule(".btn-row", "display", "flex", "gap", "10px", "flex-wrap", "wrap", "align-items", "center");
        s.rule("form.inline", "display", "inline", "margin", "0");
    }

    // -------------------------------------------------------------------- forms

    private static void forms(StyleSheet s) {
        s.rule(".form", "display", "flex", "flex-direction", "column", "gap", "18px");
        s.rule(".form-grid", "display", "grid", "grid-template-columns", "repeat(2,minmax(0,1fr))", "gap", "18px 20px");
        s.rule(".form-grid-3", "display", "grid", "grid-template-columns", "repeat(3,minmax(0,1fr))", "gap", "18px 20px");
        s.rule(".form-section", "display", "flex", "flex-direction", "column", "gap", "16px", "padding-top", "22px",
                "border-top", "1px solid " + v("line"));
        s.rule(".form-section:first-child", "border-top", "0", "padding-top", "0");
        s.rule(".form-section-title", "font-size", "17px", "font-weight", "650");
        s.rule(".field", "display", "flex", "flex-direction", "column", "gap", "6px", "min-width", "0");
        s.rule(".label", "font-weight", "600", "font-size", "14px", "color", v("ink"));
        s.rule(".label .optional", "font-weight", "400", "color", v("muted"), "margin-left", "4px");
        s.rule(".input", "width", "100%", "min-height", "44px", "padding", "10px 12px", "border", "1px solid " + v("line-strong"),
                "border-radius", v("r-md"), "background", v("surface"), "color", v("ink"), "font", "inherit", "font-size", "15px",
                "transition", "border-color .15s, box-shadow .15s");
        s.rule(".input:hover", "border-color", v("muted"));
        s.rule(".input:focus", "outline", "none", "border-color", v("jade"), "box-shadow", "0 0 0 3px " + v("jade-soft"));
        s.rule("select.input", "appearance", "none", "padding-right", "36px", "background-image",
                "linear-gradient(45deg,transparent 50%," + v("muted") + " 50%),linear-gradient(135deg," + v("muted") + " 50%,transparent 50%)",
                "background-position", "calc(100% - 18px) 52%,calc(100% - 13px) 52%", "background-size", "5px 5px,5px 5px",
                "background-repeat", "no-repeat");
        s.rule("textarea.input", "min-height", "90px", "resize", "vertical");
        s.rule(".input-sm", "min-height", "34px", "padding", "5px 9px", "font-size", "14px", "border-radius", v("r-sm"));
        s.rule(".input-wrap", "position", "relative", "display", "flex", "align-items", "center");
        s.rule(".input-wrap .input", "padding-right", "52px");
        s.rule(".suffix", "position", "absolute", "right", "12px", "color", v("muted"), "font-size", "14px", "pointer-events", "none");
        s.rule(".hint", "font-size", "13px", "color", v("muted"));
        s.rule(".field-error", "font-size", "13px", "color", v("chili"), "font-weight", "500");
        s.rule(".has-error .input", "border-color", v("chili"));
        s.rule(".has-error .input:focus", "box-shadow", "0 0 0 3px " + v("chili-soft"));
        s.rule("fieldset", "border", "0", "margin", "0", "padding", "0", "min-width", "0");
        s.rule("legend", "padding", "0", "margin-bottom", "8px");
        s.rule(".choices", "display", "grid", "gap", "10px");
        s.rule(".choices.cols-2", "grid-template-columns", "repeat(2,minmax(0,1fr))");
        s.rule(".choices.cols-4", "grid-template-columns", "repeat(4,minmax(0,1fr))");
        s.rule(".choice", "position", "relative", "display", "block", "cursor", "pointer");
        s.rule(".choice input,.chip input", "position", "absolute", "opacity", "0", "width", "1px", "height", "1px");
        s.rule(".choice-body", "display", "flex", "flex-direction", "column", "gap", "3px", "height", "100%", "padding", "13px 15px",
                "border", "1px solid " + v("line-strong"), "border-radius", v("r-md"), "background", v("surface"),
                "transition", "border-color .15s, background .15s");
        s.rule(".choice:hover .choice-body", "border-color", v("muted"));
        s.rule(".choice input:checked+.choice-body", "border-color", v("jade"), "background", v("jade-soft"),
                "box-shadow", "inset 0 0 0 1px " + v("jade"));
        s.rule(".choice input:focus-visible+.choice-body,.chip input:focus-visible+span", "outline", "3px solid " + v("focus"), "outline-offset", "2px");
        s.rule(".choice-title", "font-weight", "600", "font-size", "14.5px");
        s.rule(".choice-desc", "font-size", "13px", "color", v("muted"), "line-height", "1.4");
        s.rule(".chips", "display", "flex", "flex-wrap", "wrap", "gap", "8px");
        s.rule(".chip", "position", "relative", "cursor", "pointer");
        s.rule(".chip span", "display", "inline-flex", "align-items", "center", "gap", "6px", "padding", "7px 13px",
                "border", "1px solid " + v("line-strong"), "border-radius", "999px", "background", v("surface"),
                "font-size", "14px", "font-weight", "500", "transition", "background .15s, border-color .15s");
        s.rule(".chip:hover span", "border-color", v("muted"));
        s.rule(".chip input:checked+span", "background", v("ink"), "border-color", v("ink"), "color", v("surface"));
        s.rule(".segmented", "display", "inline-flex", "padding", "3px", "background", v("sunken"), "border-radius", v("r-md"), "gap", "2px");
        s.rule(".segmented a", "padding", "6px 13px", "border-radius", v("r-sm"), "text-decoration", "none", "color", v("ink-2"),
                "font-weight", "500", "font-size", "14px");
        s.rule(".segmented a:hover", "color", v("ink"));
        s.rule(".segmented a.active", "background", v("surface"), "color", v("ink"), "box-shadow", "0 1px 2px rgba(0,0,0,.08)");
        s.rule(".check", "display", "flex", "gap", "10px", "align-items", "flex-start", "font-size", "14px", "cursor", "pointer");
        s.rule(".check input", "width", "18px", "height", "18px", "margin", "2px 0 0", "accent-color", v("jade"), "flex", "none");
        s.rule(".filters", "display", "grid", "grid-template-columns", "minmax(0,2fr) repeat(4,minmax(0,1fr))", "gap", "12px",
                "align-items", "end");
        s.rule(".search-input", "padding-left", "40px");
        s.rule(".search-wrap", "position", "relative");
        s.rule(".search-wrap .icon", "position", "absolute", "left", "12px", "top", "12px", "color", v("muted"));
        s.rule(".inline-form", "display", "flex", "gap", "8px", "align-items", "center", "flex-wrap", "wrap");
        s.rule(".inline-form .input", "width", "auto", "max-width", "100%");
        s.rule(".inline-form input[type=number].input", "width", "96px");
        s.rule(".form-actions", "display", "flex", "gap", "10px", "flex-wrap", "wrap", "padding-top", "4px");
    }

    // ------------------------------------------------------------------- tables

    private static void tables(StyleSheet s) {
        s.rule(".table-wrap", "overflow-x", "auto", "margin", "0 -4px");
        s.rule(".table", "width", "100%", "border-collapse", "collapse", "font-size", "14.5px");
        s.rule(".table th", "text-align", "left", "font-weight", "600", "font-size", "13px", "color", v("muted"),
                "padding", "10px 12px", "border-bottom", "1px solid " + v("line"), "white-space", "nowrap");
        s.rule(".table td", "padding", "12px", "border-bottom", "1px solid " + v("line"), "vertical-align", "middle");
        s.rule(".table tr:last-child td", "border-bottom", "0");
        s.rule(".table tbody tr:hover", "background", v("surface-2"));
        s.rule(".table .num,.table th.num", "text-align", "right", "font-variant-numeric", "tabular-nums", "white-space", "nowrap");
        s.rule(".table .actions", "text-align", "right", "white-space", "nowrap");
        s.rule(".table a.row-link", "color", v("ink"), "font-weight", "600", "text-decoration", "none");
        s.rule(".table a.row-link:hover", "color", v("jade"), "text-decoration", "underline");
        s.rule(".cell-sub", "display", "block", "font-size", "13px", "color", v("muted"), "margin-top", "2px");
        s.rule(".pager", "display", "flex", "align-items", "center", "justify-content", "space-between", "gap", "12px",
                "margin-top", "16px", "flex-wrap", "wrap");
        s.rule(".pager-info", "color", v("muted"), "font-size", "14px");
    }

    // ----------------------------------------------------------------- feedback

    private static void feedback(StyleSheet s) {
        s.rule(".flashes", "display", "flex", "flex-direction", "column", "gap", "10px", "margin-bottom", "22px");
        s.rule(".flash", "display", "flex", "align-items", "flex-start", "gap", "10px", "padding", "12px 16px",
                "border-radius", v("r-md"), "font-weight", "500", "font-size", "14.5px", "border", "1px solid transparent",
                "animation", "flash-in .35s ease-out");
        s.keyframes("flash-in", new LinkedHashMap<>(Map.of("from", new String[]{"opacity", "0", "transform", "translateY(-4px)"},
                "to", new String[]{"opacity", "1", "transform", "none"})));
        s.rule(".flash-success", "background", v("jade-soft"), "color", v("jade-strong"), "border-color", "rgba(29,107,80,.2)");
        s.rule(".flash-error", "background", v("chili-soft"), "color", v("chili"), "border-color", "rgba(191,59,37,.25)");
        s.rule(".flash-info", "background", v("blueberry-soft"), "color", v("blueberry"), "border-color", "rgba(68,89,166,.2)");
        s.rule(".flash-warn", "background", v("turmeric-soft"), "color", v("ink"), "border-color", "rgba(217,151,15,.35)");
        s.rule(".badge", "display", "inline-flex", "align-items", "center", "gap", "5px", "padding", "2px 9px", "border-radius", "999px",
                "font-size", "12.5px", "font-weight", "600", "white-space", "nowrap", "background", v("sunken"), "color", v("ink-2"),
                "line-height", "1.6");
        s.rule(".badge-jade", "background", v("jade-soft"), "color", v("jade-strong"));
        s.rule(".badge-turmeric", "background", v("turmeric-soft"), "color", v("ink"));
        s.rule(".badge-chili", "background", v("chili-soft"), "color", v("chili"));
        s.rule(".badge-blueberry", "background", v("blueberry-soft"), "color", v("blueberry"));
        s.rule(".badge-beet", "background", v("beet-soft"), "color", v("beet"));
        s.rule(".badge-outline", "background", "transparent", "border", "1px solid " + v("line-strong"));
        s.rule(".diet-mark", "width", "16px", "height", "16px", "flex", "none", "display", "inline-block", "vertical-align", "-3px");
        s.rule(".dm-frame", "fill", "none", "stroke-width", "1.6");
        s.rule(".dm-veg .dm-frame", "stroke", v("veg"));
        s.rule(".dm-veg .dm-dot", "fill", v("veg"));
        s.rule(".dm-vegan .dm-frame", "stroke", v("veg"));
        s.rule(".dm-vegan .dm-dot", "fill", v("veg"));
        s.rule(".dm-egg .dm-frame", "stroke", v("egg"));
        s.rule(".dm-egg .dm-dot", "fill", v("egg"));
        s.rule(".dm-nonveg .dm-frame", "stroke", v("nonveg"));
        s.rule(".dm-nonveg .dm-dot", "fill", v("nonveg"));
        s.rule(".empty", "display", "flex", "flex-direction", "column", "align-items", "flex-start", "gap", "8px",
                "padding", "26px", "border", "1px dashed " + v("line-strong"), "border-radius", v("r-lg"), "background", v("surface-2"));
        s.rule(".empty-icon", "display", "grid", "place-items", "center", "width", "44px", "height", "44px", "border-radius", "50%",
                "background", v("jade-soft"), "color", v("jade"), "margin-bottom", "4px");
        s.rule(".empty h3", "font-size", "17px");
        s.rule(".empty p", "color", v("muted"), "max-width", "52ch", "font-size", "14.5px");
        s.rule(".empty .btn", "margin-top", "8px");
        s.rule(".notice", "display", "flex", "gap", "12px", "padding", "14px 16px", "border-radius", v("r-md"),
                "background", v("turmeric-soft"), "font-size", "14.5px", "align-items", "flex-start");
        s.rule(".notice .icon", "color", v("turmeric"), "margin-top", "1px");
        s.rule(".modal", "display", "none", "position", "fixed", "inset", "0", "z-index", "60", "place-items", "center", "padding", "20px");
        s.rule(".modal:target", "display", "grid");
        s.rule(".modal-scrim", "position", "absolute", "inset", "0", "background", v("overlay"));
        s.rule(".modal-card", "position", "relative", "width", "min(460px,100%)", "background", v("surface"), "border-radius", v("r-lg"),
                "padding", "24px", "box-shadow", v("shadow"), "display", "flex", "flex-direction", "column", "gap", "12px",
                "animation", "flash-in .2s ease-out");
        s.rule(".modal-card h2", "font-size", "20px");
        s.rule(".modal-card p", "color", v("ink-2"));
        s.rule(".modal-actions", "display", "flex", "gap", "10px", "justify-content", "flex-end", "margin-top", "8px", "flex-wrap", "wrap");
        s.rule(".tabs", "display", "flex", "gap", "4px", "border-bottom", "1px solid " + v("line"), "margin-bottom", "22px",
                "overflow-x", "auto");
        s.rule(".tab", "padding", "10px 14px", "color", v("muted"), "font-weight", "600", "text-decoration", "none",
                "border-bottom", "2px solid transparent", "margin-bottom", "-1px", "white-space", "nowrap", "font-size", "14.5px");
        s.rule(".tab:hover", "color", v("ink"));
        s.rule(".tab.active", "color", v("ink"), "border-bottom-color", v("jade"));
        s.rule("details.menu", "position", "relative");
        s.rule("details.menu>summary", "list-style", "none", "cursor", "pointer");
        s.rule("details.menu>summary::-webkit-details-marker,details.inline-edit>summary::-webkit-details-marker", "display", "none");
        s.rule("details.inline-edit>summary", "list-style", "none");
        s.rule("details.inline-edit[open]>summary", "background", v("sunken"), "color", v("ink"));
        s.rule(".edit-panel", "margin-top", "10px", "padding", "12px", "background", v("surface-2"), "border-radius", v("r-md"),
                "border", "1px solid " + v("line"));
    }

    // ------------------------------------------------------------------- meters

    private static void meters(StyleSheet s) {
        s.rule(".meter", "height", "8px", "border-radius", "999px", "background", v("sunken"), "overflow", "hidden");
        s.rule(".meter-fill", "height", "100%", "border-radius", "999px", "background", v("jade"), "transition", "width .6s ease-out");
        s.rule(".meter-lg", "height", "12px");
        s.rule(".tone-protein .meter-fill", "background", v("beet"));
        s.rule(".tone-carbs .meter-fill", "background", v("turmeric"));
        s.rule(".tone-fat .meter-fill", "background", v("blueberry"));
        s.rule(".tone-fiber .meter-fill", "background", v("millet"));
        s.rule(".tone-water .meter-fill", "background", v("blueberry"));
        s.rule(".tone-over .meter-fill", "background", v("chili"));
        for (int i = 0; i <= 100; i++) s.rule(".w-" + i, "width", i + "%");
        s.rule(".macro", "display", "flex", "flex-direction", "column", "gap", "7px");
        s.rule(".macro-top", "display", "flex", "justify-content", "space-between", "align-items", "baseline", "gap", "8px");
        s.rule(".macro-name", "display", "inline-flex", "align-items", "center", "gap", "8px", "font-weight", "600", "font-size", "14.5px");
        s.rule(".macro-val", "font-size", "14.5px", "font-weight", "600", "font-variant-numeric", "tabular-nums");
        s.rule(".swatch", "width", "10px", "height", "10px", "border-radius", "3px", "flex", "none", "display", "inline-block");
        s.rule(".sw-protein", "background", v("beet"));
        s.rule(".sw-carbs", "background", v("turmeric"));
        s.rule(".sw-fat", "background", v("blueberry"));
        s.rule(".sw-fiber", "background", v("millet"));
        s.rule(".sw-meal-1", "background", v("meal-1"));
        s.rule(".sw-meal-2", "background", v("meal-2"));
        s.rule(".sw-meal-3", "background", v("meal-3"));
        s.rule(".sw-meal-4", "background", v("meal-4"));
        s.rule(".macro-list", "display", "flex", "flex-direction", "column", "gap", "16px");
        s.rule(".stats", "display", "grid", "grid-template-columns", "repeat(auto-fit,minmax(150px,1fr))", "gap", "14px");
        s.rule(".stat", "display", "flex", "flex-direction", "column", "gap", "4px", "padding", "16px 18px", "background", v("surface"),
                "border", "1px solid " + v("line"), "border-radius", v("r-md"));
        s.rule(".stat-label", "font-size", "13.5px", "color", v("muted"), "font-weight", "500");
        s.rule(".stat-value", "font-family", v("font-display"), "font-size", "28px", "font-weight", "700", "letter-spacing", "-0.02em",
                "font-variant-numeric", "tabular-nums", "line-height", "1.1");
        s.rule(".stat-hint", "font-size", "13px", "color", v("muted"));
    }

    // -------------------------------------------------------------------- meals

    private static void meals(StyleSheet s) {
        s.rule(".day-nav", "display", "flex", "align-items", "center", "gap", "8px", "flex-wrap", "wrap");
        s.rule(".day-nav .date-label", "font-family", v("font-display"), "font-weight", "650", "font-size", "18px", "min-width", "200px",
                "text-align", "center");
        s.rule(".totals-strip", "display", "grid", "grid-template-columns", "1.2fr repeat(4,1fr)", "gap", "18px", "align-items", "end",
                "padding", "20px 24px", "background", v("surface"), "border", "1px solid " + v("line"), "border-radius", v("r-lg"),
                "margin-bottom", "22px");
        s.rule(".totals-kcal .display-num", "font-size", "38px");
        s.rule(".meals", "display", "flex", "flex-direction", "column", "gap", "16px");
        s.rule(".meal", "background", v("surface"), "border", "1px solid " + v("line"), "border-radius", v("r-lg"), "overflow", "hidden");
        s.rule(".meal-head", "display", "flex", "align-items", "center", "gap", "12px", "padding", "14px 20px", "flex-wrap", "wrap",
                "border-bottom", "1px solid " + v("line"));
        s.rule(".meal-icon", "display", "grid", "place-items", "center", "width", "36px", "height", "36px", "border-radius", "10px",
                "background", v("jade-soft"), "color", v("jade"), "flex", "none");
        s.rule(".meal-title", "font-size", "17px", "font-weight", "650");
        s.rule(".meal-kcal", "color", v("muted"), "font-size", "14px", "font-variant-numeric", "tabular-nums");
        s.rule(".meal-actions", "margin-left", "auto", "display", "flex", "gap", "8px", "flex-wrap", "wrap", "align-items", "center");
        s.rule(".items", "list-style", "none", "margin", "0", "padding", "0");
        s.rule(".item", "display", "grid", "grid-template-columns", "minmax(0,1fr) auto auto", "gap", "16px", "align-items", "center",
                "padding", "12px 20px", "border-bottom", "1px solid " + v("line"));
        s.rule(".item:last-child", "border-bottom", "0");
        s.rule(".item-main", "min-width", "0", "display", "flex", "flex-direction", "column", "gap", "2px");
        s.rule(".item-name", "font-weight", "600", "color", v("ink"), "text-decoration", "none", "display", "inline-flex", "gap", "8px",
                "align-items", "center");
        s.rule("a.item-name:hover", "color", v("jade"));
        s.rule(".item-meta", "font-size", "13px", "color", v("muted"));
        s.rule(".item-sub", "display", "flex", "align-items", "center", "gap", "4px", "flex-wrap", "wrap");
        s.rule(".item-sub details[open]", "flex-basis", "100%");
        s.rule(".item-sub .btn-icon", "min-height", "26px", "width", "26px");
        s.rule(".plan-row", "display", "flex", "justify-content", "space-between", "align-items", "flex-start", "gap", "12px");
        s.rule(".plan-row-main", "display", "flex", "gap", "12px", "min-width", "0", "flex", "1");
        s.rule(".item-macros", "display", "flex", "gap", "14px", "font-size", "13.5px", "color", v("ink-2"), "font-variant-numeric", "tabular-nums");
        s.rule(".item-macros strong", "color", v("ink"), "font-weight", "650");
        s.rule(".item-actions", "display", "flex", "gap", "4px", "align-items", "center");
        s.rule(".meal-empty", "padding", "16px 20px", "color", v("muted"), "font-size", "14px");
        s.rule(".mt", "display", "inline-flex", "align-items", "baseline", "gap", "3px");
        s.rule(".mt .k", "font-size", "11.5px", "color", v("muted"), "font-weight", "600");
        s.rule(".water", "display", "flex", "align-items", "center", "gap", "14px", "flex-wrap", "wrap");
        s.rule(".glasses", "display", "flex", "gap", "5px", "flex-wrap", "wrap");
        s.rule(".glass", "width", "16px", "height", "22px", "border-radius", "3px 3px 6px 6px", "border", "1.5px solid " + v("blueberry"),
                "background", "transparent");
        s.rule(".glass.full", "background", v("blueberry"));
        s.rule(".picker-list", "list-style", "none", "margin", "0", "padding", "0", "display", "flex", "flex-direction", "column");
        s.rule(".picker-item", "display", "flex", "align-items", "center", "justify-content", "space-between", "gap", "14px",
                "padding", "12px 4px", "border-bottom", "1px solid " + v("line"));
        s.rule(".picker-item:last-child", "border-bottom", "0");
    }

    private static void planner(StyleSheet s) {
        s.rule(".week", "display", "grid", "grid-template-columns", "repeat(7,minmax(0,1fr))", "gap", "8px", "margin-bottom", "22px");
        s.rule(".day", "display", "flex", "flex-direction", "column", "gap", "2px", "padding", "10px 12px", "border-radius", v("r-md"),
                "border", "1px solid " + v("line"), "background", v("surface"), "text-decoration", "none", "color", v("ink"),
                "transition", "border-color .15s, background .15s");
        s.rule(".day:hover", "border-color", v("line-strong"), "color", v("ink"));
        s.rule(".day.active", "background", v("ink"), "border-color", v("ink"), "color", v("surface"));
        s.rule(".day-name", "font-size", "12.5px", "font-weight", "600", "opacity", ".75");
        s.rule(".day-num", "font-family", v("font-display"), "font-size", "22px", "font-weight", "700", "line-height", "1.1");
        s.rule(".day-kcal", "font-size", "12.5px", "font-variant-numeric", "tabular-nums", "opacity", ".8");
        s.rule(".day.is-today .day-name::after", "content", "\"\"", "display", "inline-block", "width", "6px", "height", "6px",
                "border-radius", "50%", "background", v("turmeric"), "margin-left", "6px", "vertical-align", "2px");
        s.rule(".day.empty-day .day-kcal", "opacity", ".55");
        s.rule(".plan-summary", "display", "grid", "grid-template-columns", "repeat(4,minmax(0,1fr))", "gap", "16px");
    }

    // ------------------------------------------------------------------- charts

    private static void charts(StyleSheet s) {
        s.rule(".chart", "width", "100%", "height", "auto", "overflow", "visible");
        s.rule(".chart text", "font-family", v("font-text"), "fill", v("muted"), "font-size", "12px");
        s.rule(".ch-grid", "stroke", v("line"), "stroke-width", "1");
        s.rule(".ch-area", "fill", v("jade-soft"), "opacity", ".8");
        s.rule(".ch-line", "fill", "none", "stroke", v("jade"), "stroke-width", "2.5", "stroke-linejoin", "round", "stroke-linecap", "round");
        s.rule(".ch-dot", "fill", v("surface"), "stroke", v("jade"), "stroke-width", "2");
        s.rule(".ch-dot-last", "fill", v("jade"), "stroke", v("surface"), "stroke-width", "2.5");
        s.rule(".ch-target", "stroke", v("turmeric"), "stroke-width", "1.6", "stroke-dasharray", "5 5", "fill", "none");
        s.rule(".chart text.ch-target-label", "fill", v("ink-2"), "font-weight", "600");
        s.rule(".chart text.ch-value,.chart text.ch-target-label", "paint-order", "stroke", "stroke", v("surface"), "stroke-width", "4px",
                "stroke-linejoin", "round");
        s.rule(".donut-wrap", "width", "100%", "max-width", "200px", "flex", "none");
        s.rule(".donut-wrap.center", "margin", "0 auto");
        s.rule(".split-row", "display", "flex", "align-items", "center", "gap", "28px", "flex-wrap", "wrap");
        s.rule(".ch-bar", "fill", v("jade"));
        s.rule(".ch-bar.over", "fill", v("turmeric"));
        s.rule(".ch-bar.none", "fill", v("sunken"));
        s.rule(".ch-bar.today", "fill", v("jade-strong"));
        s.rule(".chart text.ch-value", "fill", v("ink-2"), "font-weight", "600", "font-size", "11.5px");
        s.rule(".plate", "width", "100%", "max-width", "300px", "height", "auto");
        s.rule(".pl-rim", "fill", v("surface"), "stroke", v("line"), "stroke-width", "1.5");
        s.rule(".pl-well", "fill", v("surface-2"), "stroke", v("line"), "stroke-width", "1");
        s.rule(".pl-track", "fill", "none", "stroke", v("sunken"));
        s.rule(".pl-arc", "fill", "none", "stroke-linecap", "butt");
        s.rule(".pl-m1", "stroke", v("meal-1"));
        s.rule(".pl-m2", "stroke", v("meal-2"));
        s.rule(".pl-m3", "stroke", v("meal-3"));
        s.rule(".pl-m4", "stroke", v("meal-4"));
        s.rule(".pl-over", "stroke", v("chili"));
        s.rule(".pl-gap", "fill", "none", "stroke", v("surface"), "stroke-width", "3");
        s.rule(".plate text", "font-family", v("font-text"));
        s.rule(".plate text.pl-value", "font-family", v("font-display"), "font-weight", "700", "fill", v("ink"), "letter-spacing", "-1px");
        s.rule(".plate text.pl-label", "fill", v("muted"), "font-weight", "500");
        s.rule(".plate text.pl-over-label", "fill", v("chili"), "font-weight", "600");
        s.rule(".dn-protein", "stroke", v("beet"));
        s.rule(".dn-carbs", "stroke", v("turmeric"));
        s.rule(".dn-fat", "stroke", v("blueberry"));
        s.rule(".dn-track", "stroke", v("sunken"));
        s.rule(".donut text.dn-value", "font-family", v("font-display"), "font-weight", "700", "fill", v("ink"));
        s.rule(".donut text.dn-label", "fill", v("muted"));
        s.rule(".donut text", "font-family", v("font-text"));
        s.rule(".gauge-under", "fill", v("blueberry"));
        s.rule(".gauge-healthy", "fill", v("jade"));
        s.rule(".gauge-over", "fill", v("turmeric"));
        s.rule(".gauge-obese", "fill", v("chili"));
        s.rule(".gauge-marker", "fill", v("ink"));
        s.rule(".gauge text", "font-family", v("font-text"), "fill", v("muted"), "font-size", "11px");
        s.rule(".gauge text.gauge-value", "fill", v("ink"), "font-weight", "700", "font-size", "13px");
        s.rule(".legend", "display", "flex", "flex-wrap", "wrap", "gap", "8px 16px", "font-size", "13.5px", "color", v("ink-2"));
        s.rule(".legend span", "display", "inline-flex", "align-items", "center", "gap", "6px");
    }

    // ---------------------------------------------------------------- dashboard

    private static void dashboard(StyleSheet s) {
        s.rule(".today", "display", "grid", "grid-template-columns", "minmax(240px,300px) minmax(0,1fr)", "gap", "36px",
                "align-items", "center", "padding", "28px 32px", "background", v("surface"), "border", "1px solid " + v("line"),
                "border-radius", v("r-xl"), "margin-bottom", "20px");
        s.rule(".today-figures", "display", "flex", "flex-direction", "column", "gap", "22px", "min-width", "0");
        s.rule(".remaining", "display", "flex", "align-items", "baseline", "gap", "14px", "flex-wrap", "wrap");
        s.rule(".remaining .display-num", "font-size", "64px");
        s.rule(".remaining-label", "font-size", "16px", "color", v("ink-2"), "max-width", "30ch");
        s.rule(".meal-legend", "display", "grid", "grid-template-columns", "repeat(4,minmax(0,1fr))", "gap", "10px");
        s.rule(".meal-legend-item", "display", "flex", "flex-direction", "column", "gap", "2px", "padding-left", "12px",
                "border-left", "3px solid " + v("meal-1"));
        s.rule(".meal-legend-item.m2", "border-left-color", v("meal-2"));
        s.rule(".meal-legend-item.m3", "border-left-color", v("meal-3"));
        s.rule(".meal-legend-item.m4", "border-left-color", v("meal-4"));
        s.rule(".meal-legend-item .name", "font-size", "13px", "color", v("muted"), "font-weight", "500");
        s.rule(".meal-legend-item .val", "font-weight", "650", "font-variant-numeric", "tabular-nums");
        s.rule(".insight", "display", "flex", "gap", "12px", "align-items", "flex-start", "padding", "14px 0",
                "border-bottom", "1px solid " + v("line"));
        s.rule(".insight:first-child", "padding-top", "0");
        s.rule(".insight:last-child", "border-bottom", "0", "padding-bottom", "0");
        s.rule(".insight .icon", "color", v("turmeric"), "margin-top", "2px");
        s.rule(".suggestions", "display", "flex", "flex-wrap", "wrap", "gap", "6px", "margin-top", "8px");
        s.rule(".next-meal", "display", "flex", "flex-direction", "column", "gap", "8px");
        s.rule(".next-meal li", "display", "flex", "justify-content", "space-between", "gap", "10px", "font-size", "14.5px");
        s.rule(".greeting", "font-family", v("font-display"), "font-size", "34px", "font-weight", "700", "letter-spacing", "-0.025em");
    }

    private static void foods(StyleSheet s) {
        s.rule(".nutrition-label", "border", "2px solid " + v("ink"), "border-radius", v("r-md"), "padding", "14px 16px", "background", v("surface"));
        s.rule(".nl-title", "font-family", v("font-display"), "font-size", "24px", "font-weight", "800", "letter-spacing", "-0.02em",
                "border-bottom", "1px solid " + v("ink"), "padding-bottom", "6px");
        s.rule(".nl-row", "display", "grid", "grid-template-columns", "1fr auto auto", "gap", "18px", "padding", "7px 0",
                "border-bottom", "1px solid " + v("line"), "font-variant-numeric", "tabular-nums", "font-size", "14.5px");
        s.rule(".nl-row.head", "font-size", "12.5px", "color", v("muted"), "font-weight", "600");
        s.rule(".nl-row.major", "font-weight", "700", "border-bottom", "3px solid " + v("ink"));
        s.rule(".nl-row.sub span:first-child", "padding-left", "16px", "color", v("ink-2"));
        s.rule(".nl-row span:not(:first-child)", "text-align", "right", "min-width", "72px");
        s.rule(".nl-row:last-child", "border-bottom", "0");
        s.rule(".food-title-row", "display", "flex", "align-items", "center", "gap", "10px", "flex-wrap", "wrap");
        s.rule(".tag-row", "display", "flex", "gap", "6px", "flex-wrap", "wrap");
    }

    // ------------------------------------------------------------------ landing

    private static void landing(StyleSheet s) {
        s.rule(".site-head", "display", "flex", "align-items", "center", "justify-content", "space-between", "gap", "16px",
                "max-width", "1180px", "margin", "0 auto", "padding", "22px 28px");
        s.rule(".site-head .btn-row", "gap", "8px");
        s.rule(".hero", "display", "grid", "grid-template-columns", "minmax(0,1.1fr) minmax(0,1fr)", "gap", "56px", "align-items", "center",
                "max-width", "1180px", "margin", "0 auto", "padding", "40px 28px 72px");
        s.rule(".hero-title", "font-family", v("font-display"), "font-size", "clamp(40px,6vw,68px)", "font-weight", "750",
                "letter-spacing", "-0.03em", "line-height", "1", "font-stretch", "92%");
        s.rule(".hero-copy", "margin-top", "22px", "font-size", "18px", "color", v("ink-2"), "max-width", "48ch", "line-height", "1.6");
        s.rule(".hero .btn-row", "margin-top", "30px");
        s.rule(".hero-note", "margin-top", "14px", "font-size", "13.5px", "color", v("muted"));
        s.rule(".hero-plate", "display", "grid", "grid-template-columns", "minmax(0,1fr)", "gap", "18px", "justify-items", "center",
                "padding", "30px", "background", v("surface"), "border", "1px solid " + v("line"), "border-radius", "36px");
        s.rule(".hero-plate .plate", "max-width", "330px");
        s.rule(".plate-menu", "width", "100%", "display", "grid", "grid-template-columns", "repeat(2,minmax(0,1fr))", "gap", "14px 22px");
        s.rule(".plate-menu .meal-legend-item .val", "font-weight", "500", "font-size", "14px", "color", v("ink-2"));
        s.rule(".band", "background", v("surface"), "border-top", "1px solid " + v("line"), "border-bottom", "1px solid " + v("line"));
        s.rule(".band-inner", "max-width", "1180px", "margin", "0 auto", "padding", "64px 28px");
        s.rule(".features", "display", "grid", "grid-template-columns", "repeat(3,minmax(0,1fr))", "gap", "40px");
        s.rule(".feature h3", "font-size", "20px", "margin", "14px 0 8px");
        s.rule(".feature p", "color", v("ink-2"));
        s.rule(".feature-icon", "display", "grid", "place-items", "center", "width", "46px", "height", "46px", "border-radius", "14px",
                "background", v("jade-soft"), "color", v("jade"));
        s.rule(".section-title", "font-size", "clamp(28px,3.4vw,40px)", "font-weight", "700", "letter-spacing", "-0.03em", "max-width", "22ch");
        s.rule(".how", "max-width", "1180px", "margin", "0 auto", "padding", "72px 28px", "display", "grid",
                "grid-template-columns", "minmax(0,0.9fr) minmax(0,1.1fr)", "gap", "56px");
        s.rule(".steps", "list-style", "none", "margin", "0", "padding", "0", "display", "flex", "flex-direction", "column", "gap", "22px",
                "counter-reset", "step");
        s.rule(".steps li", "display", "grid", "grid-template-columns", "44px 1fr", "gap", "16px", "counter-increment", "step");
        s.rule(".steps li::before", "content", "counter(step)", "display", "grid", "place-items", "center", "width", "40px", "height", "40px",
                "border-radius", "50%", "border", "1.5px solid " + v("jade"), "color", v("jade"), "font-family", v("font-display"),
                "font-weight", "700", "font-size", "17px");
        s.rule(".steps h3", "font-size", "18px", "margin-bottom", "4px");
        s.rule(".steps p", "color", v("ink-2"));
        s.rule(".site-foot", "max-width", "1180px", "margin", "0 auto", "padding", "28px", "color", v("muted"), "font-size", "13.5px",
                "display", "flex", "justify-content", "space-between", "gap", "16px", "flex-wrap", "wrap", "border-top", "1px solid " + v("line"));
        s.rule(".diet-row", "display", "flex", "flex-wrap", "wrap", "gap", "8px", "margin-top", "18px");
    }

    private static void auth(StyleSheet s) {
        s.rule(".auth", "display", "grid", "grid-template-columns", "minmax(0,1fr) minmax(0,1fr)", "min-height", "100vh");
        s.rule(".auth-aside", "display", "flex", "flex-direction", "column", "justify-content", "space-between", "gap", "28px",
                "padding", "36px 48px", "background", v("jade-strong"), "color", "#F2F8F4");
        s.rule(".theme-dark .auth-aside", "background", "#10261D");
        s.rule(".auth-aside .brand", "color", "#fff", "padding", "0");
        s.rule(".auth-aside h2", "color", "#fff", "font-size", "clamp(30px,3.6vw,46px)", "font-weight", "750", "letter-spacing", "-0.035em",
                "line-height", "1.04", "max-width", "15ch", "font-stretch", "90%");
        s.rule(".auth-aside p", "color", "rgba(242,248,244,.82)", "max-width", "42ch", "margin-top", "16px", "font-size", "16px");
        s.rule(".auth-aside .plate", "max-width", "250px");
        s.rule(".auth-aside .pl-rim", "fill", "rgba(255,255,255,.06)", "stroke", "rgba(255,255,255,.25)");
        s.rule(".auth-aside .pl-well", "fill", "rgba(255,255,255,.04)", "stroke", "rgba(255,255,255,.15)");
        s.rule(".auth-aside .pl-track", "stroke", "rgba(255,255,255,.12)");
        s.rule(".auth-aside .pl-gap", "stroke", "#14523D");
        s.rule(".auth-aside .plate text.pl-value", "fill", "#fff");
        s.rule(".auth-aside .plate text.pl-label", "fill", "rgba(255,255,255,.7)");
        s.rule(".auth-main", "display", "flex", "align-items", "center", "justify-content", "center", "padding", "40px 28px");
        s.rule(".auth-card", "width", "min(440px,100%)", "display", "flex", "flex-direction", "column", "gap", "22px");
        s.rule(".auth-card h1", "font-size", "32px", "font-weight", "700", "letter-spacing", "-0.025em");
        s.rule(".auth-switch", "color", v("muted"), "font-size", "14.5px");
        s.rule(".demo-hint", "padding", "12px 14px", "border-radius", v("r-md"), "background", v("surface"), "border", "1px dashed " + v("line-strong"),
                "font-size", "14px", "color", v("ink-2"));
        s.rule(".onboard", "max-width", "820px", "margin", "0 auto", "padding", "32px 24px 56px");
        s.rule(".progress-steps", "display", "flex", "gap", "8px", "list-style", "none", "margin", "0 0 26px", "padding", "0");
        s.rule(".progress-steps li", "flex", "1", "display", "flex", "flex-direction", "column", "gap", "8px", "font-size", "13.5px",
                "color", v("muted"), "font-weight", "500");
        s.rule(".progress-steps li::before", "content", "\"\"", "height", "4px", "border-radius", "999px", "background", v("line"));
        s.rule(".progress-steps li.done::before,.progress-steps li.current::before", "background", v("jade"));
        s.rule(".progress-steps li.current", "color", v("ink"), "font-weight", "600");
        s.rule(".target-card", "display", "grid", "grid-template-columns", "repeat(4,minmax(0,1fr))", "gap", "14px");
    }

    private static void admin(StyleSheet s) {
        s.rule(".status-dot", "display", "inline-block", "width", "8px", "height", "8px", "border-radius", "50%", "background", v("jade"),
                "margin-right", "6px");
        s.rule(".status-dot.off", "background", v("chili"));
        s.rule(".status-dot.locked", "background", v("turmeric"));
        s.rule(".mini-bars", "display", "flex", "align-items", "flex-end", "gap", "3px", "height", "48px");
        s.rule(".secret", "font-family", "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace", "font-size", "18px",
                "padding", "10px 14px", "background", v("surface"), "border", "1px dashed " + v("line-strong"), "border-radius", v("r-sm"),
                "user-select", "all", "letter-spacing", "0.04em");
    }

    // ---------------------------------------------------------------- utilities

    private static void utilities(StyleSheet s) {
        s.rule(".mt-0", "margin-top", "0");
        s.rule(".mt-1", "margin-top", "6px");
        s.rule(".mt-2", "margin-top", "12px");
        s.rule(".mt-3", "margin-top", "20px");
        s.rule(".mt-4", "margin-top", "32px");
        s.rule(".mb-2", "margin-bottom", "12px");
        s.rule(".mb-3", "margin-bottom", "20px");
        s.rule(".text-right", "text-align", "right");
        s.rule(".nowrap", "white-space", "nowrap");
        s.rule(".grow", "flex", "1");
        s.rule(".w-full", "width", "100%");
        s.rule(".hide", "display", "none");
    }

    // --------------------------------------------------------------- responsive

    private static void responsive(StyleSheet s) {
        s.media("(max-width: 1100px)", m -> {
            m.rule(".filters", "grid-template-columns", "repeat(3,minmax(0,1fr))");
            m.rule(".filters .search-field", "grid-column", "1 / -1");
            m.rule(".g-4", "grid-template-columns", "repeat(2,minmax(0,1fr))");
            m.rule(".totals-strip", "grid-template-columns", "repeat(3,minmax(0,1fr))");
            m.rule(".totals-kcal", "grid-column", "1 / -1");
        });
        s.media("(max-width: 1024px)", m -> {
            m.rule(".shell", "grid-template-columns", "1fr");
            m.rule(".sidebar", "position", "fixed", "z-index", "50", "left", "0", "top", "0", "width", "min(300px,86vw)",
                    "transform", "translateX(-105%)", "transition", "transform .25s ease", "box-shadow", v("shadow"));
            m.rule(".nav-toggle:checked~.shell .sidebar", "transform", "none");
            m.rule(".nav-toggle:checked~.shell .scrim", "display", "block", "position", "fixed", "inset", "0", "z-index", "40",
                    "background", v("overlay"));
            m.rule(".topbar", "display", "flex", "align-items", "center", "gap", "12px", "padding", "12px 18px",
                    "background", v("surface"), "border-bottom", "1px solid " + v("line"), "position", "sticky", "top", "0", "z-index", "30");
            m.rule(".menu-btn", "display", "grid", "place-items", "center", "width", "40px", "height", "40px", "border-radius", v("r-md"),
                    "cursor", "pointer", "color", v("ink"));
            m.rule(".menu-btn:hover", "background", v("surface-2"));
            m.rule(".content", "padding", "24px 22px 40px");
            m.rule(".app-foot", "padding", "16px 22px 26px");
            m.rule(".g-main,.g-side,.g-3", "grid-template-columns", "1fr");
            m.rule(".today", "grid-template-columns", "1fr", "justify-items", "center", "padding", "24px");
            m.rule(".today-figures", "width", "100%");
            m.rule(".hero,.how", "grid-template-columns", "1fr", "gap", "36px");
            m.rule(".features", "grid-template-columns", "1fr", "gap", "28px");
            m.rule(".auth", "grid-template-columns", "1fr");
            m.rule(".auth-aside", "padding", "26px 24px");
            m.rule(".auth-aside .plate,.auth-aside .aside-extra", "display", "none");
        });
        s.media("(max-width: 720px)", m -> {
            m.rule("body", "font-size", "14.5px");
            m.rule(".page-title,.greeting", "font-size", "27px");
            m.rule(".g-2,.g-4,.form-grid,.form-grid-3,.choices.cols-2,.choices.cols-4,.target-card,.plan-summary", "grid-template-columns", "1fr");
            m.rule(".span-2", "grid-column", "auto");
            m.rule(".filters", "grid-template-columns", "1fr 1fr");
            m.rule(".panel", "padding", "18px");
            m.rule(".item", "grid-template-columns", "1fr auto", "padding", "12px 16px");
            m.rule(".item-macros", "grid-column", "1 / -1", "grid-row", "2", "flex-wrap", "wrap", "gap", "10px");
            m.rule(".meal-head", "padding", "12px 16px");
            m.rule(".meal-actions", "margin-left", "0", "width", "100%");
            m.rule(".week", "grid-template-columns", "repeat(7,minmax(64px,1fr))", "overflow-x", "auto");
            m.rule(".meal-legend", "grid-template-columns", "repeat(2,minmax(0,1fr))");
            m.rule(".remaining .display-num", "font-size", "50px");
            m.rule(".totals-strip", "grid-template-columns", "1fr 1fr", "padding", "18px");
            m.rule(".site-head", "padding", "18px 20px");
            m.rule(".hero", "padding", "16px 20px 48px");
            m.rule(".plate-menu", "grid-template-columns", "1fr");
            m.rule(".day-nav .date-label", "min-width", "0", "flex", "1");
            m.rule(".chart text", "font-size", "20px");
            m.rule(".chart text.ch-value", "font-size", "17px");
            m.rule(".gauge text", "font-size", "15px");
        });
        s.media("print", m -> {
            m.rule(".sidebar,.topbar,.page-actions,.meal-actions,.item-actions,.app-foot,.btn", "display", "none !important");
            m.rule(".shell", "display", "block");
            m.rule("body", "background", "#fff");
        });
    }

    private static String hash(String css) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(css.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(css.hashCode());
        }
    }
}
