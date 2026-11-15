package com.nutriplanner.web.view;

import com.nutriplanner.web.ViewContext;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.html.Node;
import com.nutriplanner.web.html.Svg;
import com.nutriplanner.web.http.Response;
import com.nutriplanner.web.style.AppStyles;
import com.nutriplanner.web.ui.Forms;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;

/** Page frames: the signed-in app with its sidebar, the split sign-in screens, and public pages. */
public final class Layout {

    private Layout() {
    }

    /** Sidebar destinations. */
    public enum Nav {
        DASHBOARD("Dashboard", "/dashboard", Icon.DASHBOARD),
        DIARY("Food diary", "/diary", Icon.DIARY),
        PLANNER("Meal planner", "/planner", Icon.CALENDAR),
        PLAN("Diet plan", "/plan", Icon.PLAN),
        FOODS("Foods", "/foods", Icon.APPLE),
        PROGRESS("Progress", "/progress", Icon.TREND),
        CALCULATORS("Calculators", "/calculators", Icon.CALCULATOR),
        PROFILE("Profile and goals", "/profile", Icon.USER),
        SETTINGS("Settings", "/settings", Icon.SETTINGS),
        ADMIN("Overview", "/admin", Icon.SHIELD),
        ADMIN_USERS("Members", "/admin/users", Icon.USERS),
        ADMIN_FOODS("Food database", "/admin/foods", Icon.LIST),
        ADMIN_AUDIT("Audit log", "/admin/audit", Icon.CLOCK),
        NONE("", "", Icon.DASHBOARD);

        final String label;
        final String href;
        final Icon icon;

        Nav(String label, String href, Icon icon) {
            this.label = label;
            this.href = href;
            this.icon = icon;
        }
    }

    // ---------------------------------------------------------------- document

    public static String document(String title, String bodyClass, Node body) {
        Element head = new Element("head").add(
                new Element("meta").attr("charset", "utf-8"),
                new Element("meta").attr("name", "viewport").attr("content", "width=device-width, initial-scale=1"),
                new Element("meta").attr("name", "description").attr("content",
                        "Nutri Planner: personalised calorie targets, meal plans and a food diary."),
                new Element("meta").attr("name", "theme-color").attr("content", "#1D6B50"),
                new Element("meta").attr("name", "color-scheme").attr("content", "light dark"),
                new Element("title").text(title.isEmpty() ? "Nutri Planner" : title + " | Nutri Planner"),
                new Element("link").attr("rel", "icon").attr("type", "image/svg+xml").attr("href", "/favicon.svg"),
                new Element("link").attr("rel", "preload").attr("href", "/assets/fonts/InterVariable.woff2").attr("as", "font")
                        .attr("type", "font/woff2").attr("crossorigin", ""),
                new Element("link").attr("rel", "stylesheet").attr("href", "/assets/app.css?v=" + AppStyles.version()));
        Element html = new Element("html").attr("lang", "en").cls(bodyClass).add(head, new Element("body").add(body));
        return "<!DOCTYPE html>" + html.render();
    }

    // ---------------------------------------------------------------- app shell

    public static Response app(ViewContext ctx, String title, Nav active, Node... content) {
        return app(ctx, 200, title, active, content);
    }

