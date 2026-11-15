package com.nutriplanner.domain;

public enum ThemePreference implements Labeled {
    SYSTEM("Match my device"),
    LIGHT("Light"),
    DARK("Dark");

    private final String label;

    ThemePreference(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
