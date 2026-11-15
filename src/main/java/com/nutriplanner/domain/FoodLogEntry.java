package com.nutriplanner.domain;

import com.nutriplanner.storage.Doc;
import com.nutriplanner.storage.EntityMapper;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One food eaten on a day. The food's name and the nutrients for the logged amount are copied at logging time,
 * so the diary stays correct even if the food is later edited or removed.
 */
public final class FoodLogEntry {

    public static final EntityMapper<FoodLogEntry> MAPPER =
            EntityMapper.of(FoodLogEntry::getId, FoodLogEntry::setId, FoodLogEntry::toDoc, FoodLogEntry::fromDoc);

    private long id;
    private long userId;
    private LocalDate date;
    private MealType mealType = MealType.BREAKFAST;
    private long foodId;
    private String foodName;
    private double quantity;
    private PortionUnit unit = PortionUnit.SERVING;
    private double grams;
    private Nutrients nutrients = Nutrients.ZERO;
    private Instant createdAt;

    public Doc toDoc() {
        return new Doc()
                .put("userId", userId)
                .put("date", date)
                .put("mealType", mealType)
                .put("foodId", foodId)
                .put("foodName", foodName)
                .put("quantity", quantity)
                .put("unit", unit)
                .put("grams", grams)
                .put("nutrients", nutrients.toDoc())
                .put("createdAt", createdAt);
    }

    public static FoodLogEntry fromDoc(long id, Doc d) {
        FoodLogEntry e = new FoodLogEntry();
        e.id = id;
        e.userId = d.getLong("userId", 0);
        e.date = d.getDate("date");
        e.mealType = d.getEnum("mealType", MealType.class, MealType.BREAKFAST);
        e.foodId = d.getLong("foodId", 0);
        e.foodName = d.getString("foodName", "Food");
        e.quantity = d.getDouble("quantity", 1);
        e.unit = d.getEnum("unit", PortionUnit.class, PortionUnit.SERVING);
        e.grams = d.getDouble("grams", 0);
        e.nutrients = Nutrients.fromDoc(d.getDoc("nutrients"));
        e.createdAt = d.getInstant("createdAt");
        return e;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public MealType getMealType() { return mealType; }
    public void setMealType(MealType mealType) { this.mealType = mealType; }
    public long getFoodId() { return foodId; }
    public void setFoodId(long foodId) { this.foodId = foodId; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public PortionUnit getUnit() { return unit; }
    public void setUnit(PortionUnit unit) { this.unit = unit; }
    public double getGrams() { return grams; }
    public void setGrams(double grams) { this.grams = grams; }
    public Nutrients getNutrients() { return nutrients; }
    public void setNutrients(Nutrients nutrients) { this.nutrients = nutrients; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
