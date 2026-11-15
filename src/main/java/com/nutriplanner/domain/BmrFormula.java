package com.nutriplanner.domain;

/** Equations for estimating basal metabolic rate. */
public enum BmrFormula implements Labeled {
    MIFFLIN_ST_JEOR("Mifflin–St Jeor", "The most accurate general-purpose equation for most adults (1990)"),
    HARRIS_BENEDICT("Harris–Benedict (revised)", "The classic equation, revised by Roza and Shizgal (1984)"),
    KATCH_MCARDLE("Katch–McArdle", "Uses lean body mass, so it needs your body-fat percentage");

    private final String label;
    private final String description;

    BmrFormula(String label, String description) {
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
