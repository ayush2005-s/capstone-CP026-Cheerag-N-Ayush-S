package com.nutriplanner.service;

import com.nutriplanner.domain.Allergen;
import com.nutriplanner.domain.DietType;
import com.nutriplanner.domain.DietaryRestriction;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodCategory;
import com.nutriplanner.domain.Goal;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.PlanRole;
import com.nutriplanner.domain.Profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Turns a profile into a written, personalised diet plan: targets per meal, foods to favour, and guidance. */
public final class DietGuideService {

    private final FoodService foods;

    public DietGuideService(FoodService foods) {
        this.foods = foods;
    }

    public record MealShare(MealType meal, int kcal, int proteinG, int carbsG, int fatG) {
    }

    public record Guidance(String title, String text) {
    }

    public record DietGuide(NutritionTargets targets, List<MealShare> meals, List<Food> proteinPicks,
                            List<Food> fiberPicks, List<Food> fatPicks, List<String> avoiding, List<Guidance> guidance) {
    }

    public DietGuide guide(long userId, Profile profile, NutritionTargets t) {
        List<MealShare> meals = new ArrayList<>();
        for (MealType m : MealType.values()) {
            meals.add(new MealShare(m, (int) Math.round(t.calories() * m.share()), (int) Math.round(t.proteinG() * m.share()),
                    (int) Math.round(t.carbsG() * m.share()), (int) Math.round(t.fatG() * m.share())));
        }
        List<Food> suitable = foods.visibleFoods(userId).stream()
                .filter(f -> FoodService.assess(f, profile).suitable())
                .collect(Collectors.toList());

        List<Food> protein = suitable.stream()
                .filter(f -> f.getPlanRole() == PlanRole.PROTEIN || f.getCategory() == FoodCategory.PULSES)
                .filter(f -> f.getCategory() != FoodCategory.SUPPLEMENTS)
                .sorted(Comparator.comparingDouble(Food::proteinPer100Kcal).reversed())
                .limit(6).collect(Collectors.toList());
        List<Food> fiber = suitable.stream()
                .filter(f -> f.getKcal() > 0)
                .sorted(Comparator.comparingDouble((Food f) -> f.getFiber() / f.getKcal()).reversed())
                .limit(6).collect(Collectors.toList());
        List<Food> fats = suitable.stream()
                .filter(f -> f.getCategory() == FoodCategory.NUTS_SEEDS || f.getName().toLowerCase(Locale.ROOT).contains("avocado")
                        || f.getName().toLowerCase(Locale.ROOT).contains("olive oil") || f.getCategory() == FoodCategory.SEAFOOD)
                .sorted(Comparator.comparing(Food::getName))
                .limit(6).collect(Collectors.toList());

        List<String> avoiding = new ArrayList<>();
        if (profile.getDietType() != DietType.NON_VEGETARIAN) {
            avoiding.add(profile.getDietType().label() + ": " + profile.getDietType().description().toLowerCase(Locale.ROOT));
        }
        for (Allergen a : profile.getAllergens()) avoiding.add("Allergy or intolerance: " + a.label().toLowerCase(Locale.ROOT));
        for (DietaryRestriction r : profile.getRestrictions()) avoiding.add(r.label() + ": " + lowerFirst(r.description()));

        return new DietGuide(t, meals, protein, fiber, fats, avoiding, guidance(profile, t));
    }

    private static List<Guidance> guidance(Profile p, NutritionTargets t) {
        List<Guidance> g = new ArrayList<>();
        int perMealProtein = (int) Math.round(t.proteinG() / 3.5);
        switch (p.getGoal()) {
            case LOSE_WEIGHT:
                g.add(new Guidance("Keep a steady deficit",
                        String.format(Locale.ROOT, "Your target is %,d kcal, about %,d below maintenance. At this pace you can expect roughly %.2f kg a week; weigh in at the same time of day and judge the trend over two to three weeks.",
                                t.calories(), Math.abs(t.adjustment()), p.getWeeklyChangeKg())));
                g.add(new Guidance("Protect your muscle",
                        "Aim for about " + perMealProtein + " g of protein at each main meal and keep up strength training two or three times a week, so most of what you lose is fat."));
                g.add(new Guidance("Fill half the plate with vegetables",
                        "Vegetables, salads and dal add volume and fibre for few calories, which makes a deficit easier to live with."));
                break;
            case GAIN_WEIGHT:
                g.add(new Guidance("Eat a little more, consistently",
                        String.format(Locale.ROOT, "Your target is %,d kcal, about %,d above maintenance. If the scale hasn't moved after two weeks, add another 150–200 kcal a day.",
                                t.calories(), t.adjustment())));
                g.add(new Guidance("Choose energy-dense foods",
                        "Nuts, seeds, nut butters, whole milk, paneer, dried fruit and an extra roti or portion of rice add calories without making meals much bigger."));
                g.add(new Guidance("Don't skip meals",
                        "Eating every three to four hours, including a substantial snack, makes a surplus far easier than relying on large meals."));
                break;
            case BUILD_MUSCLE:
                g.add(new Guidance("Fuel training with a small surplus",
                        String.format(Locale.ROOT, "Your target is %,d kcal, about %,d above maintenance. A lean surplus supports muscle growth while limiting fat gain.",
                                t.calories(), t.adjustment())));
                g.add(new Guidance("Spread protein across the day",
                        "Aim for " + t.proteinG() + " g a day, roughly " + perMealProtein + " g at each main meal, with a protein-rich snack after training."));
                g.add(new Guidance("Train with progressive overload",
                        "Lift three to five times a week and add weight or repetitions over time. Food supports muscle growth; training drives it."));
                break;
            default:
                g.add(new Guidance("Eat around your maintenance",
                        String.format(Locale.ROOT, "Your target is %,d kcal. Small day-to-day swings are fine; aim for the weekly average to land near it.", t.calories())));
                g.add(new Guidance("Build balanced plates",
                        "A quarter of the plate protein, a quarter grains or bread, and half vegetables is an easy pattern to repeat."));
                break;
        }
        switch (p.getDietType()) {
            case VEGAN:
                g.add(new Guidance("Combine plant proteins",
                        "Pair pulses with grains (dal with rice, chole with roti) and include soy foods like tofu or soya chunks to cover every essential amino acid."));
                g.add(new Guidance("Mind vitamin B12",
                        "B12 isn't reliably found in plant foods. Many people on a vegan diet use fortified foods or a supplement; ask your doctor what's right for you."));
                break;
            case VEGETARIAN:
                g.add(new Guidance("Lean on dairy and pulses",
                        "Paneer, curd, Greek yogurt, milk and every kind of dal are your main protein sources. Include one at each meal."));
                break;
            case EGGETARIAN:
                g.add(new Guidance("Eggs make protein easy",
                        "Two eggs give about 12 g of protein. Combine them with dairy and pulses to reach your daily target without much effort."));
                break;
            default:
                g.add(new Guidance("Choose lean meats and fish",
                        "Prefer grilled, baked or curried chicken and fish over fried options, and try to include oily fish like salmon twice a week."));
                break;
        }
        g.add(new Guidance("Drink enough water",
                String.format(Locale.ROOT, "Aim for about %.1f litres a day (%d glasses), more on hot days or when you train.",
                        t.waterMl() / 1000.0, (int) Math.ceil(t.waterMl() / 250.0))));
        if (p.getGoal() == Goal.LOSE_WEIGHT && t.calorieLimited()) {
            g.add(new Guidance("About your calorie target", t.calorieNote()));
        }
        return g;
    }

    private static String lowerFirst(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
