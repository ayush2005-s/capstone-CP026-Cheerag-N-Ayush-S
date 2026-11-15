package com.nutriplanner.domain;

/** Physical activity level with the standard TDEE multipliers applied to BMR. */
public enum ActivityLevel implements Labeled {
    SEDENTARY(1.2, "Sedentary", "Desk job and little or no exercise", 0),
    LIGHT(1.375, "Lightly active", "Light exercise or sport 1–3 days a week", 250),
    MODERATE(1.55, "Moderately active", "Moderate exercise or sport 3–5 days a week", 500),
    VERY(1.725, "Very active", "Hard exercise or sport 6–7 days a week", 750),
    EXTRA(1.9, "Extra active", "Physical job, or training twice a day", 1000);

    private final double factor;
    private final String label;
    private final String description;
    private final int extraWaterMl;

    ActivityLevel(double factor, String label, String description, int extraWaterMl) {
        this.factor = factor;
        this.label = label;
        this.description = description;
        this.extraWaterMl = extraWaterMl;
    }

    public double factor() {
        return factor;
    }

    @Override
    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public int extraWaterMl() {
        return extraWaterMl;
    }
}
