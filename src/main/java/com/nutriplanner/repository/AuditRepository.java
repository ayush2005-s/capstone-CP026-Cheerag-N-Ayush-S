package com.nutriplanner.repository;

import com.nutriplanner.domain.AuditEvent;
import com.nutriplanner.storage.Database;
import com.nutriplanner.storage.EntityStore;

import java.util.Comparator;
import java.util.List;

public final class AuditRepository {

    /** Oldest events are pruned once the log grows past this size. */
    static final int MAX_EVENTS = 20_000;

    private final EntityStore<AuditEvent> store;
    private final Database db;

    public AuditRepository(Database db) {
        this.db = db;
        this.store = new EntityStore<>(db, "audit", AuditEvent.MAPPER);
    }

    public AuditEvent insert(AuditEvent event) {
        return db.write(() -> {
            AuditEvent saved = store.insert(event);
            if (store.count() > MAX_EVENTS + 500) {
                List<AuditEvent> all = store.findAll();
                for (int i = 0; i < all.size() - MAX_EVENTS; i++) store.delete(all.get(i).getId());
            }
            return saved;
        });
    }

    /** All events, newest first. */
    public List<AuditEvent> findAllNewestFirst() {
        List<AuditEvent> all = store.findAll();
        all.sort(Comparator.comparingLong(AuditEvent::getId).reversed());
        return all;
    }

    public long count() {
        return store.count();
    }
}
