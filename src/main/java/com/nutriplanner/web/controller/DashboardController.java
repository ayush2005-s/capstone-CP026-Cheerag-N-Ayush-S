package com.nutriplanner.web.controller;

import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.Nutrients;
import com.nutriplanner.service.DashboardService;
import com.nutriplanner.service.MealPlanService;
import com.nutriplanner.service.NutritionTargets;
import com.nutriplanner.service.WeightService;
import com.nutriplanner.web.Params;
import com.nutriplanner.web.ViewContext;
import com.nutriplanner.web.Web;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.html.Node;
import com.nutriplanner.web.http.Request;
import com.nutriplanner.web.http.Response;
import com.nutriplanner.web.http.Router;
import com.nutriplanner.web.ui.Charts;
import com.nutriplanner.web.ui.Fmt;
import com.nutriplanner.web.ui.Forms;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;
import com.nutriplanner.web.view.Layout;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.nutriplanner.web.http.Router.Access.MEMBER;

/** The member's home: today's plate, macros, weight, water, plan and tips. */
public final class DashboardController {

    private final Web web;

    public DashboardController(Web web) {
        this.web = web;
    }

    public void register(Router r) {
        r.get("/dashboard", MEMBER, this::dashboard);
        r.post("/water", MEMBER, this::water);
    }

    static MealType mealForNow(LocalTime time) {
        int h = time.getHour();
        if (h < 11) return MealType.BREAKFAST;
        if (h < 16) return MealType.LUNCH;
        if (h < 18) return MealType.SNACK;
        return MealType.DINNER;
    }

    private Response dashboard(Request req) {
        ViewContext ctx = web.context(req);
        DashboardService.Dashboard d = web.services.dashboard.build(ctx.userId());
        NutritionTargets t = d.targets();
        LocalTime now = LocalTime.now(web.clock);
        String greeting = now.getHour() < 12 ? "Good morning" : now.getHour() < 17 ? "Good afternoon" : "Good evening";
        MealType next = mealForNow(now);

        Element head = new Element("header").cls("page-head").add(
                Html.div(null, Html.h(1, "greeting", greeting + ", " + ctx.user().firstName()),
                        Html.p("page-sub", Fmt.longDate(d.today()) + (d.streak() > 1 ? ". " + d.streak() + " days logged in a row." : "."))),
                Html.div("page-actions",
                        Ui.link("/progress#log", "Log weight", "btn-secondary", Icon.SCALE),
                        Ui.link("/diary/add?date=" + d.today() + "&meal=" + next.name(), "Log " + next.label().toLowerCase(), "btn-primary", Icon.PLUS)));

        return Layout.app(ctx, "Dashboard", Layout.Nav.DASHBOARD, head, today(d, t),
                Html.div("grid g-main",
                        Html.div("stack", weekPanel(d, t), planPanel(ctx, d)),
                        Html.div("stack", weightPanel(d), waterPanel(ctx, d), insightsPanel(d))));
    }

    private static Element today(DashboardService.Dashboard d, NutritionTargets t) {
        Nutrients total = d.day().total();
        double[] meals = new double[4];
        int i = 0;
        for (MealType m : MealType.values()) meals[i++] = d.day().mealTotals().get(m).kcal();
        int remaining = d.remainingKcal();
        String centre = Fmt.kcal(Math.abs(remaining));
        Element plate = Charts.plate(meals, t.calories(), centre, remaining >= 0 ? "kcal left" : "kcal over",
                Fmt.kcal(total.kcal()) + " of " + Fmt.kcal(t.calories()));

        Element legend = Html.div("meal-legend");
        i = 1;
        for (MealType m : MealType.values()) {
            legend.add(Html.div("meal-legend-item m" + i++, Html.span("name", m.label()),
                    Html.span("val", Fmt.kcal(d.day().mealTotals().get(m).kcal()) + " kcal")));
        }
        Element remainingText = Html.div("remaining",
                Html.span("display-num " + (remaining < 0 ? "over" : ""), Fmt.kcal(total.kcal())),
                Html.span("remaining-label", "kcal eaten today, against a target of " + Fmt.kcal(t.calories()) + " kcal"
                        + (t.adjustment() < 0 ? " for steady weight loss." : t.adjustment() > 0 ? " to support your gain." : " to maintain.")));
        Element macros = Html.div("macro-list",
                Ui.macro("Protein", "sw-protein", total.protein(), t.proteinG(), "g", "tone-protein"),
                Ui.macro("Carbohydrate", "sw-carbs", total.carbs(), t.carbsG(), "g", "tone-carbs"),
                Ui.macro("Fat", "sw-fat", total.fat(), t.fatG(), "g", "tone-fat"),
                Ui.macro("Fibre", "sw-fiber", total.fiber(), t.fiberG(), "g", "tone-fiber"));
        return Html.section("today", Html.div(null, plate), Html.div("today-figures", remainingText, legend, macros))
                .attr("aria-label", "Today");
    }

