package com.nutriplanner.service;

import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.MealPlanDay;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.Nutrients;
import com.nutriplanner.domain.PlanRole;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Builds meals that hit calorie and macro targets.
 * <p>
 * Every meal has a plate template (for example lunch = staple + protein + vegetable + optional curd). For each
 * attempt the generator picks one suitable food per slot, preferring foods not eaten recently, then searches
 * portion sizes in half-serving steps to minimise the weighted error against the meal's targets. The best of
 * many attempts wins. Only foods that already passed the diet, allergy and restriction checks are offered.
 */
public final class MealPlanGenerator {

    /** One slot on a plate: the role it needs, and whether the meal can do without it. */
    record Slot(PlanRole role, boolean optional) {
    }

    static final Map<MealType, List<Slot>> TEMPLATES = new EnumMap<>(MealType.class);

    static {
        TEMPLATES.put(MealType.BREAKFAST, List.of(new Slot(PlanRole.BASE, false), new Slot(PlanRole.PROTEIN, false),
                new Slot(PlanRole.FRUIT, true)));
        TEMPLATES.put(MealType.LUNCH, List.of(new Slot(PlanRole.BASE, false), new Slot(PlanRole.PROTEIN, false),
                new Slot(PlanRole.VEGETABLE, false), new Slot(PlanRole.DAIRY, true)));
        TEMPLATES.put(MealType.DINNER, List.of(new Slot(PlanRole.BASE, false), new Slot(PlanRole.PROTEIN, false),
                new Slot(PlanRole.VEGETABLE, false)));
        TEMPLATES.put(MealType.SNACK, List.of(new Slot(PlanRole.SNACK, false), new Slot(PlanRole.FRUIT, true)));
    }

    static final int ATTEMPTS = 36;
    static final double STEP = 0.5;

    /** Calorie and macro targets for one meal. */
    public record MealTarget(double kcal, double protein, double carbs, double fat) {
        public static MealTarget of(NutritionTargets t, MealType meal) {
            double s = meal.share();
            return new MealTarget(t.calories() * s, t.proteinG() * s, t.carbsG() * s, t.fatG() * s);
        }
    }

    public List<MealPlanDay.Item> generateDay(NutritionTargets targets, List<Food> candidates, Set<Long> recentlyUsed, Random random) {
        List<MealPlanDay.Item> items = new ArrayList<>();
        Set<Long> usedToday = new HashSet<>();
        for (MealType meal : MealType.values()) {
            List<MealPlanDay.Item> mealItems = generateMeal(meal, MealTarget.of(targets, meal), candidates, usedToday, recentlyUsed, random);
            for (MealPlanDay.Item item : mealItems) usedToday.add(item.foodId());
            items.addAll(mealItems);
        }
        return items;
    }

