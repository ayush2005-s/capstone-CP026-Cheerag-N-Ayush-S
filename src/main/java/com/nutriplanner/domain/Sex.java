package com.nutriplanner.domain;

/** Biological sex, used only because metabolic-rate formulas differ between male and female bodies. */
public enum Sex implements Labeled {
    MALE("Male"),
    FEMALE("Female");

    private final String label;

    Sex(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
