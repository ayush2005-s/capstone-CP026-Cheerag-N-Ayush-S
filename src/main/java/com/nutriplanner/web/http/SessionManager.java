package com.nutriplanner.web.http;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store with idle and absolute timeouts. Session ids are 256-bit random values, and a new id is
 * issued whenever someone logs in to prevent session fixation.
 */
public final class SessionManager {

    public static final Duration ABSOLUTE_TIMEOUT = Duration.ofHours(12);
    private static final int MAX_SESSIONS = 50_000;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final Duration idleTimeout;

    public SessionManager(Clock clock, Duration idleTimeout) {
        this.clock = clock;
        this.idleTimeout = idleTimeout;
    }

    public Duration idleTimeout() {
        return idleTimeout;
    }

    public Session create() {
        if (sessions.size() >= MAX_SESSIONS) evict();
        Session s = new Session(token(), token(), clock.instant());
        sessions.put(s.id(), s);
        return s;
    }

    /** The live session for an id, or null if it is unknown or expired. */
    public Session find(String id) {
        if (id == null || id.isEmpty()) return null;
        Session s = sessions.get(id);
        if (s == null) return null;
        Instant now = clock.instant();
        if (expired(s, now)) {
            sessions.remove(id);
            return null;
        }
        s.touch(now);
        return s;
    }

    /** Marks the session as logged in for a user, under a fresh id. */
    public void login(Session session, long userId) {
        sessions.remove(session.id());
        session.setId(token());
        session.authenticate(userId, clock.instant());
        sessions.put(session.id(), session);
    }

    public void invalidate(Session session) {
        if (session != null) sessions.remove(session.id());
    }

    /** Ends every session of a user, for example after their account is disabled or deleted. */
    public void invalidateUser(long userId) {
        sessions.values().removeIf(s -> s.userId() == userId);
    }

    public void purgeExpired() {
        Instant now = clock.instant();
        sessions.values().removeIf(s -> expired(s, now));
    }

    public int size() {
        return sessions.size();
    }

    private boolean expired(Session s, Instant now) {
        return s.lastSeen().plus(idleTimeout).isBefore(now) || s.createdAt().plus(ABSOLUTE_TIMEOUT).isBefore(now);
    }

    private void evict() {
        purgeExpired();
        if (sessions.size() < MAX_SESSIONS) return;
        sessions.values().stream()
                .filter(s -> !s.isAuthenticated())
                .sorted(Comparator.comparing(Session::lastSeen))
                .limit(MAX_SESSIONS / 10)
                .forEach(s -> sessions.remove(s.id()));
    }

    private String token() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
