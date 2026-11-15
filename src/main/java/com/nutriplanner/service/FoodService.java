package com.nutriplanner.service;

import com.nutriplanner.domain.Allergen;
import com.nutriplanner.domain.DietaryRestriction;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodCategory;
import com.nutriplanner.domain.FoodDiet;
import com.nutriplanner.domain.FoodLogEntry;
import com.nutriplanner.domain.Labeled;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.PlanRole;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.repository.Repositories;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** The food database: search and filtering, suitability checks, and custom foods. */
public final class FoodService {

    private final Repositories repos;
    private final Clock clock;

    public FoodService(Repositories repos, Clock clock) {
        this.repos = repos;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ search

    public enum Sort implements Labeled {
        NAME("Name"),
        KCAL_ASC("Fewest calories"),
        KCAL_DESC("Most calories"),
        PROTEIN("Most protein"),
        PROTEIN_DENSITY("Most protein per calorie"),
        FIBER("Most fibre");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }
    }

    public record Query(String text, FoodCategory category, FoodDiet diet, MealType meal, boolean suitableOnly,
                        boolean mineOnly, Sort sort, int page, int pageSize) {
        public static Query all() {
            return new Query("", null, null, null, false, false, Sort.NAME, 1, 1000);
        }
    }

    /** Foods visible to the user (global foods plus their own) that match the query. */
    public Page<Food> search(long userId, Profile profile, Query q) {
        List<Food> matches = new ArrayList<>();
        String[] tokens = tokens(q.text());
        for (Food f : visibleFoods(userId)) {
            if (q.mineOnly() && f.getOwnerId() != userId) continue;
            if (q.category() != null && f.getCategory() != q.category()) continue;
            if (q.diet() != null && f.getDiet() != q.diet()) continue;
            if (q.meal() != null && !f.suitsMeal(q.meal())) continue;
            if (q.suitableOnly() && profile != null && !assess(f, profile).suitable()) continue;
            if (!matchesText(f, tokens)) continue;
            matches.add(f);
        }
        matches.sort(comparator(q.sort() == null ? Sort.NAME : q.sort(), tokens));
        return Page.of(matches, q.page(), q.pageSize());
    }

    /** All foods including inactive ones, for administrators. */
    public Page<Food> searchGlobal(String text, FoodCategory category, Boolean active, int page, int pageSize) {
        String[] tokens = tokens(text);
        List<Food> matches = new ArrayList<>();
        for (Food f : repos.foods.findGlobal()) {
            if (category != null && f.getCategory() != category) continue;
            if (active != null && f.isActive() != active) continue;
            if (!matchesText(f, tokens)) continue;
            matches.add(f);
        }
        matches.sort(comparator(Sort.NAME, tokens));
        return Page.of(matches, page, pageSize);
    }

    public List<Food> visibleFoods(long userId) {
        List<Food> foods = new ArrayList<>();
        for (Food f : repos.foods.findGlobal()) {
            if (f.isActive()) foods.add(f);
        }
        if (userId > 0) foods.addAll(repos.foods.findByOwner(userId));
        return foods;
    }

    /** A food the user may see, or {@link NotFoundException}. */
    public Food visibleFood(long userId, long foodId) {
        return repos.foods.findById(foodId).filter(f -> f.visibleTo(userId))
                .orElseThrow(() -> new NotFoundException("That food isn't in the database."));
    }

    public Food anyFood(long foodId) {
        return repos.foods.findById(foodId).orElseThrow(() -> new NotFoundException("That food isn't in the database."));
    }

    /** Foods the user logged most recently (newest first, without duplicates). */
    public List<Food> recentFoods(long userId, int limit) {
        LocalDate today = LocalDate.now(clock);
        List<FoodLogEntry> entries = repos.foodLog.findByUserBetween(userId, today.minusDays(30), today);
        entries.sort(Comparator.comparing(FoodLogEntry::getDate).thenComparing(FoodLogEntry::getId).reversed());
        Set<Long> seen = new LinkedHashSet<>();
        List<Food> out = new ArrayList<>();
        for (FoodLogEntry e : entries) {
            if (out.size() >= limit) break;
            if (!seen.add(e.getFoodId())) continue;
            repos.foods.findById(e.getFoodId()).filter(f -> f.visibleTo(userId)).ifPresent(out::add);
        }
        return out;
    }

    // ------------------------------------------------------------- suitability

    public record Suitability(List<String> issues) {
        public boolean suitable() {
            return issues.isEmpty();
        }
    }

