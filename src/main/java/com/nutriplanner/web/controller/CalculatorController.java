package com.nutriplanner.web.controller;

import com.nutriplanner.domain.ActivityLevel;
import com.nutriplanner.domain.BmiCategory;
import com.nutriplanner.domain.BmrFormula;
import com.nutriplanner.domain.Goal;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.Sex;
import com.nutriplanner.service.NutritionCalculator;
import com.nutriplanner.service.NutritionTargets;
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
import com.nutriplanner.web.view.Layout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.nutriplanner.web.http.Router.Access.PUBLIC;

/**
 * BMI, BMR, TDEE and calorie/macro calculators. They are open to visitors too; signed-in members get their own
 * measurements filled in.
 */
public final class CalculatorController {

    private final Web web;

    public CalculatorController(Web web) {
        this.web = web;
    }

    public void register(Router r) {
        r.get("/calculators", PUBLIC, this::page);
    }

    /** Inputs after parsing and unit conversion. */
    private record Inputs(Sex sex, Integer age, Double heightCm, Double weightKg, Double bodyFat, ActivityLevel activity,
                          Goal goal, Double pace, BmrFormula formula, boolean imperial) {
    }

    private Response page(Request req) {
        ViewContext ctx = web.context(req);
        Params p = new Params(req);
        String tool = p.text("tool");
        if (!List.of("bmi", "bmr", "tdee", "macros").contains(tool)) tool = "bmi";
        boolean imperial = "imperial".equals(p.text("units"));
        boolean submitted = req.param("calc") != null;

        FormState st = FormState.empty();
        req.queryFields().forEach((k, v) -> st.set(k, v.isEmpty() ? "" : v.get(0)));
        Profile profile = ctx.profile();
        if (!submitted && profile != null && profile.hasBodyMetrics()) {
            st.setIfAbsent("sex", profile.getSex()).setIfAbsent("age", profile.age(ctx.today()))
                    .setIfAbsent("activity", profile.getActivityLevel()).setIfAbsent("goal", profile.getGoal())
                    .setIfAbsent("pace", profile.getWeeklyChangeKg() > 0 ? profile.getWeeklyChangeKg() : 0.5)
                    .setIfAbsent("formula", profile.getBmrFormula());
            if (profile.getBodyFatPercent() != null) st.setIfAbsent("bodyFat", profile.getBodyFatPercent());
            if (imperial) {
                double inches = profile.getHeightCm() / 2.54;
                st.setIfAbsent("heightFt", (int) (inches / 12)).setIfAbsent("heightIn", Math.round(inches % 12))
                        .setIfAbsent("weightLb", Math.round(profile.getWeightKg() * 2.20462));
            } else {
                st.setIfAbsent("heightCm", profile.getHeightCm()).setIfAbsent("weightKg", profile.getWeightKg());
            }
        }
        st.setIfAbsent("sex", Sex.FEMALE).setIfAbsent("activity", ActivityLevel.LIGHT).setIfAbsent("goal", Goal.MAINTAIN)
                .setIfAbsent("pace", "0.5").setIfAbsent("formula", BmrFormula.MIFFLIN_ST_JEOR);

        Node result = Node.EMPTY;
        Inputs in = null;
        if (submitted || (profile != null && profile.hasBodyMetrics())) {
            Params values = new Params(req);
            in = parse(values, st, imperial);
            Map<String, String> errors = validate(in, tool);
            errors.putAll(values.errors());
            if (!errors.isEmpty() && submitted) {
                st.errors(errors);
                in = null;
            } else if (!errors.isEmpty()) {
                in = null;
            }
        }
        if (in != null) {
            switch (tool) {
                case "bmr": result = bmr(in); break;
                case "tdee": result = tdee(in); break;
                case "macros": result = macros(in, ctx); break;
                default: result = bmi(in);
            }
        }

        String t = tool;
        Element tabs = Ui.tabs(List.of(
                new Ui.Tab(link("bmi", imperial), "BMI", t.equals("bmi")),
                new Ui.Tab(link("bmr", imperial), "BMR", t.equals("bmr")),
                new Ui.Tab(link("tdee", imperial), "TDEE", t.equals("tdee")),
                new Ui.Tab(link("macros", imperial), "Calories and macros", t.equals("macros"))));
        Element units = Ui.segmented(List.of(new Ui.Tab(link(tool, false), "Metric", !imperial), new Ui.Tab(link(tool, true), "Imperial", imperial)), "Units");

        Element form = Forms.get("/calculators").cls("form").add(Forms.hidden("tool", tool), Forms.hidden("calc", "1"),
                Forms.hidden("units", imperial ? "imperial" : "metric"));
        if (!tool.equals("bmi")) {
            form.add(Forms.choices(st, "sex", "Sex", List.of(new Forms.Choice("FEMALE", "Female", null), new Forms.Choice("MALE", "Male", null)), "cols-2"));
            form.add(Forms.number(st, "age", "Age", "years", "1", "16", "100", null, true));
        }
        if (imperial) {
            form.add(Html.div("form-grid", Forms.number(st, "heightFt", "Height", "ft", "1", "3", "8", null, true),
                    Forms.number(st, "heightIn", "and", "in", "0.5", "0", "11.5", null, true)));
            form.add(Forms.number(st, "weightLb", "Weight", "lb", "0.1", "55", "770", null, true));
        } else {
            form.add(Html.div("form-grid", Forms.number(st, "heightCm", "Height", "cm", "0.1", "100", "250", null, true),
                    Forms.number(st, "weightKg", "Weight", "kg", "0.1", "25", "350", null, true)));
        }
        if (!tool.equals("bmi")) {
            form.add(Forms.number(st, "bodyFat", "Body fat", "%", "0.1", "3", "70", "Optional. Needed for Katch–McArdle.", false));
            form.add(Forms.select(st, "formula", "Formula", Forms.Option.of(BmrFormula.values()), null, null));
        }
        if (tool.equals("tdee") || tool.equals("macros")) {
            form.add(Forms.select(st, "activity", "Activity level", activityOptions(), null, null));
        }
        if (tool.equals("macros")) {
            form.add(Html.div("form-grid", Forms.select(st, "goal", "Goal", Forms.Option.of(Goal.values()), null, null),
                    Forms.select(st, "pace", "Pace", List.of(new Forms.Option("0.25", "0.25 kg a week"), new Forms.Option("0.5", "0.5 kg a week"),
                            new Forms.Option("0.75", "0.75 kg a week"), new Forms.Option("1", "1 kg a week")), null, "Ignored when maintaining.")));
        }
        form.add(Forms.submit("Calculate", "btn-primary", Icon.CALCULATOR));

        Node intro = Html.p("lead", description(tool));
        Element body = Html.div("grid g-side", Ui.panel("Your details", null, units, form),
                result == Node.EMPTY ? Ui.empty(Icon.CALCULATOR, "Enter your details", "Results appear here, with what they mean.", null) : result);
        Element[] content = {Ui.pageHead("Calculators", "Evidence-based estimates for body size and energy needs."), tabs, Html.div("mb-3", intro), body};
        if (ctx.user() != null) return Layout.app(ctx, "Calculators", Layout.Nav.CALCULATORS, content);
        return Layout.site(ctx, 200, "Calculators", content);
    }

