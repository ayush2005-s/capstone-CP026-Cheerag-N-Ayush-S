package com.nutriplanner.web.controller;

import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.User;
import com.nutriplanner.security.PasswordPolicy;
import com.nutriplanner.seed.DataSeeder;
import com.nutriplanner.service.AuthService;
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
import com.nutriplanner.web.ui.Charts;
import com.nutriplanner.web.ui.FormState;
import com.nutriplanner.web.ui.Forms;
import com.nutriplanner.web.ui.Fmt;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;
import com.nutriplanner.web.view.Layout;
import com.nutriplanner.web.view.ProfileForms;

import static com.nutriplanner.web.http.Router.Access.AUTHENTICATED;
import static com.nutriplanner.web.http.Router.Access.GUEST;

/** Log in, sign up, log out, and the first-run onboarding steps. */
public final class AuthController {

    private final Web web;

    public AuthController(Web web) {
        this.web = web;
    }

    public void register(Router r) {
        r.get("/login", GUEST, req -> loginPage(req, FormState.empty(), 200));
        r.post("/login", GUEST, this::login);
        r.get("/register", GUEST, req -> registerPage(req, FormState.empty(), 200));
        r.post("/register", GUEST, this::signUp);
        r.add("POST", "/logout", AUTHENTICATED, this::logout, true);
        r.get("/onboarding", AUTHENTICATED, this::onboarding);
        r.post("/onboarding", AUTHENTICATED, this::saveOnboarding);
    }

    // ------------------------------------------------------------------- login

    private Response loginPage(Request req, FormState st, int status) {
        ViewContext ctx = web.context(req);
        String next = WebServer.safePath(req.param("next"));
        Element form = Forms.post("/login", ctx.csrf()).cls("form");
        if (next != null) form.add(Forms.hidden("next", next));
        form.add(Forms.formMessage(st),
                Forms.input(st, "login", "Username or email", "text", null, true, "username"),
                Forms.input(st, "password", "Password", "password", null, true, "current-password"),
                Forms.submit("Log in", "btn-primary btn-lg btn-block", null));
        Element demo = ctx.config().showDemoLogin
                ? Html.p("demo-hint", Html.text("Exploring? Log in as "), Html.strong(DataSeeder.DEMO_USERNAME),
                Html.text(" with the password "), Html.strong(DataSeeder.DEMO_PASSWORD), Html.text(" to see an account with eight weeks of history."))
                : null;
        return Layout.auth(ctx, status, "Log in", aside("Know what to eat today.",
                        "Your targets, your meal plan and your food diary, all in one place."),
                Html.div(null, Html.h(1, null, "Log in"), Html.p("auth-switch mt-1", Html.text("New here? "), Html.a("/register", null, "Create an account"))),
                form, demo);
    }

    private Response login(Request req) {
        FormState st = FormState.from(req);
        if (!web.loginLimiter.tryAcquire(req.clientIp())) {
            return loginPage(req, st.message("Too many login attempts from your network. Wait 15 minutes and try again."), 429);
        }
        AuthService.LoginResult result = web.services.auth.login(req.param("login"), req.param("password"), req.clientIp());
        if (!result.success()) {
            return loginPage(req, st.message(result.message(web.clock)), 401);
        }
        User user = result.user();
        web.auth.login(req, user);
        if (user.isMustChangePassword()) {
            req.session().info("Choose a new password to finish signing in.");
            return Response.redirect("/settings/password");
        }
        Profile profile = web.services.profiles.profile(user.getId());
        req.session().success("Welcome back, " + user.firstName() + ".");
        String next = WebServer.safePath(req.param("next"));
        if (next != null && !next.startsWith("/login")) return Response.redirect(next);
        if (profile.isCompleted()) return Response.redirect("/dashboard");
        return Response.redirect(user.isAdmin() ? "/admin" : "/onboarding");
    }

    private Response logout(Request req) {
        web.services.auth.recordLogout(req.user(), req.clientIp());
        web.auth.logout(req);
        req.session().success("You've logged out.");
        return Response.redirect("/login");
    }

    // ---------------------------------------------------------------- register

