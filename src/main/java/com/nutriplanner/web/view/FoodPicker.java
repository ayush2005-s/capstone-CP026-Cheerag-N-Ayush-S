package com.nutriplanner.web.view;

import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodCategory;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.Nutrients;
import com.nutriplanner.domain.PortionUnit;
import com.nutriplanner.service.FoodService;
import com.nutriplanner.service.Page;
import com.nutriplanner.web.Params;
import com.nutriplanner.web.ViewContext;
import com.nutriplanner.web.Web;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.html.Node;
import com.nutriplanner.web.http.Request;
import com.nutriplanner.web.http.Response;
import com.nutriplanner.web.ui.FormState;
import com.nutriplanner.web.ui.Forms;
import com.nutriplanner.web.ui.Fmt;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** The "add a food" screen, shared by the diary (log what you ate) and the planner (plan what you'll eat). */
public final class FoodPicker {

    public enum Mode {
        DIARY("/diary/add", "/diary"),
        PLAN("/planner/add", "/planner");

        final String action;
        final String back;

        Mode(String action, String back) {
            this.action = action;
            this.back = back;
        }
    }

    private final Web web;

    public FoodPicker(Web web) {
        this.web = web;
    }

    public Response render(Request req, Mode mode, FormState st, int status) {
        ViewContext ctx = web.context(req);
        Params p = new Params(req);
        LocalDate date = p.date("date", ctx.today());
        MealType meal = p.choice("meal", MealType.class);
        if (meal == null) meal = MealType.BREAKFAST;
        String q = p.text("q");
        FoodCategory category = p.choice("category", FoodCategory.class);
        String backUrl = mode == Mode.DIARY ? "/diary?date=" + date : "/planner?day=" + date;
        String base = mode.action + "?date=" + date + "&meal=" + meal.name();

        Node chosen = Node.EMPTY;
        String foodParam = p.text("food");
        if (!foodParam.isEmpty()) {
            try {
                Food food = web.services.foods.visibleFood(ctx.userId(), Long.parseLong(foodParam));
                chosen = amountPanel(ctx, mode, food, date, meal, st);
            } catch (NumberFormatException e) {
                chosen = Node.EMPTY;
            }
        }

        Element search = Forms.get(mode.action).cls("filters").add(
                Forms.hidden("date", date.toString()), Forms.hidden("meal", meal.name()),
                Html.div("field search-field", new Element("label").cls("sr-only").attr("for", "q").text("Search foods"),
                        Html.div("search-wrap", Icon.SEARCH.svg(), new Element("input").cls("input search-input").id("q")
                                .attr("type", "search").attr("name", "q").attr("value", q).attr("placeholder", "Search, for example dal, paneer, banana")
                                .flag("autofocus", chosen == Node.EMPTY))),
                Html.div("field", Forms.bareSelect("category", Forms.Option.of(FoodCategory.values()),
                        category == null ? "" : category.name(), "All categories", "Category")),
                Html.div("field", Forms.submit("Search", "btn-secondary", null)));

        Element results;
        if (q.isEmpty() && category == null) {
            List<Food> recent = web.services.foods.recentFoods(ctx.userId(), 8);
            FoodService.Query suggested = new FoodService.Query("", null, null, meal, true, false, FoodService.Sort.NAME, 1, 500);
            List<Food> mealFoods = new java.util.ArrayList<>();
            for (Food f : web.services.foods.search(ctx.userId(), ctx.profile(), suggested).items()) {
                if (f.getPlanRole() != com.nutriplanner.domain.PlanRole.NONE) mealFoods.add(f);
            }
            java.util.Collections.shuffle(mealFoods, new java.util.Random(date.toEpochDay() + meal.ordinal()));
            Page<Food> forMeal = Page.of(mealFoods, 1, 12);
            results = Html.div("stack",
                    recent.isEmpty() ? Node.EMPTY : Ui.panel("Recently logged", null, null, list(ctx, mode, recent, date, meal, base)),
                    Ui.panel("Good for " + meal.label().toLowerCase(Locale.ROOT), "Foods that fit your diet and allergies", null,
                            list(ctx, mode, forMeal.items(), date, meal, base)));
        } else {
            FoodService.Query query = new FoodService.Query(q, category, null, null, false, false, FoodService.Sort.NAME, p.page(), 25);
            Page<Food> page = web.services.foods.search(ctx.userId(), ctx.profile(), query);
            String pageBase = base + "&q=" + enc(q) + (category == null ? "" : "&category=" + category.name());
            results = Ui.panel(page.total() + (page.total() == 1 ? " food found" : " foods found"), null,
                    Ui.link("/foods/new", "Add a custom food", "btn-ghost btn-sm", Icon.PLUS),
                    page.total() == 0
                            ? Ui.empty(Icon.SEARCH, "No foods match “" + q + "”", "Try a shorter word, or add it as a custom food with its nutrition label.",
                            Ui.link("/foods/new", "Add a custom food", "btn-primary", Icon.PLUS))
                            : list(ctx, mode, page.items(), date, meal, pageBase),
                    Ui.pager(page, n -> pageBase + "&page=" + n, "foods"));
        }

        String verb = mode == Mode.DIARY ? "Log food for " : "Plan food for ";
        Element mealTabs = Ui.segmented(List.of(
                tab(mode, date, MealType.BREAKFAST, meal, q), tab(mode, date, MealType.LUNCH, meal, q),
                tab(mode, date, MealType.DINNER, meal, q), tab(mode, date, MealType.SNACK, meal, q)), "Meal");
        return Layout.app(ctx, status, verb + meal.label().toLowerCase(Locale.ROOT), mode == Mode.DIARY ? Layout.Nav.DIARY : Layout.Nav.PLANNER,
                Ui.backLink(backUrl, mode == Mode.DIARY ? "Back to diary" : "Back to planner"),
                Ui.pageHead(verb + meal.label().toLowerCase(Locale.ROOT), Fmt.longDate(date), mealTabs),
                chosen, Html.div("panel mb-3", search), results);
    }

    private static Ui.Tab tab(Mode mode, LocalDate date, MealType m, MealType active, String q) {
        return new Ui.Tab(mode.action + "?date=" + date + "&meal=" + m.name() + (q.isEmpty() ? "" : "&q=" + enc(q)), m.label(), m == active);
    }

    private Element list(ViewContext ctx, Mode mode, List<Food> foods, LocalDate date, MealType meal, String base) {
        if (foods.isEmpty()) return Html.p("muted", "Nothing here yet.");
        Element ul = new Element("ul").cls("picker-list");
        for (Food f : foods) {
            Nutrients n = f.perServing();
            FoodService.Suitability fit = FoodService.assess(f, ctx.profile());
            Element info = Html.div("item-main",
                    Html.a(base + "&food=" + f.getId(), "item-name", Ui.dietMark(f.getDiet()), Html.span(null, f.getName())),
                    Html.span("item-meta", Fmt.serving(f) + ", "
                            + Fmt.kcal(n.kcal()) + " kcal, " + Fmt.amount(n.protein()) + " g protein"));
            if (!fit.suitable()) info.add(Html.div("tag-row", Ui.badge(fit.issues().get(0), "chili")));
            Element quick = Forms.post(mode.action, ctx.csrf(), base).cls("inline").add(
                    Forms.hidden("food", Long.toString(f.getId())), Forms.hidden("date", date.toString()),
                    Forms.hidden("meal", meal.name()), Forms.hidden("quantity", "1"), Forms.hidden("unit", PortionUnit.SERVING.name()),
                    Forms.submit("Add 1 serving", "btn-secondary btn-sm", Icon.PLUS));
            ul.add(Html.li("picker-item", info, Html.div("btn-row", Ui.link(base + "&food=" + f.getId(), "Choose amount", "btn-ghost btn-sm", null), quick)));
        }
        return ul;
    }

    private Element amountPanel(ViewContext ctx, Mode mode, Food food, LocalDate date, MealType meal, FormState st) {
        Nutrients n = food.perServing();
        FoodService.Suitability fit = FoodService.assess(food, ctx.profile());
        st.setIfAbsent("quantity", "1");
        st.setIfAbsent("unit", PortionUnit.SERVING);
        st.setIfAbsent("meal", meal);
        Element form = Forms.post(mode.action, ctx.csrf()).cls("form").add(
                Forms.hidden("food", Long.toString(food.getId())), Forms.hidden("date", date.toString()), Forms.formMessage(st));
        Element grid = Html.div("form-grid-3",
                Forms.number(st, "quantity", "Amount", null, "any", "0.1", null, null, true),
                mode == Mode.DIARY
                        ? Forms.select(st, "unit", "Unit", List.of(new Forms.Option("SERVING", "Servings of " + food.getServingLabel()),
                        new Forms.Option("GRAM", "Grams")), null, null)
                        : Html.div("field", Html.span("label", "Unit"), Html.p("input", "Servings of " + food.getServingLabel())),
                Forms.select(st, "meal", "Meal", Forms.Option.of(MealType.values()), null, null));
        form.add(grid);
        if (mode == Mode.PLAN) form.add(Forms.hidden("unit", PortionUnit.SERVING.name()));
        form.add(Html.div("form-actions", Forms.submit(mode == Mode.DIARY ? "Add to diary" : "Add to plan", "btn-primary", Icon.PLUS)));

        Element facts = Html.div("row",
                Ui.badge(Fmt.kcal(n.kcal()) + " kcal", null), Ui.badge(Fmt.amount(n.protein()) + " g protein", "beet"),
                Ui.badge(Fmt.amount(n.carbs()) + " g carbs", "turmeric"), Ui.badge(Fmt.amount(n.fat()) + " g fat", "blueberry"),
                Html.span("small muted", "per " + Fmt.serving(food)));
        Element panel = Ui.panel(food.getName(), food.getCategory().label(), Ui.link("/foods/" + food.getId(), "Nutrition details", "btn-ghost btn-sm", null),
                facts);
        if (!fit.suitable()) {
            panel.add(Html.div("mt-2", Ui.notice(Icon.ALERT, Html.p(null, "Heads up: " + String.join(", ", fit.issues()).toLowerCase(Locale.ROOT)
                    + ". You can still " + (mode == Mode.DIARY ? "log" : "plan") + " it."))));
        }
        panel.add(Html.div("mt-3", form));
        return Html.div("mb-3", panel);
    }

    static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
