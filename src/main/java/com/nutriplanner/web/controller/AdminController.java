package com.nutriplanner.web.controller;

import com.nutriplanner.domain.AccountStatus;
import com.nutriplanner.domain.AuditEvent;
import com.nutriplanner.domain.AuditType;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodCategory;
import com.nutriplanner.domain.Role;
import com.nutriplanner.domain.User;
import com.nutriplanner.service.AdminService;
import com.nutriplanner.service.FoodService;
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
import java.time.LocalDate;
import java.util.List;

import static com.nutriplanner.web.http.Router.Access.ADMIN;

/** The admin panel: overview, member management, the global food database and the audit log. */
public final class AdminController {

    private final Web web;

    public AdminController(Web web) {
        this.web = web;
    }

    public void register(Router r) {
        r.get("/admin", ADMIN, this::overview);
        r.get("/admin/users", ADMIN, this::users);
        r.get("/admin/users/{id}", ADMIN, this::user);
        r.post("/admin/users/{id}/status", ADMIN, this::status);
        r.post("/admin/users/{id}/role", ADMIN, this::role);
        r.post("/admin/users/{id}/unlock", ADMIN, this::unlock);
        r.post("/admin/users/{id}/reset-password", ADMIN, this::resetPassword);
        r.post("/admin/users/{id}/delete", ADMIN, this::deleteUser);
        r.get("/admin/foods", ADMIN, this::foods);
        r.get("/admin/foods/new", ADMIN, req -> foodForm(req, null, FoodController.blankState(), 200));
        r.post("/admin/foods/new", ADMIN, req -> saveFood(req, null));
        r.get("/admin/foods/{id}/edit", ADMIN, req -> {
            Food f = web.services.foods.anyFood(req.pathId("id"));
            return foodForm(req, f, FoodForm.fill(f, FormState.empty()), 200);
        });
        r.post("/admin/foods/{id}/edit", ADMIN, req -> saveFood(req, web.services.foods.anyFood(req.pathId("id"))));
        r.post("/admin/foods/{id}/active", ADMIN, this::foodActive);
        r.post("/admin/foods/{id}/delete", ADMIN, this::deleteFood);
        r.get("/admin/audit", ADMIN, this::audit);
    }

    // ---------------------------------------------------------------- overview

    private Response overview(Request req) {
        ViewContext ctx = web.context(req);
        AdminService.Overview o = web.services.admin.overview(ctx.user());
        Element stats = Html.div("stats",
                Ui.stat("Members", Integer.toString(o.users()), null, o.admins() + (o.admins() == 1 ? " administrator" : " administrators")),
                Ui.stat("Active this week", Integer.toString(o.activeLast7Days()), null, "Logged in within 7 days"),
                Ui.stat("New this month", Integer.toString(o.newLast30Days()), null, "Joined in the last 30 days"),
                Ui.stat("Disabled or locked", Integer.toString(o.disabled() + o.locked()), null, o.locked() + " locked right now"),
                Ui.stat("Foods", Integer.toString(o.globalFoods()), null, o.inactiveFoods() + " hidden, " + o.customFoods() + " custom"),
                Ui.stat("Diary entries", Fmt.kcal(o.diaryEntries()), null, o.entriesLast7Days() + " in the last 7 days"));
        return Layout.app(ctx, "Administration", Layout.Nav.ADMIN,
                Ui.pageHead("Administration", "How Nutri Planner is being used, and what needs attention.",
                        Ui.link("/admin/users", "Manage members", "btn-secondary", Icon.USERS),
                        Ui.link("/admin/foods/new", "Add a food", "btn-primary", Icon.PLUS)),
                Html.div("stack", stats,
                        Html.div("grid g-2",
                                Ui.panel("Sign-ups", "Last 14 days", null, Charts.countBars(o.signups())),
                                Ui.panel("Diary entries logged", "Last 14 days", null, Charts.countBars(o.activity()))),
                        Ui.panel("Recent events", null, Ui.link("/admin/audit", "Full audit log", "btn-ghost btn-sm", null), auditTable(ctx, o.recentEvents()))));
    }

    private static Node auditTable(ViewContext ctx, List<AuditEvent> events) {
        if (events.isEmpty()) return Html.p("muted", "No events yet.");
        Ui.Table t = new Ui.Table().caption("Audit events").col("When", false).col("Event", false).col("Account", false)
                .col("Details", false).col("IP address", false);
        for (AuditEvent e : events) {
            String tone = e.getType().isSecurityEvent() ? "chili" : e.getType().name().startsWith("ADMIN") ? "blueberry" : null;
            t.row(Html.span("nowrap", Fmt.dateTime(e.getAt(), ctx.zone())), Ui.badge(e.getType().label(), tone),
                    Html.text(e.getActorName().isEmpty() ? "–" : e.getActorName()), Html.span("small", e.getDetail()), Html.span("small muted", e.getIp()));
        }
        return t.build();
    }

