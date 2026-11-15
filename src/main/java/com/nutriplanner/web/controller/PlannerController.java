package com.nutriplanner.web.controller;

import com.nutriplanner.domain.MealPlanDay;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.Nutrients;
import com.nutriplanner.service.MealPlanService;
import com.nutriplanner.service.NutritionTargets;
import com.nutriplanner.service.ValidationException;
import com.nutriplanner.web.Params;
import com.nutriplanner.web.ViewContext;
import com.nutriplanner.web.Web;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.html.Node;
import com.nutriplanner.web.http.Request;
import com.nutriplanner.web.http.Response;
import com.nutriplanner.web.http.Router;
import com.nutriplanner.web.http.WebServer;
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

/** The weekly meal planner. */
public final class PlannerController {

    private final Web web;
    private final FoodPicker picker;

    public PlannerController(Web web) {
        this.web = web;
        this.picker = new FoodPicker(web);
    }

    public void register(Router r) {
        r.get("/planner", MEMBER, this::planner);
        r.get("/planner/add", MEMBER, req -> picker.render(req, FoodPicker.Mode.PLAN, FormState.empty(), 200));
        r.post("/planner/add", MEMBER, this::add);
        r.post("/planner/generate-week", MEMBER, this::generateWeek);
        r.post("/planner/generate-day", MEMBER, req -> action(req, (uid, d, p) -> {
            web.services.plans.generateDay(uid, d);
            return "Planned a fresh day of meals for " + Fmt.date(d) + ".";
        }));
        r.post("/planner/regenerate-meal", MEMBER, req -> action(req, (uid, d, p) -> {
            MealType meal = meal(p);
            web.services.plans.regenerateMeal(uid, d, meal);
            return "New ideas for " + meal.label().toLowerCase(Locale.ROOT) + ".";
        }));
        r.post("/planner/swap", MEMBER, req -> action(req, (uid, d, p) -> {
            web.services.plans.swapItem(uid, d, index(p));
            return "Swapped in something different.";
        }));
        r.post("/planner/remove", MEMBER, req -> action(req, (uid, d, p) -> {
            web.services.plans.removeItem(uid, d, index(p));
            return "Removed from the plan.";
        }));
        r.post("/planner/servings", MEMBER, req -> action(req, (uid, d, p) -> {
            Double servings = p.decimal("servings");
            web.services.plans.updateServings(uid, d, index(p), servings);
            return "Portion updated.";
        }));
        r.post("/planner/log-meal", MEMBER, req -> action(req, (uid, d, p) -> {
            MealType meal = meal(p);
            int n = web.services.plans.logMeal(uid, d, meal);
            return "Logged " + meal.label().toLowerCase(Locale.ROOT) + " to your diary (" + n + (n == 1 ? " food)." : " foods).");
        }));
        r.post("/planner/log-day", MEMBER, req -> action(req, (uid, d, p) -> {
            int n = web.services.plans.logDay(uid, d);
            return "Logged the whole day to your diary (" + n + " foods).";
        }));
        r.post("/planner/clear", MEMBER, req -> action(req, (uid, d, p) -> {
            web.services.plans.clearDay(uid, d);
            return "Cleared the plan for " + Fmt.date(d) + ".";
        }));
    }

    @FunctionalInterface
    private interface PlanAction {
        String run(long userId, LocalDate date, Params params);
    }

    private Response action(Request req, PlanAction action) {
        Params p = new Params(req);
        LocalDate date = p.date("date", web.today());
        String message = action.run(req.user().getId(), date, p);
        req.session().success(message);
        String back = WebServer.safePath(req.param(WebServer.BACK_FIELD));
        return Response.redirect(back != null ? back : "/planner?day=" + date);
    }

    private static MealType meal(Params p) {
        MealType m = p.choice("meal", MealType.class);
        if (m == null) throw new ValidationException("Choose a meal.");
        return m;
    }

    private static int index(Params p) {
        Integer i = p.integer("index");
        if (i == null) throw new ValidationException("That planned food no longer exists.");
        return i;
    }

    // ----------------------------------------------------------------- page

