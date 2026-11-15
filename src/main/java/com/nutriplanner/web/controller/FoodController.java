package com.nutriplanner.web.controller;

import com.nutriplanner.domain.Allergen;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodCategory;
import com.nutriplanner.domain.FoodDiet;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.Nutrients;
import com.nutriplanner.domain.PlanRole;
import com.nutriplanner.domain.PortionUnit;
import com.nutriplanner.service.FoodService;
import com.nutriplanner.service.NotFoundException;
import com.nutriplanner.service.Page;
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
import com.nutriplanner.web.ui.Charts;
import com.nutriplanner.web.ui.FormState;
import com.nutriplanner.web.ui.Forms;
import com.nutriplanner.web.ui.Fmt;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;
import com.nutriplanner.web.view.FoodForm;
import com.nutriplanner.web.view.Layout;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import static com.nutriplanner.web.http.Router.Access.MEMBER;

/** Searching the food database, food details, and members' own custom foods. */
public final class FoodController {

    private final Web web;

    public FoodController(Web web) {
        this.web = web;
    }

    public void register(Router r) {
        r.get("/foods", MEMBER, this::list);
        r.get("/foods/new", MEMBER, req -> formPage(req, null, blankState(), 200));
        r.post("/foods/new", MEMBER, this::create);
        r.get("/foods/{id}", MEMBER, this::detail);
        r.get("/foods/{id}/edit", MEMBER, this::edit);
        r.post("/foods/{id}/edit", MEMBER, this::update);
        r.post("/foods/{id}/delete", MEMBER, this::delete);
    }

    // ------------------------------------------------------------------ search

    private Response list(Request req) {
        ViewContext ctx = web.context(req);
        Params p = new Params(req);
        String q = p.text("q");
        FoodCategory category = p.choice("category", FoodCategory.class);
        FoodDiet diet = p.choice("diet", FoodDiet.class);
        MealType meal = p.choice("meal", MealType.class);
        FoodService.Sort sort = p.choice("sort", FoodService.Sort.class);
        if (sort == null) sort = FoodService.Sort.NAME;
        boolean suitable = p.flag("suitable");
        boolean mine = p.flag("mine");
        boolean per100 = "100g".equals(p.text("basis"));
        Page<Food> page = web.services.foods.search(ctx.userId(), ctx.profile(),
                new FoodService.Query(q, category, diet, meal, suitable, mine, sort, p.page(), 20));

        Element filters = Forms.get("/foods").cls("panel mb-3").add(Html.div("filters",
                Html.div("field search-field", new Element("label").cls("label").attr("for", "q").text("Search"),
                        Html.div("search-wrap", Icon.SEARCH.svg(), new Element("input").cls("input search-input").id("q")
                                .attr("type", "search").attr("name", "q").attr("value", q).attr("placeholder", "Name, like dal or paneer"))),
                labelled("Category", Forms.bareSelect("category", Forms.Option.of(FoodCategory.values()), category == null ? "" : category.name(), "All", "Category")),
                labelled("Diet", Forms.bareSelect("diet", Forms.Option.of(FoodDiet.values()), diet == null ? "" : diet.name(), "Any", "Diet")),
                labelled("Meal", Forms.bareSelect("meal", Forms.Option.of(MealType.values()), meal == null ? "" : meal.name(), "Any", "Meal")),
                labelled("Sort by", Forms.bareSelect("sort", Forms.Option.of(FoodService.Sort.values()), sort.name(), null, "Sort"))),
                Html.div("row-between mt-2",
                        Html.div("row",
                                checkbox("suitable", suitable, "Only foods that fit my diet and allergies"),
                                checkbox("mine", mine, "Only my custom foods"),
                                Forms.hidden("basis", per100 ? "100g" : "serving")),
                        Forms.submit("Apply filters", "btn-primary", Icon.FILTER)));

        String query = "q=" + enc(q) + (category == null ? "" : "&category=" + category) + (diet == null ? "" : "&diet=" + diet)
                + (meal == null ? "" : "&meal=" + meal) + "&sort=" + sort + (suitable ? "&suitable=on" : "") + (mine ? "&mine=on" : "");
        Element basis = Ui.segmented(List.of(
                new Ui.Tab("/foods?" + query + "&basis=serving", "Per serving", !per100),
                new Ui.Tab("/foods?" + query + "&basis=100g", "Per 100 g", per100)), "Show nutrition");

        Node body;
        if (page.total() == 0) {
            body = Ui.empty(Icon.SEARCH, "No foods match these filters", "Clear a filter or search for something shorter. "
                    + "You can also add a food yourself from its nutrition label.", Ui.link("/foods/new", "Add a custom food", "btn-primary", Icon.PLUS));
        } else {
            Ui.Table table = new Ui.Table().caption("Foods").col("Food", false).col(per100 ? "Amount" : "Serving", false)
                    .col("Energy", true).col("Protein", true).col("Carbs", true).col("Fat", true).col("Fibre", true);
            for (Food f : page.items()) {
                Nutrients n = per100 ? f.per100g() : f.perServing();
                FoodService.Suitability fit = FoodService.assess(f, ctx.profile());
                Element name = Html.div(null, Html.a("/foods/" + f.getId(), "row-link", f.getName()));
                Element sub = Html.span("cell-sub", f.getCategory().label());
                Element cell = Html.div("row", Ui.dietMark(f.getDiet()), name.add(sub));
                if (!f.isGlobal()) name.add(Html.text(" "), Ui.badge("Custom", "blueberry"));
                if (!fit.suitable()) sub.add(Html.text(". "), Html.span("over", fit.issues().get(0)));
                table.row(cell, Html.text(per100 ? "100 g" : Fmt.serving(f)),
                        Html.text(Fmt.kcal(n.kcal()) + " kcal"), Html.text(Fmt.grams(n.protein())), Html.text(Fmt.grams(n.carbs())),
                        Html.text(Fmt.grams(n.fat())), Html.text(Fmt.grams(n.fiber())));
            }
            String pageBase = "/foods?" + query + "&basis=" + (per100 ? "100g" : "serving");
            body = Html.fragment(table.build(), Ui.pager(page, n -> pageBase + "&page=" + n, "foods"));
        }
        return Layout.app(ctx, "Foods", Layout.Nav.FOODS,
                Ui.pageHead("Foods", "Nutrition for " + web.services.foods.visibleFoods(ctx.userId()).size()
                                + " foods, with your diet and allergies flagged.",
                        Ui.link("/foods/new", "Add a custom food", "btn-primary", Icon.PLUS)),
                filters, Ui.panel(page.total() + (page.total() == 1 ? " food" : " foods"), null, basis, body));
    }

