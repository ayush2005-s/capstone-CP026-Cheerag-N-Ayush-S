package com.nutriplanner.repository;

import com.nutriplanner.storage.Database;

/** All repositories over one database. */
public final class Repositories {

    public final Database db;
    public final UserRepository users;
    public final ProfileRepository profiles;
    public final FoodRepository foods;
    public final FoodLogRepository foodLog;
    public final WeightRepository weights;
    public final MealPlanRepository mealPlans;
    public final WaterRepository water;
    public final AuditRepository audit;

    public Repositories(Database db) {
        this.db = db;
        this.users = new UserRepository(db);
        this.profiles = new ProfileRepository(db);
        this.foods = new FoodRepository(db);
        this.foodLog = new FoodLogRepository(db);
        this.weights = new WeightRepository(db);
        this.mealPlans = new MealPlanRepository(db);
        this.water = new WaterRepository(db);
        this.audit = new AuditRepository(db);
    }
}