    private static String link(String tool, boolean imperial) {
        return "/calculators?tool=" + tool + (imperial ? "&units=imperial" : "");
    }

    private static List<Forms.Option> activityOptions() {
        List<Forms.Option> out = new ArrayList<>();
        for (ActivityLevel a : ActivityLevel.values()) out.add(new Forms.Option(a.name(), a.label() + ": " + a.description().toLowerCase()));
        return out;
    }

    private static String description(String tool) {
        switch (tool) {
            case "bmr": return "Basal metabolic rate is the energy your body uses at complete rest: breathing, circulation and keeping warm.";
            case "tdee": return "Total daily energy expenditure is your BMR plus everything you do. Eating this much keeps your weight steady.";
            case "macros": return "Your daily calorie target for a goal, split into protein, carbohydrate and fat, with fibre and water.";
            default: return "Body mass index compares weight with height. It's a quick screening number, not a diagnosis: it can't tell muscle from fat.";
        }
    }

    private static Inputs parse(Params p, FormState st, boolean imperial) {
        Double height, weight;
        if (imperial) {
            Double ft = decimal(p, st, "heightFt"), inch = decimal(p, st, "heightIn"), lb = decimal(p, st, "weightLb");
            height = ft == null ? null : (ft * 12 + (inch == null ? 0 : inch)) * 2.54;
            weight = lb == null ? null : lb / 2.20462;
        } else {
            height = decimal(p, st, "heightCm");
            weight = decimal(p, st, "weightKg");
        }
        Double age = decimal(p, st, "age");
        return new Inputs(enumOf(st.value("sex"), Sex.class), age == null ? null : (int) Math.round(age), height, weight,
                decimal(p, st, "bodyFat"), enumOf(st.value("activity"), ActivityLevel.class), enumOf(st.value("goal"), Goal.class),
                decimal(p, st, "pace"), enumOf(st.value("formula"), BmrFormula.class), imperial);
    }