    private static Element weekPanel(DashboardService.Dashboard d, NutritionTargets t) {
        Map<LocalDate, Double> kcal = new LinkedHashMap<>();
        double sum = 0;
        int days = 0;
        for (Map.Entry<LocalDate, Nutrients> e : d.lastSevenDays().entrySet()) {
            kcal.put(e.getKey(), e.getValue().kcal());
            if (e.getValue().kcal() > 0 && !e.getKey().equals(d.today())) {
                sum += e.getValue().kcal();
                days++;
            }
        }
        String sub = days == 0 ? "Calories logged over the last seven days" :
                "Average " + Fmt.kcal(sum / days) + " kcal on the " + days + " earlier days you logged";
        return Ui.panel("Last seven days", sub, Ui.link("/progress", "See progress", "btn-ghost btn-sm", null),
                Charts.dailyBars(kcal, t.calories(), d.today()));
    }

    private static Element planPanel(ViewContext ctx, DashboardService.Dashboard d) {
        MealPlanService.PlannedDay plan = d.plan();
        if (plan.isEmpty()) {
            Element generate = Forms.post("/planner/generate-week", ctx.csrf(), "/dashboard").cls("inline")
                    .add(Forms.hidden("week", d.today().toString()), Forms.submit("Plan this week", "btn-primary", Icon.CALENDAR));
            return Ui.panel("Today's plan", null, null, Ui.empty(Icon.CALENDAR, "No meals planned for today",
                    "Generate a week of meals that fit your targets, diet and allergies. You can swap anything you don't fancy.", generate));
        }
        Element list = Html.div("stack-sm");
        for (MealType m : MealType.values()) {
            var items = plan.meals().get(m);
            if (items.isEmpty()) continue;
            StringBuilder names = new StringBuilder();
            for (MealPlanService.PlannedItem item : items) {
                if (item.food() == null) continue;
                if (names.length() > 0) names.append(", ");
                names.append(item.food().getName());
            }
            boolean logged = !d.day().meals().get(m).isEmpty();
            Element action = logged ? Ui.badge("Logged", "jade")
                    : Forms.post("/planner/log-meal", ctx.csrf(), "/dashboard").cls("inline").add(
                    Forms.hidden("date", d.today().toString()), Forms.hidden("meal", m.name()),
                    Forms.submit("Log it", "btn-secondary btn-sm", Icon.CHECK));
            list.add(Html.div("plan-row", Html.div("plan-row-main",
                            Html.span("meal-icon", Ui.mealIcon(m).svg("icon icon-sm")),
                            Html.div(null, Html.strong(m.label() + " "), Html.span("muted small", Fmt.kcal(plan.mealTotals().get(m).kcal()) + " kcal"),
                                    Html.p("small muted", names.toString()))),
                    action));
        }
        return Ui.panel("Today's plan", Fmt.kcal(plan.total().kcal()) + " kcal planned", Ui.link("/planner?day=" + d.today(), "Open planner", "btn-ghost btn-sm", null), list);
    }

