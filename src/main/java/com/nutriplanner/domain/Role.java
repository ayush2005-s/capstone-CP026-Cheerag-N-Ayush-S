package com.nutriplanner.domain;

public enum Role implements Labeled {
    USER("Member"),
    ADMIN("Administrator");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
