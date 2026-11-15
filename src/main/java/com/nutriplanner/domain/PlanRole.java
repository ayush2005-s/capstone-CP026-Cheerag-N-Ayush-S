package com.nutriplanner.domain;

/** The part a food plays on a plate when the meal planner builds a meal. */
public enum PlanRole implements Labeled {
    BASE("Staple (grain or bread)", 0.5, 4),
    PROTEIN("Protein source", 0.5, 3),
    VEGETABLE("Vegetable side", 0.5, 2),
    FRUIT("Fruit", 0.5, 2),
    DAIRY("Milk or curd side", 0.5, 2),
    SNACK("Snack", 0.5, 2),
    NONE("Not used in generated plans", 0, 0);

    private final String label;
    private final double minServings;
    private final double maxServings;

    PlanRole(String label, double minServings, double maxServings) {
        this.label = label;
        this.minServings = minServings;
        this.maxServings = maxServings;
    }

    @Override
    public String label() {
        return label;
    }

    public double minServings() {
        return minServings;
    }

    public double maxServings() {
        return maxServings;
    }
}