    // ------------------------------------------------------------------ users

    private Response users(Request req) {
        ViewContext ctx = web.context(req);
        Params p = new Params(req);
        String q = p.text("q");
        Role role = p.choice("role", Role.class);
        AccountStatus status = p.choice("status", AccountStatus.class);
        Page<User> page = web.services.admin.users(ctx.user(), q, role, status, p.page(), 20);
        Element filters = Forms.get("/admin/users").cls("panel mb-3").add(Html.div("filters",
                Html.div("field search-field", new Element("label").cls("label").attr("for", "q").text("Search"),
                        Html.div("search-wrap", Icon.SEARCH.svg(), new Element("input").cls("input search-input").id("q").attr("type", "search")
                                .attr("name", "q").attr("value", q).attr("placeholder", "Name, username or email"))),
                Html.div("field", Html.span("label", "Role"), Forms.bareSelect("role", Forms.Option.of(Role.values()), role == null ? "" : role.name(), "Any", "Role")),
                Html.div("field", Html.span("label", "Status"), Forms.bareSelect("status", Forms.Option.of(AccountStatus.values()), status == null ? "" : status.name(), "Any", "Status")),
                Html.div("field", Forms.submit("Search", "btn-primary", Icon.FILTER))));
        Ui.Table t = new Ui.Table().caption("Members").col("Member", false).col("Role", false).col("Status", false).col("Joined", false)
                .col("Last login", false).col("", false);
        for (User u : page.items()) {
            t.row(Html.div("row", Ui.avatar(u, null), Html.div(null, Html.a("/admin/users/" + u.getId(), "row-link", u.getFullName()),
                            Html.span("cell-sub", "@" + u.getUsername() + ", " + u.getEmail()))),
                    Ui.badge(u.getRole().label(), u.isAdmin() ? "blueberry" : null),
                    statusLabel(u, web.clock.instant()),
                    Html.span("nowrap small", u.getCreatedAt() == null ? "–" : Fmt.mediumDate(LocalDate.ofInstant(u.getCreatedAt(), ctx.zone()))),
                    Html.span("nowrap small", Fmt.dateTime(u.getLastLoginAt(), ctx.zone())),
                    Html.div("text-right", Ui.link("/admin/users/" + u.getId(), "Manage", "btn-secondary btn-sm", null)));
        }
        String base = "/admin/users?q=" + enc(q) + (role == null ? "" : "&role=" + role) + (status == null ? "" : "&status=" + status);
        return Layout.app(ctx, "Members", Layout.Nav.ADMIN_USERS,
                Ui.pageHead("Members", page.total() + (page.total() == 1 ? " account" : " accounts")),
                filters, Ui.panel(null, null, null, page.total() == 0 ? Ui.empty(Icon.USERS, "No accounts match", "Try a different search.", null) : t.build(),
                        Ui.pager(page, n -> base + "&page=" + n, "accounts")));
    }

    private static Element statusLabel(User u, java.time.Instant now) {
        if (!u.isActive()) return Html.span("nowrap", Html.span("status-dot off", ""), Html.text("Disabled"));
        if (u.isLocked(now)) return Html.span("nowrap", Html.span("status-dot locked", ""), Html.text("Locked"));
        return Html.span("nowrap", Html.span("status-dot", ""), Html.text("Active"));
    }

