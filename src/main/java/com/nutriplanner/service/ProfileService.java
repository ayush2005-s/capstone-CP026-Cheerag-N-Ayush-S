package com.nutriplanner.service;

import com.nutriplanner.domain.ActivityLevel;
import com.nutriplanner.domain.Allergen;
import com.nutriplanner.domain.BmrFormula;
import com.nutriplanner.domain.DietType;
import com.nutriplanner.domain.DietaryRestriction;
import com.nutriplanner.domain.Goal;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.Sex;
import com.nutriplanner.domain.ThemePreference;
import com.nutriplanner.domain.User;
import com.nutriplanner.repository.Repositories;
import com.nutriplanner.storage.DuplicateKeyException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Reads and updates a member's account details, body measurements, goals and diet. */
public final class ProfileService {

    public static final int MIN_AGE = 16;
    public static final int MAX_AGE = 100;

    private final Repositories repos;
    private final WeightService weights;
    private final Clock clock;

    public ProfileService(Repositories repos, WeightService weights, Clock clock) {
        this.repos = repos;
        this.weights = weights;
        this.clock = clock;
    }

    public Profile profile(long userId) {
        return repos.profiles.findByUserId(userId).orElseGet(() -> new Profile(userId));
    }

    public Optional<NutritionTargets> targets(long userId) {
        Profile p = profile(userId);
        return p.hasBodyMetrics() ? Optional.of(NutritionCalculator.targetsFor(p, today())) : Optional.empty();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // ------------------------------------------------------------------ inputs

    public record BodyDetails(Sex sex, LocalDate birthDate, Double heightCm, Double weightKg, Double bodyFatPercent) {
    }

    public record GoalDetails(Goal goal, Double targetWeightKg, Double weeklyChangeKg, ActivityLevel activityLevel) {
    }

    public record DietDetails(DietType dietType, Set<Allergen> allergens, Set<DietaryRestriction> restrictions) {
    }

    // ----------------------------------------------------------------- updates

    public Profile updateBody(long userId, BodyDetails body) {
        LocalDate today = today();
        ValidationException.Errors errors = new ValidationException.Errors();
        errors.check(body.sex() != null, "sex", "Choose the option used for your metabolic rate.");
        if (body.birthDate() == null) {
            errors.add("birthDate", "Enter your date of birth.");
        } else {
            int age = java.time.Period.between(body.birthDate(), today).getYears();
            if (body.birthDate().isAfter(today) || age < MIN_AGE) {
                errors.add("birthDate", "Nutri Planner's formulas are designed for people aged " + MIN_AGE + " and over.");
            } else if (age > MAX_AGE) {
                errors.add("birthDate", "Check the year of your date of birth.");
            }
        }
        if (body.heightCm() == null) errors.add("heightCm", "Enter your height in centimetres.");
        else if (body.heightCm() < 100 || body.heightCm() > 250) errors.add("heightCm", "Enter a height between 100 and 250 cm.");
        if (body.weightKg() == null) errors.add("weightKg", "Enter your weight in kilograms.");
        else if (body.weightKg() < WeightService.MIN_KG || body.weightKg() > WeightService.MAX_KG) {
            errors.add("weightKg", "Enter a weight between 25 and 350 kg.");
        }
        if (body.bodyFatPercent() != null && (body.bodyFatPercent() < 3 || body.bodyFatPercent() > 70)) {
            errors.add("bodyFatPercent", "Body fat should be between 3% and 70%, or leave it empty.");
        }
        errors.throwIfAny();

        return repos.db.write(() -> {
            Profile p = profile(userId);
            double weight = round1(body.weightKg());
            boolean weightChanged = Math.abs(p.getWeightKg() - weight) > 0.001;
            p.setSex(body.sex());
            p.setBirthDate(body.birthDate());
            p.setHeightCm(round1(body.heightCm()));
            p.setWeightKg(weight);
            p.setBodyFatPercent(body.bodyFatPercent() == null ? null : round1(body.bodyFatPercent()));
            if (p.getBmrFormula() == BmrFormula.KATCH_MCARDLE && p.getBodyFatPercent() == null) {
                p.setBmrFormula(BmrFormula.MIFFLIN_ST_JEOR);
            }
            p.setOnboardingStep(Math.max(p.getOnboardingStep(), 1));
            p.setUpdatedAt(clock.instant());
            repos.profiles.save(p);
            if (weightChanged) weights.log(userId, today, weight, "");
            return profile(userId);
        });
    }

    public Profile updateGoal(long userId, GoalDetails goal) {
        Profile current = profile(userId);
        ValidationException.Errors errors = new ValidationException.Errors();
        errors.check(goal.goal() != null, "goal", "Choose a goal.");
        errors.check(goal.activityLevel() != null, "activityLevel", "Choose how active you are.");
        Double target = goal.targetWeightKg();
        double weekly = goal.weeklyChangeKg() == null ? 0 : goal.weeklyChangeKg();
        if (goal.goal() != null && goal.goal() != Goal.MAINTAIN) {
            if (target == null) {
                errors.add("targetWeightKg", "Enter the weight you're aiming for.");
            } else if (target < WeightService.MIN_KG || target > WeightService.MAX_KG) {
                errors.add("targetWeightKg", "Enter a target between 25 and 350 kg.");
            } else if (current.getWeightKg() > 0) {
                if (goal.goal() == Goal.LOSE_WEIGHT && target >= current.getWeightKg()) {
                    errors.add("targetWeightKg", String.format(Locale.ROOT,
                            "To lose weight, choose a target below your current %.1f kg.", current.getWeightKg()));
                } else if (goal.goal().gainsWeight() && target < current.getWeightKg()) {
                    errors.add("targetWeightKg", String.format(Locale.ROOT,
                            "To gain, choose a target at or above your current %.1f kg.", current.getWeightKg()));
                }
                if (!errors.has("targetWeightKg") && current.getHeightCm() > 0) {
                    double minHealthy = NutritionCalculator.weightForBmi(18.5, current.getHeightCm());
                    if (target < minHealthy) {
                        errors.add("targetWeightKg", String.format(Locale.ROOT,
                                "That target is below a healthy weight for your height. The healthy range starts at %.1f kg.", minHealthy));
                    }
                }
            }
            double min = 0.1, max = goal.goal() == Goal.LOSE_WEIGHT ? 1.0 : 0.5;
            if (weekly < min || weekly > max) {
                errors.add("weeklyChangeKg", String.format(Locale.ROOT, "Choose a pace between %.2f and %.2f kg a week.", min, max));
            }
        }
        errors.throwIfAny();

        return repos.db.write(() -> {
            Profile p = profile(userId);
            p.setGoal(goal.goal());
            p.setActivityLevel(goal.activityLevel());
            if (goal.goal() == Goal.MAINTAIN) {
                p.setTargetWeightKg(p.getWeightKg() > 0 ? p.getWeightKg() : null);
                p.setWeeklyChangeKg(0);
            } else {
                p.setTargetWeightKg(round1(target));
                p.setWeeklyChangeKg(weekly);
            }
            p.setOnboardingStep(Math.max(p.getOnboardingStep(), 2));
            p.setUpdatedAt(clock.instant());
            return repos.profiles.save(p);
        });
    }

    public Profile updateDiet(long userId, DietDetails diet) {
        if (diet.dietType() == null) throw ValidationException.field("dietType", "Choose how you eat.");
        return repos.db.write(() -> {
            Profile p = profile(userId);
            p.setDietType(diet.dietType());
            p.setAllergens(diet.allergens() == null ? Set.of() : diet.allergens());
            p.setRestrictions(diet.restrictions() == null ? Set.of() : diet.restrictions());
            p.setOnboardingStep(Math.max(p.getOnboardingStep(), 3));
            p.setUpdatedAt(clock.instant());
            return repos.profiles.save(p);
        });
    }

    /** Marks onboarding as finished once every section has been filled in. */
    public Profile completeOnboarding(long userId) {
        return repos.db.write(() -> {
            Profile p = profile(userId);
            if (!p.hasBodyMetrics()) throw new ValidationException("Fill in your body measurements first.");
            p.setCompleted(true);
            p.setOnboardingStep(3);
            p.setUpdatedAt(clock.instant());
            return repos.profiles.save(p);
        });
    }

    public Profile updatePreferences(long userId, BmrFormula formula, Integer calorieOverride) {
        Profile current = profile(userId);
        ValidationException.Errors errors = new ValidationException.Errors();
        errors.check(formula != null, "bmrFormula", "Choose a formula.");
        if (formula == BmrFormula.KATCH_MCARDLE && current.getBodyFatPercent() == null) {
            errors.add("bmrFormula", "Katch–McArdle needs your body-fat percentage. Add it under Body first.");
        }
        if (calorieOverride != null && (calorieOverride < 1000 || calorieOverride > 6000)) {
            errors.add("calorieOverride", "Enter a daily target between 1,000 and 6,000 kcal, or leave it empty.");
        }
        errors.throwIfAny();
        return repos.db.write(() -> {
            Profile p = profile(userId);
            p.setBmrFormula(formula);
            p.setCalorieOverride(calorieOverride);
            p.setUpdatedAt(clock.instant());
            return repos.profiles.save(p);
        });
    }

    public void updateTheme(long userId, ThemePreference theme) {
        if (theme == null) throw ValidationException.field("theme", "Choose a theme.");
        repos.db.write(() -> {
            Profile p = profile(userId);
            p.setTheme(theme);
            repos.profiles.save(p);
        });
    }

    public User updateAccount(long userId, String fullName, String email) {
        String name = AuthService.clean(fullName);
        String mail = AuthService.clean(email).toLowerCase(Locale.ROOT);
        ValidationException.Errors errors = new ValidationException.Errors();
        AuthService.validateFullName(name, errors);
        AuthService.validateEmail(mail, errors);
        if (!errors.has("email")) {
            repos.users.findByEmail(mail).filter(u -> u.getId() != userId)
                    .ifPresent(u -> errors.add("email", "Another account already uses this email."));
        }
        errors.throwIfAny();
        User user = repos.users.findById(userId).orElseThrow(() -> new NotFoundException("Account not found"));
        user.setFullName(name);
        user.setEmail(mail);
        try {
            return repos.users.update(user);
        } catch (DuplicateKeyException e) {
            throw ValidationException.field("email", "Another account already uses this email.");
        }
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