    private Response registerPage(Request req, FormState st, int status) {
        ViewContext ctx = web.context(req);
        Element form = Forms.post("/register", ctx.csrf()).cls("form").add(Forms.formMessage(st),
                Forms.input(st, "fullName", "Your name", "text", null, true, "name"),
                Forms.input(st, "username", "Username", "text", "3–24 letters, numbers, dots or underscores.", true, "username"),
                Forms.input(st, "email", "Email", "email", null, true, "email"),
                Html.div("form-grid",
                        Forms.input(st, "password", "Password", "password", PasswordPolicy.HINT, true, "new-password"),
                        Forms.input(st, "confirmPassword", "Repeat password", "password", null, true, "new-password")),
                Forms.checkbox(st, "acceptedTerms", "yes",
                        Html.text("I understand Nutri Planner gives general nutrition guidance, not medical advice.")),
                Forms.submit("Create account", "btn-primary btn-lg btn-block", null));
        return Layout.auth(ctx, status, "Create an account", aside("Start with how you already eat.",
                        "Tell us your diet and what to avoid. Every plan and suggestion works around it."),
                Html.div(null, Html.h(1, null, "Create your account"),
                        Html.p("auth-switch mt-1", Html.text("Already have one? "), Html.a("/login", null, "Log in"))),
                form);
    }

    private Response signUp(Request req) {
        FormState st = FormState.from(req);
        try {
            User user = web.services.auth.register(new AuthService.Registration(req.param("fullName"), req.param("username"),
                    req.param("email"), req.param("password"), req.param("confirmPassword"),
                    "yes".equals(req.param("acceptedTerms"))), req.clientIp());
            web.auth.login(req, user);
            req.session().success("Welcome to Nutri Planner, " + user.firstName() + ". Three quick steps and your plan is ready.");
            return Response.redirect("/onboarding?step=1");
        } catch (ValidationException e) {
            return registerPage(req, st.errors(e.fieldErrors()).message(e.fieldErrors().isEmpty() ? e.getMessage() : null), 422);
        }
    }

    private static Node aside(String title, String text) {
        Element plate = Charts.plate(new double[]{430, 560, 480, 150}, 1800, "1,620", "kcal so far", "of 1,800 today");
        return Html.div(null, Html.h(2, null, title), Html.p(null, text), Html.div("mt-4 aside-extra", plate));
    }

    // -------------------------------------------------------------- onboarding

    private static final String[] STEPS = {"About you", "Your goal", "How you eat", "Your plan"};

    private Response onboarding(Request req) {
        Profile profile = web.services.profiles.profile(req.user().getId());
        String step = req.param("step");
        if (step == null) {
            if (profile.isCompleted()) return Response.redirect("/dashboard");
            step = Integer.toString(Math.min(3, profile.getOnboardingStep() + 1));
        }
        return onboardingPage(req, step, ProfileForms.fill(profile, FormState.empty()), 200);
    }

    private Response onboardingPage(Request req, String step, FormState st, int status) {
        ViewContext ctx = web.context(req);
        int index;
        switch (step) {
            case "2": index = 1; break;
            case "3": index = 2; break;
            case "done": index = 3; break;
            default: index = 0;
        }
        Element progress = new Element("ol").cls("progress-steps").attr("aria-label", "Setup progress");
        for (int i = 0; i < STEPS.length; i++) {
            Element li = Html.li(i < index ? "done" : i == index ? "current" : null, STEPS[i]);
            if (i == index) li.attr("aria-current", "step");
            progress.add(li);
        }
        if (index == 3) return Layout.bare(ctx, status, "Your plan", progress, summary(ctx));

        Element fields;
        String title, intro;
        if (index == 0) {
            title = "First, a little about you";
            intro = "These numbers drive every calculation: BMI, metabolic rate and your daily calorie needs.";
            fields = ProfileForms.bodyFields(st, ctx.today());
        } else if (index == 1) {
            title = "What are you working towards?";
            intro = "Your goal and activity level set how many calories you need and how they split into protein, carbs and fat.";
            fields = ProfileForms.goalFields(st);
        } else {
            title = "How do you eat?";
            intro = "Your meal plans and food suggestions only use foods that fit these choices.";
            fields = ProfileForms.dietFields(st);
        }
        Element form = Forms.post("/onboarding", ctx.csrf()).cls("form").add(Forms.hidden("step", Integer.toString(index + 1)),
                Forms.formMessage(st), fields);
        Element actions = Html.div("form-actions",
                Forms.submit(index == 2 ? "See my plan" : "Continue", "btn-primary btn-lg", null));
        if (index > 0) actions.add(Ui.link("/onboarding?step=" + index, "Back", "btn-ghost btn-lg", null));
        form.add(actions);
        return Layout.bare(ctx, status, "Set up your plan", progress,
                Html.div("stack", Html.div(null, Html.h(1, "page-title", title), Html.p("page-sub", intro)), Html.div("panel", form)));
    }