    public static Suitability assess(Food food, Profile profile) {
        List<String> issues = new ArrayList<>();
        if (!profile.getDietType().allows(food.getDiet())) {
            issues.add("Not part of a " + profile.getDietType().label().toLowerCase(Locale.ROOT) + " diet");
        }
        for (Allergen a : food.getAllergens()) {
            if (profile.getAllergens().contains(a)) issues.add("Contains " + a.label().toLowerCase(Locale.ROOT));
        }
        for (DietaryRestriction r : profile.getRestrictions()) {
            r.violation(food).ifPresent(reason -> issues.add(reason + " (" + r.label().toLowerCase(Locale.ROOT) + ")"));
        }
        return new Suitability(issues);
    }

    /** Foods the meal planner may use for this profile. */
    public List<Food> planCandidates(long userId, Profile profile) {
        List<Food> out = new ArrayList<>();
        for (Food f : visibleFoods(userId)) {
            if (f.getPlanRole() != PlanRole.NONE && !f.getMealTypes().isEmpty() && assess(f, profile).suitable()) out.add(f);
        }
        return out;
    }

    // ---------------------------------------------------------------- editing

    public record FoodInput(String name, FoodCategory category, FoodDiet diet, Set<Allergen> allergens,
                            Set<MealType> mealTypes, PlanRole planRole, Double kcal, Double protein, Double carbs,
                            Double fat, Double fiber, Double sugar, Double sodiumMg, Double servingGrams,
                            String servingLabel) {
    }

    public Food createCustom(long userId, FoodInput input) {
        validate(input, 0, false);
        Food food = new Food();
        apply(food, input);
        food.setOwnerId(userId);
        food.setCreatedAt(clock.instant());
        food.setUpdatedAt(clock.instant());
        return repos.foods.save(food);
    }

    public Food updateCustom(long userId, long foodId, FoodInput input) {
        Food food = repos.foods.findById(foodId).filter(f -> f.getOwnerId() == userId && userId != 0)
                .orElseThrow(() -> new NotFoundException("You can only edit foods you added."));
        validate(input, foodId, false);
        apply(food, input);
        food.setUpdatedAt(clock.instant());
        return repos.foods.save(food);
    }

    public void deleteCustom(long userId, long foodId) {
        Food food = repos.foods.findById(foodId).filter(f -> f.getOwnerId() == userId && userId != 0)
                .orElseThrow(() -> new NotFoundException("You can only delete foods you added."));
        repos.foods.delete(food.getId());
    }

    /** Used by {@link AdminService} after it has checked the caller is an administrator. */
    Food saveGlobal(Long foodId, FoodInput input) {
        validate(input, foodId == null ? 0 : foodId, true);
        Food food = foodId == null ? new Food() : repos.foods.findById(foodId).filter(Food::isGlobal)
                .orElseThrow(() -> new NotFoundException("That food doesn't exist."));
        apply(food, input);
        food.setOwnerId(0);
        if (food.getCreatedAt() == null) food.setCreatedAt(clock.instant());
        food.setUpdatedAt(clock.instant());
        return repos.foods.save(food);
    }

    private void validate(FoodInput in, long id, boolean global) {
        ValidationException.Errors e = new ValidationException.Errors();
        String name = in.name() == null ? "" : in.name().strip();
        e.check(name.length() >= 2 && name.length() <= 80, "name", "Enter a name of 2–80 characters.");
        if (global && !e.has("name") && repos.foods.globalNameExists(name, id)) {
            e.add("name", "A food with this name already exists.");
        }
        e.check(in.category() != null, "category", "Choose a category.");
        e.check(in.diet() != null, "diet", "Choose which diets this food fits.");
        e.check(in.planRole() != null, "planRole", "Choose how the planner may use this food.");
        range(e, "kcal", in.kcal(), 0, 950, "Calories per 100 g must be between 0 and 950.");
        range(e, "protein", in.protein(), 0, 100, "Protein per 100 g must be between 0 and 100 g.");
        range(e, "carbs", in.carbs(), 0, 100, "Carbohydrate per 100 g must be between 0 and 100 g.");
        range(e, "fat", in.fat(), 0, 100, "Fat per 100 g must be between 0 and 100 g.");
        range(e, "fiber", in.fiber(), 0, 100, "Fibre per 100 g must be between 0 and 100 g.");
        range(e, "sugar", in.sugar(), 0, 100, "Sugar per 100 g must be between 0 and 100 g.");
        range(e, "sodiumMg", in.sodiumMg(), 0, 40000, "Sodium per 100 g must be between 0 and 40,000 mg.");
        range(e, "servingGrams", in.servingGrams(), 1, 2000, "A serving must weigh between 1 and 2,000 g.");
        String label = in.servingLabel() == null ? "" : in.servingLabel().strip();
        e.check(label.length() >= 1 && label.length() <= 40, "servingLabel", "Describe the serving in up to 40 characters, like “1 bowl”.");
        if (!e.has("protein") && !e.has("carbs") && !e.has("fat")) {
            double sum = in.protein() + in.carbs() + in.fat();
            if (sum > 100.5) e.add("fat", "Protein, carbs and fat can't add up to more than 100 g per 100 g.");
            if (!e.has("fiber") && in.fiber() > in.carbs() + 0.5) e.add("fiber", "Fibre is part of carbohydrate, so it can't be higher than carbs.");
            if (!e.has("sugar") && in.sugar() > in.carbs() + 0.5) e.add("sugar", "Sugar is part of carbohydrate, so it can't be higher than carbs.");
            if (!e.has("kcal")) {
                double expected = in.protein() * 4 + in.carbs() * 4 + in.fat() * 9;
                double tolerance = Math.max(25, 0.3 * Math.max(in.kcal(), expected));
                if (Math.abs(in.kcal() - expected) > tolerance) {
                    e.add("kcal", String.format(Locale.ROOT,
                            "Calories don't match the macros: protein, carbs and fat give about %.0f kcal per 100 g.", expected));
                }
            }
        }
        e.throwIfAny();
    }

