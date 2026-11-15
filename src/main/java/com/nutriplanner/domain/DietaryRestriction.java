package com.nutriplanner.domain;

import java.util.Optional;

/** Nutrient-based restrictions. Each one decides from a food's values per 100 g whether it fits. */
public enum DietaryRestriction implements Labeled {
    LOW_SODIUM("Low sodium", "Skip foods with more than 400 mg sodium per 100 g"),
    LOW_SUGAR("Low sugar", "Skip foods with more than 15 g sugar per 100 g"),
    LOW_CARB("Low carb", "Skip foods with more than 35 g carbohydrate per 100 g and cap carbs at 26% of calories"),
    LOW_FAT("Low fat", "Skip foods with more than 20 g fat per 100 g and cap fat at 20% of calories");

    private final String label;
    private final String description;

    DietaryRestriction(String label, String description) {
        this.label = label;
        this.description = description;
    }

    @Override
    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /** Returns why {@code food} breaks this restriction, or empty when it fits. */
    public Optional<String> violation(Food food) {
        switch (this) {
            case LOW_SODIUM:
                return food.getSodiumMg() > 400 ? Optional.of("High in sodium") : Optional.empty();
            case LOW_SUGAR:
                return food.getSugar() > 15 ? Optional.of("High in sugar") : Optional.empty();
            case LOW_CARB:
                return food.getCarbs() > 35 ? Optional.of("High in carbohydrate") : Optional.empty();
            case LOW_FAT:
                return food.getFat() > 20 ? Optional.of("High in fat") : Optional.empty();
            default:
                return Optional.empty();
        }
    }
}
