package com.nutriplanner.web;

import com.nutriplanner.config.AppConfig;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.ThemePreference;
import com.nutriplanner.service.Services;
import com.nutriplanner.web.http.HttpAuth;
import com.nutriplanner.web.http.RateLimiter;
import com.nutriplanner.web.http.Request;
import com.nutriplanner.web.http.Session;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Everything a page handler needs: services, settings and helpers for building views. */
public final class Web {

    public final Services services;
    public final AppConfig config;
    public final HttpAuth auth;
    public final RateLimiter loginLimiter;
    public final Clock clock;

    public Web(Services services, AppConfig config, HttpAuth auth, RateLimiter loginLimiter) {
        this.services = services;
        this.config = config;
        this.auth = auth;
        this.loginLimiter = loginLimiter;
        this.clock = services.clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Per-request information that layouts and pages need. */
    public ViewContext context(Request req) {
        Profile profile = req.user() == null ? null : services.profiles.profile(req.user().getId());
        ThemePreference theme = profile == null ? ThemePreference.SYSTEM : profile.getTheme();
        List<Session.Flash> flashes = req.session() == null ? List.of() : req.session().takeFlashes();
        return new ViewContext(req, req.user(), profile, req.session() == null ? "" : req.session().csrfToken(), flashes,
                req.path(), theme, today(), config, clock.getZone());
    }
}
