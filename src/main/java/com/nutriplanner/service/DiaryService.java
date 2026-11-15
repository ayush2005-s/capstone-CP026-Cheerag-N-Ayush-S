package com.nutriplanner.service;

import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodLogEntry;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.Nutrients;
import com.nutriplanner.domain.PortionUnit;
import com.nutriplanner.repository.Repositories;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The food diary: logging what was eaten and adding it up per meal and per day. */
public final class DiaryService {

    public static final double MAX_SERVINGS = 20;
    public static final double MAX_GRAMS = 3000;

    private final Repositories repos;
    private final FoodService foods;
    private final Clock clock;

    public DiaryService(Repositories repos, FoodService foods, Clock clock) {
        this.repos = repos;
        this.foods = foods;
        this.clock = clock;
    }

    public FoodLogEntry add(long userId, LocalDate date, MealType meal, long foodId, Double quantity, PortionUnit unit) {
        Food food = foods.visibleFood(userId, foodId);
        validate(date, meal, quantity, unit);
        FoodLogEntry entry = new FoodLogEntry();
        entry.setUserId(userId);
        entry.setDate(date);
        entry.setMealType(meal);
        entry.setFoodId(food.getId());
        entry.setFoodName(food.getName());
        applyAmount(entry, food, quantity, unit);
        entry.setCreatedAt(clock.instant());
        return repos.foodLog.save(entry);
    }

    public FoodLogEntry update(long userId, long entryId, MealType meal, Double quantity, PortionUnit unit) {
        FoodLogEntry entry = own(userId, entryId);
        validate(entry.getDate(), meal, quantity, unit);
        Food food = repos.foods.findById(entry.getFoodId()).orElse(null);
        entry.setMealType(meal);
        if (food != null) {
            applyAmount(entry, food, quantity, unit);
        } else {
            // The food was removed from the database: rescale the stored snapshot instead.
            double grams;
            if (unit == entry.getUnit() && entry.getQuantity() > 0) {
                grams = entry.getGrams() * quantity / entry.getQuantity();
            } else if (unit == PortionUnit.GRAM) {
                grams = quantity;
            } else {
                throw ValidationException.field("unit", "This food was removed from the database, so enter the amount in grams.");
            }
            double factor = entry.getGrams() > 0 ? grams / entry.getGrams() : 0;
            entry.setNutrients(entry.getNutrients().scale(factor));
            entry.setGrams(grams);
            entry.setQuantity(quantity);
            entry.setUnit(unit);
        }
        return repos.foodLog.save(entry);
    }

    public void delete(long userId, long entryId) {
        repos.foodLog.delete(own(userId, entryId).getId());
    }

    private FoodLogEntry own(long userId, long entryId) {
        return repos.foodLog.findById(entryId).filter(e -> e.getUserId() == userId)
                .orElseThrow(() -> new NotFoundException("That diary entry doesn't exist."));
    }

    private void validate(LocalDate date, MealType meal, Double quantity, PortionUnit unit) {
        LocalDate today = LocalDate.now(clock);
        ValidationException.Errors errors = new ValidationException.Errors();
        if (date == null) errors.add("date", "Choose a date.");
        else if (date.isAfter(today)) errors.add("date", "You can log food for today or earlier. Use the meal planner for future days.");
        else if (date.isBefore(today.minusYears(3))) errors.add("date", "Choose a date within the last 3 years.");
        errors.check(meal != null, "meal", "Choose a meal.");
        errors.check(unit != null, "unit", "Choose servings or grams.");
        if (quantity == null || quantity.isNaN() || quantity <= 0) {
            errors.add("quantity", "Enter an amount greater than zero.");
        } else if (unit == PortionUnit.SERVING && quantity > MAX_SERVINGS) {
            errors.add("quantity", "Enter at most 20 servings at a time.");
        } else if (unit == PortionUnit.GRAM && quantity > MAX_GRAMS) {
            errors.add("quantity", "Enter at most 3,000 g at a time.");
        }
        errors.throwIfAny();
    }

