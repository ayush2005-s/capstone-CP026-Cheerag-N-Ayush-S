package com.nutriplanner.service;

import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodCategory;
import com.nutriplanner.domain.Nutrients;
import com.nutriplanner.domain.PlanRole;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.WeightEntry;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Collects everything the dashboard shows for today. */
public final class DashboardService {

    private final ProfileService profiles;
    private final DiaryService diary;
    private final WeightService weights;
    private final WaterService water;
    private final MealPlanService plans;
    private final FoodService foods;
    private final Clock clock;

    public DashboardService(ProfileService profiles, DiaryService diary, WeightService weights, WaterService water,
                            MealPlanService plans, FoodService foods, Clock clock) {
        this.profiles = profiles;
        this.diary = diary;
        this.weights = weights;
        this.water = water;
        this.plans = plans;
        this.foods = foods;
        this.clock = clock;
    }

    public record Insight(String text, List<Food> suggestions) {
    }

    public record Dashboard(LocalDate today, Profile profile, NutritionTargets targets, DiaryService.DaySummary day,
                            int waterGlasses, int waterGoalGlasses, WeightService.WeightStats weight,
                            List<WeightEntry> recentWeights, Map<LocalDate, Nutrients> lastSevenDays, int streak,
                            MealPlanService.PlannedDay plan, List<Insight> insights) {
        public int remainingKcal() {
            return (int) Math.round(targets.calories() - day.total().kcal());
        }
    }

    public Dashboard build(long userId) {
        LocalDate today = LocalDate.now(clock);
        Profile profile = profiles.profile(userId);
        NutritionTargets targets = NutritionCalculator.targetsFor(profile, today);
        DiaryService.DaySummary day = diary.day(userId, today);
        List<WeightEntry> history = weights.history(userId);
        List<WeightEntry> recent = history.stream().filter(w -> !w.getDate().isBefore(today.minusDays(90)))
                .collect(Collectors.toList());
        return new Dashboard(today, profile, targets, day, water.glasses(userId, today),
                (int) Math.ceil(targets.waterMl() / 250.0), weights.stats(userId, profile), recent,
                diary.dailyTotals(userId, today.minusDays(6), today), diary.streak(userId, today),
                plans.day(userId, today), insights(userId, profile, targets, day));
    }

    private List<Insight> insights(long userId, Profile profile, NutritionTargets t, DiaryService.DaySummary day) {
        List<Insight> out = new ArrayList<>();
        Nutrients total = day.total();
        if (day.isEmpty()) {
            out.add(new Insight("Nothing logged yet today. Add breakfast to the diary, or log a meal straight from your plan.", List.of()));
        }
        double proteinGap = t.proteinG() - total.protein();
        if (!day.isEmpty() && proteinGap > 15) {
            List<Food> picks = foods.visibleFoods(userId).stream()
                    .filter(f -> FoodService.assess(f, profile).suitable())
                    .filter(f -> f.getPlanRole() == PlanRole.PROTEIN || f.getPlanRole() == PlanRole.SNACK)
                    .filter(f -> f.getCategory() != FoodCategory.SUPPLEMENTS)
                    .filter(f -> f.perServing().kcal() <= Math.max(150, t.calories() - total.kcal()))
                    .sorted(Comparator.comparingDouble(Food::proteinPer100Kcal).reversed())
                    .limit(3).collect(Collectors.toList());
            out.add(new Insight(String.format(Locale.ROOT, "You're %.0f g short of today's protein target. These fit your diet:", proteinGap), picks));
        }
        double over = total.kcal() - t.calories();
        if (over > t.calories() * 0.05) {
            out.add(new Insight(String.format(Locale.ROOT, "You're %,.0f kcal over today's target. A lighter dinner or a walk evens things out; one day won't undo your progress.", over), List.of()));
        }
        if (!day.isEmpty() && total.fiber() < t.fiberG() * 0.5) {
            out.add(new Insight(String.format(Locale.ROOT, "Fibre is at %.0f of %d g. Pulses, vegetables, fruit and whole grains will close the gap.", total.fiber(), t.fiberG()), List.of()));
        }
        return out;
    }
}