    private Response planner(Request req) {
        ViewContext ctx = web.context(req);
        Params p = new Params(req);
        LocalDate day = p.date("day", p.date("week", ctx.today()));
        LocalDate start = MealPlanService.weekStart(day);
        List<MealPlanService.PlannedDay> week = web.services.plans.week(ctx.userId(), start);
        MealPlanService.PlannedDay selected = week.get((int) java.time.temporal.ChronoUnit.DAYS.between(start, day));
        NutritionTargets t = web.services.profiles.targets(ctx.userId()).orElseThrow();
        boolean anyPlanned = week.stream().anyMatch(d -> !d.isEmpty());

        Element strip = Html.div("week");
        for (MealPlanService.PlannedDay d : week) {
            Element a = Html.a("/planner?day=" + d.date(), "day",
                    Html.span("day-name", Fmt.weekday(d.date())),
                    Html.span("day-num", Integer.toString(d.date().getDayOfMonth())),
                    Html.span("day-kcal", d.isEmpty() ? "No plan" : Fmt.kcal(d.total().kcal()) + " kcal"));
            if (d.date().equals(day)) a.cls("active").attr("aria-current", "date");
            if (d.date().equals(ctx.today())) a.cls("is-today");
            if (d.isEmpty()) a.cls("empty-day");
            strip.add(a);
        }

        String back = "/planner?day=" + day;
        Element generateWeek = Forms.post("/planner/generate-week", ctx.csrf(), back).cls("inline")
                .add(Forms.hidden("week", start.toString()), Forms.submit(anyPlanned ? "Replace week" : "Plan this week", "btn-danger", Icon.REFRESH));
        Node weekButton = anyPlanned
                ? Ui.confirmTrigger("replace-week", "Plan the whole week", "btn-secondary", Icon.REFRESH)
                : Forms.post("/planner/generate-week", ctx.csrf(), back).cls("inline")
                .add(Forms.hidden("week", start.toString()), Forms.submit("Plan the whole week", "btn-primary", Icon.SPARK));
        Element nav = Html.div("day-nav",
                Ui.iconLink("/planner?day=" + day.minusWeeks(1), Icon.CHEVRON_LEFT, "Previous week"),
                Html.span("date-label", Fmt.dayMonth(start) + " – " + Fmt.dayMonth(start.plusDays(6))),
                Ui.iconLink("/planner?day=" + day.plusWeeks(1), Icon.CHEVRON_RIGHT, "Next week"));

        Element body;
        if (selected.isEmpty()) {
            Element gen = Forms.post("/planner/generate-day", ctx.csrf(), back).cls("inline")
                    .add(Forms.hidden("date", day.toString()), Forms.submit("Plan " + Fmt.weekday(day), "btn-primary", Icon.SPARK));
            body = Html.div("stack", Ui.empty(Icon.CALENDAR, "Nothing planned for " + Fmt.longDate(day),
                            "Let Nutri Planner build the day from foods that fit your diet, or add dishes yourself meal by meal.",
                            Html.div("btn-row", gen, Ui.link("/planner/add?date=" + day + "&meal=BREAKFAST", "Add foods myself", "btn-secondary", Icon.PLUS))));
        } else {
            body = Html.div("stack", summary(ctx, selected, t, day, back), meals(ctx, selected, t, day, back));
        }
        Node confirm = Ui.confirm("replace-week", "Replace this week's plan?",
                "Every day from " + Fmt.dayMonth(start) + " to " + Fmt.dayMonth(start.plusDays(6)) + " gets new meals. Your diary isn't affected.",
                generateWeek);
        return Layout.app(ctx, "Meal planner", Layout.Nav.PLANNER,
                Ui.pageHead("Meal planner", "Meals portioned to your " + Fmt.kcal(t.calories()) + " kcal target, using only foods that fit your diet.",
                        nav, weekButton),
                strip, body, confirm);
    }

