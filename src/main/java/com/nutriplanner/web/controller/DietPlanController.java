package com.nutriplanner.web.controller;

import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.Goal;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.service.DietGuideService;
import com.nutriplanner.service.NutritionTargets;
import com.nutriplanner.web.ViewContext;
import com.nutriplanner.web.Web;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.http.Request;
import com.nutriplanner.web.http.Response;
import com.nutriplanner.web.http.Router;
import com.nutriplanner.web.ui.Fmt;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;
import com.nutriplanner.web.view.Layout;

import java.util.List;

import static com.nutriplanner.web.http.Router.Access.MEMBER;

/** The personalised diet plan: targets, how they split across meals, foods to favour, and guidance. */
public final class DietPlanController {

    private final Web web;

    public DietPlanController(Web web) {
        this.web = web;
    }

    public void register(Router r) {
        r.get("/plan", MEMBER, this::plan);
    }

    private Response plan(Request req) {
        ViewContext ctx = web.context(req);
        Profile p = ctx.profile();
        NutritionTargets t = web.services.profiles.targets(ctx.userId()).orElseThrow();
        DietGuideService.DietGuide g = web.services.guides.guide(ctx.userId(), p, t);

        String goalLine = p.getGoal() == Goal.MAINTAIN ? "Maintain about " + Fmt.one(p.getWeightKg()) + " kg"
                : p.getGoal().label() + " from " + Fmt.one(p.getWeightKg()) + " to " + Fmt.one(p.getTargetWeightKg()) + " kg";
        if (t.projectedGoalDate() != null && t.weeksToGoal() != null && t.weeksToGoal() > 0) {
            goalLine += ", in about " + Math.round(t.weeksToGoal()) + " weeks (around " + Fmt.mediumDate(t.projectedGoalDate()) + ")";
        }

        Element targets = Html.div("stats",
                Ui.stat("Calories", Fmt.kcal(t.calories()), "kcal", t.customCalories() ? "Your custom target" : "Maintenance is " + Fmt.kcal(t.tdee())),
                Ui.stat("Protein", Integer.toString(t.proteinG()), "g", t.macros().proteinPct() + "% of calories"),
                Ui.stat("Carbohydrate", Integer.toString(t.carbsG()), "g", t.macros().carbsPct() + "% of calories"),
                Ui.stat("Fat", Integer.toString(t.fatG()), "g", t.macros().fatPct() + "% of calories"),
                Ui.stat("Fibre", Integer.toString(t.fiberG()), "g", "14 g per 1,000 kcal"),
                Ui.stat("Water", Fmt.one(t.waterMl() / 1000.0), "L", (int) Math.ceil(t.waterMl() / 250.0) + " glasses"));

        Ui.Table split = new Ui.Table().caption("Targets per meal").col("Meal", false).col("Share", true).col("Calories", true)
                .col("Protein", true).col("Carbs", true).col("Fat", true);
        for (DietGuideService.MealShare m : g.meals()) {
            split.row(Html.div("row", Html.span("meal-icon", Ui.mealIcon(m.meal()).svg("icon icon-sm")), Html.strong(m.meal().label())),
                    Html.text(Math.round(m.meal().share() * 100) + "%"), Html.text(Fmt.kcal(m.kcal()) + " kcal"),
                    Html.text(m.proteinG() + " g"), Html.text(m.carbsG() + " g"), Html.text(m.fatG() + " g"));
        }

        Element guidance = Html.div("stack-sm");
        for (DietGuideService.Guidance item : g.guidance()) {
            guidance.add(Html.div("insight", Icon.CHECK_CIRCLE.svg(), Html.div(null, Html.h(3, "h3", item.title()), Html.p("muted mt-1", item.text()))));
        }

        Element avoid = g.avoiding().isEmpty() ? Html.p("muted", "No restrictions set. Every food in the database is available to you.")
                : new Element("ul").cls("bullets").add(g.avoiding().stream().map(s -> Html.li(null, s)).toList());

        return Layout.app(ctx, "Diet plan", Layout.Nav.PLAN,
                Ui.pageHead("Your diet plan", goalLine + ".",
                        Ui.link("/profile", "Change goals", "btn-secondary", Icon.TARGET),
                        Ui.link("/planner", "Open meal planner", "btn-primary", Icon.CALENDAR)),
                Html.div("stack",
                        targets,
                        t.calorieLimited() ? Ui.notice(Icon.INFO, Html.p(null, t.calorieNote())) : Html.span(null, ""),
                        Html.div("grid g-main",
                                Html.div("stack",
                                        Ui.panel("How your day splits", "Your calories and macros across four meals", null, split.build()),
                                        Ui.panel("Guidance for you", "Based on your goal, diet and activity", null, guidance)),
                                Html.div("stack",
                                        foodsPanel("Best protein sources for you", "Most protein per calorie", g.proteinPicks(), f -> Fmt.one(f.proteinPer100Kcal()) + " g per 100 kcal"),
                                        foodsPanel("Fibre-rich picks", "Most fibre per calorie", g.fiberPicks(), f -> Fmt.grams(f.perServing().fiber()) + " per serving"),
                                        foodsPanel("Healthy fats", "In moderation", g.fatPicks(), f -> Fmt.grams(f.perServing().fat()) + " fat per serving"),
                                        Ui.panel("What your plan avoids", null, Ui.link("/profile#diet", "Edit", "btn-ghost btn-sm", null), avoid))),
                        Html.p("small muted", "This plan is general guidance based on widely used equations. It isn't a substitute for advice from a doctor or "
                                + "registered dietitian, especially if you are pregnant, have a medical condition or take medication.")));
    }

    private static Element foodsPanel(String title, String sub, List<Food> foods, java.util.function.Function<Food, String> detail) {
        if (foods.isEmpty()) return Ui.panel(title, sub, null, Html.p("muted", "No matching foods yet."));
        Element list = new Element("ul").cls("list-plain");
        for (Food f : foods) {
            list.add(Html.li("row-between", Html.a("/foods/" + f.getId(), "item-name", Ui.dietMark(f.getDiet()), Html.span(null, f.getName())),
                    Html.span("small muted nowrap", detail.apply(f))));
        }
        return Ui.panel(title, sub, null, list);
    }
}
