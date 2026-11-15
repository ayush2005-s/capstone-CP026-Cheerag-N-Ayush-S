package com.nutriplanner.domain;

public enum FoodCategory implements Labeled {
    GRAINS("Grains and cereals"),
    BREADS("Breads and flatbreads"),
    PULSES("Pulses, soy and legumes"),
    DAIRY("Dairy"),
    EGGS("Eggs"),
    MEAT("Meat and poultry"),
    SEAFOOD("Fish and seafood"),
    VEGETABLES("Vegetables"),
    FRUITS("Fruits"),
    NUTS_SEEDS("Nuts and seeds"),
    FATS_OILS("Fats and oils"),
    BEVERAGES("Beverages"),
    SNACKS("Snacks"),
    SWEETS("Sweets and desserts"),
    PREPARED("Prepared dishes"),
    SUPPLEMENTS("Supplements");

    private final String label;

    FoodCategory(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