    private Element summary(ViewContext ctx, MealPlanService.PlannedDay d, NutritionTargets t, LocalDate day, String back) {
        Nutrients n = d.total();
        boolean canLog = !day.isAfter(ctx.today());
        Element actions = Html.div("btn-row",
                Forms.post("/planner/generate-day", ctx.csrf(), back).cls("inline").add(Forms.hidden("date", day.toString()),
                        Forms.submit("Regenerate day", "btn-secondary btn-sm", Icon.REFRESH)),
                canLog ? Forms.post("/planner/log-day", ctx.csrf(), back).cls("inline").add(Forms.hidden("date", day.toString()),
                        Forms.submit("Log whole day", "btn-primary btn-sm", Icon.CHECK)) : Node.EMPTY,
                Forms.post("/planner/clear", ctx.csrf(), back).cls("inline").add(Forms.hidden("date", day.toString()),
                        Forms.submit("Clear", "btn-ghost btn-sm", Icon.CLOSE)));
        int diff = (int) Math.round(n.kcal() - t.calories());
        String sub = Fmt.kcal(n.kcal()) + " kcal planned, " + (Math.abs(diff) < 25 ? "right on target"
                : Fmt.kcal(Math.abs(diff)) + " kcal " + (diff > 0 ? "over" : "under") + " target");
        return Ui.panel(Fmt.longDate(day), sub, actions,
                Html.div("plan-summary",
                        Ui.macro("Calories", "sw-meal-3", n.kcal(), t.calories(), "kcal", null),
                        Ui.macro("Protein", "sw-protein", n.protein(), t.proteinG(), "g", "tone-protein"),
                        Ui.macro("Carbs", "sw-carbs", n.carbs(), t.carbsG(), "g", "tone-carbs"),
                        Ui.macro("Fat", "sw-fat", n.fat(), t.fatG(), "g", "tone-fat")));
    }

    private Element meals(ViewContext ctx, MealPlanService.PlannedDay d, NutritionTargets t, LocalDate day, String back) {
        Element wrap = Html.div("meals");
        boolean canLog = !day.isAfter(ctx.today());
        for (MealType m : MealType.values()) {
            List<MealPlanService.PlannedItem> items = d.meals().get(m);
            Nutrients mt = d.mealTotals().get(m);
            Element actions = Html.div("meal-actions",
                    Ui.link("/planner/add?date=" + day + "&meal=" + m.name(), "Add", "btn-ghost btn-sm", Icon.PLUS),
                    Forms.post("/planner/regenerate-meal", ctx.csrf(), back).cls("inline").add(Forms.hidden("date", day.toString()),
                            Forms.hidden("meal", m.name()), Forms.submit("New ideas", "btn-ghost btn-sm", Icon.REFRESH)));
            if (!items.isEmpty()) {
                actions.add(canLog
                        ? Forms.post("/planner/log-meal", ctx.csrf(), back).cls("inline").add(Forms.hidden("date", day.toString()),
                        Forms.hidden("meal", m.name()), Forms.submit("Log to diary", "btn-secondary btn-sm", Icon.CHECK))
                        : Ui.badge("Log it on " + Fmt.weekday(day), "outline"));
            }
            Element section = Html.section("meal").attr("aria-label", m.label()).add(new Element("header").cls("meal-head").add(
                    Html.span("meal-icon", Ui.mealIcon(m).svg()), Html.h(2, "meal-title", m.label()),
                    Html.span("meal-kcal", Fmt.kcal(mt.kcal()) + " of " + Fmt.kcal(t.calories() * m.share()) + " kcal"), actions));
            if (items.isEmpty()) {
                section.add(Html.p("meal-empty", "Nothing planned. Add a food or ask for new ideas."));
                wrap.add(section);
                continue;
            }
            Element ul = new Element("ul").cls("items");
            for (MealPlanService.PlannedItem item : items) {
                MealPlanDay.Item it = item.item();
                if (!item.available()) {
                    ul.add(Html.li("item", Html.div("item-main", Html.span("item-name", "A food that's no longer available"),
                                    Html.span("item-meta", "Remove it or swap in something else.")), Html.span(null, ""),
                            Html.div("item-actions", removeForm(ctx, day, item.index(), back))));
                    continue;
                }
                Nutrients n = item.nutrients();
                Element edit = new Element("details").cls("inline-edit").add(
                        new Element("summary").cls("btn btn-icon btn-ghost").attr("aria-label", "Change portion of " + item.food().getName())
                                .attr("title", "Change portion").add(Icon.EDIT.svg("icon icon-sm")),
                        Forms.post("/planner/servings", ctx.csrf(), back).cls("inline-form edit-panel").add(
                                Forms.hidden("date", day.toString()), Forms.hidden("index", Integer.toString(item.index())),
                                new Element("label").cls("small").attr("for", "s-" + item.index()).text("Servings"),
                                new Element("input").cls("input input-sm").id("s-" + item.index()).attr("type", "number")
                                        .attr("name", "servings").attr("step", "0.25").attr("min", "0.25").attr("max", "20")
                                        .attr("value", Fmt.trim(it.servings())),
                                Forms.submit("Save", "btn-primary btn-sm", null)));
                ul.add(Html.li("item",
                        Html.div("item-main",
                                Html.a("/foods/" + item.food().getId(), "item-name", Ui.dietMark(item.food().getDiet()), Html.span(null, item.food().getName())),
                                Html.div("item-sub", Html.span("item-meta", Fmt.servings(it.servings()) + " of " + item.food().getServingLabel()
                                        + " (" + Fmt.grams(it.servings() * item.food().getServingGrams()) + ")"), edit)),
                        Html.div("item-macros",
                                Html.span("mt", Html.strong(Fmt.kcal(n.kcal())), Html.span("k", "kcal")),
                                Html.span("mt", Html.text(Fmt.amount(n.protein())), Html.span("k", "P")),
                                Html.span("mt", Html.text(Fmt.amount(n.carbs())), Html.span("k", "C")),
                                Html.span("mt", Html.text(Fmt.amount(n.fat())), Html.span("k", "F"))),
                        Html.div("item-actions",
                                Forms.post("/planner/swap", ctx.csrf(), back).cls("inline").add(Forms.hidden("date", day.toString()),
                                        Forms.hidden("index", Integer.toString(item.index())), Ui.iconButton(Icon.SWAP, "Swap " + item.food().getName(), null)),
                                removeForm(ctx, day, item.index(), back))));
            }
            wrap.add(section.add(ul));
        }
        return wrap;
    }

