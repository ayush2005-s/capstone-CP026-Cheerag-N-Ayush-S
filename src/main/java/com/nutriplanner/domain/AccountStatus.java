package com.nutriplanner.domain;

public enum AccountStatus implements Labeled {
    ACTIVE("Active"),
    DISABLED("Disabled");

    private final String label;

    AccountStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
