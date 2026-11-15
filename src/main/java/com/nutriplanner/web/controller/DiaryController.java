package com.nutriplanner.web.controller;

import com.nutriplanner.domain.FoodLogEntry;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.Nutrients;
import com.nutriplanner.domain.PortionUnit;
import com.nutriplanner.service.DiaryService;
import com.nutriplanner.service.NutritionTargets;
import com.nutriplanner.service.ValidationException;
import com.nutriplanner.web.Params;
import com.nutriplanner.web.ViewContext;
import com.nutriplanner.web.Web;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.http.Request;
import com.nutriplanner.web.http.Response;
import com.nutriplanner.web.http.Router;
import com.nutriplanner.web.ui.Charts;
import com.nutriplanner.web.ui.FormState;
import com.nutriplanner.web.ui.Forms;
import com.nutriplanner.web.ui.Fmt;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;
import com.nutriplanner.web.view.FoodPicker;
import com.nutriplanner.web.view.Layout;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static com.nutriplanner.web.http.Router.Access.MEMBER;

/** The food diary: one day at a time, grouped by meal. */
public final class DiaryController {

    private final Web web;
    private final FoodPicker picker;

    public DiaryController(Web web) {
        this.web = web;
        this.picker = new FoodPicker(web);
    }

    public void register(Router r) {
        r.get("/diary", MEMBER, this::diary);
        r.get("/diary/add", MEMBER, req -> picker.render(req, FoodPicker.Mode.DIARY, FormState.empty(), 200));
        r.post("/diary/add", MEMBER, this::add);
        r.post("/diary/{id}/update", MEMBER, this::update);
        r.post("/diary/{id}/delete", MEMBER, this::delete);
        r.post("/diary/copy", MEMBER, this::copy);
    }

    private Response diary(Request req) {
        ViewContext ctx = web.context(req);
        Params p = new Params(req);
        LocalDate date = p.date("date", ctx.today());
        if (date.isAfter(ctx.today())) date = ctx.today();
        DiaryService.DaySummary day = web.services.diary.day(ctx.userId(), date);
        NutritionTargets t = web.services.profiles.targets(ctx.userId()).orElseThrow();
        String back = "/diary?date=" + date;

        Element nav = Html.div("day-nav",
                Ui.iconLink("/diary?date=" + date.minusDays(1), Icon.CHEVRON_LEFT, "Previous day"),
                Html.span("date-label", Fmt.relative(date, ctx.today())),
                date.isBefore(ctx.today()) ? Ui.iconLink("/diary?date=" + date.plusDays(1), Icon.CHEVRON_RIGHT, "Next day")
                        : Html.span("btn btn-icon btn-ghost disabled", Icon.CHEVRON_RIGHT.svg("icon icon-sm")).attr("aria-hidden", "true"),
                Forms.get("/diary").cls("inline-form").add(
                        new Element("label").cls("sr-only").attr("for", "jump").text("Go to date"),
                        new Element("input").cls("input input-sm").id("jump").attr("type", "date").attr("name", "date")
                                .attr("value", date.toString()).attr("max", ctx.today().toString()),
                        Forms.submit("Go", "btn-secondary btn-sm", null)));

        Nutrients total = day.total();
        int remaining = (int) Math.round(t.calories() - total.kcal());
        Element totals = Html.div("totals-strip",
                Html.div("totals-kcal", Html.span("stat-label", "Eaten"),
                        Html.div(null, Html.span("display-num", Fmt.kcal(total.kcal())), Html.span("unit", "/ " + Fmt.kcal(t.calories()) + " kcal")),
                        Html.span(remaining >= 0 ? "small muted" : "small over", remaining >= 0 ? Fmt.kcal(remaining) + " kcal left" : Fmt.kcal(-remaining) + " kcal over")),
                Ui.macro("Protein", "sw-protein", total.protein(), t.proteinG(), "g", "tone-protein"),
                Ui.macro("Carbs", "sw-carbs", total.carbs(), t.carbsG(), "g", "tone-carbs"),
                Ui.macro("Fat", "sw-fat", total.fat(), t.fatG(), "g", "tone-fat"),
                Ui.macro("Fibre", "sw-fiber", total.fiber(), t.fiberG(), "g", "tone-fiber"));

        Element meals = Html.div("meals");
        for (MealType m : MealType.values()) meals.add(meal(ctx, day, m, date, back));

        Element side = Html.div("stack", energyPanel(day), waterPanel(ctx, date, t, back), copyPanel(ctx, date, day, back));
        return Layout.app(ctx, "Food diary", Layout.Nav.DIARY,
                Ui.pageHead("Food diary", Fmt.longDate(date), nav), totals, Html.div("grid g-main", meals, side));
    }