    private static Element removeForm(ViewContext ctx, LocalDate day, int index, String back) {
        return Forms.post("/planner/remove", ctx.csrf(), back).cls("inline").add(Forms.hidden("date", day.toString()),
                Forms.hidden("index", Integer.toString(index)), Ui.iconButton(Icon.CLOSE, "Remove from plan", "btn-danger-ghost"));
    }

    // ---------------------------------------------------------------- actions

    private Response generateWeek(Request req) {
        Params p = new Params(req);
        LocalDate week = p.date("week", web.today());
        web.services.plans.generateWeek(req.user().getId(), week);
        LocalDate start = MealPlanService.weekStart(week);
        req.session().success("Your week is planned: " + Fmt.dayMonth(start) + " to " + Fmt.dayMonth(start.plusDays(6)) + ". Swap anything you don't fancy.");
        LocalDate today = web.today();
        LocalDate focus = !today.isBefore(start) && !today.isAfter(start.plusDays(6)) ? today : start;
        return Response.redirect("/planner?day=" + focus);
    }

    private Response add(Request req) {
        Params p = new Params(req);
        LocalDate date = p.date("date", web.today());
        boolean fromPicker = req.param(WebServer.BACK_FIELD) == null || req.param(WebServer.BACK_FIELD).startsWith("/planner/add");
        try {
            Double servings = p.decimal("quantity");
            if (p.hasErrors()) throw new ValidationException(p.errors());
            long foodId = DiaryController.parseId(req.param("food"));
            web.services.plans.addItem(req.user().getId(), date, p.choice("meal", MealType.class), foodId, servings);
            req.session().success("Added to " + Fmt.date(date) + ".");
            return Response.redirect("/planner?day=" + date);
        } catch (ValidationException e) {
            if (!fromPicker) throw e;
            FormState st = FormState.from(req).errors(e.fieldErrors());
            if (e.fieldErrors().isEmpty()) st.message(e.getMessage());
            return picker.render(req, FoodPicker.Mode.PLAN, st, 422);
        }
    }
}
