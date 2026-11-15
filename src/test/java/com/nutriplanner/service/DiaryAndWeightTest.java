package com.nutriplanner.service;

import com.nutriplanner.domain.DietType;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodLogEntry;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.PortionUnit;
import com.nutriplanner.domain.User;
import com.nutriplanner.domain.WeightEntry;
import com.nutriplanner.testkit.Test;
import com.nutriplanner.testkit.TestServices;

import java.util.List;
import java.util.Set;

import static com.nutriplanner.testkit.Assert.assertEquals;
import static com.nutriplanner.testkit.Assert.assertNear;
import static com.nutriplanner.testkit.Assert.assertThrows;
import static com.nutriplanner.testkit.Assert.assertTrue;

public class DiaryAndWeightTest extends TestServices {

    private Food food(String name) {
        return repos.foods.findAll().stream().filter(f -> f.getName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void logsServingsAndGrams() {
        User u = member("ravi", DietType.VEGETARIAN, Set.of());
        Food roti = food("Chapati / roti (whole wheat)");
        Food dal = food("Dal tadka (toor dal)");
        services.diary.add(u.getId(), TODAY, MealType.LUNCH, roti.getId(), 2.0, PortionUnit.SERVING);
        services.diary.add(u.getId(), TODAY, MealType.LUNCH, dal.getId(), 200.0, PortionUnit.GRAM);
        DiaryService.DaySummary day = services.diary.day(u.getId(), TODAY);
        assertEquals(2, day.meals().get(MealType.LUNCH).size());
        assertNear(2 * 40 * 2.64 + 200 * 1.28, day.total().kcal(), 0.01, "2 rotis + 200 g dal");
        assertNear(2 * 40 * 0.096 + 200 * 0.068, day.total().protein(), 0.01, "protein adds up");
        assertNear(day.total().kcal(), day.mealTotals().get(MealType.LUNCH).kcal(), 0.001, "lunch holds everything");
    }

    @Test
    void rejectsBadEntriesAndProtectsOtherUsers() {
        User u = member("ravi", DietType.VEGETARIAN, Set.of());
        User other = member("sita", DietType.VEGETARIAN, Set.of());
        Food apple = food("Apple");
        ValidationException future = assertThrows(ValidationException.class,
                () -> services.diary.add(u.getId(), TODAY.plusDays(1), MealType.SNACK, apple.getId(), 1.0, PortionUnit.SERVING));
        assertTrue(future.fieldErrors().containsKey("date"), "no logging into the future");
        ValidationException zero = assertThrows(ValidationException.class,
                () -> services.diary.add(u.getId(), TODAY, MealType.SNACK, apple.getId(), 0.0, PortionUnit.SERVING));
        assertTrue(zero.fieldErrors().containsKey("quantity"), "amount must be positive");
        FoodLogEntry e = services.diary.add(u.getId(), TODAY, MealType.SNACK, apple.getId(), 1.0, PortionUnit.SERVING);
        assertThrows(NotFoundException.class, () -> services.diary.delete(other.getId(), e.getId()));
        assertThrows(NotFoundException.class, () -> services.diary.update(other.getId(), e.getId(), MealType.SNACK, 2.0, PortionUnit.SERVING));
        services.diary.update(u.getId(), e.getId(), MealType.BREAKFAST, 2.0, PortionUnit.SERVING);
        DiaryService.DaySummary day = services.diary.day(u.getId(), TODAY);
        assertEquals(1, day.meals().get(MealType.BREAKFAST).size(), "moved to breakfast");
        assertNear(2 * 180 * 0.52, day.total().kcal(), 0.01, "two apples");
        services.diary.delete(u.getId(), e.getId());
        assertTrue(services.diary.day(u.getId(), TODAY).isEmpty(), "deleted");
    }

    @Test
    void copiesDaysAndCountsStreaks() {
        User u = member("ravi", DietType.VEGETARIAN, Set.of());
        Food idli = food("Idli");
        for (int i = 1; i <= 3; i++) services.diary.add(u.getId(), TODAY.minusDays(i), MealType.BREAKFAST, idli.getId(), 1.0, PortionUnit.SERVING);
        assertEquals(3, services.diary.streak(u.getId(), TODAY), "streak counts from yesterday while today is empty");
        int copied = services.diary.copyDay(u.getId(), TODAY.minusDays(1), TODAY);
        assertEquals(1, copied);
        assertEquals(4, services.diary.streak(u.getId(), TODAY), "today extends the streak");
        assertEquals(7, services.diary.dailyTotals(u.getId(), TODAY.minusDays(6), TODAY).size(), "one total per day");
    }

    @Test
    void weightLogUpdatesProfileAndStats() {
        User u = member("ravi", DietType.VEGETARIAN, Set.of());
        for (int i = 28; i >= 0; i -= 4) {
            services.weights.log(u.getId(), TODAY.minusDays(i), 70 - (28 - i) * 0.07, "");
        }
        WeightEntry latest = services.weights.latest(u.getId()).orElseThrow();
        assertNear(70 - 28 * 0.07, services.profiles.profile(u.getId()).getWeightKg(), 0.05, "profile follows the latest weigh-in");
        WeightService.WeightStats s = services.weights.stats(u.getId(), services.profiles.profile(u.getId()));
        assertNear(-0.49, s.weeklyTrendKg(), 0.02, "about half a kilo a week");
        assertTrue(s.progressPercent() > 30 && s.progressPercent() < 50, "progress toward 65 kg: " + s.progressPercent());
        assertTrue(s.projectedDate() != null && s.projectedDate().isAfter(TODAY), "projection is in the future");

        services.weights.log(u.getId(), TODAY, 68.0, "re-weighed");
        List<WeightEntry> all = services.weights.history(u.getId());
        assertEquals(latest.getId(), all.get(all.size() - 1).getId(), "same-day entries are updated, not duplicated");
        services.weights.delete(u.getId(), latest.getId());
        assertNear(70 - 24 * 0.07, services.profiles.profile(u.getId()).getWeightKg(), 0.05, "profile falls back to previous entry");
        assertThrows(ValidationException.class, () -> services.weights.log(u.getId(), TODAY, 10.0, ""));
    }
}
