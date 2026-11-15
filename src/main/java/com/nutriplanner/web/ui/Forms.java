package com.nutriplanner.web.ui;

import com.nutriplanner.domain.Labeled;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.html.Node;
import com.nutriplanner.web.http.WebServer;

import java.util.ArrayList;
import java.util.List;

/** Form controls with labels, hints and inline errors. */
public final class Forms {

    private Forms() {
    }

    public record Option(String value, String label) {
        public static <E extends Enum<E> & Labeled> List<Option> of(E[] values) {
            List<Option> out = new ArrayList<>();
            for (E e : values) out.add(new Option(e.name(), e.label()));
            return out;
        }
    }

    public record Choice(String value, String title, String description) {
    }

    /** A POST form carrying the session's CSRF token. */
    public static Element post(String action, String csrf) {
        return new Element("form").attr("method", "post").attr("action", action)
                .add(hidden(WebServer.CSRF_FIELD, csrf));
    }

    /** A POST form that returns to {@code back} if the action is rejected. */
    public static Element post(String action, String csrf, String back) {
        Element f = post(action, csrf);
        if (back != null) f.add(hidden(WebServer.BACK_FIELD, back));
        return f;
    }

    public static Element get(String action) {
        return new Element("form").attr("method", "get").attr("action", action);
    }

    public static Element hidden(String name, String value) {
        return new Element("input").attr("type", "hidden").attr("name", name).attr("value", value == null ? "" : value);
    }

    private static String fieldId(String name) {
        return "f-" + name.replaceAll("[^A-Za-z0-9_-]", "-");
    }

    private static Element field(FormState st, String name, String label, boolean optional, String hint, Node control) {
        String error = st.error(name);
        Element f = Html.div("field").clsIf(error != null, "has-error");
        Element l = new Element("label").cls("label").attr("for", fieldId(name)).text(label);
        if (optional) l.add(Html.span("optional", "optional"));
        f.add(l, control);
        if (error != null) f.add(Html.p("field-error", error).id(fieldId(name) + "-error"));
        else if (hint != null) f.add(Html.p("hint", hint).id(fieldId(name) + "-hint"));
        return f;
    }

    private static Element describe(Element input, FormState st, String name, String hint) {
        if (st.error(name) != null) {
            input.attr("aria-invalid", "true").attr("aria-describedby", fieldId(name) + "-error");
        } else if (hint != null) {
            input.attr("aria-describedby", fieldId(name) + "-hint");
        }
        return input;
    }

    /** A single-line input: {@code type} is text, email, password, date and so on. */
    public static Element input(FormState st, String name, String label, String type, String hint, boolean required,
                                String autocomplete) {
        Element input = new Element("input").cls("input").id(fieldId(name)).attr("name", name).attr("type", type);
        if (!type.equals("password")) input.attr("value", st.value(name));
        input.flag("required", required);
        if (autocomplete != null) input.attr("autocomplete", autocomplete);
        describe(input, st, name, hint);
        return field(st, name, label, !required, hint, input);
    }

    public static Element text(FormState st, String name, String label, String hint, boolean required) {
        return input(st, name, label, "text", hint, required, null);
    }

    public static Element date(FormState st, String name, String label, String hint, boolean required, String min, String max) {
        Element input = new Element("input").cls("input").id(fieldId(name)).attr("name", name).attr("type", "date")
                .attr("value", st.value(name));
        if (min != null) input.attr("min", min);
        if (max != null) input.attr("max", max);
        input.flag("required", required);
        describe(input, st, name, hint);
        return field(st, name, label, !required, hint, input);
    }

    /** A number input with an optional unit shown inside the box. */
    public static Element number(FormState st, String name, String label, String unit, String step, String min, String max,
                                 String hint, boolean required) {
        Element input = new Element("input").cls("input").id(fieldId(name)).attr("name", name).attr("type", "number")
                .attr("inputmode", "decimal").attr("value", st.value(name)).attr("step", step);
        if (min != null) input.attr("min", min);
        if (max != null) input.attr("max", max);
        input.flag("required", required);
        describe(input, st, name, hint);
        Node control = unit == null ? input : Html.div("input-wrap", input, Html.span("suffix", unit));
        return field(st, name, label, !required, hint, control);
    }

