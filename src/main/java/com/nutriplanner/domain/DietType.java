package com.nutriplanner.domain;

import java.util.EnumSet;
import java.util.Set;

/** A person's dietary preference. */
public enum DietType implements Labeled {
    VEGAN("Vegan", "No animal products at all", EnumSet.of(FoodDiet.VEGAN)),
    VEGETARIAN("Vegetarian", "Plants and dairy, no egg, meat or fish",
            EnumSet.of(FoodDiet.VEGAN, FoodDiet.VEGETARIAN)),
    EGGETARIAN("Eggetarian", "Vegetarian food plus eggs",
            EnumSet.of(FoodDiet.VEGAN, FoodDiet.VEGETARIAN, FoodDiet.EGG)),
    NON_VEGETARIAN("Non-vegetarian", "Everything, including meat and fish", EnumSet.allOf(FoodDiet.class));

    private final String label;
    private final String description;
    private final Set<FoodDiet> allowed;

    DietType(String label, String description, Set<FoodDiet> allowed) {
        this.label = label;
        this.description = description;
        this.allowed = allowed;
    }

    @Override
    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public boolean allows(FoodDiet diet) {
        return allowed.contains(diet);
    }

    public boolean allowsDairy() {
        return this != VEGAN;
    }
}
