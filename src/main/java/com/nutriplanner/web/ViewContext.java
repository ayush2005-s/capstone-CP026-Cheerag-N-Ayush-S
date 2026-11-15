package com.nutriplanner.web;

import com.nutriplanner.config.AppConfig;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.ThemePreference;
import com.nutriplanner.domain.User;
import com.nutriplanner.web.http.Request;
import com.nutriplanner.web.http.Session;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** What every page knows about the current request. */
public record ViewContext(Request request, User user, Profile profile, String csrf, List<Session.Flash> flashes,
                          String path, ThemePreference theme, LocalDate today, AppConfig config, ZoneId zone) {

    public long userId() {
        return user == null ? 0 : user.getId();
    }

    public boolean isAdmin() {
        return user != null && user.isAdmin();
    }

    public boolean onboarded() {
        return profile != null && profile.isCompleted();
    }

    public String themeClass() {
        switch (theme) {
            case DARK: return "theme-dark";
            case LIGHT: return "theme-light";
            default: return "theme-system";
        }
    }
}