    private static Element labelled(String label, Element control) {
        return Html.div("field", Html.span("label", label), control);
    }

    private static Element checkbox(String name, boolean checked, String label) {
        return new Element("label").cls("check").add(new Element("input").attr("type", "checkbox").attr("name", name).attr("value", "on")
                .flag("checked", checked), Html.span(null, label));
    }

    // ------------------------------------------------------------------ detail

    private Response detail(Request req) {
        ViewContext ctx = web.context(req);
        Food food = web.services.foods.visibleFood(ctx.userId(), req.pathId("id"));
        Nutrients s = food.perServing();
        Nutrients h = food.per100g();
        FoodService.Suitability fit = FoodService.assess(food, ctx.profile());

        Element label = Html.div("nutrition-label",
                Html.div("nl-title", "Nutrition"),
                Html.div("nl-row head", Html.span(null, ""), Html.span(null, Fmt.serving(food)),
                        Html.span(null, "100 g")),
                row("Energy", Fmt.kcal(s.kcal()) + " kcal", Fmt.kcal(h.kcal()) + " kcal", "major"),
                row("Protein", Fmt.grams(s.protein()), Fmt.grams(h.protein()), null),
                row("Carbohydrate", Fmt.grams(s.carbs()), Fmt.grams(h.carbs()), null),
                row("of which sugar", Fmt.grams(s.sugar()), Fmt.grams(h.sugar()), "sub"),
                row("of which fibre", Fmt.grams(s.fiber()), Fmt.grams(h.fiber()), "sub"),
                row("Fat", Fmt.grams(s.fat()), Fmt.grams(h.fat()), null),
                row("Sodium", Fmt.kcal(s.sodiumMg()) + " mg", Fmt.kcal(h.sodiumMg()) + " mg", null));

        double[] split = s.energySplit();
        Element energy = Ui.panel("Energy split", "Share of calories from each macronutrient", null,
                Html.div("split-row", Html.div("donut-wrap", Charts.macroDonut(s.protein(), s.carbs(), s.fat(), Fmt.kcal(s.kcal()), "kcal")),
                        Html.div("stack-sm grow",
                                share("Protein", "sw-protein", "tone-protein", split[0], s.protein()),
                                share("Carbohydrate", "sw-carbs", "tone-carbs", split[1], s.carbs()),
                                share("Fat", "sw-fat", "tone-fat", split[2], s.fat()),
                                Html.p("small muted", Fmt.one(food.proteinPer100Kcal()) + " g protein per 100 kcal"))));

        Element tags = Html.div("tag-row", Ui.dietMark(food.getDiet()), Ui.badge(food.getDiet().label(), null));
        for (Allergen a : food.getAllergens()) tags.add(Ui.badge("Contains " + a.label().toLowerCase(Locale.ROOT), "turmeric"));
        for (MealType m : food.getMealTypes()) tags.add(Ui.badge(m.label(), "outline"));
        Element suit = fit.suitable()
                ? Html.div("flash flash-success", Icon.CHECK_CIRCLE.svg(), Html.span(null, "Fits your " + ctx.profile().getDietType().label().toLowerCase(Locale.ROOT)
                + " diet, allergies and restrictions."))
                : Html.div("flash flash-error", Icon.ALERT.svg(), Html.span(null, "Not a match for you: " + String.join(", ", fit.issues()).toLowerCase(Locale.ROOT) + "."));

        FormState diaryState = FormState.empty().set("quantity", "1").set("unit", PortionUnit.SERVING)
                .set("meal", DashboardController.mealForNow(java.time.LocalTime.now(web.clock))).set("date", ctx.today().toString());
        Element logForm = Forms.post("/diary/add", ctx.csrf(), "/foods/" + food.getId()).cls("form").add(
                Forms.hidden("food", Long.toString(food.getId())),
                Html.div("form-grid",
                        Forms.number(diaryState, "quantity", "Amount", null, "any", "0.1", null, null, true),
                        Forms.select(diaryState, "unit", "Unit", List.of(new Forms.Option("SERVING", "Servings"), new Forms.Option("GRAM", "Grams")), null, null),
                        Forms.select(diaryState, "meal", "Meal", Forms.Option.of(MealType.values()), null, null),
                        Forms.date(diaryState, "date", "Date", null, true, null, ctx.today().toString())),
                Forms.submit("Add to diary", "btn-primary", Icon.PLUS));
        FormState planState = FormState.empty().set("quantity", "1").set("meal", diaryState.value("meal")).set("date", ctx.today().plusDays(1).toString());
        Element planForm = Forms.post("/planner/add", ctx.csrf(), "/foods/" + food.getId()).cls("form").add(
                Forms.hidden("food", Long.toString(food.getId())), Forms.hidden("unit", "SERVING"),
                Html.div("form-grid",
                        Forms.number(planState, "quantity", "Servings", null, "0.25", "0.25", "20", null, true),
                        Forms.select(planState, "meal", "Meal", Forms.Option.of(MealType.values()), null, null),
                        Forms.date(planState, "date", "Day", null, true, ctx.today().minusDays(60).toString(), ctx.today().plusDays(90).toString())),
                Forms.submit("Add to meal plan", "btn-secondary", Icon.CALENDAR));

        Node[] actions = new Node[0];
        Node confirm = Node.EMPTY;
        if (food.getOwnerId() == ctx.userId()) {
            String id = "delete-food";
            confirm = Ui.confirm(id, "Delete " + food.getName() + "?", "It will disappear from search and plans. Entries already in your diary stay as they are.",
                    Forms.post("/foods/" + food.getId() + "/delete", ctx.csrf()).cls("inline").add(Forms.submit("Delete food", "btn-danger", Icon.TRASH)));
            actions = new Node[]{Ui.link("/foods/" + food.getId() + "/edit", "Edit", "btn-secondary", Icon.EDIT),
                    Ui.confirmTrigger(id, "Delete", "btn-danger-ghost", Icon.TRASH)};
        } else if (ctx.isAdmin()) {
            actions = new Node[]{Ui.link("/admin/foods/" + food.getId() + "/edit", "Edit in admin", "btn-secondary", Icon.EDIT)};
        }

        return Layout.app(ctx, food.getName(), Layout.Nav.FOODS,
                Ui.backLink("/foods", "All foods"),
                Ui.pageHead(food.getName(), food.getCategory().label() + (food.isGlobal() ? "" : ". A custom food only you can see."), actions),
                Html.div("grid g-main",
                        Html.div("stack", tags, suit, label, energy),
                        Html.div("stack", Ui.panel("Log it", "Add this food to your diary.", null, logForm),
                                Ui.panel("Plan it", "Put it in a future meal.", null, planForm))),
                confirm);
    }

