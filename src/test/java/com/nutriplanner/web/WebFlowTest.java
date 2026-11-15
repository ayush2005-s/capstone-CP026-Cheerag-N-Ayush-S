package com.nutriplanner.web;

import com.nutriplanner.NutriPlannerApplication;
import com.nutriplanner.config.AppConfig;
import com.nutriplanner.testkit.Test;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nutriplanner.testkit.Assert.assertContains;
import static com.nutriplanner.testkit.Assert.assertEquals;
import static com.nutriplanner.testkit.Assert.assertNotContains;
import static com.nutriplanner.testkit.Assert.assertTrue;

/** Drives the real web server over HTTP, the way a browser would. */
public class WebFlowTest implements AutoCloseable {

    private final NutriPlannerApplication app;
    private final String base;
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

    public WebFlowTest() throws IOException {
        Path dir = Files.createTempDirectory("np-web");
        Properties props = new Properties();
        props.setProperty("nutri.zone", "Asia/Kolkata");
        AppConfig config = AppConfig.forTesting(dir, props);
        app = new NutriPlannerApplication(config, Clock.system(config.zone));
        app.start();
        base = "http://127.0.0.1:" + app.port();
    }

    @Override
    public void close() {
        app.close();
    }

    // ------------------------------------------------------------------ client

    record Page(int status, String body, String location, HttpResponse<String> raw) {
    }

    final class Browser {
        final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER).build();
        String csrf = "";