    private Response user(Request req) {
        ViewContext ctx = web.context(req);
        AdminService.UserDetail d = web.services.admin.user(ctx.user(), req.pathId("id"));
        User u = d.user();
        boolean self = u.getId() == ctx.userId();
        String base = "/admin/users/" + u.getId();
        String temp = req.session().take("temp-password:" + u.getId());

        Element account = Ui.panel("Account", null, null, new Element("dl").cls("kv").add(
                kv("Status", statusLabel(u, web.clock.instant())),
                kv("Role", Html.text(u.getRole().label())),
                kv("Joined", Html.text(Fmt.dateTime(u.getCreatedAt(), ctx.zone()))),
                kv("Last login", Html.text(Fmt.dateTime(u.getLastLoginAt(), ctx.zone()))),
                kv("Failed logins", Html.text(Integer.toString(u.getFailedLogins()))),
                kv("Locked until", Html.text(u.isLocked(web.clock.instant()) ? Fmt.dateTime(u.getLockedUntil(), ctx.zone()) : "Not locked")),
                kv("Password", Html.text(u.isMustChangePassword() ? "Must be changed at next login" : "Set by the member"))));
        Element activity = Ui.panel("Activity", null, null, new Element("dl").cls("kv").add(
                kv("Diary entries", Html.text(Integer.toString(d.diaryEntries()))),
                kv("Last logged food", Html.text(d.lastLogged() == null ? "Never" : Fmt.mediumDate(d.lastLogged()))),
                kv("Weigh-ins", Html.text(Integer.toString(d.weightEntries()))),
                kv("Custom foods", Html.text(Integer.toString(d.customFoods()))),
                kv("Diet", Html.text(d.profile() == null || !d.profile().isCompleted() ? "Setup not finished" : d.profile().getDietType().label())),
                kv("Goal", Html.text(d.profile() == null || !d.profile().isCompleted() ? "–" : d.profile().getGoal().label()))));

        Element actions = Html.div("stack-sm");
        if (self) {
            actions.add(Html.p("muted", "This is your own account. Change your password or delete your account from Settings."));
        } else {
            actions.add(Forms.post(base + "/status", ctx.csrf()).cls("inline").add(
                    Forms.hidden("status", u.isActive() ? "DISABLED" : "ACTIVE"),
                    Forms.submit(u.isActive() ? "Disable account" : "Re-enable account", u.isActive() ? "btn-secondary btn-block" : "btn-primary btn-block",
                            u.isActive() ? Icon.LOCK : Icon.UNLOCK)));
            actions.add(Forms.post(base + "/role", ctx.csrf()).cls("inline").add(
                    Forms.hidden("role", u.isAdmin() ? "USER" : "ADMIN"),
                    Forms.submit(u.isAdmin() ? "Remove administrator role" : "Make administrator", "btn-secondary btn-block", Icon.SHIELD)));
            if (u.isLocked(web.clock.instant())) {
                actions.add(Forms.post(base + "/unlock", ctx.csrf()).cls("inline").add(Forms.submit("Unlock now", "btn-secondary btn-block", Icon.UNLOCK)));
            }
            actions.add(Ui.confirmTrigger("reset", "Reset password", "btn-secondary btn-block", Icon.KEY));
            actions.add(Ui.confirmTrigger("delete", "Delete account", "btn-danger-ghost btn-block", Icon.TRASH));
        }
        Node resetModal = Ui.confirm("reset", "Reset " + u.getUsername() + "'s password?",
                "A temporary password will be shown once. They'll have to choose a new one when they log in.",
                Forms.post(base + "/reset-password", ctx.csrf()).cls("inline").add(Forms.submit("Reset password", "btn-primary", Icon.KEY)));
        Node deleteModal = Ui.confirm("delete", "Delete " + u.getUsername() + "?",
                "Their profile, diary, weigh-ins, plans and custom foods are deleted permanently. Type the username to confirm.",
                Forms.post(base + "/delete", ctx.csrf()).cls("inline-form").add(
                        new Element("label").cls("sr-only").attr("for", "confirmUsername").text("Username"),
                        new Element("input").cls("input input-sm").id("confirmUsername").attr("name", "confirmUsername")
                                .attr("placeholder", u.getUsername()).attr("required", "").attr("autocomplete", "off"),
                        Forms.submit("Delete", "btn-danger", Icon.TRASH)));

        Element side = Html.div("stack");
        if (temp != null) {
            side.add(Ui.panel("Temporary password", "Share it securely. It won't be shown again.", null, Html.p("secret", temp)));
        }
        side.add(Ui.panel("Actions", null, null, actions));
        return Layout.app(ctx, u.getFullName(), Layout.Nav.ADMIN_USERS,
                Ui.backLink("/admin/users", "All members"),
                Html.div("row mb-3", Ui.avatar(u, "avatar-lg"),
                        Html.div(null, Html.h(1, "page-title", u.getFullName()), Html.p("page-sub", "@" + u.getUsername() + ", " + u.getEmail()))),
                Html.div("grid g-main", Html.div("stack", account, activity), side), resetModal, deleteModal);
    }

    private static Node kv(String k, Node v) {
        return Html.fragment(new Element("dt").text(k), new Element("dd").add(v));
    }

    private Response status(Request req) {
        long id = req.pathId("id");
        AccountStatus s = new Params(req).choice("status", AccountStatus.class);
        web.services.admin.setStatus(req.user(), id, s, req.clientIp());
        if (s == AccountStatus.DISABLED) web.auth.logoutEverywhere(id);
        req.session().success(s == AccountStatus.DISABLED ? "Account disabled and signed out everywhere." : "Account re-enabled.");
        return Response.redirect("/admin/users/" + id);
    }

