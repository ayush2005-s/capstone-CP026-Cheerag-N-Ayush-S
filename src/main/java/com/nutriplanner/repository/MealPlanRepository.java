package com.nutriplanner.repository;

import com.nutriplanner.domain.Keys;
import com.nutriplanner.domain.MealPlanDay;
import com.nutriplanner.storage.Database;
import com.nutriplanner.storage.EntityStore;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class MealPlanRepository {

    private final EntityStore<MealPlanDay> store;
    private final EntityStore.Index<MealPlanDay> byUserDate;

    public MealPlanRepository(Database db) {
        store = new EntityStore<>(db, "meal_plans", MealPlanDay.MAPPER);
        byUserDate = store.uniqueIndex("user_date", p -> Keys.userDate(p.getUserId(), p.getDate()));
    }

    public Optional<MealPlanDay> findByUserAndDate(long userId, LocalDate date) {
        return store.findUnique(byUserDate, Keys.userDate(userId, date));
    }

    public List<MealPlanDay> findByUserBetween(long userId, LocalDate from, LocalDate to) {
        return store.findRange(byUserDate, Keys.userDate(userId, from), Keys.userDate(userId, to.plusDays(1)));
    }

    public List<MealPlanDay> findByUser(long userId) {
        return store.findByPrefix(byUserDate, Keys.userPrefix(userId));
    }

    public MealPlanDay save(MealPlanDay plan) {
        return store.save(plan);
    }

    public boolean delete(long id) {
        return store.delete(id);
    }

    public int deleteByUser(long userId) {
        int n = 0;
        for (MealPlanDay p : findByUser(userId)) {
            if (store.delete(p.getId())) n++;
        }
        return n;
    }
}
