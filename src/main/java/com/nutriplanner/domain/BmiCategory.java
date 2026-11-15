package com.nutriplanner.domain;

/** WHO adult BMI classification. */
public enum BmiCategory implements Labeled {
    UNDERWEIGHT("Underweight", 0, 18.5),
    NORMAL("Healthy weight", 18.5, 25),
    OVERWEIGHT("Overweight", 25, 30),
    OBESE_I("Obesity class I", 30, 35),
    OBESE_II("Obesity class II", 35, 40),
    OBESE_III("Obesity class III", 40, Double.MAX_VALUE);

    private final String label;
    private final double from;
    private final double to;

    BmiCategory(String label, double from, double to) {
        this.label = label;
        this.from = from;
        this.to = to;
    }

    @Override
    public String label() {
        return label;
    }

    public double from() {
        return from;
    }

    public double to() {
        return to;
    }

    public static BmiCategory of(double bmi) {
        for (BmiCategory c : values()) {
            if (bmi < c.to) return c;
        }
        return OBESE_III;
    }

    public String range() {
        if (this == UNDERWEIGHT) return "below 18.5";
        if (this == OBESE_III) return "40 and above";
        return String.format("%.1f – %.1f", from, to - 0.1);
    }
}
