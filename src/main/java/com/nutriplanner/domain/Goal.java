package com.nutriplanner.domain;

/** Fitness goal that shapes calorie and macronutrient targets. */
public enum Goal implements Labeled {
    LOSE_WEIGHT("Lose weight", "Eat in a moderate calorie deficit while keeping protein high", 0.5),
    MAINTAIN("Maintain weight", "Eat at your maintenance calories with a balanced plate", 0),
    GAIN_WEIGHT("Gain weight", "Eat in a steady calorie surplus to add healthy weight", 0.25),
    BUILD_MUSCLE("Build muscle", "A small surplus with high protein to support strength training", 0.25);

    private final String label;
    private final String description;
    private final double defaultWeeklyChangeKg;

    Goal(String label, String description, double defaultWeeklyChangeKg) {
        this.label = label;
        this.description = description;
        this.defaultWeeklyChangeKg = defaultWeeklyChangeKg;
    }

    @Override
    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public double defaultWeeklyChangeKg() {
        return defaultWeeklyChangeKg;
    }

    public boolean losesWeight() {
        return this == LOSE_WEIGHT;
    }

    public boolean gainsWeight() {
        return this == GAIN_WEIGHT || this == BUILD_MUSCLE;
    }
}