    private static void applyAmount(FoodLogEntry entry, Food food, double quantity, PortionUnit unit) {
        double grams = unit == PortionUnit.SERVING ? quantity * food.getServingGrams() : quantity;
        entry.setQuantity(quantity);
        entry.setUnit(unit);
        entry.setGrams(grams);
        entry.setNutrients(food.forGrams(grams));
    }

    // ----------------------------------------------------------------- reading

    public record DaySummary(LocalDate date, Map<MealType, List<FoodLogEntry>> meals, Map<MealType, Nutrients> mealTotals,
                             Nutrients total) {
        public boolean isEmpty() {
            return meals.values().stream().allMatch(List::isEmpty);
        }

        public int entryCount() {
            return meals.values().stream().mapToInt(List::size).sum();
        }
    }

    public DaySummary day(long userId, LocalDate date) {
        Map<MealType, List<FoodLogEntry>> meals = new EnumMap<>(MealType.class);
        Map<MealType, Nutrients> totals = new EnumMap<>(MealType.class);
        for (MealType m : MealType.values()) {
            meals.put(m, new ArrayList<>());
            totals.put(m, Nutrients.ZERO);
        }
        Nutrients total = Nutrients.ZERO;
        List<FoodLogEntry> entries = repos.foodLog.findByUserAndDate(userId, date);
        entries.sort(Comparator.comparingLong(FoodLogEntry::getId));
        for (FoodLogEntry e : entries) {
            meals.get(e.getMealType()).add(e);
            totals.put(e.getMealType(), totals.get(e.getMealType()).plus(e.getNutrients()));
            total = total.plus(e.getNutrients());
        }
        return new DaySummary(date, meals, totals, total);
    }

    /** Totals for every day in the range (inclusive), including days with nothing logged. */
    public Map<LocalDate, Nutrients> dailyTotals(long userId, LocalDate from, LocalDate to) {
        Map<LocalDate, Nutrients> totals = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) totals.put(d, Nutrients.ZERO);
        for (FoodLogEntry e : repos.foodLog.findByUserBetween(userId, from, to)) {
            totals.computeIfPresent(e.getDate(), (d, n) -> n.plus(e.getNutrients()));
        }
        return totals;
    }

    /** Days in the range with at least one entry. */
    public Set<LocalDate> loggedDays(long userId, LocalDate from, LocalDate to) {
        Set<LocalDate> days = new HashSet<>();
        for (FoodLogEntry e : repos.foodLog.findByUserBetween(userId, from, to)) days.add(e.getDate());
        return days;
    }

    /** Consecutive days with food logged, ending today (or yesterday, if today is still empty). */
    public int streak(long userId, LocalDate today) {
        Set<LocalDate> days = loggedDays(userId, today.minusDays(400), today);
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /** Copies every entry from one day to another and returns how many were copied. */
    public int copyDay(long userId, LocalDate from, LocalDate to) {
        if (from.equals(to)) throw new ValidationException("Choose a different day to copy from.");
        validate(to, MealType.BREAKFAST, 1.0, PortionUnit.SERVING);
        List<FoodLogEntry> source = repos.foodLog.findByUserAndDate(userId, from);
        if (source.isEmpty()) throw new ValidationException("There's nothing logged on " + from + " to copy.");
        return repos.db.write(() -> {
            for (FoodLogEntry s : source) {
                FoodLogEntry copy = new FoodLogEntry();
                copy.setUserId(userId);
                copy.setDate(to);
                copy.setMealType(s.getMealType());
                copy.setFoodId(s.getFoodId());
                copy.setFoodName(s.getFoodName());
                copy.setQuantity(s.getQuantity());
                copy.setUnit(s.getUnit());
                copy.setGrams(s.getGrams());
                copy.setNutrients(s.getNutrients());
                copy.setCreatedAt(clock.instant());
                repos.foodLog.save(copy);
            }
            return source.size();
        });
    }
}
