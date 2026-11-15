package com.nutriplanner.service;

import com.nutriplanner.repository.Repositories;
import com.nutriplanner.security.PasswordHasher;

import java.time.Clock;

/** Wires every service together. One instance per running application. */
public final class Services {

    public final Repositories repos;
    public final Clock clock;
    public final AuditService audit;
    public final AuthService auth;
    public final WeightService weights;
    public final ProfileService profiles;
    public final FoodService foods;
    public final DiaryService diary;
    public final WaterService water;
    public final MealPlanService plans;
    public final DietGuideService guides;
    public final DashboardService dashboard;
    public final AccountService accounts;
    public final AdminService admin;

    public Services(Repositories repos, Clock clock, PasswordHasher hasher) {
        this.repos = repos;
        this.clock = clock;
        this.audit = new AuditService(repos.audit, clock);
        this.auth = new AuthService(repos, hasher, audit, clock);
        this.weights = new WeightService(repos, clock);
        this.profiles = new ProfileService(repos, weights, clock);
        this.foods = new FoodService(repos, clock);
        this.diary = new DiaryService(repos, foods, clock);
        this.water = new WaterService(repos, clock);
        this.plans = new MealPlanService(repos, foods, profiles, diary, clock);
        this.guides = new DietGuideService(foods);
        this.dashboard = new DashboardService(profiles, diary, weights, water, plans, foods, clock);
        this.accounts = new AccountService(repos, auth, audit);
        this.admin = new AdminService(repos, foods, accounts, auth, audit, clock);
    }
}
