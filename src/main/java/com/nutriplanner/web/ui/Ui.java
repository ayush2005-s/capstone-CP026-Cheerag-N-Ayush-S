package com.nutriplanner.web.ui;

import com.nutriplanner.domain.FoodDiet;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.User;
import com.nutriplanner.service.Page;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.html.Node;
import com.nutriplanner.web.html.Svg;
import com.nutriplanner.web.http.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/** Reusable interface components. */
public final class Ui {

    private Ui() {
    }

    // ---------------------------------------------------------------- headings

    public static Element pageHead(String title, String subtitle, Node... actions) {
        Element text = Html.div(null, Html.h(1, "page-title", title));
        if (subtitle != null) text.add(Html.p("page-sub", subtitle));
        Element head = new Element("header").cls("page-head").add(text);
        Element acts = Html.div("page-actions", actions);
        if (!acts.isEmpty()) head.add(acts);
        return head;
    }

    public static Element backLink(String href, String label) {
        return Html.a(href, "back-link", Icon.ARROW_LEFT.svg("icon icon-sm"), Html.span(null, label));
    }

    public static Element panel(String title, String subtitle, Node actions, Node... body) {
        Element p = Html.section("panel");
        if (title != null) {
            Element text = Html.div(null, Html.h(2, "panel-title", title));
            if (subtitle != null) text.add(Html.p("panel-sub", subtitle));
            Element head = new Element("header").cls("panel-head").add(text);
            if (actions != null) head.add(Html.div("panel-actions", actions));
            p.add(head);
        }
        return p.add(body);
    }

    // ----------------------------------------------------------------- buttons

    public static Element link(String href, String label, String variant, Icon icon) {
        Element a = Html.a(href, "btn " + (variant == null ? "btn-secondary" : variant), (Node) null);
        if (icon != null) a.add(icon.svg());
        return a.add(Html.span(null, label));
    }

    public static Element button(String label, String variant, Icon icon) {
        return Forms.submit(label, variant, icon);
    }

    /** A square button with only an icon; {@code label} is announced to screen readers and shown as a tooltip. */
    public static Element iconButton(Icon icon, String label, String variant) {
        return new Element("button").attr("type", "submit").cls("btn btn-icon " + (variant == null ? "btn-ghost" : variant))
                .attr("aria-label", label).attr("title", label).add(icon.svg("icon icon-sm"));
    }

    public static Element iconLink(String href, Icon icon, String label) {
        return Html.a(href, "btn btn-icon btn-ghost", icon.svg("icon icon-sm")).attr("aria-label", label).attr("title", label);
    }

    // ----------------------------------------------------------------- labels

    public static Element badge(String text, String tone) {
        return Html.span("badge" + (tone == null ? "" : " badge-" + tone), text);
    }

    /** The Indian food-label convention: a square with a dot, green for vegetarian, brown for non-vegetarian. */
    public static Element dietMark(FoodDiet diet) {
        String cls;
        switch (diet) {
            case VEGAN: cls = "dm-vegan"; break;
            case EGG: cls = "dm-egg"; break;
            case NON_VEG: cls = "dm-nonveg"; break;
            default: cls = "dm-veg";
        }
        Element svg = Svg.root(16, 16, "diet-mark " + cls).attr("role", "img").attr("aria-label", diet.label());
        svg.add(Svg.title(diet.label()));
        svg.add(Svg.rect(1.5, 1.5, 13, 13, 2, "dm-frame"));
        if (diet == FoodDiet.NON_VEG) svg.add(Svg.path("M8 4.2 L11.6 10.8 L4.4 10.8 Z", "dm-dot"));
        else if (diet == FoodDiet.VEGAN) svg.add(Svg.path("M5 10.5 C5 6.5 8 4.6 11.4 4.6 C11.4 8.4 9.2 11 5 10.5 Z", "dm-dot"));
        else svg.add(Svg.circle(8, 8, 3.4, "dm-dot"));
        return svg;
    }

    public static Icon mealIcon(MealType meal) {
        switch (meal) {
            case BREAKFAST: return Icon.COFFEE;
            case LUNCH: return Icon.SUN;
            case DINNER: return Icon.MOON;
            default: return Icon.APPLE;
        }
    }

    public static Element avatar(User user, String extra) {
        return Html.span("avatar" + (extra == null ? "" : " " + extra), user.initials()).attr("aria-hidden", "true");
    }

    // -------------------------------------------------------------- progress

    public static Element meter(double value, double max, String tone) {
        int pct = Fmt.pct(value, max);
        boolean over = max > 0 && value > max * 1.05;
        return Html.div("meter " + (over && tone != null && !tone.equals("tone-water") ? "tone-over" : tone == null ? "" : tone),
                        Html.div("meter-fill w-" + pct))
                .attr("role", "progressbar").attr("aria-valuemin", "0").attr("aria-valuemax", Long.toString(Math.round(max)))
                .attr("aria-valuenow", Long.toString(Math.round(value)));
    }

    /** A labelled macro bar: "Protein 82 / 112 g". */
    public static Element macro(String name, String swatch, double value, double target, String unit, String tone) {
        Element top = Html.div("macro-top",
                Html.span("macro-name", Html.span("swatch " + swatch, ""), Html.text(name)),
                Html.span("macro-val", Html.text(Fmt.amount(value) + " "), Html.span("of", "/ " + Fmt.amount(target) + " " + unit)));
        return Html.div("macro", top, meter(value, target, tone));
    }

