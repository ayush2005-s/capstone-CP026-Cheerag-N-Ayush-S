package com.nutriplanner.repository;

import com.nutriplanner.domain.FoodLogEntry;
import com.nutriplanner.domain.Keys;
import com.nutriplanner.storage.Database;
import com.nutriplanner.storage.EntityStore;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class FoodLogRepository {

    private final EntityStore<FoodLogEntry> store;
    private final EntityStore.Index<FoodLogEntry> byUserDate;

    public FoodLogRepository(Database db) {
        store = new EntityStore<>(db, "food_log", FoodLogEntry.MAPPER);
        byUserDate = store.index("user_date", e -> Keys.userDate(e.getUserId(), e.getDate()));
    }

    public Optional<FoodLogEntry> findById(long id) {
        return store.findById(id);
    }

    public List<FoodLogEntry> findByUserAndDate(long userId, LocalDate date) {
        return store.findByKey(byUserDate, Keys.userDate(userId, date));
    }

    /** Entries between two dates, both inclusive, ordered by date. */
    public List<FoodLogEntry> findByUserBetween(long userId, LocalDate from, LocalDate to) {
        return store.findRange(byUserDate, Keys.userDate(userId, from), Keys.userDate(userId, to.plusDays(1)));
    }

    public List<FoodLogEntry> findByUser(long userId) {
        return store.findByPrefix(byUserDate, Keys.userPrefix(userId));
    }

    public long count() {
        return store.count();
    }

    public List<FoodLogEntry> findAll() {
        return store.findAll();
    }

    public FoodLogEntry save(FoodLogEntry entry) {
        return store.save(entry);
    }

    public boolean delete(long id) {
        return store.delete(id);
    }

    public int deleteByUser(long userId) {
        int n = 0;
        for (FoodLogEntry e : findByUser(userId)) {
            if (store.delete(e.getId())) n++;
        }
        return n;
    }
}
