package com.nutriplanner.web.view;

import com.nutriplanner.domain.Allergen;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodCategory;
import com.nutriplanner.domain.FoodDiet;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.PlanRole;
import com.nutriplanner.service.FoodService;
import com.nutriplanner.web.Params;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.ui.FormState;
import com.nutriplanner.web.ui.Forms;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;

/** The add/edit food form, used for members' custom foods and for the admin food database. */
public final class FoodForm {

    private FoodForm() {
    }

    public static FormState fill(Food f, FormState st) {
        st.setIfAbsent("name", f.getName()).setIfAbsent("category", f.getCategory()).setIfAbsent("diet", f.getDiet())
                .setIfAbsent("allergens", f.getAllergens()).setIfAbsent("mealTypes", f.getMealTypes())
                .setIfAbsent("planRole", f.getPlanRole()).setIfAbsent("kcal", f.getKcal()).setIfAbsent("protein", f.getProtein())
                .setIfAbsent("carbs", f.getCarbs()).setIfAbsent("fat", f.getFat()).setIfAbsent("fiber", f.getFiber())
                .setIfAbsent("sugar", f.getSugar()).setIfAbsent("sodiumMg", f.getSodiumMg())
                .setIfAbsent("servingGrams", f.getServingGrams()).setIfAbsent("servingLabel", f.getServingLabel());
        return st;
    }

    public static Element form(String action, String csrf, FormState st, String submitLabel, String cancelHref) {
        return Forms.post(action, csrf).cls("form").add(
                Forms.formMessage(st),
                Html.div("form-section",
                        Html.h(2, "form-section-title", "About the food"),
                        Forms.text(st, "name", "Name", "For example “Grandma's methi thepla”.", true),
                        Html.div("form-grid",
                                Forms.select(st, "category", "Category", Forms.Option.of(FoodCategory.values()), null, null),
                                Forms.select(st, "diet", "Diet", Forms.Option.of(FoodDiet.values()), null,
                                        "Vegetarian means it may contain dairy or honey, but no egg, meat or fish."),
                                Forms.text(st, "servingLabel", "Serving description", "Like “1 bowl” or “2 pieces”.", true),
                                Forms.number(st, "servingGrams", "Serving weight", "g", "0.1", "1", "2000", null, true))),
                Html.div("form-section",
                        Html.h(2, "form-section-title", "Nutrition per 100 g"),
                        Html.p("hint", "Copy these from the nutrition label. Calories are checked against protein, carbs and fat."),
                        Html.div("form-grid-3",
                                Forms.number(st, "kcal", "Energy", "kcal", "0.1", "0", "950", null, true),
                                Forms.number(st, "protein", "Protein", "g", "0.1", "0", "100", null, true),
                                Forms.number(st, "carbs", "Carbohydrate", "g", "0.1", "0", "100", null, true),
                                Forms.number(st, "fat", "Fat", "g", "0.1", "0", "100", null, true),
                                Forms.number(st, "fiber", "Fibre", "g", "0.1", "0", "100", null, true),
                                Forms.number(st, "sugar", "Sugar", "g", "0.1", "0", "100", null, true),
                                Forms.number(st, "sodiumMg", "Sodium", "mg", "1", "0", "40000", null, true))),
                Html.div("form-section",
                        Html.h(2, "form-section-title", "Allergens and meals"),
                        Forms.chips(st, "allergens", "Contains", Forms.Option.of(Allergen.values()), null),
                        Forms.chips(st, "mealTypes", "Good for", Forms.Option.of(MealType.values()),
                                "The meal planner only uses a food at the meals you tick here."),
                        Forms.select(st, "planRole", "In generated meal plans", Forms.Option.of(PlanRole.values()), null,
                                "Choose the part it plays on a plate, or leave it out of generated plans.")),
                Html.div("form-actions", Forms.submit(submitLabel, "btn-primary", Icon.CHECK), Ui.link(cancelHref, "Cancel", "btn-ghost", null)));
    }

    public static FoodService.FoodInput parse(Params p) {
        return new FoodService.FoodInput(p.text("name"), p.choice("category", FoodCategory.class), p.choice("diet", FoodDiet.class),
                p.choices("allergens", Allergen.class), p.choices("mealTypes", MealType.class), p.choice("planRole", PlanRole.class),
                p.decimal("kcal"), p.decimal("protein"), p.decimal("carbs"), p.decimal("fat"), p.decimal("fiber"), p.decimal("sugar"),
                p.decimal("sodiumMg"), p.decimal("servingGrams"), p.text("servingLabel"));
    }
}
