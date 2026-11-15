package com.nutriplanner.domain;

/** The four meals of the day and the share of daily calories each one gets in a plan. */
public enum MealType implements Labeled {
    BREAKFAST("Breakfast", 0.25),
    LUNCH("Lunch", 0.35),
    DINNER("Dinner", 0.30),
    SNACK("Snacks", 0.10);

    private final String label;
    private final double share;

    MealType(String label, double share) {
        this.label = label;
        this.share = share;
    }

    @Override
    public String label() {
        return label;
    }

    public double share() {
        return share;
    }
}