    private Response role(Request req) {
        long id = req.pathId("id");
        Role role = new Params(req).choice("role", Role.class);
        web.services.admin.setRole(req.user(), id, role, req.clientIp());
        req.session().success(role == Role.ADMIN ? "They're now an administrator." : "Administrator role removed.");
        return Response.redirect("/admin/users/" + id);
    }

    private Response unlock(Request req) {
        long id = req.pathId("id");
        web.services.admin.unlock(req.user(), id, req.clientIp());
        req.session().success("Account unlocked.");
        return Response.redirect("/admin/users/" + id);
    }

    private Response resetPassword(Request req) {
        long id = req.pathId("id");
        String temp = web.services.admin.resetPassword(req.user(), id, req.clientIp());
        web.auth.logoutEverywhere(id);
        req.session().put("temp-password:" + id, temp);
        req.session().success("Password reset. They've been signed out of every device.");
        return Response.redirect("/admin/users/" + id);
    }

    private Response deleteUser(Request req) {
        long id = req.pathId("id");
        String username = web.services.admin.user(req.user(), id).user().getUsername();
        web.services.admin.deleteUser(req.user(), id, req.param("confirmUsername"), req.clientIp());
        web.auth.logoutEverywhere(id);
        req.session().success("Deleted " + username + " and all their data.");
        return Response.redirect("/admin/users");
    }

    // ------------------------------------------------------------------ foods

    private Response foods(Request req) {
        ViewContext ctx = web.context(req);
        AdminService.requireAdmin(ctx.user());
        Params p = new Params(req);
        String q = p.text("q");
        FoodCategory category = p.choice("category", FoodCategory.class);
        String state = p.text("state");
        Boolean active = state.equals("active") ? Boolean.TRUE : state.equals("hidden") ? Boolean.FALSE : null;
        Page<Food> page = web.services.foods.searchGlobal(q, category, active, p.page(), 25);
        Element filters = Forms.get("/admin/foods").cls("panel mb-3").add(Html.div("filters",
                Html.div("field search-field", new Element("label").cls("label").attr("for", "q").text("Search"),
                        Html.div("search-wrap", Icon.SEARCH.svg(), new Element("input").cls("input search-input").id("q").attr("type", "search")
                                .attr("name", "q").attr("value", q))),
                Html.div("field", Html.span("label", "Category"), Forms.bareSelect("category", Forms.Option.of(FoodCategory.values()),
                        category == null ? "" : category.name(), "All", "Category")),
                Html.div("field", Html.span("label", "Visibility"), Forms.bareSelect("state", List.of(new Forms.Option("active", "Visible"),
                        new Forms.Option("hidden", "Hidden")), state, "All", "Visibility")),
                Html.div("field", Forms.submit("Search", "btn-primary", Icon.FILTER))));
        Ui.Table t = new Ui.Table().caption("Foods").col("Food", false).col("Diet", false).col("Per 100 g", true).col("Protein", true)
                .col("Planner", false).col("", false);
        for (Food f : page.items()) {
            Element vis = Forms.post("/admin/foods/" + f.getId() + "/active", ctx.csrf(), "/admin/foods").cls("inline").add(
                    Forms.hidden("active", f.isActive() ? "false" : "true"),
                    Ui.iconButton(f.isActive() ? Icon.EYE_OFF : Icon.EYE, f.isActive() ? "Hide from members" : "Show to members", null));
            t.row(Html.div(null, Html.a("/admin/foods/" + f.getId() + "/edit", "row-link", f.getName()),
                            Html.span("cell-sub", f.getCategory().label() + (f.isActive() ? "" : ". Hidden from members"))),
                    Html.div("row", Ui.dietMark(f.getDiet()), Html.span("small", f.getDiet().label())),
                    Html.text(Fmt.kcal(f.getKcal()) + " kcal"), Html.text(Fmt.grams(f.getProtein())),
                    Html.span("small", f.getPlanRole().label()),
                    Html.div("btn-row", Ui.iconLink("/admin/foods/" + f.getId() + "/edit", Icon.EDIT, "Edit " + f.getName()), vis));
        }
        String base = "/admin/foods?q=" + enc(q) + (category == null ? "" : "&category=" + category) + (state.isEmpty() ? "" : "&state=" + state);
        return Layout.app(ctx, "Food database", Layout.Nav.ADMIN_FOODS,
                Ui.pageHead("Food database", page.total() + " shared foods. Members see visible foods; hidden ones stay out of search and plans.",
                        Ui.link("/admin/foods/new", "Add a food", "btn-primary", Icon.PLUS)),
                filters, Ui.panel(null, null, null, page.total() == 0 ? Ui.empty(Icon.SEARCH, "No foods match", null, null) : t.build(),
                        Ui.pager(page, n -> base + "&page=" + n, "foods")));
    }