    public List<MealPlanDay.Item> generateMeal(MealType meal, MealTarget target, List<Food> candidates, Set<Long> avoid,
                                               Set<Long> recentlyUsed, Random random) {
        List<Slot> slots = TEMPLATES.get(meal);
        List<List<Food>> pools = new ArrayList<>();
        List<Slot> usableSlots = new ArrayList<>();
        for (Slot slot : slots) {
            List<Food> pool = pool(candidates, slot.role(), meal, avoid);
            if (pool.isEmpty()) pool = pool(candidates, slot.role(), meal, Set.of());
            if (pool.isEmpty()) continue;
            pools.add(pool);
            usableSlots.add(slot);
        }
        if (usableSlots.isEmpty()) return List.of();

        Combination best = null;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            List<Food> picks = new ArrayList<>();
            Set<Long> picked = new HashSet<>();
            for (List<Food> pool : pools) {
                Food choice = weightedPick(pool, picked, recentlyUsed, random);
                picks.add(choice);
                if (choice != null) picked.add(choice.getId());
            }
            Combination c = optimise(picks, usableSlots, target);
            if (c != null && (best == null || c.cost < best.cost)) best = c;
        }
        if (best == null) return List.of();
        List<MealPlanDay.Item> items = new ArrayList<>();
        for (int i = 0; i < best.foods.size(); i++) {
            Food f = best.foods.get(i);
            if (f != null && best.servings[i] > 0) items.add(new MealPlanDay.Item(meal, f.getId(), best.servings[i]));
        }
        return items;
    }

    private static List<Food> pool(List<Food> candidates, PlanRole role, MealType meal, Set<Long> avoid) {
        List<Food> pool = new ArrayList<>();
        for (Food f : candidates) {
            if (f.getPlanRole() == role && f.suitsMeal(meal) && !avoid.contains(f.getId())) pool.add(f);
        }
        return pool;
    }

    private static Food weightedPick(List<Food> pool, Set<Long> exclude, Set<Long> recentlyUsed, Random random) {
        double total = 0;
        double[] weights = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            Food f = pool.get(i);
            weights[i] = exclude.contains(f.getId()) ? 0 : recentlyUsed.contains(f.getId()) ? 1 : 4;
            total += weights[i];
        }
        if (total <= 0) return null;
        double r = random.nextDouble() * total;
        for (int i = 0; i < pool.size(); i++) {
            r -= weights[i];
            if (r <= 0 && weights[i] > 0) return pool.get(i);
        }
        for (int i = pool.size() - 1; i >= 0; i--) {
            if (weights[i] > 0) return pool.get(i);
        }
        return null;
    }

    private static final class Combination {
        final List<Food> foods;
        final double[] servings;
        final double cost;

        Combination(List<Food> foods, double[] servings, double cost) {
            this.foods = foods;
            this.servings = servings;
            this.cost = cost;
        }
    }

    /** Coordinate descent over portion sizes in half-serving steps. */
    private static Combination optimise(List<Food> foods, List<Slot> slots, MealTarget target) {
        int n = foods.size();
        double[] servings = new double[n];
        double[] min = new double[n];
        double[] max = new double[n];
        boolean any = false;
        for (int i = 0; i < n; i++) {
            Food f = foods.get(i);
            if (f == null) continue;
            any = true;
            PlanRole role = slots.get(i).role();
            min[i] = slots.get(i).optional() ? 0 : role.minServings();
            max[i] = role.maxServings();
            servings[i] = Math.max(min[i], Math.min(max[i], 1.0));
        }
        if (!any) return null;
        double cost = cost(foods, servings, target);
        for (int iteration = 0; iteration < 60; iteration++) {
            int bestIndex = -1;
            double bestValue = 0, bestCost = cost;
            for (int i = 0; i < n; i++) {
                if (foods.get(i) == null) continue;
                for (double delta : new double[]{-STEP, STEP}) {
                    double v = servings[i] + delta;
                    if (v < min[i] - 1e-9 || v > max[i] + 1e-9) continue;
                    double old = servings[i];
                    servings[i] = v;
                    double c = cost(foods, servings, target);
                    servings[i] = old;
                    if (c < bestCost - 1e-9) {
                        bestCost = c;
                        bestIndex = i;
                        bestValue = v;
                    }
                }
            }
            if (bestIndex < 0) break;
            servings[bestIndex] = bestValue;
            cost = bestCost;
        }
        return new Combination(foods, servings, cost);
    }

    static double cost(List<Food> foods, double[] servings, MealTarget t) {
        Nutrients total = Nutrients.ZERO;
        int items = 0;
        for (int i = 0; i < foods.size(); i++) {
            Food f = foods.get(i);
            if (f == null || servings[i] <= 0) continue;
            total = total.plus(f.perServing().scale(servings[i]));
            items++;
        }
        if (items == 0) return Double.MAX_VALUE;
        return 4 * sq((total.kcal() - t.kcal()) / Math.max(t.kcal(), 50))
                + 2 * sq((total.protein() - t.protein()) / Math.max(t.protein(), 5))
                + sq((total.carbs() - t.carbs()) / Math.max(t.carbs(), 10))
                + sq((total.fat() - t.fat()) / Math.max(t.fat(), 5));
    }

    private static double sq(double v) {
        return v * v;
    }
}
