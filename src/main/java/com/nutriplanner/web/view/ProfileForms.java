package com.nutriplanner.web.view;

import com.nutriplanner.domain.ActivityLevel;
import com.nutriplanner.domain.Allergen;
import com.nutriplanner.domain.DietType;
import com.nutriplanner.domain.DietaryRestriction;
import com.nutriplanner.domain.Goal;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.Sex;
import com.nutriplanner.service.ProfileService;
import com.nutriplanner.web.Params;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.ui.FormState;
import com.nutriplanner.web.ui.Forms;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Profile form sections shared by onboarding and the profile page. */
public final class ProfileForms {

    private ProfileForms() {
    }

    public static FormState fill(Profile p, FormState st) {
        st.setIfAbsent("sex", p.getSex());
        if (p.getBirthDate() != null) st.setIfAbsent("birthDate", p.getBirthDate().toString());
        if (p.getHeightCm() > 0) st.setIfAbsent("heightCm", p.getHeightCm());
        if (p.getWeightKg() > 0) st.setIfAbsent("weightKg", p.getWeightKg());
        if (p.getBodyFatPercent() != null) st.setIfAbsent("bodyFatPercent", p.getBodyFatPercent());
        st.setIfAbsent("goal", p.getGoal());
        if (p.getTargetWeightKg() != null && p.getGoal() != Goal.MAINTAIN) st.setIfAbsent("targetWeightKg", p.getTargetWeightKg());
        st.setIfAbsent("weeklyChangeKg", p.getWeeklyChangeKg() > 0 ? Double.valueOf(p.getWeeklyChangeKg()) : Double.valueOf(0.5));
        st.setIfAbsent("activityLevel", p.getActivityLevel());
        st.setIfAbsent("dietType", p.getDietType());
        st.setIfAbsent("allergens", p.getAllergens());
        st.setIfAbsent("restrictions", p.getRestrictions());
        st.setIfAbsent("bmrFormula", p.getBmrFormula());
        if (p.getCalorieOverride() != null) st.setIfAbsent("calorieOverride", p.getCalorieOverride());
        return st;
    }

    // ------------------------------------------------------------------ body

    public static Element bodyFields(FormState st, LocalDate today) {
        return Html.div("form",
                Forms.choices(st, "sex", "Sex", List.of(
                        new Forms.Choice(Sex.FEMALE.name(), "Female", null),
                        new Forms.Choice(Sex.MALE.name(), "Male", null)), "cols-2"),
                Html.p("hint", "Used only for the metabolic-rate formulas, which differ for male and female bodies."),
                Html.div("form-grid",
                        Forms.date(st, "birthDate", "Date of birth", "You need to be 16 or older.", true,
                                today.minusYears(100).toString(), today.minusYears(16).toString()),
                        Forms.number(st, "heightCm", "Height", "cm", "0.1", "100", "250", null, true),
                        Forms.number(st, "weightKg", "Current weight", "kg", "0.1", "25", "350", null, true),
                        Forms.number(st, "bodyFatPercent", "Body fat", "%", "0.1", "3", "70",
                                "If you know it. Enables the Katch–McArdle formula.", false)));
    }

    public static ProfileService.BodyDetails parseBody(Params p) {
        return new ProfileService.BodyDetails(p.choice("sex", Sex.class), p.date("birthDate"), p.decimal("heightCm"),
                p.decimal("weightKg"), p.decimal("bodyFatPercent"));
    }

    // ------------------------------------------------------------------ goal

    public static Element goalFields(FormState st) {
        List<Forms.Choice> goals = new ArrayList<>();
        for (Goal g : Goal.values()) goals.add(new Forms.Choice(g.name(), g.label(), g.description()));
        List<Forms.Choice> levels = new ArrayList<>();
        for (ActivityLevel a : ActivityLevel.values()) levels.add(new Forms.Choice(a.name(), a.label(), a.description()));
        List<Forms.Option> pace = List.of(
                new Forms.Option("0.1", "0.1 kg a week (very gradual)"),
                new Forms.Option("0.25", "0.25 kg a week (gentle)"),
                new Forms.Option("0.5", "0.5 kg a week (steady)"),
                new Forms.Option("0.75", "0.75 kg a week (faster, losing only)"),
                new Forms.Option("1", "1 kg a week (ambitious, losing only)"));
        return Html.div("form",
                Forms.choices(st, "goal", "Your goal", goals, "cols-2"),
                Html.div("form-grid",
                        Forms.number(st, "targetWeightKg", "Target weight", "kg", "0.1", "25", "350",
                                "Not needed if you're maintaining.", false),
                        Forms.select(st, "weeklyChangeKg", "Pace", pace, null,
                                "Gaining or building muscle works best at 0.5 kg a week or less.")),
                Forms.choices(st, "activityLevel", "How active are you?", levels, "cols-2"));
    }

    public static ProfileService.GoalDetails parseGoal(Params p) {
        return new ProfileService.GoalDetails(p.choice("goal", Goal.class), p.decimal("targetWeightKg"),
                p.decimal("weeklyChangeKg"), p.choice("activityLevel", ActivityLevel.class));
    }

    // ------------------------------------------------------------------ diet

    public static Element dietFields(FormState st) {
        List<Forms.Choice> diets = new ArrayList<>();
        for (DietType d : DietType.values()) diets.add(new Forms.Choice(d.name(), d.label(), d.description()));
        List<Forms.Option> restrictions = new ArrayList<>();
        for (DietaryRestriction r : DietaryRestriction.values()) restrictions.add(new Forms.Option(r.name(), r.label()));
        return Html.div("form",
                Forms.choices(st, "dietType", "How do you eat?", diets, "cols-2"),
                Forms.chips(st, "allergens", "Allergies and intolerances", Forms.Option.of(Allergen.values()),
                        "Foods containing these are left out of your plans and flagged everywhere else."),
                Forms.chips(st, "restrictions", "Dietary restrictions", restrictions,
                        "Low sodium: under 400 mg per 100 g. Low sugar: under 15 g. Low carb: under 35 g. Low fat: under 20 g."));
    }

    public static ProfileService.DietDetails parseDiet(Params p) {
        return new ProfileService.DietDetails(p.choice("dietType", DietType.class), p.choices("allergens", Allergen.class),
                p.choices("restrictions", DietaryRestriction.class));
    }
}
