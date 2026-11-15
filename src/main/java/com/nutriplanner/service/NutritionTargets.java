package com.nutriplanner.service;

import com.nutriplanner.domain.BmiCategory;
import com.nutriplanner.domain.BmrFormula;

import java.time.LocalDate;

/** Everything computed from a profile: body indices, energy needs and daily nutrient targets. */
public record NutritionTargets(
        double bmi,
        BmiCategory bmiCategory,
        double healthyWeightMinKg,
        double healthyWeightMaxKg,
        int age,
        BmrFormula formula,
        int bmr,
        int tdee,
        int calories,
        int adjustment,
        boolean calorieLimited,
        String calorieNote,
        boolean customCalories,
        Macros macros,
        int waterMl,
        Double weeksToGoal,
        LocalDate projectedGoalDate) {

    /** Daily macronutrient targets in grams, with the share of calories each provides. */
    public record Macros(int proteinG, int carbsG, int fatG, int fiberG, int proteinPct, int carbsPct, int fatPct) {
    }

    public int proteinG() {
        return macros.proteinG();
    }

    public int carbsG() {
        return macros.carbsG();
    }

    public int fatG() {
        return macros.fatG();
    }

    public int fiberG() {
        return macros.fiberG();
    }
}
