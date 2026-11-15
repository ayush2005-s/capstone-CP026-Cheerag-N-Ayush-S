package com.nutriplanner.seed;

import com.nutriplanner.domain.ActivityLevel;
import com.nutriplanner.domain.Allergen;
import com.nutriplanner.domain.DietType;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.Goal;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.Role;
import com.nutriplanner.domain.Sex;
import com.nutriplanner.domain.User;
import com.nutriplanner.service.MealPlanService;
import com.nutriplanner.service.ProfileService;
import com.nutriplanner.service.Services;
import com.nutriplanner.service.ValidationException;
import com.nutriplanner.storage.Doc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/** First-run setup: the food database, the first administrator and (optionally) a demo member with history. */
public final class DataSeeder {

    public static final String ADMIN_USERNAME = "admin";
    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "Healthy#2026";

    private static final Logger LOG = Logger.getLogger(DataSeeder.class.getName());

    private final Services services;

    public DataSeeder(Services services) {
        this.services = services;
    }

    public void seed(String adminPassword, boolean demo) {
        seedFoods();
        seedAdmin(adminPassword);
        if (demo) seedDemo();
    }

    void seedFoods() {
        if (services.repos.foods.count() > 0) return;
        Instant now = services.clock.instant();
        services.repos.db.write(() -> {
            for (Food f : FoodCatalog.foods()) {
                f.setCreatedAt(now);
                f.setUpdatedAt(now);
                services.repos.foods.save(f);
            }
        });
        LOG.info("Loaded " + services.repos.foods.count() + " foods into the database.");
    }

    void seedAdmin(String password) {
        if (services.repos.users.countAdmins(false) > 0) return;
        User admin = services.auth.createAccount("Admin", ADMIN_USERNAME, "admin@nutriplanner.local",
                password, Role.ADMIN, true);
        LOG.info("Created administrator '" + admin.getUsername() + "'. The password must be changed at first login.");
    }

    /** A realistic member with eight weeks of weigh-ins, three weeks of diary entries and a plan for this week. */
    void seedDemo() {
        if (services.repos.users.findByUsername(DEMO_USERNAME).isPresent()) return;
        if (services.repos.db.meta("demo.seeded") != null) return;
        LocalDate today = LocalDate.now(services.clock);
        User user = services.auth.createAccount("Asha Verma", DEMO_USERNAME, "demo@nutriplanner.local", DEMO_PASSWORD, Role.USER, false);
        long id = user.getId();
        Random random = new Random(20261114L);

        double startWeight = 72.4;
        services.profiles.updateBody(id, new ProfileService.BodyDetails(Sex.FEMALE, LocalDate.of(1993, 8, 21), 163.0, startWeight, 31.0));
        services.profiles.updateGoal(id, new ProfileService.GoalDetails(Goal.LOSE_WEIGHT, 62.0, 0.5, ActivityLevel.MODERATE));
        services.profiles.updateDiet(id, new ProfileService.DietDetails(DietType.VEGETARIAN, EnumSet.of(Allergen.PEANUT), Set.of()));
        services.profiles.completeOnboarding(id);

        // Weigh-ins every two or three days over eight weeks, trending down about 0.45 kg a week.
        for (int daysAgo = 56; daysAgo >= 1; daysAgo -= 2 + random.nextInt(2)) {
            double weight = startWeight - (56 - daysAgo) * 0.065 + (random.nextDouble() - 0.5) * 0.6;
            services.weights.log(id, today.minusDays(daysAgo), weight, daysAgo == 56 ? "Starting point" : "");
        }
        services.weights.log(id, today, startWeight - 56 * 0.065 + 0.1, "Morning, before breakfast");

        // Plans for the current week, then diary history built from generated plans.
        LocalDate weekStart = MealPlanService.weekStart(today);
        services.plans.generateWeek(id, today);
        for (int daysAgo = 21; daysAgo >= 1; daysAgo--) {
            LocalDate date = today.minusDays(daysAgo);
            if (daysAgo == 9 || daysAgo == 16) continue; // a couple of days without logging, like real life
            if (date.isBefore(weekStart)) services.plans.generateDay(id, date);
            for (MealType meal : MealType.values()) {
                if (meal == MealType.SNACK && random.nextInt(3) == 0) continue;
                try {
                    services.plans.logMeal(id, date, meal);
                } catch (ValidationException ignored) {
                    // nothing planned for that meal
                }
            }
            services.water.change(id, date, 5 + random.nextInt(5));
        }
        for (MealType meal : new MealType[]{MealType.BREAKFAST, MealType.LUNCH}) {
            try {
                services.plans.logMeal(id, today, meal);
            } catch (ValidationException ignored) {
                // nothing planned for that meal
            }
        }
        services.water.change(id, today, 4);
        services.repos.db.putMeta("demo.seeded", new Doc().put("at", services.clock.instant().toString()).toString());
        LOG.info("Created demo member '" + DEMO_USERNAME + "'.");
    }
}
