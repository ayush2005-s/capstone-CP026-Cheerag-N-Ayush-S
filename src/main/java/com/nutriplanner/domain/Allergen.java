package com.nutriplanner.domain;

/** Common food allergens and intolerances. */
public enum Allergen implements Labeled {
    GLUTEN("Gluten"),
    DAIRY("Milk and dairy"),
    EGG("Egg"),
    PEANUT("Peanuts"),
    TREE_NUT("Tree nuts"),
    SOY("Soy"),
    FISH("Fish"),
    SHELLFISH("Shellfish"),
    SESAME("Sesame");

    private final String label;

    Allergen(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