    private static Double decimal(Params p, FormState st, String name) {
        String v = st.value(name).replace(',', '.').trim();
        if (v.isEmpty()) return null;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            st.error(name, "Enter a number.");
            return null;
        }
    }

    private static <E extends Enum<E>> E enumOf(String v, Class<E> type) {
        try {
            return v == null || v.isEmpty() ? null : Enum.valueOf(type, v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Map<String, String> validate(Inputs in, String tool) {
        Map<String, String> e = new LinkedHashMap<>();
        String h = in.imperial() ? "heightFt" : "heightCm", w = in.imperial() ? "weightLb" : "weightKg";
        if (in.heightCm() == null || in.heightCm() < 100 || in.heightCm() > 250) e.put(h, "Enter a height between 100 and 250 cm (3′3″ to 8′2″).");
        if (in.weightKg() == null || in.weightKg() < 25 || in.weightKg() > 350) e.put(w, "Enter a weight between 25 and 350 kg (55 to 770 lb).");
        if (!tool.equals("bmi")) {
            if (in.sex() == null) e.put("sex", "Choose one.");
            if (in.age() == null || in.age() < 16 || in.age() > 100) e.put("age", "These formulas are for ages 16 to 100.");
            if (in.bodyFat() != null && (in.bodyFat() < 3 || in.bodyFat() > 70)) e.put("bodyFat", "Body fat should be between 3% and 70%.");
        }
        return e;
    }

    // ------------------------------------------------------------------ results

    private static Element bmi(Inputs in) {
        double bmi = NutritionCalculator.bmi(in.weightKg(), in.heightCm());
        BmiCategory c = BmiCategory.of(bmi);
        double[] range = NutritionCalculator.healthyWeightRange(in.heightCm());
        String tone = c == BmiCategory.NORMAL ? "jade" : c == BmiCategory.UNDERWEIGHT ? "blueberry" : c == BmiCategory.OVERWEIGHT ? "turmeric" : "chili";
        String rangeText = in.imperial()
                ? Math.round(range[0] * 2.20462) + "–" + Math.round(range[1] * 2.20462) + " lb"
                : Fmt.one(range[0]) + "–" + Fmt.one(range[1]) + " kg";
        Ui.Table table = new Ui.Table().caption("BMI categories").col("Category", false).col("BMI", true);
        for (BmiCategory b : BmiCategory.values()) {
            table.row(b == c ? Html.strong(b.label()) : Html.text(b.label()), Html.text(b.range()));
        }
        return Ui.panel("Your BMI", null, null,
                Html.div("row", Html.span("display-num h1", Fmt.one(bmi)), Ui.badge(c.label(), tone)),
                Html.div("mt-3", Charts.bmiGauge(bmi)),
                Html.p("mt-2", "A healthy weight for your height is " + rangeText + " (BMI 18.5 to 24.9)."),
                Html.p("small muted mt-1", "Some guidelines for South Asian adults suggest lower thresholds: overweight from 23 and obesity from 27.5."),
                Html.div("mt-3", table.build()));
    }

    private static double bmrOf(BmrFormula f, Inputs in) {
        return NutritionCalculator.bmr(f, in.sex(), in.weightKg(), in.heightCm(), in.age(), in.bodyFat());
    }

    private static Element bmr(Inputs in) {
        BmrFormula chosen = NutritionCalculator.effectiveFormula(in.formula(), in.bodyFat());
        double value = bmrOf(chosen, in);
        Ui.Table table = new Ui.Table().caption("BMR by formula").col("Formula", false).col("BMR", true);
        for (BmrFormula f : BmrFormula.values()) {
            if (f == BmrFormula.KATCH_MCARDLE && in.bodyFat() == null) {
                table.row(Html.div(null, Html.text(f.label()), Html.span("cell-sub", "Add your body fat to use this")), Html.span("muted", "–"));
                continue;
            }
            Element name = Html.div(null, f == chosen ? Html.strong(f.label()) : Html.text(f.label()), Html.span("cell-sub", f.description()));
            table.row(name, Html.text(Fmt.kcal(bmrOf(f, in)) + " kcal"));
        }
        Element panel = Ui.panel("Your basal metabolic rate", chosen.label(), null,
                Html.div("row", Html.span("display-num h1", Fmt.kcal(value)), Html.span("unit", "kcal a day")),
                Html.p("mt-2 muted", "That's what your body burns doing nothing at all. Your real daily needs are higher; see TDEE."),
                Html.div("mt-3", table.build()));
        if (in.formula() == BmrFormula.KATCH_MCARDLE && chosen != BmrFormula.KATCH_MCARDLE) {
            panel.add(Html.div("mt-2", Ui.notice(Icon.INFO, Html.p(null, "Katch–McArdle needs body fat, so Mifflin–St Jeor was used instead."))));
        }
        return panel;
    }

    private static Element tdee(Inputs in) {
        BmrFormula f = NutritionCalculator.effectiveFormula(in.formula(), in.bodyFat());
        double bmr = bmrOf(f, in);
        ActivityLevel selected = in.activity() == null ? ActivityLevel.LIGHT : in.activity();
        double value = NutritionCalculator.tdee(bmr, selected);
        Ui.Table table = new Ui.Table().caption("TDEE by activity level").col("Activity", false).col("Factor", true).col("Calories", true);
        for (ActivityLevel a : ActivityLevel.values()) {
            Element name = Html.div(null, a == selected ? Html.strong(a.label()) : Html.text(a.label()), Html.span("cell-sub", a.description()));
            table.row(name, Html.text("× " + a.factor()), Html.text(Fmt.kcal(NutritionCalculator.tdee(bmr, a)) + " kcal"));
        }
        return Ui.panel("Your maintenance calories", selected.label() + ", BMR " + Fmt.kcal(bmr) + " kcal (" + f.label() + ")", null,
                Html.div("row", Html.span("display-num h1", Fmt.kcal(value)), Html.span("unit", "kcal a day")),
                Html.p("mt-2 muted", "Eat about this much and your weight should stay roughly the same. Eat 500 less for about 0.45 kg of loss a week."),
                Html.div("mt-3", table.build()));
    }

    private static Element macros(Inputs in, ViewContext ctx) {
        Profile p = new Profile();
        p.setSex(in.sex());
        p.setBirthDate(ctx.today().minusYears(in.age()));
        p.setHeightCm(in.heightCm());
        p.setWeightKg(in.weightKg());
        p.setBodyFatPercent(in.bodyFat());
        p.setActivityLevel(in.activity() == null ? ActivityLevel.LIGHT : in.activity());
        Goal goal = in.goal() == null ? Goal.MAINTAIN : in.goal();
        p.setGoal(goal);
        double pace = in.pace() == null ? 0.5 : in.pace();
        if (goal.gainsWeight()) pace = Math.min(0.5, pace);
        p.setWeeklyChangeKg(goal == Goal.MAINTAIN ? 0 : pace);
        p.setBmrFormula(in.formula() == null ? BmrFormula.MIFFLIN_ST_JEOR : in.formula());
        p.setRestrictions(Set.of());
        NutritionTargets t = NutritionCalculator.targetsFor(p, ctx.today());
        Element cards = Html.div("stats",
                Ui.stat("Protein", Integer.toString(t.proteinG()), "g", t.macros().proteinPct() + "%"),
                Ui.stat("Carbohydrate", Integer.toString(t.carbsG()), "g", t.macros().carbsPct() + "%"),
                Ui.stat("Fat", Integer.toString(t.fatG()), "g", t.macros().fatPct() + "%"),
                Ui.stat("Fibre", Integer.toString(t.fiberG()), "g", null),
                Ui.stat("Water", Fmt.one(t.waterMl() / 1000.0), "L", null));
        Element panel = Ui.panel("Your daily target", goal.label() + (goal == Goal.MAINTAIN ? "" : " at " + Fmt.trim(pace) + " kg a week"), null,
                Html.div("row", Html.span("display-num h1", Fmt.kcal(t.calories())), Html.span("unit", "kcal a day"),
                        Ui.badge(t.adjustment() == 0 ? "Maintenance" : Fmt.signedKcal(t.adjustment()) + " vs maintenance", "outline")),
                Html.p("muted mt-2", "Maintenance is " + Fmt.kcal(t.tdee()) + " kcal (BMR " + Fmt.kcal(t.bmr()) + " × " + p.getActivityLevel().factor() + ")."),
                Html.div("mt-3", cards));
        if (t.calorieLimited()) panel.add(Html.div("mt-3", Ui.notice(Icon.INFO, Html.p(null, t.calorieNote()))));
        if (ctx.user() == null) {
            panel.add(Html.div("mt-3", Ui.notice(Icon.SPARK, Html.p(null, Html.text("Want meals planned to these numbers? "),
                    Html.a("/register", null, "Create a free account"), Html.text(".")))));
        }
        return panel;
    }
}
