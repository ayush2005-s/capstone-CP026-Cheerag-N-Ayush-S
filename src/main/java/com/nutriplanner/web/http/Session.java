package com.nutriplanner.web.http;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side session state. The browser only ever holds the random session id in an HttpOnly cookie. */
public final class Session {

    /** A one-time message shown on the next page. */
    public record Flash(String kind, String message) {
    }

    private volatile String id;
    private final String csrfToken;
    private final Instant createdAt;
    private volatile Instant lastSeen;
    private volatile long userId;
    private volatile Instant authenticatedAt;
    private final List<Flash> flashes = new ArrayList<>();
    private final Map<String, String> attributes = new ConcurrentHashMap<>();

    Session(String id, String csrfToken, Instant now) {
        this.id = id;
        this.csrfToken = csrfToken;
        this.createdAt = now;
        this.lastSeen = now;
    }

    public String id() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    public String csrfToken() {
        return csrfToken;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastSeen() {
        return lastSeen;
    }

    void touch(Instant now) {
        this.lastSeen = now;
    }

    public long userId() {
        return userId;
    }

    public boolean isAuthenticated() {
        return userId > 0;
    }

    public Instant authenticatedAt() {
        return authenticatedAt;
    }

    void authenticate(long userId, Instant now) {
        this.userId = userId;
        this.authenticatedAt = now;
    }

    public synchronized void flash(String kind, String message) {
        flashes.add(new Flash(kind, message));
    }

    public void success(String message) {
        flash("success", message);
    }

    public void error(String message) {
        flash("error", message);
    }

    public void info(String message) {
        flash("info", message);
    }

    /** Returns and clears pending flash messages. */
    public synchronized List<Flash> takeFlashes() {
        List<Flash> out = new ArrayList<>(flashes);
        flashes.clear();
        return out;
    }

    public void put(String key, String value) {
        if (value == null) attributes.remove(key);
        else attributes.put(key, value);
    }

    public String get(String key) {
        return attributes.get(key);
    }

    public String take(String key) {
        return attributes.remove(key);
    }
}
