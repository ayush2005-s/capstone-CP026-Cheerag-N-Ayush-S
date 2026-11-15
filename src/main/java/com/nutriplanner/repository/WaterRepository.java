package com.nutriplanner.repository;

import com.nutriplanner.domain.Keys;
import com.nutriplanner.domain.WaterLog;
import com.nutriplanner.storage.Database;
import com.nutriplanner.storage.EntityStore;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class WaterRepository {

    private final EntityStore<WaterLog> store;
    private final EntityStore.Index<WaterLog> byUserDate;

    public WaterRepository(Database db) {
        store = new EntityStore<>(db, "water", WaterLog.MAPPER);
        byUserDate = store.uniqueIndex("user_date", w -> Keys.userDate(w.getUserId(), w.getDate()));
    }

    public Optional<WaterLog> findByUserAndDate(long userId, LocalDate date) {
        return store.findUnique(byUserDate, Keys.userDate(userId, date));
    }

    public List<WaterLog> findByUserBetween(long userId, LocalDate from, LocalDate to) {
        return store.findRange(byUserDate, Keys.userDate(userId, from), Keys.userDate(userId, to.plusDays(1)));
    }

    public WaterLog save(WaterLog log) {
        return store.save(log);
    }

    public int deleteByUser(long userId) {
        int n = 0;
        for (WaterLog w : store.findByPrefix(byUserDate, Keys.userPrefix(userId))) {
            if (store.delete(w.getId())) n++;
        }
        return n;
    }
}