    private static Element share(String name, String swatch, String tone, double fraction, double grams) {
        return Html.div("macro", Html.div("macro-top",
                        Html.span("macro-name", Html.span("swatch " + swatch, ""), Html.text(name)),
                        Html.span("macro-val", Html.text(Fmt.percent(fraction) + " "), Html.span("of", Fmt.grams(grams)))),
                Ui.meter(fraction * 100, 100, tone));
    }

    private static Element row(String name, String serving, String per100, String cls) {
        return Html.div("nl-row" + (cls == null ? "" : " " + cls), Html.span(null, name), Html.span(null, serving), Html.span(null, per100));
    }

    // ------------------------------------------------------------- custom foods

    /** A new food starts with sensible choices but empty nutrition boxes. */
    public static FormState blankState() {
        return FormState.empty().set("category", FoodCategory.PREPARED).set("diet", FoodDiet.VEGETARIAN)
                .set("planRole", PlanRole.NONE).set("servingLabel", "1 serving").set("servingGrams", "100")
                .set("fiber", "0").set("sugar", "0").set("sodiumMg", "0").set("mealTypes", EnumSet.noneOf(MealType.class));
    }

    private Response formPage(Request req, Food existing, FormState st, int status) {
        ViewContext ctx = web.context(req);
        String action = existing == null ? "/foods/new" : "/foods/" + existing.getId() + "/edit";
        String cancel = existing == null ? "/foods" : "/foods/" + existing.getId();
        return Layout.app(ctx, status, existing == null ? "Add a custom food" : "Edit " + existing.getName(), Layout.Nav.FOODS,
                Ui.backLink(cancel, existing == null ? "All foods" : existing.getName()),
                Ui.pageHead(existing == null ? "Add a custom food" : "Edit " + existing.getName(),
                        "Custom foods are private to you. They appear in search, the diary and, if you choose, your meal plans."),
                Html.div("panel", FoodForm.form(action, ctx.csrf(), st, existing == null ? "Save food" : "Save changes", cancel)));
    }

