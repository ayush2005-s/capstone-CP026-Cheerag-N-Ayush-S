package com.nutriplanner.service;

import com.nutriplanner.domain.AuditEvent;
import com.nutriplanner.domain.AuditType;
import com.nutriplanner.domain.User;
import com.nutriplanner.repository.AuditRepository;

import java.time.Clock;
import java.util.List;

/** Records security-relevant events for the admin audit log. */
public final class AuditService {

    private final AuditRepository repository;
    private final Clock clock;

    public AuditService(AuditRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(AuditType type, User actor, String detail, String ip) {
        long actorId = actor == null ? 0 : actor.getId();
        String actorName = actor == null ? "" : actor.getUsername();
        record(type, actorId, actorName, detail, ip);
    }

    public void record(AuditType type, long actorId, String actorName, String detail, String ip) {
        repository.insert(new AuditEvent(clock.instant(), type, actorId, actorName,
                detail == null ? "" : detail, ip == null ? "" : ip));
    }

    public List<AuditEvent> newestFirst() {
        return repository.findAllNewestFirst();
    }
}
