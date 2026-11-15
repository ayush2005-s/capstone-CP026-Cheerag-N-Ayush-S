package com.nutriplanner.domain;

/** Which kind of diet a food belongs to, from most to least restrictive. */
public enum FoodDiet implements Labeled {
    VEGAN("Vegan", "Plant-based, no animal products"),
    VEGETARIAN("Vegetarian", "May contain dairy or honey, no egg, meat or fish"),
    EGG("Contains egg", "Vegetarian food that contains egg"),
    NON_VEG("Non-vegetarian", "Contains meat, poultry, fish or seafood");

    private final String label;
    private final String description;

    FoodDiet(String label, String description) {
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
}