    public static Response app(ViewContext ctx, int status, String title, Nav active, Node... content) {
        Element nav = new Element("nav").cls("nav").attr("aria-label", "Main");
        boolean member = ctx.onboarded();
        if (member) {
            for (Nav n : new Nav[]{Nav.DASHBOARD, Nav.DIARY, Nav.PLANNER, Nav.PLAN, Nav.FOODS, Nav.PROGRESS, Nav.CALCULATORS}) {
                nav.add(navLink(n, active));
            }
        } else {
            nav.add(navLink(Nav.CALCULATORS, active));
            nav.add(Html.a("/onboarding", "nav-link" + (ctx.path().startsWith("/onboarding") ? " active" : ""),
                    Icon.SPARK.svg(), Html.span(null, "Set up your plan")));
        }
        nav.add(Html.div("nav-label", "Account"));
        if (member) nav.add(navLink(Nav.PROFILE, active));
        nav.add(navLink(Nav.SETTINGS, active));
        if (ctx.isAdmin()) {
            nav.add(Html.div("nav-label", "Administration"));
            for (Nav n : new Nav[]{Nav.ADMIN, Nav.ADMIN_USERS, Nav.ADMIN_FOODS, Nav.ADMIN_AUDIT}) nav.add(navLink(n, active));
        }

        Element logout = Forms.post("/logout", ctx.csrf()).cls("logout")
                .add(new Element("button").attr("type", "submit").cls("nav-link").add(Icon.LOGOUT.svg(), Html.span(null, "Log out")));
        Element userChip = Html.a(member ? "/profile" : "/settings", "user-chip", Ui.avatar(ctx.user(), null),
                Html.span("user-meta", Html.span("user-name", ctx.user().getFullName()),
                        Html.span("user-role", ctx.user().isAdmin() ? "Administrator" : "@" + ctx.user().getUsername())));

        Element sidebar = new Element("aside").cls("sidebar").attr("aria-label", "Navigation").add(
                brand(member ? "/dashboard" : "/settings"),
                nav,
                Html.div("sidebar-foot", userChip, logout));

        Element topbar = new Element("header").cls("topbar").add(
                new Element("label").attr("for", "nav-toggle").cls("menu-btn").attr("aria-label", "Open menu").add(Icon.MENU.svg()),
                brand(member ? "/dashboard" : "/settings"));

        Element main = new Element("main").id("main").cls("content").add(Ui.flashes(ctx.flashes())).add(content);
        Node page = Html.fragment(
                Html.a("#main", "skip", "Skip to content"),
                new Element("input").attr("type", "checkbox").id("nav-toggle").cls("nav-toggle").attr("aria-hidden", "true").attr("tabindex", "-1"),
                Html.div("shell", sidebar,
                        new Element("label").attr("for", "nav-toggle").cls("scrim").attr("aria-hidden", "true"),
                        Html.div("main", topbar, main,
                                new Element("footer").cls("app-foot").text(
                                        "Nutri Planner gives general nutrition guidance, not medical advice. Talk to a doctor or registered dietitian about medical conditions."))));
        return Response.html(status, document(title, ctx.themeClass(), page));
    }

    private static Element navLink(Nav n, Nav active) {
        Element a = Html.a(n.href, "nav-link", n.icon.svg(), Html.span(null, n.label));
        if (n == active) a.cls("active").attr("aria-current", "page");
        return a;
    }

    public static Element brand(String href) {
        return Html.a(href, "brand", logo("brand-mark"), Html.span("brand-name", "Nutri Planner"));
    }

    /** The mark: a plate seen from above, with a leaf resting on it. */
    public static Element logo(String cls) {
        Element svg = Svg.root(40, 40, cls).attr("aria-hidden", "true");
        svg.add(Svg.circle(20, 20, 18.5, null).attr("fill", "#1D6B50"));
        svg.add(Svg.circle(20, 20, 12.5, null).attr("fill", "none").attr("stroke", "#9FD3B8").attr("stroke-width", "1.6"));
        svg.add(Svg.path("M14 25.5c0-6.6 4.4-10.6 12.2-10.9 0 7.4-4.3 11.4-12.2 10.9z", null).attr("fill", "#F3B53A"));
        svg.add(Svg.path("M14.6 25 C18 21.6 20.6 19.6 23.6 18", null).attr("fill", "none").attr("stroke", "#1D6B50")
                .attr("stroke-width", "1.4").attr("stroke-linecap", "round"));
        return svg;
    }

    // ---------------------------------------------------------------- auth

    public static Response auth(ViewContext ctx, int status, String title, Node aside, Node... main) {
        Element page = Html.div("auth",
                Html.div("auth-aside", brand("/"), aside,
                        Html.p("small aside-extra", "General guidance, not medical advice.")),
                new Element("main").id("main").cls("auth-main").add(
                        Html.div("auth-card", Ui.flashes(ctx.flashes())).add(main)));
        return Response.html(status, document(title, ctx.themeClass(), page));
    }

    // ---------------------------------------------------------------- public

    /** A public page with the site header, for visitors who aren't logged in. */
    public static Response site(ViewContext ctx, int status, String title, Node... content) {
        Element head = new Element("header").cls("site-head").add(brand("/"),
                Html.div("btn-row", Ui.link("/login", "Log in", "btn-ghost", null), Ui.link("/register", "Create account", "btn-primary", null)));
        Element page = Html.div(null, head,
                new Element("main").id("main").cls("site-main").add(Ui.flashes(ctx.flashes())).add(content));
        return Response.html(status, document(title, ctx.themeClass(), page));
    }

    public static Response bare(ViewContext ctx, int status, String title, Node... content) {
        Element page = Html.div(null, new Element("header").cls("site-head").add(brand("/")),
                new Element("main").id("main").cls("onboard").add(Ui.flashes(ctx.flashes())).add(content));
        return Response.html(status, document(title, ctx.themeClass(), page));
    }
}
