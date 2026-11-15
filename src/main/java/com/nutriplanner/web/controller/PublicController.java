package com.nutriplanner.web.controller;

import com.nutriplanner.web.ViewContext;
import com.nutriplanner.web.Web;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.http.Request;
import com.nutriplanner.web.http.Response;
import com.nutriplanner.web.http.Router;
import com.nutriplanner.web.style.AppStyles;
import com.nutriplanner.web.ui.Charts;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;
import com.nutriplanner.web.view.Layout;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.nutriplanner.web.http.Router.Access.PUBLIC;

/** The landing page and static assets (stylesheet, fonts, icon). */
public final class PublicController {

    private static final Map<String, String> FONTS = Map.of(
            "InterVariable.woff2", "font/woff2",
            "BricolageGrotesque.ttf", "font/ttf");
    private static final String YEAR = "public, max-age=31536000, immutable";

    private final Web web;
    private final Map<String, byte[]> cache = new ConcurrentHashMap<>();

    public PublicController(Web web) {
        this.web = web;
    }

    public void register(Router r) {
        r.get("/", PUBLIC, this::landing);
        r.get("/assets/app.css", PUBLIC, req -> etagged(req, "text/css; charset=utf-8", AppStyles.css().getBytes(StandardCharsets.UTF_8), AppStyles.version()));
        r.get("/assets/fonts/{file}", PUBLIC, this::font);
        r.get("/favicon.svg", PUBLIC, req -> Response.bytes("image/svg+xml", favicon(), "public, max-age=86400"));
        r.get("/robots.txt", PUBLIC, req -> Response.bytes("text/plain; charset=utf-8",
                "User-agent: *\nDisallow: /dashboard\nDisallow: /admin\n".getBytes(StandardCharsets.UTF_8), "public, max-age=86400"));
        r.get("/health", PUBLIC, req -> Response.text(200, "ok"));
    }

    private Response landing(Request req) {
        if (req.user() != null) return Response.redirect(web.services.profiles.profile(req.user().getId()).isCompleted() ? "/dashboard" : "/onboarding");
        ViewContext ctx = web.context(req);
        Element head = new Element("header").cls("site-head").add(Layout.brand("/"),
                Html.div("btn-row", Ui.link("/login", "Log in", "btn-ghost", null), Ui.link("/register", "Create account", "btn-primary", null)));

        Element plate = Charts.plate(new double[]{460, 610, 540, 190}, 1900, "1,800", "kcal planned", "of 1,900 target");
        Element menu = Html.div("plate-menu",
                legend("m1", "Breakfast", "Besan chilla, papaya"),
                legend("m2", "Lunch", "Rajma, jeera rice, kachumber"),
                legend("m3", "Dinner", "Paneer bhurji, two rotis, palak"),
                legend("m4", "Snacks", "Roasted chana, buttermilk"));
        Element hero = Html.section("hero",
                Html.div(null,
                        Html.h(1, "hero-title", "Meals that fit your body, your diet and your day."),
                        Html.p("hero-copy", "Nutri Planner works out your calories and macros from your height, weight and goal, "
                                + "plans a week of meals that respect how you eat and what you can't eat, and keeps a food diary that adds up as you go."),
                        Html.div("diet-row", Ui.badge("Vegetarian", "jade"), Ui.badge("Vegan", "jade"), Ui.badge("Eggetarian", "turmeric"),
                                Ui.badge("Non-vegetarian", "chili"), Ui.badge("Allergy-aware", "blueberry")),
                        Html.div("btn-row", Ui.link("/register", "Create your plan", "btn-primary btn-lg", null),
                                Ui.link("/calculators", "Try the calculators", "btn-secondary btn-lg", Icon.CALCULATOR)),
                        Html.p("hero-note", "Free to use. Your data stays on this server.")),
                Html.div("hero-plate", plate, menu));

        Element features = Html.div("band", Html.div("band-inner",
                Html.h(2, "section-title", "Everything a day of eating needs, in one place"),
                Html.div("features mt-4",
                        feature(Icon.TARGET, "Targets from your body",
                                "BMI, BMR and TDEE from recognised formulas, then calories, protein, carbs, fat, fibre and water set for your goal."),
                        feature(Icon.CALENDAR, "A week of meals, planned",
                                "Breakfast, lunch, dinner and snacks portioned to your targets. Swap any dish, change portions, or log a planned meal in one tap."),
                        feature(Icon.TREND, "Progress you can read",
                                "Weigh-ins, trend per week and a projected goal date, next to how your calories have tracked against target."))));

        Element how = Html.section("how",
                Html.div(null, Html.h(2, "section-title", "Set up in about two minutes"),
                        Html.p("lead mt-2", "Answer a few questions once. Nutri Planner recalculates your targets whenever your weight or goal changes.")),
                new Element("ol").cls("steps").add(
                        step("Tell us about you", "Height, weight, age and how active you are."),
                        step("Choose a goal", "Lose weight, maintain, gain, or build muscle, and how fast you want to go."),
                        step("Say how you eat", "Vegetarian, vegan, eggetarian or non-vegetarian, plus allergies and restrictions."),
                        step("Get your plan", "Daily targets, a meal plan for the week and a diary ready to fill in.")));

        Element foot = new Element("footer").cls("site-foot").add(
                Html.span(null, "Nutri Planner gives general nutrition guidance, not medical advice."),
                Html.span(null, "Nutrition data per 100 g from USDA FoodData Central and the Indian Food Composition Tables."));
        return Response.html(Layout.document("Plan meals that fit you", ctx.themeClass(),
                Html.fragment(head, new Element("main").id("main").add(Ui.flashes(ctx.flashes()), hero, features, how), foot)));
    }

    private static Element legend(String cls, String meal, String foods) {
        return Html.div("meal-legend-item " + cls, Html.span("name", meal), Html.span("val", foods));
    }

    private static Element feature(Icon icon, String title, String text) {
        return Html.div("feature", Html.span("feature-icon", icon.svg()), Html.h(3, null, title), Html.p(null, text));
    }

    private static Element step(String title, String text) {
        return Html.li(null, Html.div(null, Html.h(3, null, title), Html.p(null, text)));
    }

    // ------------------------------------------------------------------- assets

    private Response font(Request req) throws IOException {
        String file = req.pathVar("file");
        String type = FONTS.get(file);
        if (type == null) return Response.text(404, "Not found");
        byte[] bytes = cache.get(file);
        if (bytes == null) {
            try (InputStream in = PublicController.class.getResourceAsStream("/assets/fonts/" + file)) {
                if (in == null) return Response.text(404, "Not found");
                bytes = in.readAllBytes();
            }
            cache.put(file, bytes);
        }
        return Response.bytes(type, bytes, YEAR);
    }

    private static Response etagged(Request req, String type, byte[] body, String version) {
        String etag = "\"" + version + "\"";
        if (etag.equals(req.header("If-None-Match"))) return Response.notModified().header("ETag", etag);
        return Response.bytes(type, body, YEAR).header("ETag", etag);
    }

    private static byte[] favicon() {
        return Layout.logo(null).render().getBytes(StandardCharsets.UTF_8);
    }
}
