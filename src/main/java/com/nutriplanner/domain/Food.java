package com.nutriplanner.domain;

import com.nutriplanner.storage.Doc;
import com.nutriplanner.storage.EntityMapper;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * A food with its nutrition per 100 g and a typical serving. Global foods (owner 0) are managed by admins;
 * members can add private custom foods that only they see.
 */
public final class Food {

    public static final EntityMapper<Food> MAPPER = EntityMapper.of(Food::getId, Food::setId, Food::toDoc, Food::fromDoc);

    private long id;
    private String name;
    private FoodCategory category = FoodCategory.PREPARED;
    private FoodDiet diet = FoodDiet.VEGAN;
    private EnumSet<Allergen> allergens = EnumSet.noneOf(Allergen.class);
    private EnumSet<MealType> mealTypes = EnumSet.noneOf(MealType.class);
    private PlanRole planRole = PlanRole.NONE;
    private double kcal;
    private double protein;
    private double carbs;
    private double fat;
    private double fiber;
    private double sugar;
    private double sodiumMg;
    private double servingGrams = 100;
    private String servingLabel = "100 g";
    private long ownerId;
    private boolean active = true;
    private Instant createdAt;
    private Instant updatedAt;

    /** Nutrients in the per-100 g reference amount. */
    public Nutrients per100g() {
        return new Nutrients(kcal, protein, carbs, fat, fiber, sugar, sodiumMg);
    }

    public Nutrients forGrams(double grams) {
        return per100g().scale(grams / 100.0);
    }

    public Nutrients perServing() {
        return forGrams(servingGrams);
    }

    public double proteinPer100Kcal() {
        return kcal <= 0 ? 0 : protein * 100.0 / kcal;
    }

    public boolean isGlobal() {
        return ownerId == 0;
    }

    public boolean visibleTo(long userId) {
        return (isGlobal() && active) || ownerId == userId;
    }

    public boolean suitsMeal(MealType meal) {
        return mealTypes.contains(meal);
    }

    public Doc toDoc() {
        return new Doc()
                .put("name", name)
                .put("category", category)
                .put("diet", diet)
                .put("allergens", allergens)
                .put("mealTypes", mealTypes)
                .put("planRole", planRole)
                .put("kcal", kcal)
                .put("protein", protein)
                .put("carbs", carbs)
                .put("fat", fat)
                .put("fiber", fiber)
                .put("sugar", sugar)
                .put("sodiumMg", sodiumMg)
                .put("servingGrams", servingGrams)
                .put("servingLabel", servingLabel)
                .put("ownerId", ownerId)
                .put("active", active)
                .put("createdAt", createdAt)
                .put("updatedAt", updatedAt);
    }

    public static Food fromDoc(long id, Doc d) {
        Food f = new Food();
        f.id = id;
        f.name = d.getString("name");
        f.category = d.getEnum("category", FoodCategory.class, FoodCategory.PREPARED);
        f.diet = d.getEnum("diet", FoodDiet.class, FoodDiet.VEGAN);
        f.allergens = d.getEnumSet("allergens", Allergen.class);
        f.mealTypes = d.getEnumSet("mealTypes", MealType.class);
        f.planRole = d.getEnum("planRole", PlanRole.class, PlanRole.NONE);
        f.kcal = d.getDouble("kcal", 0);
        f.protein = d.getDouble("protein", 0);
        f.carbs = d.getDouble("carbs", 0);
        f.fat = d.getDouble("fat", 0);
        f.fiber = d.getDouble("fiber", 0);
        f.sugar = d.getDouble("sugar", 0);
        f.sodiumMg = d.getDouble("sodiumMg", 0);
        f.servingGrams = d.getDouble("servingGrams", 100);
        f.servingLabel = d.getString("servingLabel", "100 g");
        f.ownerId = d.getLong("ownerId", 0);
        f.active = d.getBool("active", true);
        f.createdAt = d.getInstant("createdAt");
        f.updatedAt = d.getInstant("updatedAt");
        return f;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public FoodCategory getCategory() { return category; }
    public void setCategory(FoodCategory category) { this.category = category; }
    public FoodDiet getDiet() { return diet; }
    public void setDiet(FoodDiet diet) { this.diet = diet; }
    public EnumSet<Allergen> getAllergens() { return allergens.isEmpty() ? EnumSet.noneOf(Allergen.class) : EnumSet.copyOf(allergens); }
    public void setAllergens(Set<Allergen> allergens) { this.allergens = allergens.isEmpty() ? EnumSet.noneOf(Allergen.class) : EnumSet.copyOf(allergens); }
    public EnumSet<MealType> getMealTypes() { return mealTypes.isEmpty() ? EnumSet.noneOf(MealType.class) : EnumSet.copyOf(mealTypes); }
    public void setMealTypes(Set<MealType> mealTypes) { this.mealTypes = mealTypes.isEmpty() ? EnumSet.noneOf(MealType.class) : EnumSet.copyOf(mealTypes); }
    public PlanRole getPlanRole() { return planRole; }
    public void setPlanRole(PlanRole planRole) { this.planRole = planRole; }
    public double getKcal() { return kcal; }
    public void setKcal(double kcal) { this.kcal = kcal; }
    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }
    public double getCarbs() { return carbs; }
    public void setCarbs(double carbs) { this.carbs = carbs; }
    public double getFat() { return fat; }
    public void setFat(double fat) { this.fat = fat; }
    public double getFiber() { return fiber; }
    public void setFiber(double fiber) { this.fiber = fiber; }
    public double getSugar() { return sugar; }
    public void setSugar(double sugar) { this.sugar = sugar; }
    public double getSodiumMg() { return sodiumMg; }
    public void setSodiumMg(double sodiumMg) { this.sodiumMg = sodiumMg; }
    public double getServingGrams() { return servingGrams; }
    public void setServingGrams(double servingGrams) { this.servingGrams = servingGrams; }
    public String getServingLabel() { return servingLabel; }
    public void setServingLabel(String servingLabel) { this.servingLabel = servingLabel; }
    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