        Page get(String path) throws Exception {
            HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create(base + path)).build(), HttpResponse.BodyHandlers.ofString());
            Matcher m = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(r.body());
            if (m.find()) csrf = m.group(1);
            return new Page(r.statusCode(), r.body(), r.headers().firstValue("Location").orElse(""), r);
        }

        Page post(String path, String... fields) throws Exception {
            return post(path, true, null, fields);
        }

        Page post(String path, boolean withCsrf, String origin, String... fields) throws Exception {
            Map<String, String> form = new LinkedHashMap<>();
            for (int i = 0; i < fields.length; i += 2) form.put(fields[i], fields[i + 1]);
            if (withCsrf) form.put("_csrf", csrf);
            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, String> e : form.entrySet()) {
                if (body.length() > 0) body.append('&');
                body.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=')
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
            if (origin != null) b.header("Origin", origin);
            HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Page(r.statusCode(), r.body(), r.headers().firstValue("Location").orElse(""), r);
        }

        void login(String user, String password) throws Exception {
            get("/login");
            Page p = post("/login", "login", user, "password", password);
            assertEquals(303, p.status(), "login redirects");
        }
    }

    private Browser member(String username) throws Exception {
        Browser b = new Browser();
        b.get("/register");
        Page reg = b.post("/register", "fullName", "Kavya Nair", "username", username, "email", username + "@example.com",
                "password", "Ragi#Dosa77", "confirmPassword", "Ragi#Dosa77", "acceptedTerms", "yes");
        assertEquals(303, reg.status(), "registration redirects");
        assertEquals("/onboarding?step=1", reg.location());
        b.get("/onboarding?step=1");
        assertEquals("/onboarding?step=2", b.post("/onboarding", "step", "1", "sex", "FEMALE", "birthDate", "1995-05-05",
                "heightCm", "160", "weightKg", "60").location());
        assertEquals("/onboarding?step=3", b.post("/onboarding", "step", "2", "goal", "LOSE_WEIGHT", "targetWeightKg", "56",
                "weeklyChangeKg", "0.5", "activityLevel", "LIGHT").location());
        assertEquals("/onboarding?step=done", b.post("/onboarding", "step", "3", "dietType", "VEGAN", "allergens", "SOY").location());
        return b;
    }

    // ------------------------------------------------------------------- tests

    @Test
    void publicPagesCarrySecurityHeaders() throws Exception {
        Browser b = new Browser();
        Page landing = b.get("/");
        assertEquals(200, landing.status());
        assertContains(landing.body(), "Meals that fit your body");
        assertTrue(landing.raw().headers().firstValue("Content-Security-Policy").orElse("").contains("default-src 'none'"), "CSP header");
        assertEquals("DENY", landing.raw().headers().firstValue("X-Frame-Options").orElse(""));
        assertEquals("nosniff", landing.raw().headers().firstValue("X-Content-Type-Options").orElse(""));
        assertTrue(landing.raw().headers().firstValue("Set-Cookie").orElse("").contains("HttpOnly"), "session cookie is HttpOnly");
        assertNotContains(landing.body(), "<script");
        Page dash = b.get("/dashboard");
        assertEquals(303, dash.status());
        assertTrue(dash.location().startsWith("/login?next="), "anonymous users are sent to login");
        assertEquals(404, b.get("/no-such-page").status());
        Page css = b.get("/assets/app.css");
        assertEquals(200, css.status());
        assertTrue(css.raw().headers().firstValue("Content-Type").orElse("").startsWith("text/css"), "stylesheet type");
        Page calc = b.get("/calculators?tool=bmi&calc=1&heightCm=175&weightKg=70");
        assertContains(calc.body(), "22.9");
        assertContains(calc.body(), "Healthy weight");
    }

    @Test
    void memberJourneyFromSignUpToDiary() throws Exception {
        Browser b = member("kavya");
        Page done = b.get("/onboarding?step=done");
        assertContains(done.body(), "Your daily targets are ready");
        Page dash = b.get("/dashboard");
        assertEquals(200, dash.status());
        assertContains(dash.body(), "Good ");
        assertContains(dash.body(), "No meals planned for today");

        Page generated = b.post("/planner/generate-week", "week", today.toString());
        assertEquals(303, generated.status());
        Page planner = b.get(generated.location());
        assertEquals(200, planner.status());
        assertContains(planner.body(), "Breakfast");
        assertNotContains(planner.body(), "Paneer");
        assertNotContains(planner.body(), "Tofu");

        Page search = b.get("/foods?q=banana");
        Matcher m = Pattern.compile("href=\"/foods/(\\d+)\"[^>]*>Banana<").matcher(search.body());
        assertTrue(m.find(), "banana is in the database");
        String bananaId = m.group(1);
        Page added = b.post("/diary/add", "food", bananaId, "date", today.toString(), "meal", "SNACK", "quantity", "2", "unit", "SERVING");
        assertEquals(303, added.status());
        Page diary = b.get(added.location());
        assertContains(diary.body(), "Added Banana (2 servings (236 g)) to snacks.");
        assertContains(diary.body(), "210");

        Page future = b.post("/diary/add", "food", bananaId, "date", today.plusDays(2).toString(), "meal", "SNACK", "quantity", "1", "unit", "SERVING");
        assertEquals(422, future.status());
        assertContains(future.body(), "Use the meal planner for future days");

        Page weight = b.post("/progress/weight", "date", today.toString(), "weightKg", "59.4", "note", "");
        assertEquals(303, weight.status());
        assertContains(b.get("/progress").body(), "Saved 59.4 kg");

        Page plan = b.get("/plan");
        assertContains(plan.body(), "Combine plant proteins");
        assertContains(plan.body(), "Allergy or intolerance: soy");
    }

    @Test
    void formsRequireCsrfTokensAndSameOrigin() throws Exception {
        Browser b = member("ravi");
        b.get("/dashboard");
        assertEquals(403, b.post("/water", false, null, "delta", "1").status());
        assertEquals(403, b.post("/water", true, "https://evil.example", "delta", "1").status());
        assertEquals(303, b.post("/water", true, base, "delta", "1", "date", today.toString()).status());
        assertEquals(403, b.get("/admin").status(), "members can't open the admin panel");
        assertEquals(403, b.post("/admin/users/1/role", "role", "USER").status(), "or use admin actions");
    }

    @Test
    void validationErrorsAreShownInPlace() throws Exception {
        Browser b = new Browser();
        b.get("/register");
        Page p = b.post("/register", "fullName", "Meera", "username", "meera", "email", "not-an-email",
                "password", "onlyletters", "confirmPassword", "onlyletters", "acceptedTerms", "yes");
        assertEquals(422, p.status());
        assertContains(p.body(), "Enter a valid email address");
        assertContains(p.body(), "Include both letters and at least one number.");
        assertContains(p.body(), "value=\"meera\"");
        assertNotContains(p.body(), "onlyletters");
    }

    @Test
    void userInputIsEscaped() throws Exception {
        Browser b = new Browser();
        b.get("/register");
        b.post("/register", "fullName", "<script>alert(1)</script>", "username", "xss_test", "email", "xss@example.com",
                "password", "Ragi#Dosa77", "confirmPassword", "Ragi#Dosa77", "acceptedTerms", "yes");
        Page settings = b.get("/settings");
        assertEquals(200, settings.status());
        assertContains(settings.body(), "&lt;script&gt;alert(1)&lt;/script&gt;");
        assertNotContains(settings.body(), "<script>alert(1)");
    }

    @Test
    void administratorFlow() throws Exception {
        member("sunita");
        Browser admin = new Browser();
        admin.get("/login");
        Page login = admin.post("/login", "login", "admin", "password", "Planner#Start1");
        assertEquals("/settings/password", login.location(), "first login forces a new password");
        assertEquals("/settings/password", admin.get("/admin").location(), "nothing else is reachable until then");
        admin.get("/settings/password");
        Page changed = admin.post("/settings/password", "currentPassword", "Planner#Start1", "newPassword", "Kitchen#Garden42",
                "confirmPassword", "Kitchen#Garden42");
        assertEquals("/admin", changed.location());
        assertContains(admin.get("/admin").body(), "Administration");
        Page users = admin.get("/admin/users?q=sunita");
        Matcher m = Pattern.compile("href=\"/admin/users/(\\d+)\"").matcher(users.body());
        assertTrue(m.find(), "member is listed");
        String id = m.group(1);
        admin.get("/admin/users/" + id);
        assertEquals(303, admin.post("/admin/users/" + id + "/reset-password").status());
        Page detail = admin.get("/admin/users/" + id);
        assertContains(detail.body(), "Temporary password");
        assertNotContains(admin.get("/admin/users/" + id).body(), "Temporary password");
        assertEquals(303, admin.post("/admin/users/" + id + "/status", "status", "DISABLED").status());
        Browser blocked = new Browser();
        blocked.get("/login");
        Page denied = blocked.post("/login", "login", "sunita", "password", "Ragi#Dosa77");
        assertEquals(401, denied.status(), "old password no longer works after reset");
        assertContains(admin.get("/admin/audit").body(), "Password reset by admin");
    }

    @Test
    void repeatedFailedLoginsLockTheAccount() throws Exception {
        member("arjun");
        Browser b = new Browser();
        b.get("/login");
        Page last = null;
        for (int i = 0; i < 5; i++) last = b.post("/login", "login", "arjun", "password", "wrong-" + i);
        assertContains(last.body(), "Too many failed attempts");
        Page right = b.post("/login", "login", "arjun", "password", "Ragi#Dosa77");
        assertEquals(401, right.status(), "locked even with the right password");
    }

    @Test
    void plannerEditsCustomFoodsAndProfile() throws Exception {
        Browser b = member("meena");
        String day = today.toString();
        b.get("/planner");
        assertEquals(303, b.post("/planner/generate-day", "date", day).status());
        for (String action : new String[]{"/planner/swap", "/planner/remove"}) {
            Page p = b.post(action, "date", day, "index", "0");
            assertEquals(303, p.status());
            assertContains(b.get(p.location()).body(), "flash-success");
        }
        assertEquals(303, b.post("/planner/servings", "date", day, "index", "0", "servings", "1.5").status());
        assertContains(b.get("/planner?day=" + day).body(), "1.5 servings");
        assertEquals(303, b.post("/planner/regenerate-meal", "date", day, "meal", "DINNER").status());
        Page logged = b.post("/planner/log-meal", "date", day, "meal", "LUNCH");
        assertContains(b.get(logged.location()).body(), "Logged lunch to your diary");
        Page tooEarly = b.post("/planner/log-day", "date", today.plusDays(3).toString(), "_back", "/planner");
        assertEquals("/planner", tooEarly.location(), "rejected actions return to the page with a message");
        assertContains(b.get("/planner").body(), "flash-error");

        b.get("/foods/new");
        Page bad = b.post("/foods/new", "name", "X", "category", "PREPARED", "diet", "VEGAN", "planRole", "NONE", "kcal", "abc",
                "protein", "1", "carbs", "1", "fat", "1", "fiber", "0", "sugar", "0", "sodiumMg", "0", "servingGrams", "100", "servingLabel", "1 bowl");
        assertEquals(422, bad.status());
        assertContains(bad.body(), "Enter a number.");
        Page created = b.post("/foods/new", "name", "Ragi mudde", "category", "PREPARED", "diet", "VEGAN", "planRole", "BASE",
                "mealTypes", "LUNCH", "mealTypes", "DINNER", "kcal", "120", "protein", "2.6", "carbs", "26", "fat", "0.5",
                "fiber", "3", "sugar", "0", "sodiumMg", "5", "servingGrams", "150", "servingLabel", "1 ball");
        assertEquals(303, created.status());
        String foodPath = created.location();
        assertContains(b.get(foodPath).body(), "A custom food only you can see");
        b.get(foodPath + "/edit");
        assertEquals(303, b.post(foodPath + "/edit", "name", "Ragi mudde (finger millet)", "category", "PREPARED", "diet", "VEGAN",
                "planRole", "BASE", "kcal", "120", "protein", "2.6", "carbs", "26", "fat", "0.5", "fiber", "3", "sugar", "0",
                "sodiumMg", "5", "servingGrams", "150", "servingLabel", "1 ball").status());
        assertContains(b.get("/foods?mine=on").body(), "Ragi mudde (finger millet)");
        Browser other = member("other_member");
        assertEquals(404, other.get(foodPath).status(), "custom foods are private");
        assertEquals(303, b.post(foodPath + "/delete").status());

        b.get("/profile");
        Page goal = b.post("/profile/goal", "goal", "LOSE_WEIGHT", "targetWeightKg", "40", "weeklyChangeKg", "0.5", "activityLevel", "LIGHT");
        assertEquals(422, goal.status());
        assertContains(goal.body(), "below a healthy weight");
        assertEquals(303, b.post("/profile/goal", "goal", "BUILD_MUSCLE", "targetWeightKg", "63", "weeklyChangeKg", "0.25",
                "activityLevel", "VERY").status());
        assertContains(b.get("/plan").body(), "Train with progressive overload");
        assertEquals(303, b.post("/settings/theme", "theme", "DARK").status());
        assertContains(b.get("/dashboard").body(), "class=\"theme-dark\"");
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        Browser b = member("leela");
        b.get("/dashboard");
        Page out = b.post("/logout");
        assertEquals("/login", out.location());
        assertEquals(303, b.get("/dashboard").status());
    }
}
