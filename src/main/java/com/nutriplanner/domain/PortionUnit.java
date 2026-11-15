package com.nutriplanner.domain;

public enum PortionUnit implements Labeled {
    SERVING("servings"),
    GRAM("grams");

    private final String label;

    PortionUnit(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