    private Response saveOnboarding(Request req) {
        long userId = req.user().getId();
        String step = req.param("step", "1");
        Params p = new Params(req);
        FormState st = FormState.from(req);
        try {
            switch (step) {
                case "1": {
                    var body = ProfileForms.parseBody(p);
                    if (p.hasErrors()) throw new ValidationException(p.errors());
                    web.services.profiles.updateBody(userId, body);
                    return Response.redirect("/onboarding?step=2");
                }
                case "2": {
                    var goal = ProfileForms.parseGoal(p);
                    if (p.hasErrors()) throw new ValidationException(p.errors());
                    web.services.profiles.updateGoal(userId, goal);
                    return Response.redirect("/onboarding?step=3");
                }
                default: {
                    var diet = ProfileForms.parseDiet(p);
                    if (p.hasErrors()) throw new ValidationException(p.errors());
                    web.services.profiles.updateDiet(userId, diet);
                    web.services.profiles.completeOnboarding(userId);
                    return Response.redirect("/onboarding?step=done");
                }
            }
        } catch (ValidationException e) {
            st.errors(e.fieldErrors());
            if (e.fieldErrors().isEmpty()) st.message(e.getMessage());
            return onboardingPage(req, step.equals("done") ? "3" : step, st, 422);
        }
    }

    private Node summary(ViewContext ctx) {
        NutritionTargets t = web.services.profiles.targets(ctx.userId()).orElse(null);
        if (t == null || !ctx.onboarded()) {
            return Ui.empty(Icon.ALERT, "A few details are missing", "Finish the earlier steps to see your targets.",
                    Ui.link("/onboarding?step=1", "Go to step 1", "btn-primary", null));
        }
        Element targets = Html.div("target-card",
                Ui.stat("Daily calories", Fmt.kcal(t.calories()), "kcal", t.adjustment() == 0 ? "Maintenance" : Fmt.signedKcal(t.adjustment()) + " vs maintenance"),
                Ui.stat("Protein", Integer.toString(t.proteinG()), "g", t.macros().proteinPct() + "% of calories"),
                Ui.stat("Carbohydrate", Integer.toString(t.carbsG()), "g", t.macros().carbsPct() + "% of calories"),
                Ui.stat("Fat", Integer.toString(t.fatG()), "g", t.macros().fatPct() + "% of calories"));
        Element body = Html.div("stats",
                Ui.stat("BMI", Fmt.one(t.bmi()), null, t.bmiCategory().label()),
                Ui.stat("BMR", Fmt.kcal(t.bmr()), "kcal", "Energy at complete rest"),
                Ui.stat("TDEE", Fmt.kcal(t.tdee()), "kcal", "Maintenance with your activity"),
                Ui.stat("Water", Fmt.one(t.waterMl() / 1000.0), "litres", "About " + (int) Math.ceil(t.waterMl() / 250.0) + " glasses"));
        Element generate = Forms.post("/planner/generate-week", ctx.csrf(), "/onboarding?step=done").cls("inline")
                .add(Forms.hidden("week", ctx.today().toString()), Forms.submit("Plan my week of meals", "btn-primary btn-lg", Icon.CALENDAR));
        Element content = Html.div("stack",
                Html.div(null, Html.h(1, "page-title", "Your daily targets are ready"),
                        Html.p("page-sub", "Calculated with the " + t.formula().label() + " equation. You can change anything later under Profile and goals.")),
                targets, body);
        if (t.calorieLimited()) content.add(Ui.notice(Icon.INFO, Html.p(null, t.calorieNote())));
        content.add(Html.div("btn-row", generate, Ui.link("/dashboard", "Go to my dashboard", "btn-secondary btn-lg", null)));
        return content;
    }
}