    private Response create(Request req) {
        Params p = new Params(req);
        FoodService.FoodInput input = FoodForm.parse(p);
        try {
            if (p.hasErrors()) throw new ValidationException(p.errors());
            Food food = web.services.foods.createCustom(req.user().getId(), input);
            req.session().success("Added " + food.getName() + " to your foods.");
            return Response.redirect("/foods/" + food.getId());
        } catch (ValidationException e) {
            return formPage(req, null, FormState.from(req).errors(e.fieldErrors()).message(e.fieldErrors().isEmpty() ? e.getMessage() : null), 422);
        }
    }

    private Food ownFood(Request req) {
        Food food = web.services.foods.visibleFood(req.user().getId(), req.pathId("id"));
        if (food.getOwnerId() != req.user().getId()) throw new NotFoundException("You can only edit foods you added.");
        return food;
    }

    private Response edit(Request req) {
        Food food = ownFood(req);
        return formPage(req, food, FoodForm.fill(food, FormState.empty()), 200);
    }

    private Response update(Request req) {
        Food food = ownFood(req);
        Params p = new Params(req);
        FoodService.FoodInput input = FoodForm.parse(p);
        try {
            if (p.hasErrors()) throw new ValidationException(p.errors());
            web.services.foods.updateCustom(req.user().getId(), food.getId(), input);
            req.session().success("Saved " + input.name() + ".");
            return Response.redirect("/foods/" + food.getId());
        } catch (ValidationException e) {
            return formPage(req, food, FormState.from(req).errors(e.fieldErrors()).message(e.fieldErrors().isEmpty() ? e.getMessage() : null), 422);
        }
    }

    private Response delete(Request req) {
        Food food = ownFood(req);
        web.services.foods.deleteCustom(req.user().getId(), food.getId());
        req.session().success("Deleted " + food.getName() + ".");
        return Response.redirect("/foods?mine=on");
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