    private Element meal(ViewContext ctx, DiaryService.DaySummary day, MealType m, LocalDate date, String back) {
        List<FoodLogEntry> entries = day.meals().get(m);
        Nutrients mt = day.mealTotals().get(m);
        Element section = Html.section("meal").attr("aria-label", m.label());
        section.add(new Element("header").cls("meal-head").add(
                Html.span("meal-icon", Ui.mealIcon(m).svg()),
                Html.h(2, "meal-title", m.label()),
                Html.span("meal-kcal", Fmt.kcal(mt.kcal()) + " kcal"),
                Html.div("meal-actions", Ui.link("/diary/add?date=" + date + "&meal=" + m.name(), "Add food", "btn-secondary btn-sm", Icon.PLUS))));
        if (entries.isEmpty()) {
            section.add(Html.p("meal-empty", "Nothing logged for " + m.label().toLowerCase(Locale.ROOT) + "."));
            return section;
        }
        Element ul = new Element("ul").cls("items");
        for (FoodLogEntry e : entries) {
            Nutrients n = e.getNutrients();
            Element name = e.getFoodId() > 0 ? Html.a("/foods/" + e.getFoodId(), "item-name", e.getFoodName()) : Html.span("item-name", e.getFoodName());
            Element edit = new Element("details").cls("inline-edit").add(
                    new Element("summary").cls("btn btn-icon btn-ghost").attr("aria-label", "Edit " + e.getFoodName())
                            .attr("title", "Edit").add(Icon.EDIT.svg("icon icon-sm")));
            FormState st = FormState.empty().set("quantity", e.getQuantity()).set("unit", e.getUnit()).set("meal", e.getMealType());
            Element form = Forms.post("/diary/" + e.getId() + "/update", ctx.csrf(), back).cls("inline-form edit-panel").add(
                    new Element("label").cls("sr-only").attr("for", "q-" + e.getId()).text("Amount"),
                    new Element("input").cls("input input-sm").id("q-" + e.getId()).attr("type", "number").attr("name", "quantity")
                            .attr("step", "any").attr("min", "0.1").attr("value", st.value("quantity")).attr("required", ""),
                    Forms.bareSelect("unit", Forms.Option.of(PortionUnit.values()), st.value("unit"), null, "Unit"),
                    Forms.bareSelect("meal", Forms.Option.of(MealType.values()), st.value("meal"), null, "Meal"),
                    Forms.submit("Save", "btn-primary btn-sm", null));
            Element delete = Forms.post("/diary/" + e.getId() + "/delete", ctx.csrf(), back).cls("inline")
                    .add(Ui.iconButton(Icon.TRASH, "Remove " + e.getFoodName(), "btn-danger-ghost"));
            ul.add(Html.li("item",
                    Html.div("item-main", name, Html.div("item-sub", Html.span("item-meta", Fmt.quantity(e)), edit.add(form))),
                    Html.div("item-macros",
                            Html.span("mt", Html.strong(Fmt.kcal(n.kcal())), Html.span("k", "kcal")),
                            Html.span("mt", Html.text(Fmt.amount(n.protein())), Html.span("k", "P")),
                            Html.span("mt", Html.text(Fmt.amount(n.carbs())), Html.span("k", "C")),
                            Html.span("mt", Html.text(Fmt.amount(n.fat())), Html.span("k", "F"))),
                    Html.div("item-actions", delete)));
        }
        return section.add(ul);
    }

    private static Element energyPanel(DiaryService.DaySummary day) {
        Nutrients n = day.total();
        Element legend = Html.div("legend mt-2",
                Html.span(null, Html.span("swatch sw-protein", ""), Html.text("Protein " + Fmt.percent(n.energySplit()[0]))),
                Html.span(null, Html.span("swatch sw-carbs", ""), Html.text("Carbs " + Fmt.percent(n.energySplit()[1]))),
                Html.span(null, Html.span("swatch sw-fat", ""), Html.text("Fat " + Fmt.percent(n.energySplit()[2]))));
        return Ui.panel("Where the calories came from", null, null,
                Html.div("donut-wrap center", Charts.macroDonut(n.protein(), n.carbs(), n.fat(), Fmt.kcal(n.kcal()), "kcal")),
                legend,
                Html.p("small muted mt-2", "Sugar " + Fmt.grams(n.sugar()) + ", sodium " + Fmt.kcal(n.sodiumMg()) + " mg."));
    }

