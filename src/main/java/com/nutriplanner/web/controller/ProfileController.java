package com.nutriplanner.web.controller;

import com.nutriplanner.domain.BmrFormula;
import com.nutriplanner.domain.Profile;
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
import com.nutriplanner.web.ui.FormState;
import com.nutriplanner.web.ui.Forms;
import com.nutriplanner.web.ui.Fmt;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;
import com.nutriplanner.web.view.Layout;
import com.nutriplanner.web.view.ProfileForms;

import java.util.ArrayList;
import java.util.List;

import static com.nutriplanner.web.http.Router.Access.MEMBER;

/** Profile and goals: account details, body, goal, diet and calculation preferences, each saved separately. */
public final class ProfileController {

    private final Web web;

    public ProfileController(Web web) {
        this.web = web;
    }

    public void register(Router r) {
        r.get("/profile", MEMBER, req -> page(req, null, FormState.empty(), 200));
        r.post("/profile/account", MEMBER, req -> save(req, "account"));
        r.post("/profile/body", MEMBER, req -> save(req, "body"));
        r.post("/profile/goal", MEMBER, req -> save(req, "goal"));
        r.post("/profile/diet", MEMBER, req -> save(req, "diet"));
        r.post("/profile/preferences", MEMBER, req -> save(req, "preferences"));
    }

    private Response page(Request req, String failedSection, FormState failed, int status) {
        ViewContext ctx = web.context(req);
        Profile p = ctx.profile();
        FormState account = state("account", failedSection, failed).setIfAbsent("fullName", ctx.user().getFullName())
                .setIfAbsent("email", ctx.user().getEmail());
        FormState body = ProfileForms.fill(p, state("body", failedSection, failed));
        FormState goal = ProfileForms.fill(p, state("goal", failedSection, failed));
        FormState diet = ProfileForms.fill(p, state("diet", failedSection, failed));
        FormState prefs = ProfileForms.fill(p, state("preferences", failedSection, failed));

        Element accountForm = section(ctx, "account", "Account", "Your name and the email you log in with.", account,
                Html.div("form-grid", Forms.input(account, "fullName", "Name", "text", null, true, "name"),
                        Forms.input(account, "email", "Email", "email", null, true, "email")));
        Element bodyForm = section(ctx, "body", "Body", "Changing your weight here also records a weigh-in for today.", body,
                ProfileForms.bodyFields(body, ctx.today()));
        Element goalForm = section(ctx, "goal", "Goal and activity", null, goal, ProfileForms.goalFields(goal));
        Element dietForm = section(ctx, "diet", "Diet, allergies and restrictions", "Meal plans and suggestions only use foods that fit.", diet,
                ProfileForms.dietFields(diet));
        List<Forms.Option> formulas = new ArrayList<>();
        for (BmrFormula f : BmrFormula.values()) formulas.add(new Forms.Option(f.name(), f.label() + ": " + f.description()));
        Element prefsForm = section(ctx, "preferences", "Calculation preferences", null, prefs,
                Html.div("form-grid",
                        Forms.select(prefs, "bmrFormula", "Metabolic rate formula", formulas, null, null),
                        Forms.number(prefs, "calorieOverride", "Custom daily calories", "kcal", "10", "1000", "6000",
                                "Leave empty to use the calculated target. Use this if a dietitian gave you a number.", false)));

        NutritionTargets t = web.services.profiles.targets(ctx.userId()).orElse(null);
        Element side = Html.div("stack");
        Element card = Html.div("row", Ui.avatar(ctx.user(), "avatar-lg"),
                Html.div(null, Html.h(2, "h3", ctx.user().getFullName()), Html.p("small muted", "@" + ctx.user().getUsername()
                        + (ctx.user().getCreatedAt() == null ? "" : ", member since "
                        + Fmt.mediumDate(java.time.LocalDate.ofInstant(ctx.user().getCreatedAt(), ctx.zone()))))));
        side.add(Html.div("panel", card));
        if (t != null) {
            Element dl = new Element("dl").cls("kv").add(
                    kv("Age", t.age() + " years"), kv("BMI", Fmt.one(t.bmi()) + ", " + t.bmiCategory().label().toLowerCase()),
                    kv("BMR", Fmt.kcal(t.bmr()) + " kcal"), kv("Maintenance", Fmt.kcal(t.tdee()) + " kcal"),
                    kv("Daily target", Fmt.kcal(t.calories()) + " kcal"),
                    kv("Macros", t.proteinG() + " g protein, " + t.carbsG() + " g carbs, " + t.fatG() + " g fat"),
                    kv("Water", Fmt.one(t.waterMl() / 1000.0) + " litres"));
            side.add(Ui.panel("Your current targets", "Updated as soon as you save", Ui.link("/plan", "Diet plan", "btn-ghost btn-sm", null), dl));
        }
        return Layout.app(ctx, status, "Profile and goals", Layout.Nav.PROFILE,
                Ui.pageHead("Profile and goals", "Everything Nutri Planner uses to personalise your targets and plans."),
                Html.div("grid g-main", Html.div("stack", accountForm, bodyForm, goalForm, dietForm, prefsForm), side));
    }