    public static Element select(FormState st, String name, String label, List<Option> options, String blank, String hint) {
        Element select = new Element("select").cls("input").id(fieldId(name)).attr("name", name);
        String current = st.value(name);
        if (blank != null) select.add(option("", blank, current.isEmpty()));
        for (Option o : options) select.add(option(o.value(), o.label(), o.value().equals(current)));
        describe(select, st, name, hint);
        return field(st, name, label, false, hint, select);
    }

    /** A bare select without label, for compact filter rows. */
    public static Element bareSelect(String name, List<Option> options, String current, String blank, String ariaLabel) {
        Element select = new Element("select").cls("input").attr("name", name).attr("aria-label", ariaLabel);
        if (blank != null) select.add(option("", blank, current == null || current.isEmpty()));
        for (Option o : options) select.add(option(o.value(), o.label(), o.value().equals(current)));
        return select;
    }

    private static Element option(String value, String label, boolean selected) {
        return new Element("option").attr("value", value).flag("selected", selected).text(label);
    }

    /** Radio buttons drawn as selectable cards. */
    public static Element choices(FormState st, String name, String label, List<Choice> choices, String colsClass) {
        String error = st.error(name);
        Element set = new Element("fieldset").cls("field").clsIf(error != null, "has-error");
        set.add(new Element("legend").cls("label").text(label));
        Element grid = Html.div("choices " + (colsClass == null ? "" : colsClass));
        String current = st.value(name);
        for (Choice c : choices) {
            Element radio = new Element("input").attr("type", "radio").attr("name", name).attr("value", c.value())
                    .flag("checked", c.value().equals(current));
            Element body = Html.span("choice-body", Html.span("choice-title", c.title()));
            if (c.description() != null) body.add(Html.span("choice-desc", c.description()));
            grid.add(new Element("label").cls("choice").add(radio, body));
        }
        set.add(grid);
        if (error != null) set.add(Html.p("field-error", error));
        return set;
    }

    /** Checkboxes drawn as toggle chips. */
    public static Element chips(FormState st, String name, String label, List<Option> options, String hint) {
        Element set = new Element("fieldset").cls("field");
        set.add(new Element("legend").cls("label").text(label));
        Element wrap = Html.div("chips");
        List<String> selected = st.values(name);
        for (Option o : options) {
            Element box = new Element("input").attr("type", "checkbox").attr("name", name).attr("value", o.value())
                    .flag("checked", selected.contains(o.value()));
            wrap.add(new Element("label").cls("chip").add(box, Html.span(null, o.label())));
        }
        set.add(wrap);
        if (st.error(name) != null) set.add(Html.p("field-error", st.error(name)));
        else if (hint != null) set.add(Html.p("hint", hint));
        return set;
    }

    public static Element checkbox(FormState st, String name, String value, Node label) {
        String error = st.error(name);
        Element box = new Element("input").attr("type", "checkbox").attr("name", name).attr("value", value)
                .flag("checked", st.values(name).contains(value)).id(fieldId(name));
        Element wrap = Html.div("field").clsIf(error != null, "has-error");
        wrap.add(new Element("label").cls("check").add(box, Html.span(null, label)));
        if (error != null) wrap.add(Html.p("field-error", error));
        return wrap;
    }

    public static Element textarea(FormState st, String name, String label, String hint, int maxLength) {
        Element ta = new Element("textarea").cls("input").id(fieldId(name)).attr("name", name).attr("maxlength", maxLength)
                .text(st.value(name));
        describe(ta, st, name, hint);
        return field(st, name, label, true, hint, ta);
    }

    public static Element submit(String label, String variant, Icon icon) {
        Element b = new Element("button").attr("type", "submit").cls("btn", variant == null ? "btn-primary" : variant);
        if (icon != null) b.add(icon.svg());
        return b.add(Html.span(null, label));
    }

    /** Shows a general message for the whole form, if any. */
    public static Node formMessage(FormState st) {
        if (st.message() == null) return Node.EMPTY;
        return Html.div("flash flash-error", Icon.ALERT.svg(), Html.span(null, st.message())).attr("role", "alert");
    }
}