    private Element waterPanel(ViewContext ctx, LocalDate date, NutritionTargets t, String back) {
        int glasses = web.services.water.glasses(ctx.userId(), date);
        int goal = (int) Math.ceil(t.waterMl() / 250.0);
        Element minus = Forms.post("/water", ctx.csrf(), back).cls("inline").add(Forms.hidden("date", date.toString()),
                Forms.hidden("delta", "-1"), Ui.iconButton(Icon.MINUS, "Remove a glass", "btn-secondary"));
        Element plus = Forms.post("/water", ctx.csrf(), back).cls("inline").add(Forms.hidden("date", date.toString()),
                Forms.hidden("delta", "1"), Ui.iconButton(Icon.PLUS, "Add a glass", "btn-primary"));
        return Ui.panel("Water", glasses + " of " + goal + " glasses", Html.div("btn-row", minus, plus),
                Ui.meter(glasses, goal, "tone-water"));
    }

    private static Element copyPanel(ViewContext ctx, LocalDate date, DiaryService.DaySummary day, String back) {
        Element form = Forms.post("/diary/copy", ctx.csrf(), back).cls("form").add(
                Forms.hidden("to", date.toString()),
                Forms.date(FormState.empty().set("from", date.minusDays(1).toString()), "from", "Copy everything from", null, true, null,
                        ctx.today().toString()),
                Forms.submit("Copy to " + Fmt.relative(date, ctx.today()).toLowerCase(Locale.ROOT), "btn-secondary", Icon.COPY));
        return Ui.panel("Eat the same again?", "Copy a whole day's entries in one go.", null, form);
    }

    // ------------------------------------------------------------------ actions

    private Response add(Request req) {
        Params p = new Params(req);
        LocalDate date = p.date("date", web.today());
        MealType meal = p.choice("meal", MealType.class);
        Long foodId = parseId(req.param("food"));
        try {
            Double quantity = p.decimal("quantity");
            if (p.hasErrors()) throw new ValidationException(p.errors());
            FoodLogEntry e = web.services.diary.add(req.user().getId(), date, meal, foodId, quantity, p.choice("unit", PortionUnit.class));
            req.session().success("Added " + e.getFoodName() + " (" + Fmt.quantity(e) + ") to " + e.getMealType().label().toLowerCase(Locale.ROOT) + ".");
            return Response.redirect("/diary?date=" + date);
        } catch (ValidationException e) {
            FormState st = FormState.from(req).errors(e.fieldErrors());
            if (e.fieldErrors().isEmpty() || e.fieldErrors().containsKey("date")) st.message(e.getMessage());
            return picker.render(req, FoodPicker.Mode.DIARY, st, 422);
        }
    }

    private Response update(Request req) {
        Params p = new Params(req);
        Double quantity = p.decimal("quantity");
        if (p.hasErrors()) throw new ValidationException("Enter the amount as a number.");
        FoodLogEntry e = web.services.diary.update(req.user().getId(), req.pathId("id"), p.choice("meal", MealType.class), quantity,
                p.choice("unit", PortionUnit.class));
        req.session().success("Updated " + e.getFoodName() + ".");
        return Response.redirect("/diary?date=" + e.getDate());
    }

    private Response delete(Request req) {
        long id = req.pathId("id");
        FoodLogEntry e = web.services.repos.foodLog.findById(id).filter(x -> x.getUserId() == req.user().getId()).orElse(null);
        web.services.diary.delete(req.user().getId(), id);
        req.session().success("Removed " + (e == null ? "the entry" : e.getFoodName()) + ".");
        return Response.redirect("/diary?date=" + (e == null ? web.today() : e.getDate()));
    }

    private Response copy(Request req) {
        Params p = new Params(req);
        LocalDate from = p.date("from");
        LocalDate to = p.date("to", web.today());
        if (from == null) throw new ValidationException("Choose the day to copy from.");
        int n = web.services.diary.copyDay(req.user().getId(), from, to);
        req.session().success("Copied " + n + (n == 1 ? " entry" : " entries") + " from " + Fmt.date(from) + ".");
        return Response.redirect("/diary?date=" + to);
    }

    static long parseId(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException | NullPointerException e) {
            throw new com.nutriplanner.service.NotFoundException("That food isn't in the database.");
        }
    }
}
