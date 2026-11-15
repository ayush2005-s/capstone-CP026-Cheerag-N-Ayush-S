package com.nutriplanner.web.controller;

import com.nutriplanner.domain.ThemePreference;
import com.nutriplanner.domain.User;
import com.nutriplanner.security.PasswordPolicy;
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
import com.nutriplanner.web.ui.FormState;
import com.nutriplanner.web.ui.Forms;
import com.nutriplanner.web.ui.Fmt;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;
import com.nutriplanner.web.view.Layout;

import java.util.ArrayList;
import java.util.List;

import static com.nutriplanner.web.http.Router.Access.AUTHENTICATED;

/** Password, appearance, session and account deletion. */
public final class SettingsController {

    private final Web web;

    public SettingsController(Web web) {
        this.web = web;
    }

    public void register(Router r) {
        r.get("/settings", AUTHENTICATED, req -> page(req, null, FormState.empty(), 200));
        r.add("GET", "/settings/password", AUTHENTICATED, req -> passwordPage(req, FormState.empty(), 200), true);
        r.add("POST", "/settings/password", AUTHENTICATED, this::changePassword, true);
        r.post("/settings/theme", AUTHENTICATED, this::theme);
        r.post("/settings/delete-account", AUTHENTICATED, this::deleteAccount);
    }

    private Response page(Request req, String failed, FormState st, int status) {
        ViewContext ctx = web.context(req);
        User u = ctx.user();
        FormState pw = "password".equals(failed) ? st : FormState.empty();
        FormState del = "delete".equals(failed) ? st : FormState.empty();
        FormState theme = FormState.empty().set("theme", ctx.theme());

        List<Forms.Choice> themes = new ArrayList<>();
        for (ThemePreference t : ThemePreference.values()) {
            themes.add(new Forms.Choice(t.name(), t.label(), t == ThemePreference.SYSTEM ? "Light or dark, following your device" : null));
        }
        Element appearance = Ui.panel("Appearance", null, null, Forms.post("/settings/theme", ctx.csrf()).cls("form").add(
                Forms.choices(theme, "theme", "Theme", themes, "cols-2"),
                Html.div("form-actions", Forms.submit("Save appearance", "btn-primary", Icon.CHECK))));

        Element security = Ui.panel("Password", "Last changed " + Fmt.dateTime(u.getPasswordChangedAt(), ctx.zone()), null, passwordForm(ctx, pw));

        Element session = Ui.panel("Sign-in activity", null, null, new Element("dl").cls("kv").add(
                kv("Username", "@" + u.getUsername()),
                kv("Role", u.getRole().label()),
                kv("Last login", Fmt.dateTime(u.getLastLoginAt(), ctx.zone())),
                kv("This session started", Fmt.dateTime(req.session().authenticatedAt(), ctx.zone())),
                kv("Automatic logout", "After " + ctx.config().sessionIdleMinutes + " minutes without activity")));

        String id = "delete-account";
        Element deleteForm = Forms.post("/settings/delete-account", ctx.csrf()).cls("form").add(Forms.formMessage(del),
                Forms.input(del, "password", "Your password", "password", null, true, "current-password"),
                Forms.text(del, "confirmation", "Type DELETE to confirm", null, true),
                Html.div("form-actions", Forms.submit("Delete my account and data", "btn-danger", Icon.TRASH)));
        Element danger = Ui.panel("Delete account", "Permanently removes your profile, diary, weigh-ins, meal plans and custom foods.", null,
                "delete".equals(failed) ? deleteForm : Ui.confirmTrigger(id, "Delete my account", "btn-danger-ghost", Icon.TRASH));
        danger.cls("danger");
        Node modal = Ui.confirm(id, "Delete your account?", "This can't be undone. Enter your password and type DELETE to confirm.", deleteForm);

        return Layout.app(ctx, status, "Settings", Layout.Nav.SETTINGS,
                Ui.pageHead("Settings", "Security and appearance for @" + u.getUsername() + "."),
                Html.div("grid g-2", Html.div("stack", security, appearance), Html.div("stack", session, danger)), modal);
    }

    private static Node kv(String k, String v) {
        return Html.fragment(new Element("dt").text(k), new Element("dd").text(v));
    }

    private static Element passwordForm(ViewContext ctx, FormState st) {
        return Forms.post("/settings/password", ctx.csrf()).cls("form").add(Forms.formMessage(st),
                Forms.input(st, "currentPassword", "Current password", "password", null, true, "current-password"),
                Forms.input(st, "newPassword", "New password", "password", PasswordPolicy.HINT, true, "new-password"),
                Forms.input(st, "confirmPassword", "Repeat new password", "password", null, true, "new-password"),
                Html.div("form-actions", Forms.submit("Change password", "btn-primary", Icon.KEY)));
    }

    /** Also shown on its own when an administrator requires a new password. */
    private Response passwordPage(Request req, FormState st, int status) {
        ViewContext ctx = web.context(req);
        if (!ctx.user().isMustChangePassword()) return Response.redirect("/settings");
        return Layout.bare(ctx, status, "Choose a new password",
                Html.div("stack", Html.div(null, Html.h(1, "page-title", "Choose a new password"),
                                Html.p("page-sub", "Your account has a temporary password. Pick your own to continue.")),
                        Html.div("panel", passwordForm(ctx, st)),
                        Forms.post("/logout", ctx.csrf()).cls("inline").add(Forms.submit("Log out instead", "btn-ghost", Icon.LOGOUT))));
    }

    private Response changePassword(Request req) {
        boolean forced = req.user().isMustChangePassword();
        try {
            web.services.auth.changePassword(req.user().getId(), req.param("currentPassword"), req.param("newPassword"),
                    req.param("confirmPassword"), req.clientIp());
            req.session().success("Your password has been changed.");
            if (forced) {
                boolean onboarded = web.services.profiles.profile(req.user().getId()).isCompleted();
                return Response.redirect(onboarded ? "/dashboard" : req.user().isAdmin() ? "/admin" : "/onboarding");
            }
            return Response.redirect("/settings");
        } catch (ValidationException e) {
            FormState st = FormState.empty().errors(e.fieldErrors());
            if (e.fieldErrors().isEmpty()) st.message(e.getMessage());
            return forced ? passwordPage(req, st, 422) : page(req, "password", st, 422);
        }
    }

    private Response theme(Request req) {
        ThemePreference theme = new Params(req).choice("theme", ThemePreference.class);
        web.services.profiles.updateTheme(req.user().getId(), theme);
        req.session().success("Appearance saved.");
        return Response.redirect("/settings");
    }

    private Response deleteAccount(Request req) {
        User user = req.user();
        try {
            web.services.accounts.deleteOwnAccount(user, req.param("password"), req.param("confirmation"), req.clientIp());
        } catch (ValidationException e) {
            FormState st = FormState.from(req).errors(e.fieldErrors());
            if (e.fieldErrors().isEmpty()) st.message(e.getMessage());
            return page(req, "delete", st, 422);
        }
        web.auth.logout(req);
        web.auth.logoutEverywhere(user.getId());
        req.session().success("Your account and all its data have been deleted.");
        return Response.redirect("/login");
    }
}
