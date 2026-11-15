package com.nutriplanner.testkit;

import com.nutriplanner.domain.ActivityLevel;
import com.nutriplanner.domain.Allergen;
import com.nutriplanner.domain.DietType;
import com.nutriplanner.domain.DietaryRestriction;
import com.nutriplanner.domain.Goal;
import com.nutriplanner.domain.Role;
import com.nutriplanner.domain.Sex;
import com.nutriplanner.domain.User;
import com.nutriplanner.repository.Repositories;
import com.nutriplanner.security.PasswordHasher;
import com.nutriplanner.seed.DataSeeder;
import com.nutriplanner.service.ProfileService;
import com.nutriplanner.service.Services;
import com.nutriplanner.storage.Database;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

/** An in-memory application for service tests, with a fixed clock (14 Nov 2026, 10:00 in India). */
public class TestServices implements AutoCloseable {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    public static final Instant NOW = LocalDate.of(2026, 11, 14).atTime(10, 0).atZone(ZONE).toInstant();
    public static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    public final Database db = Database.inMemory();
    public final Repositories repos = new Repositories(db);
    public final MutableClock clock = new MutableClock(NOW, ZONE);
    public final Services services = new Services(repos, clock, new PasswordHasher(1000));

    public TestServices() {
        new DataSeeder(services).seed("Planner#Start1", false);
    }

    public User admin() {
        return repos.users.findByUsername(DataSeeder.ADMIN_USERNAME).orElseThrow();
    }

    /** A member with a completed profile. */
    public User member(String username, DietType diet, Set<Allergen> allergens) {
        User u = services.auth.createAccount("Test " + username, username, username + "@example.com", "Secret#123", Role.USER, false);
        services.profiles.updateBody(u.getId(), new ProfileService.BodyDetails(Sex.MALE, LocalDate.of(1996, 11, 14), 175.0, 70.0, null));
        services.profiles.updateGoal(u.getId(), new ProfileService.GoalDetails(Goal.LOSE_WEIGHT, 65.0, 0.5, ActivityLevel.MODERATE));
        services.profiles.updateDiet(u.getId(), new ProfileService.DietDetails(diet, allergens, Set.<DietaryRestriction>of()));
        services.profiles.completeOnboarding(u.getId());
        return u;
    }

    @Override
    public void close() {
        db.close();
    }

    /** A clock tests can move forward. */
    public static final class MutableClock extends Clock {
        private Instant now;
        private final ZoneId zone;

        public MutableClock(Instant now, ZoneId zone) {
            this.now = now;
            this.zone = zone;
        }

        public void advanceMinutes(long minutes) {
            now = now.plusSeconds(minutes * 60);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId z) {
            return new MutableClock(now, z);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
