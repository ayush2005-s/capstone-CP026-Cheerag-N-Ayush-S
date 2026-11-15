package com.nutriplanner.web.view;

import com.nutriplanner.web.ViewContext;
import com.nutriplanner.web.Web;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.http.Request;
import com.nutriplanner.web.http.Response;
import com.nutriplanner.web.http.WebServer;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;

/** Error pages in the site's own look, inside the app frame when someone is logged in. */
public final class ErrorPage implements WebServer.ErrorPages {

    private final Web web;

    public ErrorPage(Web web) {
        this.web = web;
    }

    @Override
    public Response render(Request request, int status, String title, String message) {
        ViewContext ctx = web.context(request);
        Icon icon = status == 404 ? Icon.SEARCH : status == 403 ? Icon.LOCK : Icon.ALERT;
        Element home = ctx.user() == null ? Ui.link("/", "Go to the home page", "btn-primary", null)
                : Ui.link(ctx.onboarded() ? "/dashboard" : ctx.isAdmin() ? "/admin" : "/onboarding", "Back to Nutri Planner", "btn-primary", null);
        Element body = Html.div("stack", Ui.empty(icon, title, message, home), Html.p("small muted", "Error " + status));
        if (ctx.user() != null) return Layout.app(ctx, status, title, Layout.Nav.NONE, body);
        return Layout.bare(ctx, status, title, body);
    }
}
