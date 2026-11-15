package com.nutriplanner.service;

import com.nutriplanner.domain.ActivityLevel;
import com.nutriplanner.domain.BmiCategory;
import com.nutriplanner.domain.BmrFormula;
import com.nutriplanner.domain.DietaryRestriction;
import com.nutriplanner.domain.Goal;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.Sex;
import com.nutriplanner.testkit.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static com.nutriplanner.testkit.Assert.assertEquals;
import static com.nutriplanner.testkit.Assert.assertFalse;
import static com.nutriplanner.testkit.Assert.assertNear;
import static com.nutriplanner.testkit.Assert.assertThrows;
import static com.nutriplanner.testkit.Assert.assertTrue;

public class NutritionCalculatorTest {

    @Test
    void bmiAndCategories() {
        assertNear(22.857, NutritionCalculator.bmi(70, 175), 0.001, "BMI 70 kg / 175 cm");
        assertEquals(BmiCategory.NORMAL, BmiCategory.of(22.86));
        assertEquals(BmiCategory.UNDERWEIGHT, BmiCategory.of(18.49));
        assertEquals(BmiCategory.NORMAL, BmiCategory.of(18.5));
        assertEquals(BmiCategory.OVERWEIGHT, BmiCategory.of(25.0));
        assertEquals(BmiCategory.OBESE_I, BmiCategory.of(30.0));
        assertEquals(BmiCategory.OBESE_III, BmiCategory.of(45));
        double[] range = NutritionCalculator.healthyWeightRange(175);
        assertNear(56.66, range[0], 0.01, "healthy minimum");
        assertNear(76.26, range[1], 0.01, "healthy maximum");
        assertThrows(IllegalArgumentException.class, () -> NutritionCalculator.bmi(70, 0));
    }

    @Test
    void bmrFormulasMatchPublishedEquations() {
        assertNear(1648.75, NutritionCalculator.bmrMifflinStJeor(Sex.MALE, 70, 175, 30), 0.001, "Mifflin male");
        assertNear(1482.75, NutritionCalculator.bmrMifflinStJeor(Sex.FEMALE, 70, 175, 30), 0.001, "Mifflin female");
        assertNear(1695.667, NutritionCalculator.bmrHarrisBenedict(Sex.MALE, 70, 175, 30), 0.001, "Harris-Benedict male");
        assertNear(1405.333, NutritionCalculator.bmrHarrisBenedict(Sex.FEMALE, 60, 165, 25), 0.001, "Harris-Benedict female");
        assertNear(1579.6, NutritionCalculator.bmrKatchMcArdle(70, 20), 0.001, "Katch-McArdle");
        assertNear(1648.75, NutritionCalculator.bmr(BmrFormula.KATCH_MCARDLE, Sex.MALE, 70, 175, 30, null), 0.001,
                "Katch-McArdle without body fat falls back to Mifflin");
    }

    @Test
    void tdeeUsesActivityFactors() {
        assertNear(2555.5625, NutritionCalculator.tdee(1648.75, ActivityLevel.MODERATE), 1e-6, "moderate");
        assertNear(1978.5, NutritionCalculator.tdee(1648.75, ActivityLevel.SEDENTARY), 1e-6, "sedentary");
        assertNear(3132.625, NutritionCalculator.tdee(1648.75, ActivityLevel.EXTRA), 1e-6, "extra");
    }