    public static Element stat(String label, String value, String unit, String hint) {
        Element v = Html.span("stat-value", value);
        if (unit != null) v.add(Html.span("unit", unit));
        Element s = Html.div("stat", Html.span("stat-label", label), v);
        if (hint != null) s.add(Html.span("stat-hint", hint));
        return s;
    }

    // -------------------------------------------------------------- feedback

    public static Node flashes(List<Session.Flash> flashes) {
        if (flashes.isEmpty()) return Node.EMPTY;
        Element wrap = Html.div("flashes");
        for (Session.Flash f : flashes) {
            Icon icon = f.kind().equals("success") ? Icon.CHECK_CIRCLE : f.kind().equals("error") ? Icon.ALERT : Icon.INFO;
            wrap.add(Html.div("flash flash-" + f.kind(), icon.svg(), Html.span(null, f.message()))
                    .attr("role", f.kind().equals("error") ? "alert" : "status"));
        }
        return wrap;
    }

    public static Element empty(Icon icon, String title, String text, Node action) {
        Element e = Html.div("empty", Html.span("empty-icon", icon.svg()), Html.h(3, null, title));
        if (text != null) e.add(Html.p(null, text));
        if (action != null) e.add(action);
        return e;
    }

    public static Element notice(Icon icon, Node... content) {
        return Html.div("notice", icon.svg(), Html.div(null, content));
    }

    /**
     * A confirmation dialog that opens when a link points at {@code #id}. It works without scripts, using the
     * page fragment.
     */
    public static Element confirm(String id, String title, String message, Node confirmForm) {
        return Html.div("modal", Html.a("#", "modal-scrim", "").attr("aria-label", "Cancel"),
                        Html.div("modal-card", Html.h(2, null, title), Html.p(null, message),
                                Html.div("modal-actions", Html.a("#", "btn btn-ghost", "Cancel"), confirmForm)))
                .id(id).attr("role", "dialog").attr("aria-modal", "true").attr("aria-label", title);
    }

    public static Element confirmTrigger(String id, String label, String variant, Icon icon) {
        return link("#" + id, label, variant, icon);
    }

    // ------------------------------------------------------------- navigation

    public record Tab(String href, String label, boolean active) {
    }

    public static Element tabs(List<Tab> tabs) {
        Element nav = new Element("nav").cls("tabs").attr("aria-label", "Sections");
        for (Tab t : tabs) {
            Element a = Html.a(t.href(), "tab", t.label()).clsIf(t.active(), "active");
            if (t.active()) a.attr("aria-current", "page");
            nav.add(a);
        }
        return nav;
    }

    public static Element segmented(List<Tab> tabs, String label) {
        Element nav = Html.div("segmented").attr("role", "group").attr("aria-label", label);
        for (Tab t : tabs) {
            Element a = Html.a(t.href(), null, t.label()).clsIf(t.active(), "active");
            if (t.active()) a.attr("aria-current", "true");
            nav.add(a);
        }
        return nav;
    }

    public static Node pager(Page<?> page, IntFunction<String> url, String noun) {
        if (page.total() == 0) return Node.EMPTY;
        Element nav = new Element("nav").cls("pager").attr("aria-label", "Pages");
        nav.add(Html.span("pager-info", "Showing " + page.firstIndex() + "–" + page.lastIndex() + " of " + page.total() + " " + noun));
        Element buttons = Html.div("btn-row");
        if (page.hasPrevious()) buttons.add(link(url.apply(page.page() - 1), "Previous", "btn-secondary btn-sm", Icon.CHEVRON_LEFT));
        if (page.hasNext()) buttons.add(link(url.apply(page.page() + 1), "Next", "btn-secondary btn-sm", Icon.CHEVRON_RIGHT));
        return nav.add(buttons);
    }

    // ------------------------------------------------------------------ tables

    /** A data table with optional right-aligned numeric columns. */
    public static final class Table {
        private final List<String> headers = new ArrayList<>();
        private final List<Boolean> numeric = new ArrayList<>();
        private final List<Node[]> rows = new ArrayList<>();
        private String caption;

        public Table col(String header, boolean isNumeric) {
            headers.add(header);
            numeric.add(isNumeric);
            return this;
        }

        public Table caption(String text) {
            this.caption = text;
            return this;
        }

        public Table row(Node... cells) {
            rows.add(cells);
            return this;
        }

        public boolean isEmpty() {
            return rows.isEmpty();
        }

        public Element build() {
            Element table = new Element("table").cls("table");
            if (caption != null) table.add(new Element("caption").cls("sr-only").text(caption));
            Element tr = new Element("tr");
            for (int i = 0; i < headers.size(); i++) {
                tr.add(new Element("th").attr("scope", "col").cls(numeric.get(i) ? "num" : null).text(headers.get(i)));
            }
            table.add(new Element("thead").add(tr));
            Element body = new Element("tbody");
            for (Node[] cells : rows) {
                Element row = new Element("tr");
                for (int i = 0; i < cells.length; i++) {
                    row.add(new Element("td").cls(i < numeric.size() && numeric.get(i) ? "num" : null).add(cells[i]));
                }
                body.add(row);
            }
            return Html.div("table-wrap", table.add(body));
        }
    }
}