    private static FormState state(String section, String failedSection, FormState failed) {
        return section.equals(failedSection) ? failed : FormState.empty();
    }

    private static com.nutriplanner.web.html.Node kv(String k, String v) {
        return Html.fragment(new Element("dt").text(k), new Element("dd").text(v));
    }

    private static final java.util.Map<String, String> BUTTONS = java.util.Map.of("account", "Save account", "body", "Save body details",
            "goal", "Save goal", "diet", "Save diet", "preferences", "Save preferences");

    private static Element section(ViewContext ctx, String id, String title, String sub, FormState st, Element fields) {
        Element form = Forms.post("/profile/" + id, ctx.csrf()).cls("form").add(Forms.formMessage(st), fields,
                Html.div("form-actions", Forms.submit(BUTTONS.get(id), "btn-primary", Icon.CHECK)));
        Element panel = Ui.panel(title, sub, null, form);
        panel.id(id);
        return panel;
    }

    private Response save(Request req, String section) {
        long userId = req.user().getId();
        Params p = new Params(req);
        try {
            switch (section) {
                case "account":
                    web.services.profiles.updateAccount(userId, p.text("fullName"), p.text("email"));
                    req.session().success("Saved your account details.");
                    break;
                case "body": {
                    var body = ProfileForms.parseBody(p);
                    if (p.hasErrors()) throw new ValidationException(p.errors());
                    web.services.profiles.updateBody(userId, body);
                    req.session().success("Saved. Your targets have been recalculated.");
                    break;
                }
                case "goal": {
                    var goal = ProfileForms.parseGoal(p);
                    if (p.hasErrors()) throw new ValidationException(p.errors());
                    web.services.profiles.updateGoal(userId, goal);
                    req.session().success("Saved your goal. Your targets have been recalculated.");
                    break;
                }
                case "diet": {
                    var diet = ProfileForms.parseDiet(p);
                    if (p.hasErrors()) throw new ValidationException(p.errors());
                    web.services.profiles.updateDiet(userId, diet);
                    req.session().success("Saved. New meal plans will follow these choices; regenerate existing days to update them.");
                    break;
                }
                default: {
                    BmrFormula formula = p.choice("bmrFormula", BmrFormula.class);
                    Integer override = p.integer("calorieOverride");
                    if (p.hasErrors()) throw new ValidationException(p.errors());
                    web.services.profiles.updatePreferences(userId, formula, override);
                    req.session().success("Saved your calculation preferences.");
                }
            }
            return Response.redirect("/profile#" + section);
        } catch (ValidationException e) {
            FormState st = FormState.from(req).errors(e.fieldErrors());
            if (e.fieldErrors().isEmpty()) st.message(e.getMessage());
            return page(req, section, st, 422);
        }
    }
}
