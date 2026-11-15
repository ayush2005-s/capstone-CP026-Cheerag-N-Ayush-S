package com.nutriplanner.domain;

import com.nutriplanner.storage.Doc;

/** An immutable bundle of energy and nutrient amounts. */
public record Nutrients(double kcal, double protein, double carbs, double fat, double fiber, double sugar, double sodiumMg) {

    public static final Nutrients ZERO = new Nutrients(0, 0, 0, 0, 0, 0, 0);

    public Nutrients plus(Nutrients o) {
        return new Nutrients(kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat,
                fiber + o.fiber, sugar + o.sugar, sodiumMg + o.sodiumMg);
    }

    public Nutrients scale(double factor) {
        return new Nutrients(kcal * factor, protein * factor, carbs * factor, fat * factor,
                fiber * factor, sugar * factor, sodiumMg * factor);
    }

    /** Share of energy coming from protein, carbohydrate and fat (each 0..1, summing to 1 when there is energy). */
    public double[] energySplit() {
        double p = protein * 4, c = carbs * 4, f = fat * 9;
        double total = p + c + f;
        if (total <= 0) return new double[]{0, 0, 0};
        return new double[]{p / total, c / total, f / total};
    }

    public Doc toDoc() {
        return new Doc().put("kcal", kcal).put("protein", protein).put("carbs", carbs).put("fat", fat)
                .put("fiber", fiber).put("sugar", sugar).put("sodium", sodiumMg);
    }

    public static Nutrients fromDoc(Doc d) {
        if (d == null) return ZERO;
        return new Nutrients(d.getDouble("kcal", 0), d.getDouble("protein", 0), d.getDouble("carbs", 0),
                d.getDouble("fat", 0), d.getDouble("fiber", 0), d.getDouble("sugar", 0), d.getDouble("sodium", 0));
    }
}