    private static Element weightPanel(DashboardService.Dashboard d) {
        WeightService.WeightStats s = d.weight();
        if (!s.hasData()) {
            return Ui.panel("Weight", null, null, Ui.empty(Icon.SCALE, "No weigh-ins yet", "Log your weight to see your trend.",
                    Ui.link("/progress#log", "Log weight", "btn-primary", null)));
        }
        Element body = Html.div("stack-sm");
        Element figures = Html.div("row-between",
                Html.div(null, Html.span("display-num num-lg", Fmt.one(s.currentKg())), Html.span("unit", "kg")),
                s.change30dKg() == null ? Node.EMPTY : Ui.badge(Fmt.signed(s.change30dKg()) + " kg in 30 days",
                        s.change30dKg() < 0 ? "jade" : "turmeric"));
        body.add(figures);
        if (s.targetKg() != null && s.progressPercent() != null) {
            body.add(Html.div("macro",
                    Html.div("macro-top", Html.span("small muted", "Goal " + Fmt.one(s.targetKg()) + " kg"),
                            Html.span("small", Math.round(s.progressPercent()) + "% of the way")),
                    Ui.meter(s.progressPercent(), 100, null)));
            if (s.projectedDate() != null) body.add(Html.p("small muted", "At your current pace you'll get there around " + Fmt.mediumDate(s.projectedDate()) + "."));
        }
        if (d.recentWeights().size() > 1) body.add(Html.div("mt-2", Charts.weightLine(d.recentWeights(), s.targetKg(), true)));
        return Ui.panel("Weight", null, Ui.link("/progress", "Details", "btn-ghost btn-sm", null), body);
    }

    private static Element waterPanel(ViewContext ctx, DashboardService.Dashboard d) {
        Element glasses = Html.div("glasses").attr("aria-hidden", "true");
        int goal = Math.max(d.waterGoalGlasses(), d.waterGlasses());
        for (int i = 0; i < goal; i++) glasses.add(Html.span("glass" + (i < d.waterGlasses() ? " full" : ""), ""));
        Element minus = Forms.post("/water", ctx.csrf(), "/dashboard").cls("inline").add(Forms.hidden("date", d.today().toString()),
                Forms.hidden("delta", "-1"), Ui.iconButton(Icon.MINUS, "Remove a glass", "btn-secondary"));
        Element plus = Forms.post("/water", ctx.csrf(), "/dashboard").cls("inline").add(Forms.hidden("date", d.today().toString()),
                Forms.hidden("delta", "1"), Ui.iconButton(Icon.PLUS, "Add a glass", "btn-primary"));
        return Ui.panel("Water", d.waterGlasses() + " of " + d.waterGoalGlasses() + " glasses (250 ml each)", null,
                Html.div("water", glasses, Html.div("btn-row", minus, plus)));
    }

    private static Element insightsPanel(DashboardService.Dashboard d) {
        if (d.insights().isEmpty()) {
            return Ui.panel("For you", null, null, Html.p("muted", "You're on track today. Nice work."));
        }
        Element list = Html.div(null);
        for (DashboardService.Insight insight : d.insights()) {
            Element text = Html.div(null, Html.p(null, insight.text()));
            if (!insight.suggestions().isEmpty()) {
                Element chips = Html.div("suggestions");
                for (Food f : insight.suggestions()) {
                    chips.add(Html.a("/foods/" + f.getId(), "badge badge-outline", f.getName() + ", " + Fmt.amount(f.perServing().protein()) + " g protein"));
                }
                text.add(chips);
            }
            list.add(Html.div("insight", Icon.SPARK.svg(), text));
        }
        return Ui.panel("For you", null, null, list);
    }

    private Response water(Request req) {
        Params p = new Params(req);
        LocalDate date = p.date("date", web.today());
        Integer delta = p.integer("delta");
        web.services.water.change(req.user().getId(), date, delta == null ? 0 : Math.max(-1, Math.min(1, delta)));
        String back = com.nutriplanner.web.http.WebServer.safePath(req.param("_back"));
        return Response.redirect(back == null ? "/dashboard" : back);
    }
}