    private Response foodForm(Request req, Food existing, FormState st, int status) {
        ViewContext ctx = web.context(req);
        if (existing != null && !existing.isGlobal()) throw new com.nutriplanner.service.NotFoundException("That's a member's private food.");
        String action = existing == null ? "/admin/foods/new" : "/admin/foods/" + existing.getId() + "/edit";
        Node[] headActions = new Node[0];
        Node modal = Node.EMPTY;
        if (existing != null) {
            modal = Ui.confirm("delete-food", "Delete " + existing.getName() + "?",
                    "It disappears from search and meal plans. Diary entries that already used it keep their numbers. To keep it but stop members seeing it, hide it instead.",
                    Forms.post("/admin/foods/" + existing.getId() + "/delete", ctx.csrf()).cls("inline").add(Forms.submit("Delete food", "btn-danger", Icon.TRASH)));
            headActions = new Node[]{Ui.confirmTrigger("delete-food", "Delete", "btn-danger-ghost", Icon.TRASH)};
        }
        return Layout.app(ctx, status, existing == null ? "Add a food" : "Edit " + existing.getName(), Layout.Nav.ADMIN_FOODS,
                Ui.backLink("/admin/foods", "Food database"),
                Ui.pageHead(existing == null ? "Add a food" : "Edit " + existing.getName(), "Shared foods are available to every member.", headActions),
                Html.div("panel", FoodForm.form(action, ctx.csrf(), st, existing == null ? "Add food" : "Save changes", "/admin/foods")), modal);
    }

    private Response saveFood(Request req, Food existing) {
        Params p = new Params(req);
        FoodService.FoodInput input = FoodForm.parse(p);
        try {
            if (p.hasErrors()) throw new ValidationException(p.errors());
            Food saved = web.services.admin.saveFood(req.user(), existing == null ? null : existing.getId(), input, req.clientIp());
            req.session().success((existing == null ? "Added " : "Saved ") + saved.getName() + ".");
            return Response.redirect("/admin/foods?q=" + enc(saved.getName()));
        } catch (ValidationException e) {
            FormState st = FormState.from(req).errors(e.fieldErrors());
            if (e.fieldErrors().isEmpty()) st.message(e.getMessage());
            return foodForm(req, existing, st, 422);
        }
    }

    private Response foodActive(Request req) {
        long id = req.pathId("id");
        boolean active = "true".equals(req.param("active"));
        web.services.admin.setFoodActive(req.user(), id, active, req.clientIp());
        req.session().success(active ? "The food is visible to members again." : "The food is hidden from members.");
        return Response.redirect("/admin/foods");
    }

    private Response deleteFood(Request req) {
        long id = req.pathId("id");
        String name = web.services.foods.anyFood(id).getName();
        web.services.admin.deleteFood(req.user(), id, req.clientIp());
        req.session().success("Deleted " + name + ".");
        return Response.redirect("/admin/foods");
    }

    // ------------------------------------------------------------------ audit

    private Response audit(Request req) {
        ViewContext ctx = web.context(req);
        Params p = new Params(req);
        AuditType type = p.choice("type", AuditType.class);
        String q = p.text("q");
        Page<AuditEvent> page = web.services.admin.audit(ctx.user(), type, q, p.page(), 30);
        Element filters = Forms.get("/admin/audit").cls("panel mb-3").add(Html.div("filters",
                Html.div("field search-field", new Element("label").cls("label").attr("for", "q").text("Search"),
                        Html.div("search-wrap", Icon.SEARCH.svg(), new Element("input").cls("input search-input").id("q").attr("type", "search")
                                .attr("name", "q").attr("value", q).attr("placeholder", "Account, detail or IP address"))),
                Html.div("field", Html.span("label", "Event"), Forms.bareSelect("type", Forms.Option.of(AuditType.values()), type == null ? "" : type.name(), "All events", "Event")),
                Html.div("field", Forms.submit("Search", "btn-primary", Icon.FILTER))));
        String base = "/admin/audit?q=" + enc(q) + (type == null ? "" : "&type=" + type);
        return Layout.app(ctx, "Audit log", Layout.Nav.ADMIN_AUDIT,
                Ui.pageHead("Audit log", "Logins, failed attempts, lockouts and every administrative change."),
                filters, Ui.panel(null, null, null, auditTable(ctx, page.items()), Ui.pager(page, n -> base + "&page=" + n, "events")));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
