package com.nutriplanner.repository;

import com.nutriplanner.domain.Keys;
import com.nutriplanner.domain.WeightEntry;
import com.nutriplanner.storage.Database;
import com.nutriplanner.storage.EntityStore;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class WeightRepository {

    private final EntityStore<WeightEntry> store;
    private final EntityStore.Index<WeightEntry> byUserDate;

    public WeightRepository(Database db) {
        store = new EntityStore<>(db, "weights", WeightEntry.MAPPER);
        byUserDate = store.uniqueIndex("user_date", w -> Keys.userDate(w.getUserId(), w.getDate()));
    }

    public Optional<WeightEntry> findById(long id) {
        return store.findById(id);
    }

    public Optional<WeightEntry> findByUserAndDate(long userId, LocalDate date) {
        return store.findUnique(byUserDate, Keys.userDate(userId, date));
    }

    /** All entries of a user, oldest first. */
    public List<WeightEntry> findByUser(long userId) {
        return store.findByPrefix(byUserDate, Keys.userPrefix(userId));
    }

    public WeightEntry save(WeightEntry entry) {
        return store.save(entry);
    }

    public boolean delete(long id) {
        return store.delete(id);
    }

    public int deleteByUser(long userId) {
        int n = 0;
        for (WeightEntry w : findByUser(userId)) {
            if (store.delete(w.getId())) n++;
        }
        return n;
    }
}
