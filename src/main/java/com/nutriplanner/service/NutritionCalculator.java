package com.nutriplanner.service;

import com.nutriplanner.domain.ActivityLevel;
import com.nutriplanner.domain.BmiCategory;
import com.nutriplanner.domain.BmrFormula;
import com.nutriplanner.domain.DietaryRestriction;
import com.nutriplanner.domain.Goal;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.Sex;

import java.time.LocalDate;
import java.util.Set;

/**
 * The nutrition science of the app, as pure functions.
 *
 * <ul>
 *   <li>BMI and classes follow the WHO adult classification.</li>
 *   <li>BMR uses Mifflin–St Jeor (default), the revised Harris–Benedict equation, or Katch–McArdle.</li>
 *   <li>TDEE multiplies BMR by the standard activity factors (1.2 – 1.9).</li>
 *   <li>Weight change uses the common estimate of 7,700 kcal per kilogram of body weight.</li>
 *   <li>Protein is set per kilogram of body weight (using the weight at BMI 25 when BMI is above 25), fat as a share
 *       of calories, and carbohydrate fills the rest. Fibre follows the 14 g per 1,000 kcal guideline.</li>
 * </ul>
 */
public final class NutritionCalculator {

    public static final double KCAL_PER_KG = 7700;
    public static final int MIN_KCAL_MALE = 1500;
    public static final int MIN_KCAL_FEMALE = 1200;
    /** A deficit larger than this share of maintenance calories is considered too aggressive. */
    public static final double MAX_DEFICIT_SHARE = 0.25;

    private NutritionCalculator() {
    }

    // ---------------------------------------------------------------- body indices

    public static double bmi(double weightKg, double heightCm) {
        requirePositive(weightKg, "weight");
        requirePositive(heightCm, "height");
        double m = heightCm / 100.0;
        return weightKg / (m * m);
    }

    public static double weightForBmi(double bmi, double heightCm) {
        double m = heightCm / 100.0;
        return bmi * m * m;
    }

    /** Healthy weight range for a height: BMI 18.5 to 24.9. */
    public static double[] healthyWeightRange(double heightCm) {
        return new double[]{weightForBmi(18.5, heightCm), weightForBmi(24.9, heightCm)};
    }

    // ------------------------------------------------------------------------- BMR

    public static double bmrMifflinStJeor(Sex sex, double weightKg, double heightCm, int age) {
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        return sex == Sex.MALE ? base + 5 : base - 161;
    }

    public static double bmrHarrisBenedict(Sex sex, double weightKg, double heightCm, int age) {
        if (sex == Sex.MALE) return 88.362 + 13.397 * weightKg + 4.799 * heightCm - 5.677 * age;
        return 447.593 + 9.247 * weightKg + 3.098 * heightCm - 4.330 * age;
    }

    public static double bmrKatchMcArdle(double weightKg, double bodyFatPercent) {
        double leanMass = weightKg * (1 - bodyFatPercent / 100.0);
        return 370 + 21.6 * leanMass;
    }

    /** BMR with the chosen formula. Katch–McArdle falls back to Mifflin–St Jeor when body fat is unknown. */
    public static double bmr(BmrFormula formula, Sex sex, double weightKg, double heightCm, int age, Double bodyFatPercent) {
        requirePositive(weightKg, "weight");
        requirePositive(heightCm, "height");
        switch (effectiveFormula(formula, bodyFatPercent)) {
            case HARRIS_BENEDICT:
                return bmrHarrisBenedict(sex, weightKg, heightCm, age);
            case KATCH_MCARDLE:
                return bmrKatchMcArdle(weightKg, bodyFatPercent);
            default:
                return bmrMifflinStJeor(sex, weightKg, heightCm, age);
        }
    }

    public static BmrFormula effectiveFormula(BmrFormula formula, Double bodyFatPercent) {
        if (formula == BmrFormula.KATCH_MCARDLE && (bodyFatPercent == null || bodyFatPercent <= 0)) {
            return BmrFormula.MIFFLIN_ST_JEOR;
        }
        return formula == null ? BmrFormula.MIFFLIN_ST_JEOR : formula;
    }

    public static double tdee(double bmr, ActivityLevel activity) {
        return bmr * activity.factor();
    }

    // -------------------------------------------------------------------- calories

    /** The daily calorie goal and how it was derived. */
    public record CalorieTarget(int kcal, int adjustment, boolean limited, String note) {
    }

    public static CalorieTarget calorieTarget(double tdee, Goal goal, double weeklyChangeKg, Sex sex) {
        double raw;
        switch (goal) {
            case LOSE_WEIGHT:
                raw = tdee - Math.abs(weeklyChangeKg) * KCAL_PER_KG / 7.0;
                break;
            case GAIN_WEIGHT:
                raw = tdee + Math.abs(weeklyChangeKg) * KCAL_PER_KG / 7.0;
                break;
            case BUILD_MUSCLE:
                raw = tdee + clamp(tdee * 0.10, 200, 400);
                break;
            default:
                raw = tdee;
        }
        boolean limited = false;
        String note = "";
        if (goal == Goal.LOSE_WEIGHT) {
            double floorByDeficit = tdee * (1 - MAX_DEFICIT_SHARE);
            if (raw < floorByDeficit) {
                raw = floorByDeficit;
                limited = true;
                note = "Limited so the deficit stays within 25% of your maintenance calories.";
            }
            int minimum = sex == Sex.MALE ? MIN_KCAL_MALE : MIN_KCAL_FEMALE;
            if (raw < minimum) {
                raw = minimum;
                limited = true;
                note = String.format(java.util.Locale.ENGLISH, "Raised to %,d kcal, the lowest daily intake that is generally safe without medical supervision.", minimum);
            }
        }
        int kcal = (int) (Math.round(raw / 10.0) * 10);
        int adjustment = (int) Math.round(kcal - tdee);
        return new CalorieTarget(kcal, adjustment, limited, note);
    }