    @Test
    void calorieTargetsPerGoal() {
        double tdee = 2555.5625;
        NutritionCalculator.CalorieTarget lose = NutritionCalculator.calorieTarget(tdee, Goal.LOSE_WEIGHT, 0.5, Sex.MALE);
        assertEquals(2010, lose.kcal(), "0.5 kg/week deficit of 550 kcal");
        assertFalse(lose.limited(), "a 550 kcal deficit is within limits");

        NutritionCalculator.CalorieTarget fast = NutritionCalculator.calorieTarget(tdee, Goal.LOSE_WEIGHT, 1.0, Sex.MALE);
        assertEquals(1920, fast.kcal(), "1 kg/week is capped at a 25% deficit");
        assertTrue(fast.limited(), "aggressive deficit is flagged");

        NutritionCalculator.CalorieTarget floor = NutritionCalculator.calorieTarget(1400, Goal.LOSE_WEIGHT, 0.5, Sex.FEMALE);
        assertEquals(1200, floor.kcal(), "never below 1200 kcal for women");

        assertEquals(2830, NutritionCalculator.calorieTarget(tdee, Goal.GAIN_WEIGHT, 0.25, Sex.MALE).kcal());
        assertEquals(2810, NutritionCalculator.calorieTarget(tdee, Goal.BUILD_MUSCLE, 0, Sex.MALE).kcal());
        assertEquals(2560, NutritionCalculator.calorieTarget(tdee, Goal.MAINTAIN, 0, Sex.MALE).kcal());
    }

    @Test
    void macroSplit() {
        NutritionTargets.Macros m = NutritionCalculator.macros(2010, Goal.LOSE_WEIGHT, 70, 175, Set.of());
        assertEquals(112, m.proteinG(), "1.6 g/kg protein");
        assertEquals(60, m.fatG(), "27% of calories from fat");
        assertEquals(255, m.carbsG(), "carbs fill the remainder");
        assertEquals(28, m.fiberG(), "14 g fibre per 1000 kcal");
        assertEquals(100, m.proteinPct() + m.carbsPct() + m.fatPct(), "percentages add to 100");
        double kcalFromMacros = m.proteinG() * 4 + m.carbsG() * 4 + m.fatG() * 9;
        assertNear(2010, kcalFromMacros, 15, "macros add back up to the calorie target");

        NutritionTargets.Macros heavy = NutritionCalculator.macros(2200, Goal.MAINTAIN, 100, 170, Set.of());
        assertEquals(87, heavy.proteinG(), "protein uses the weight at BMI 25 when BMI is above 25");

        NutritionTargets.Macros lowCarb = NutritionCalculator.macros(2000, Goal.MAINTAIN, 70, 175, EnumSet.of(DietaryRestriction.LOW_CARB));
        assertTrue(lowCarb.carbsG() * 4 <= 2000 * 0.26 + 4, "low carb keeps carbs at or under 26% of calories");

        NutritionTargets.Macros lowFat = NutritionCalculator.macros(2000, Goal.MAINTAIN, 70, 175, EnumSet.of(DietaryRestriction.LOW_FAT));
        assertEquals(44, lowFat.fatG(), "low fat sets fat to 20% of calories");
    }

    @Test
    void waterAndGoalTimeline() {
        assertEquals(2950, NutritionCalculator.waterMl(70, ActivityLevel.MODERATE));
        assertNear(10.0, NutritionCalculator.weeksToGoal(70, 65.0, 0.5), 1e-9, "5 kg at 0.5 kg/week");
        assertEquals(null, NutritionCalculator.weeksToGoal(70, null, 0.5));
    }

    @Test
    void fullProfileTargets() {
        Profile p = new Profile(1);
        p.setSex(Sex.MALE);
        p.setBirthDate(LocalDate.of(1996, 11, 14));
        p.setHeightCm(175);
        p.setWeightKg(70);
        p.setActivityLevel(ActivityLevel.MODERATE);
        p.setGoal(Goal.LOSE_WEIGHT);
        p.setTargetWeightKg(65.0);
        p.setWeeklyChangeKg(0.5);
        NutritionTargets t = NutritionCalculator.targetsFor(p, LocalDate.of(2026, 11, 14));
        assertEquals(30, t.age());
        assertEquals(1649, t.bmr());
        assertEquals(2556, t.tdee());
        assertEquals(2010, t.calories());
        assertEquals(LocalDate.of(2027, 1, 23), t.projectedGoalDate(), "10 weeks from today");

        p.setCalorieOverride(1800);
        NutritionTargets custom = NutritionCalculator.targetsFor(p, LocalDate.of(2026, 11, 14));
        assertEquals(1800, custom.calories());
        assertTrue(custom.customCalories(), "override is reported");
    }
}