    private static void range(ValidationException.Errors e, String field, Double value, double min, double max, String message) {
        if (value == null || value.isNaN() || value < min || value > max) e.add(field, message);
    }

    private static void apply(Food f, FoodInput in) {
        f.setName(in.name().strip());
        f.setCategory(in.category());
        f.setDiet(in.diet());
        f.setAllergens(in.allergens() == null ? Set.of() : in.allergens());
        f.setMealTypes(in.mealTypes() == null ? Set.of() : in.mealTypes());
        f.setPlanRole(in.planRole());
        f.setKcal(in.kcal());
        f.setProtein(in.protein());
        f.setCarbs(in.carbs());
        f.setFat(in.fat());
        f.setFiber(in.fiber());
        f.setSugar(in.sugar());
        f.setSodiumMg(in.sodiumMg());
        f.setServingGrams(in.servingGrams());
        f.setServingLabel(in.servingLabel().strip());
    }

    public static FoodInput toInput(Food f) {
        return new FoodInput(f.getName(), f.getCategory(), f.getDiet(), f.getAllergens(), f.getMealTypes(),
                f.getPlanRole(), f.getKcal(), f.getProtein(), f.getCarbs(), f.getFat(), f.getFiber(), f.getSugar(),
                f.getSodiumMg(), f.getServingGrams(), f.getServingLabel());
    }

    // ---------------------------------------------------------------- helpers

    private static String[] tokens(String text) {
        if (text == null || text.isBlank()) return new String[0];
        return text.toLowerCase(Locale.ROOT).trim().split("\\s+");
    }

    private static boolean matchesText(Food f, String[] tokens) {
        if (tokens.length == 0) return true;
        String haystack = (f.getName() + " " + f.getCategory().label() + " " + f.getDiet().label()).toLowerCase(Locale.ROOT);
        for (String t : tokens) {
            if (!haystack.contains(t)) return false;
        }
        return true;
    }

    private static Comparator<Food> comparator(Sort sort, String[] tokens) {
        Comparator<Food> byName = Comparator.comparing(f -> f.getName().toLowerCase(Locale.ROOT));
        switch (sort) {
            case KCAL_ASC:
                return Comparator.comparingDouble((Food f) -> f.perServing().kcal()).thenComparing(byName);
            case KCAL_DESC:
                return Comparator.comparingDouble((Food f) -> -f.perServing().kcal()).thenComparing(byName);
            case PROTEIN:
                return Comparator.comparingDouble((Food f) -> -f.perServing().protein()).thenComparing(byName);
            case PROTEIN_DENSITY:
                return Comparator.comparingDouble((Food f) -> -f.proteinPer100Kcal()).thenComparing(byName);
            case FIBER:
                return Comparator.comparingDouble((Food f) -> -f.perServing().fiber()).thenComparing(byName);
            default:
                if (tokens.length > 0) {
                    String first = tokens[0];
                    return Comparator.comparing((Food f) -> f.getName().toLowerCase(Locale.ROOT).startsWith(first) ? 0 : 1)
                            .thenComparing(byName);
                }
                return byName;
        }
    }
}