    // ---------------------------------------------------------------------- macros

    public static double proteinPerKg(Goal goal) {
        switch (goal) {
            case LOSE_WEIGHT:
                return 1.6;
            case GAIN_WEIGHT:
                return 1.4;
            case BUILD_MUSCLE:
                return 2.0;
            default:
                return 1.2;
        }
    }

    public static double fatShare(Goal goal) {
        switch (goal) {
            case LOSE_WEIGHT:
                return 0.27;
            case BUILD_MUSCLE:
                return 0.25;
            default:
                return 0.30;
        }
    }

    public static NutritionTargets.Macros macros(int kcal, Goal goal, double weightKg, double heightCm,
                                                 Set<DietaryRestriction> restrictions) {
        requirePositive(kcal, "calories");
        double referenceWeight = weightKg;
        if (heightCm > 0 && bmi(weightKg, heightCm) > 25) referenceWeight = weightForBmi(25, heightCm);

        double proteinG = referenceWeight * proteinPerKg(goal);
        proteinG = Math.min(proteinG, kcal * 0.35 / 4.0);

        double fatShare = restrictions.contains(DietaryRestriction.LOW_FAT) ? 0.20 : fatShare(goal);
        double fatKcal = kcal * fatShare;
        double carbKcal = kcal - proteinG * 4 - fatKcal;

        if (restrictions.contains(DietaryRestriction.LOW_CARB)) {
            double maxCarbKcal = kcal * 0.26;
            if (carbKcal > maxCarbKcal) {
                fatKcal += carbKcal - maxCarbKcal;
                carbKcal = maxCarbKcal;
            }
        }
        if (carbKcal < 0) {
            fatKcal = Math.max(kcal * 0.20, fatKcal + carbKcal);
            carbKcal = Math.max(0, kcal - proteinG * 4 - fatKcal);
        }

        int p = (int) Math.round(proteinG);
        int f = (int) Math.round(fatKcal / 9.0);
        int c = (int) Math.round(carbKcal / 4.0);
        int fiber = (int) Math.round(kcal * 14.0 / 1000.0);
        int pPct = (int) Math.round(p * 4 * 100.0 / kcal);
        int fPct = (int) Math.round(f * 9 * 100.0 / kcal);
        int cPct = Math.max(0, 100 - pPct - fPct);
        return new NutritionTargets.Macros(p, c, f, fiber, pPct, cPct, fPct);
    }

    /** Daily water: 35 ml per kg body weight plus extra for activity, rounded to 50 ml. */
    public static int waterMl(double weightKg, ActivityLevel activity) {
        double ml = 35 * weightKg + activity.extraWaterMl();
        return (int) (Math.round(ml / 50.0) * 50);
    }

    /** Weeks needed to move from {@code current} to {@code target} at {@code weeklyChange} kg per week. */
    public static Double weeksToGoal(double currentKg, Double targetKg, double weeklyChangeKg) {
        if (targetKg == null || weeklyChangeKg <= 0) return null;
        double diff = Math.abs(currentKg - targetKg);
        if (diff < 0.05) return 0.0;
        return diff / weeklyChangeKg;
    }

    // ------------------------------------------------------------------- profile

    /** Computes every target for a profile. The profile must have body metrics. */
    public static NutritionTargets targetsFor(Profile p, LocalDate today) {
        if (!p.hasBodyMetrics()) throw new IllegalArgumentException("Profile is missing body measurements");
        int age = p.age(today);
        double bmi = bmi(p.getWeightKg(), p.getHeightCm());
        double[] range = healthyWeightRange(p.getHeightCm());
        BmrFormula formula = effectiveFormula(p.getBmrFormula(), p.getBodyFatPercent());
        double bmr = bmr(formula, p.getSex(), p.getWeightKg(), p.getHeightCm(), age, p.getBodyFatPercent());
        double tdee = tdee(bmr, p.getActivityLevel());
        CalorieTarget target = calorieTarget(tdee, p.getGoal(), p.getWeeklyChangeKg(), p.getSex());

        int calories = target.kcal();
        boolean custom = p.getCalorieOverride() != null;
        if (custom) calories = p.getCalorieOverride();

        NutritionTargets.Macros macros = macros(calories, p.getGoal(), p.getWeightKg(), p.getHeightCm(), p.getRestrictions());

        Double weeks = null;
        LocalDate projected = null;
        if (p.getGoal() != Goal.MAINTAIN) {
            double rate = p.getGoal() == Goal.BUILD_MUSCLE && p.getWeeklyChangeKg() <= 0 ? 0.25 : p.getWeeklyChangeKg();
            weeks = weeksToGoal(p.getWeightKg(), p.getTargetWeightKg(), rate);
            if (weeks != null) projected = today.plusDays(Math.round(weeks * 7));
        }
        return new NutritionTargets(bmi, BmiCategory.of(bmi), range[0], range[1], age, formula,
                (int) Math.round(bmr), (int) Math.round(tdee), calories, custom ? (int) Math.round(calories - tdee) : target.adjustment(),
                !custom && target.limited(), custom ? "" : target.note(), custom, macros,
                waterMl(p.getWeightKg(), p.getActivityLevel()), weeks, projected);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static void requirePositive(double v, String what) {
        if (!(v > 0) || Double.isInfinite(v)) throw new IllegalArgumentException(what + " must be positive");
    }
}
