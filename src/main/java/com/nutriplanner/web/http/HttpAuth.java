package com.nutriplanner.web.http;

import com.nutriplanner.domain.User;

/** Logging a browser session in and out. */
public final class HttpAuth {

    private final SessionManager sessions;

    public HttpAuth(SessionManager sessions) {
        this.sessions = sessions;
    }

    /** Binds the session to the user under a fresh session id. */
    public void login(Request request, User user) {
        sessions.login(request.session(), user.getId());
        request.setUser(user);
    }

    /** Ends the current session and starts an anonymous one (so a "logged out" message can be shown). */
    public void logout(Request request) {
        sessions.invalidate(request.session());
        request.setSession(sessions.create());
        request.setUser(null);
    }

    public void logoutEverywhere(long userId) {
        sessions.invalidateUser(userId);
    }
}
