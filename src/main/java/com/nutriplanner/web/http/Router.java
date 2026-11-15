package com.nutriplanner.web.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Maps method + path patterns (with {@code {name}} variables) to handlers and their access rules. */
public final class Router {

    /** Who may use a route. */
    public enum Access {
        /** Anyone. */
        PUBLIC,
        /** Only visitors who are not logged in (login, sign-up); members are sent to the dashboard. */
        GUEST,
        /** Any logged-in account, even before onboarding is finished. */
        AUTHENTICATED,
        /** Logged-in members who have finished onboarding. */
        MEMBER,
        /** Administrators only. */
        ADMIN
    }

    @FunctionalInterface
    public interface Handler {
        Response handle(Request request) throws Exception;
    }

    public record Route(String method, String pattern, String[] segments, Access access, Handler handler,
                        boolean allowedWhilePasswordChangeRequired) {
    }

    public record Match(Route route, Map<String, String> variables) {
    }

    private final List<Route> routes = new ArrayList<>();

    public Router get(String pattern, Access access, Handler handler) {
        return add("GET", pattern, access, handler, false);
    }

    public Router post(String pattern, Access access, Handler handler) {
        return add("POST", pattern, access, handler, false);
    }

    /** Registers a route that stays reachable while the user is being forced to change their password. */
    public Router add(String method, String pattern, Access access, Handler handler, boolean allowedDuringPasswordChange) {
        String[] segments = split(pattern);
        routes.add(new Route(method, pattern, segments, access, handler, allowedDuringPasswordChange));
        return this;
    }

    /** The matching route, or null. Sets {@code methodMismatch[0]} when the path exists for another method. */
    public Match match(String method, String path, boolean[] methodMismatch) {
        String[] parts = split(path);
        for (Route route : routes) {
            Map<String, String> vars = matchSegments(route.segments(), parts);
            if (vars == null) continue;
            String m = method.equals("HEAD") ? "GET" : method;
            if (route.method().equals(m)) return new Match(route, vars);
            methodMismatch[0] = true;
        }
        return null;
    }

    private static Map<String, String> matchSegments(String[] pattern, String[] parts) {
        if (pattern.length != parts.length) return null;
        Map<String, String> vars = new HashMap<>();
        for (int i = 0; i < pattern.length; i++) {
            String p = pattern[i];
            if (p.startsWith("{") && p.endsWith("}")) {
                if (parts[i].isEmpty()) return null;
                vars.put(p.substring(1, p.length() - 1), parts[i]);
            } else if (!p.equals(parts[i])) {
                return null;
            }
        }
        return vars;
    }

    private static String[] split(String path) {
        String p = path.startsWith("/") ? path.substring(1) : path;
        if (p.isEmpty()) return new String[0];
        return p.split("/", -1);
    }
}
