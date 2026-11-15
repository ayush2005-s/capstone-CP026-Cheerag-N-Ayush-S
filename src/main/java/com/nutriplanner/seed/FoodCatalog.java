package com.nutriplanner.seed;

import com.nutriplanner.domain.Allergen;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodCategory;
import com.nutriplanner.domain.FoodDiet;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.PlanRole;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.nutriplanner.domain.Allergen.DAIRY;
import static com.nutriplanner.domain.Allergen.EGG;
import static com.nutriplanner.domain.Allergen.FISH;
import static com.nutriplanner.domain.Allergen.GLUTEN;
import static com.nutriplanner.domain.Allergen.PEANUT;
import static com.nutriplanner.domain.Allergen.SESAME;
import static com.nutriplanner.domain.Allergen.SHELLFISH;
import static com.nutriplanner.domain.Allergen.SOY;
import static com.nutriplanner.domain.Allergen.TREE_NUT;
import static com.nutriplanner.domain.FoodCategory.BEVERAGES;
import static com.nutriplanner.domain.FoodCategory.BREADS;
import static com.nutriplanner.domain.FoodCategory.EGGS;
import static com.nutriplanner.domain.FoodCategory.FATS_OILS;
import static com.nutriplanner.domain.FoodCategory.FRUITS;
import static com.nutriplanner.domain.FoodCategory.GRAINS;
import static com.nutriplanner.domain.FoodCategory.MEAT;
import static com.nutriplanner.domain.FoodCategory.NUTS_SEEDS;
import static com.nutriplanner.domain.FoodCategory.PREPARED;
import static com.nutriplanner.domain.FoodCategory.PULSES;
import static com.nutriplanner.domain.FoodCategory.SEAFOOD;
import static com.nutriplanner.domain.FoodCategory.SNACKS;
import static com.nutriplanner.domain.FoodCategory.SUPPLEMENTS;
import static com.nutriplanner.domain.FoodCategory.SWEETS;
import static com.nutriplanner.domain.FoodCategory.VEGETABLES;
import static com.nutriplanner.domain.FoodDiet.NON_VEG;
import static com.nutriplanner.domain.FoodDiet.VEGAN;
import static com.nutriplanner.domain.FoodDiet.VEGETARIAN;
import static com.nutriplanner.domain.PlanRole.BASE;
import static com.nutriplanner.domain.PlanRole.FRUIT;
import static com.nutriplanner.domain.PlanRole.NONE;
import static com.nutriplanner.domain.PlanRole.PROTEIN;
import static com.nutriplanner.domain.PlanRole.SNACK;
import static com.nutriplanner.domain.PlanRole.VEGETABLE;

/**
 * The starter food database. Values are per 100 g of the food as eaten, based on USDA FoodData Central and the
 * Indian Food Composition Tables (2017); home-style Indian dishes use typical recipes. Columns:
 * kcal, protein g, carbohydrate g, fat g, fibre g, sugar g, sodium mg, then the serving size in grams.
 * Meal letters: B breakfast, L lunch, D dinner, S snacks.
 */
public final class FoodCatalog {

    private FoodCatalog() {
    }

    private static final List<Food> FOODS = new ArrayList<>();

    private static void add(String name, FoodCategory category, FoodDiet diet, PlanRole role, String meals,
                            double kcal, double protein, double carbs, double fat, double fiber, double sugar, double sodium,
                            double servingGrams, String servingLabel, Allergen... allergens) {
        Food f = new Food();
        f.setName(name);
        f.setCategory(category);
        f.setDiet(diet);
        f.setPlanRole(role);
        EnumSet<MealType> mealTypes = EnumSet.noneOf(MealType.class);
        for (char c : meals.toCharArray()) {
            switch (c) {
                case 'B': mealTypes.add(MealType.BREAKFAST); break;
                case 'L': mealTypes.add(MealType.LUNCH); break;
                case 'D': mealTypes.add(MealType.DINNER); break;
                case 'S': mealTypes.add(MealType.SNACK); break;
                default: throw new IllegalArgumentException("Unknown meal letter " + c + " for " + name);
            }
        }
        f.setMealTypes(mealTypes);
        f.setKcal(kcal);
        f.setProtein(protein);
        f.setCarbs(carbs);
        f.setFat(fat);
        f.setFiber(fiber);
        f.setSugar(sugar);
        f.setSodiumMg(sodium);
        f.setServingGrams(servingGrams);
        f.setServingLabel(servingLabel);
        EnumSet<Allergen> set = EnumSet.noneOf(Allergen.class);
        for (Allergen a : allergens) set.add(a);
        f.setAllergens(set);
        FOODS.add(f);
    }

    static {
        // ---- Grains and cereals ------------------------------------------------------------------------------
        add("White rice, cooked", GRAINS, VEGAN, BASE, "LD", 130, 2.7, 28.2, 0.3, 0.4, 0.1, 1, 150, "1 cup");
        add("Brown rice, cooked", GRAINS, VEGAN, BASE, "LD", 123, 2.7, 25.6, 1.0, 1.6, 0.4, 4, 150, "1 cup");
        add("Basmati rice, cooked", GRAINS, VEGAN, BASE, "LD", 121, 3.5, 25.2, 0.4, 0.4, 0.1, 1, 150, "1 cup");
        add("Quinoa, cooked", GRAINS, VEGAN, BASE, "LD", 120, 4.4, 21.3, 1.9, 2.8, 0.9, 7, 150, "1 cup");
        add("Rolled oats, dry", GRAINS, VEGAN, NONE, "B", 379, 13.2, 67.7, 6.5, 10.1, 1.0, 6, 40, "½ cup", GLUTEN);
        add("Oats porridge with milk", PREPARED, VEGETARIAN, BASE, "B", 114, 4.9, 15.3, 3.8, 1.7, 4.4, 37, 250, "1 bowl", GLUTEN, DAIRY);
        add("Vegetable poha", GRAINS, VEGAN, BASE, "B", 158, 3.0, 26.0, 4.6, 1.6, 1.6, 280, 150, "1 plate");
        add("Rava upma", GRAINS, VEGAN, BASE, "B", 150, 3.5, 22.0, 5.5, 1.5, 1.5, 300, 200, "1 bowl", GLUTEN);
        add("Idli", GRAINS, VEGAN, BASE, "BD", 130, 4.5, 27.0, 0.5, 1.5, 0.5, 260, 80, "2 idlis");
        add("Plain dosa", GRAINS, VEGAN, BASE, "BD", 168, 3.9, 29.0, 3.7, 1.0, 0.5, 300, 80, "1 dosa");
        add("Masala dosa", PREPARED, VEGAN, BASE, "BL", 165, 3.5, 24.0, 6.0, 2.0, 1.5, 350, 180, "1 dosa");
        add("Corn flakes", GRAINS, VEGAN, BASE, "B", 357, 7.5, 84.0, 0.4, 3.3, 9.5, 729, 30, "1 cup", GLUTEN);
        add("Muesli, no added sugar", GRAINS, VEGAN, BASE, "B", 360, 10.0, 66.0, 6.0, 8.0, 12.0, 20, 45, "½ cup", GLUTEN, TREE_NUT);
        add("Vegetable daliya (broken wheat)", GRAINS, VEGAN, BASE, "BD", 110, 3.5, 19.0, 2.5, 3.5, 1.5, 200, 200, "1 bowl", GLUTEN);
        add("Pasta, cooked", GRAINS, VEGAN, BASE, "LD", 158, 5.8, 30.9, 0.9, 1.8, 0.6, 1, 140, "1 cup", GLUTEN);
        add("Whole wheat pasta, cooked", GRAINS, VEGAN, BASE, "LD", 149, 6.0, 29.0, 1.7, 3.9, 0.8, 4, 140, "1 cup", GLUTEN);
        add("Khichdi (moong dal and rice)", PREPARED, VEGAN, BASE, "LD", 120, 4.5, 18.0, 3.2, 2.4, 0.8, 280, 250, "1 bowl");

        // ---- Breads and flatbreads ---------------------------------------------------------------------------
        add("Chapati / roti (whole wheat)", BREADS, VEGAN, BASE, "LD", 264, 9.6, 49.0, 3.7, 5.5, 1.5, 230, 40, "1 roti", GLUTEN);
        add("Plain paratha", BREADS, VEGAN, BASE, "BLD", 326, 6.4, 45.0, 13.0, 4.0, 1.5, 450, 80, "1 paratha", GLUTEN);
        add("Aloo paratha", BREADS, VEGAN, BASE, "BL", 256, 5.5, 36.0, 10.0, 3.5, 1.5, 420, 120, "1 paratha", GLUTEN);
        add("Jowar roti (sorghum)", BREADS, VEGAN, BASE, "LD", 270, 8.5, 55.0, 1.8, 6.0, 1.0, 5, 45, "1 roti");
        add("Bajra roti (pearl millet)", BREADS, VEGAN, BASE, "LD", 288, 8.8, 52.0, 5.0, 7.0, 1.0, 6, 50, "1 roti");
        add("Whole wheat bread", BREADS, VEGAN, BASE, "B", 247, 13.0, 41.0, 3.4, 6.0, 6.0, 450, 64, "2 slices", GLUTEN);
        add("White bread", BREADS, VEGAN, BASE, "B", 265, 9.0, 49.0, 3.2, 2.7, 5.0, 490, 50, "2 slices", GLUTEN);
        add("Butter naan", BREADS, VEGETARIAN, NONE, "LD", 310, 9.0, 50.0, 7.5, 2.2, 3.5, 470, 90, "1 naan", GLUTEN, DAIRY);

        // ---- Pulses, soy and legumes -------------------------------------------------------------------------
        add("Dal tadka (toor dal)", PULSES, VEGAN, PROTEIN, "LD", 128, 6.8, 17.5, 3.5, 4.5, 1.2, 320, 150, "1 katori");
        add("Moong dal", PULSES, VEGAN, PROTEIN, "LD", 105, 7.0, 15.5, 1.8, 3.8, 1.0, 280, 150, "1 katori");
        add("Chana masala (chole)", PULSES, VEGAN, PROTEIN, "LD", 150, 6.5, 19.0, 5.5, 5.5, 3.0, 380, 200, "1 bowl");
        add("Rajma (kidney bean curry)", PULSES, VEGAN, PROTEIN, "LD", 140, 6.5, 18.5, 4.6, 6.0, 2.0, 350, 200, "1 bowl");
        add("Lentils, boiled", PULSES, VEGAN, PROTEIN, "LD", 116, 9.0, 20.1, 0.4, 7.9, 1.8, 2, 150, "¾ cup");
        add("Chickpeas, boiled", PULSES, VEGAN, PROTEIN, "LD", 164, 8.9, 27.4, 2.6, 7.6, 4.8, 7, 150, "¾ cup");
        add("Black beans, boiled", PULSES, VEGAN, PROTEIN, "LD", 132, 8.9, 23.7, 0.5, 8.7, 0.3, 1, 150, "¾ cup");
        add("Tofu, firm", PULSES, VEGAN, PROTEIN, "LD", 144, 17.3, 2.8, 8.7, 2.3, 0.6, 14, 100, "100 g", SOY);
        add("Tofu bhurji (scramble)", PREPARED, VEGAN, PROTEIN, "B", 152, 12.0, 5.0, 9.5, 1.8, 2.0, 300, 150, "1 plate", SOY);
        add("Soya chunk curry", PREPARED, VEGAN, PROTEIN, "LD", 120, 11.0, 8.0, 5.0, 3.0, 2.0, 350, 150, "1 bowl", SOY);
        add("Soya chunks, dry", PULSES, VEGAN, NONE, "LD", 345, 52.0, 33.0, 0.5, 13.0, 7.0, 3, 30, "⅓ cup dry", SOY);
        add("Tempeh", PULSES, VEGAN, PROTEIN, "LD", 192, 20.3, 7.6, 10.8, 4.0, 0.0, 9, 85, "85 g", SOY);
        add("Edamame", PULSES, VEGAN, SNACK, "S", 121, 11.9, 8.9, 5.2, 5.2, 2.2, 6, 100, "⅔ cup", SOY);
        add("Moong sprouts salad", PREPARED, VEGAN, PROTEIN, "BS", 50, 3.5, 8.0, 0.5, 2.5, 3.0, 120, 150, "1 bowl");
        add("Besan chilla", PREPARED, VEGAN, PROTEIN, "B", 196, 9.0, 22.0, 8.0, 4.0, 2.0, 300, 120, "2 chillas");
        add("Moong dal chilla", PREPARED, VEGAN, PROTEIN, "B", 178, 10.0, 22.0, 5.5, 4.0, 1.5, 280, 120, "2 chillas");
        add("Sambar", PREPARED, VEGAN, VEGETABLE, "BLD", 65, 3.0, 8.5, 2.2, 2.2, 2.0, 310, 150, "1 bowl");
        add("Hummus", PULSES, VEGAN, SNACK, "S", 166, 7.9, 14.3, 9.6, 6.0, 0.3, 379, 60, "4 tbsp", SESAME);
        add("Roasted chana", SNACKS, VEGAN, SNACK, "S", 369, 22.0, 58.0, 5.5, 16.0, 8.0, 25, 30, "1 handful");

        // ---- Dairy -------------------------------------------------------------------------------------------
        add("Milk, whole", FoodCategory.DAIRY, VEGETARIAN, PlanRole.DAIRY, "BS", 61, 3.2, 4.8, 3.3, 0, 5.1, 43, 250, "1 glass", DAIRY);
        add("Milk, skimmed", FoodCategory.DAIRY, VEGETARIAN, PlanRole.DAIRY, "BS", 34, 3.4, 5.0, 0.1, 0, 5.0, 42, 250, "1 glass", DAIRY);
        add("Curd (plain dahi)", FoodCategory.DAIRY, VEGETARIAN, PlanRole.DAIRY, "BLD", 61, 3.5, 4.7, 3.3, 0, 4.7, 46, 150, "1 katori", DAIRY);
        add("Cucumber raita", FoodCategory.DAIRY, VEGETARIAN, PlanRole.DAIRY, "LD", 55, 2.8, 4.5, 2.8, 0.4, 4.0, 180, 150, "1 katori", DAIRY);
        add("Buttermilk (chaas)", BEVERAGES, VEGETARIAN, PlanRole.DAIRY, "LS", 40, 3.3, 4.8, 0.9, 0, 4.8, 105, 250, "1 glass", DAIRY);
        add("Greek yogurt, plain non-fat", FoodCategory.DAIRY, VEGETARIAN, PROTEIN, "BS", 59, 10.2, 3.6, 0.4, 0, 3.2, 36, 170, "1 cup", DAIRY);
        add("Cottage cheese, low-fat", FoodCategory.DAIRY, VEGETARIAN, PROTEIN, "BS", 81, 10.5, 4.8, 2.3, 0, 4.0, 308, 113, "½ cup", DAIRY);
        add("Paneer", FoodCategory.DAIRY, VEGETARIAN, NONE, "LD", 265, 18.3, 1.2, 20.8, 0, 1.2, 22, 50, "50 g", DAIRY);
        add("Paneer bhurji", PREPARED, VEGETARIAN, PROTEIN, "BD", 220, 13.0, 5.0, 16.5, 1.0, 2.5, 350, 120, "1 plate", DAIRY);
        add("Palak paneer", PREPARED, VEGETARIAN, PROTEIN, "LD", 160, 7.5, 6.0, 12.0, 2.0, 2.0, 380, 200, "1 bowl", DAIRY);
        add("Paneer tikka", PREPARED, VEGETARIAN, PROTEIN, "LD", 232, 15.0, 6.0, 16.5, 1.0, 3.0, 400, 150, "6 pieces", DAIRY);
        add("Matar paneer", PREPARED, VEGETARIAN, PROTEIN, "LD", 150, 7.0, 9.0, 9.5, 2.5, 3.5, 380, 200, "1 bowl", DAIRY);
        add("Cheddar cheese", FoodCategory.DAIRY, VEGETARIAN, NONE, "S", 403, 24.9, 1.3, 33.1, 0, 0.5, 621, 28, "1 slice", DAIRY);
        add("Sweet lassi", BEVERAGES, VEGETARIAN, NONE, "S", 98, 2.9, 15.0, 3.0, 0, 14.0, 45, 250, "1 glass", DAIRY);
        add("Whey protein powder", SUPPLEMENTS, VEGETARIAN, NONE, "BS", 400, 80.0, 10.0, 5.0, 0, 5.0, 160, 30, "1 scoop", DAIRY);
        add("Butter", FATS_OILS, VEGETARIAN, NONE, "BLD", 717, 0.9, 0.1, 81.1, 0, 0.1, 643, 10, "2 tsp", DAIRY);
        add("Ghee", FATS_OILS, VEGETARIAN, NONE, "BLD", 900, 0, 0, 100, 0, 0, 2, 5, "1 tsp", DAIRY);

        // ---- Eggs --------------------------------------------------------------------------------------------
        add("Boiled egg", EGGS, FoodDiet.EGG, PROTEIN, "BS", 155, 12.6, 1.1, 10.6, 0, 1.1, 124, 50, "1 large egg", EGG);
        add("Egg whites", EGGS, FoodDiet.EGG, PROTEIN, "B", 52, 10.9, 0.7, 0.2, 0, 0.7, 166, 100, "whites of 3 eggs", EGG);
        add("Masala omelette", EGGS, FoodDiet.EGG, PROTEIN, "BD", 156, 10.6, 2.5, 11.5, 0.5, 1.5, 300, 130, "2-egg omelette", EGG);
        add("Egg bhurji", EGGS, FoodDiet.EGG, PROTEIN, "B", 168, 11.0, 3.0, 12.5, 0.7, 1.5, 320, 120, "1 plate", EGG);
        add("Egg curry", PREPARED, FoodDiet.EGG, PROTEIN, "LD", 142, 8.5, 4.5, 10.0, 1.0, 2.5, 380, 200, "1 bowl (2 eggs)", EGG);

        // ---- Meat and poultry --------------------------------------------------------------------------------
        add("Chicken breast, grilled", MEAT, NON_VEG, PROTEIN, "LD", 165, 31.0, 0, 3.6, 0, 0, 74, 120, "1 fillet");
        add("Chicken curry", PREPARED, NON_VEG, PROTEIN, "LD", 145, 13.5, 4.5, 8.2, 0.8, 1.8, 420, 200, "1 bowl");
        add("Tandoori chicken", MEAT, NON_VEG, PROTEIN, "LD", 148, 22.0, 3.0, 5.3, 0.5, 1.5, 480, 150, "2 pieces", DAIRY);
        add("Grilled chicken salad", PREPARED, NON_VEG, PROTEIN, "LD", 105, 12.0, 4.0, 4.5, 1.5, 2.0, 220, 250, "1 bowl");
        add("Mutton curry", PREPARED, NON_VEG, PROTEIN, "LD", 175, 14.0, 4.0, 11.5, 0.8, 1.5, 420, 200, "1 bowl");
        add("Butter chicken", PREPARED, NON_VEG, NONE, "LD", 189, 12.0, 6.0, 13.0, 1.0, 4.0, 500, 200, "1 bowl", DAIRY);
        add("Chicken biryani", PREPARED, NON_VEG, NONE, "LD", 170, 9.0, 19.5, 6.3, 1.0, 1.0, 420, 300, "1 plate", DAIRY);

        // ---- Fish and seafood --------------------------------------------------------------------------------
        add("Salmon, baked", SEAFOOD, NON_VEG, PROTEIN, "LD", 206, 22.1, 0, 12.4, 0, 0, 61, 120, "1 fillet", FISH);
        add("Tuna, canned in water", SEAFOOD, NON_VEG, PROTEIN, "LD", 116, 25.5, 0, 0.8, 0, 0, 338, 85, "1 small can", FISH);
        add("Tilapia, baked", SEAFOOD, NON_VEG, PROTEIN, "LD", 128, 26.2, 0, 2.7, 0, 0, 56, 120, "1 fillet", FISH);
        add("Fish curry", PREPARED, NON_VEG, PROTEIN, "LD", 120, 13.0, 3.5, 6.0, 0.8, 1.5, 400, 200, "1 bowl", FISH);
        add("Prawn curry", PREPARED, NON_VEG, PROTEIN, "LD", 115, 13.0, 4.0, 5.2, 0.8, 1.5, 450, 200, "1 bowl", SHELLFISH);
        add("Shrimp, cooked", SEAFOOD, NON_VEG, PROTEIN, "LD", 99, 24.0, 0.2, 0.3, 0, 0, 111, 100, "100 g", SHELLFISH);

        // ---- Vegetables --------------------------------------------------------------------------------------
        add("Mixed vegetable sabzi", VEGETABLES, VEGAN, VEGETABLE, "LD", 90, 2.4, 9.5, 4.8, 3.2, 3.5, 300, 150, "1 katori");
        add("Palak (spinach), cooked", VEGETABLES, VEGAN, VEGETABLE, "LD", 23, 3.0, 3.8, 0.3, 2.4, 0.4, 70, 150, "1 cup");
        add("Bhindi (okra) sabzi", VEGETABLES, VEGAN, VEGETABLE, "LD", 95, 2.2, 8.5, 6.0, 3.5, 2.5, 280, 150, "1 katori");
        add("Aloo gobi", VEGETABLES, VEGAN, VEGETABLE, "LD", 105, 2.5, 12.5, 5.2, 3.0, 2.5, 300, 150, "1 katori");
        add("Baingan bharta", VEGETABLES, VEGAN, VEGETABLE, "LD", 85, 1.8, 8.5, 5.0, 3.5, 4.0, 290, 150, "1 katori");
        add("Cabbage poriyal", VEGETABLES, VEGAN, VEGETABLE, "LD", 70, 1.8, 7.5, 3.8, 2.8, 3.5, 250, 150, "1 katori");
        add("Broccoli, steamed", VEGETABLES, VEGAN, VEGETABLE, "LD", 35, 2.4, 7.2, 0.4, 3.3, 1.4, 41, 150, "1 cup");
        add("Garden salad, no dressing", VEGETABLES, VEGAN, VEGETABLE, "LD", 17, 1.2, 3.3, 0.2, 1.8, 1.5, 25, 150, "1 bowl");
        add("Kachumber salad", VEGETABLES, VEGAN, VEGETABLE, "LD", 25, 1.0, 5.0, 0.2, 1.5, 3.0, 150, 150, "1 bowl");
        add("Mushroom stir-fry", VEGETABLES, VEGAN, VEGETABLE, "LD", 60, 3.0, 4.5, 3.5, 1.8, 2.0, 220, 150, "1 katori");
        add("Green beans stir-fry", VEGETABLES, VEGAN, VEGETABLE, "LD", 60, 1.9, 7.5, 2.8, 3.2, 3.0, 200, 150, "1 katori");
        add("Mixed vegetable soup", VEGETABLES, VEGAN, VEGETABLE, "LD", 35, 1.5, 6.0, 0.6, 1.5, 2.5, 300, 250, "1 bowl");
        add("Green peas, boiled", VEGETABLES, VEGAN, VEGETABLE, "LD", 84, 5.4, 15.6, 0.2, 5.5, 5.9, 3, 100, "⅔ cup");
        add("Carrot, raw", VEGETABLES, VEGAN, VEGETABLE, "LD", 41, 0.9, 9.6, 0.2, 2.8, 4.7, 69, 60, "1 medium");
        add("Sweet potato, baked", VEGETABLES, VEGAN, BASE, "LD", 90, 2.0, 20.7, 0.2, 3.3, 6.5, 36, 130, "1 medium");
        add("Potato, boiled", VEGETABLES, VEGAN, BASE, "LD", 87, 1.9, 20.1, 0.1, 1.8, 0.9, 4, 150, "1 medium");
        add("Sweet corn, boiled", VEGETABLES, VEGAN, SNACK, "S", 96, 3.4, 21.0, 1.5, 2.4, 4.5, 1, 100, "1 small cup");
        add("Cucumber", VEGETABLES, VEGAN, NONE, "LDS", 15, 0.7, 3.6, 0.1, 0.5, 1.7, 2, 100, "½ cucumber");
        add("Tomato", VEGETABLES, VEGAN, NONE, "LD", 18, 0.9, 3.9, 0.2, 1.2, 2.6, 5, 120, "1 medium");
        add("Onion", VEGETABLES, VEGAN, NONE, "LD", 40, 1.1, 9.3, 0.1, 1.7, 4.2, 4, 110, "1 medium");

        // ---- Fruits ------------------------------------------------------------------------------------------
        add("Banana", FRUITS, VEGAN, FRUIT, "BS", 89, 1.1, 22.8, 0.3, 2.6, 12.2, 1, 118, "1 medium");
        add("Apple", FRUITS, VEGAN, FRUIT, "BS", 52, 0.3, 13.8, 0.2, 2.4, 10.4, 1, 180, "1 medium");
        add("Orange", FRUITS, VEGAN, FRUIT, "BS", 47, 0.9, 11.8, 0.1, 2.4, 9.4, 0, 130, "1 medium");
        add("Papaya", FRUITS, VEGAN, FRUIT, "BS", 43, 0.5, 10.8, 0.3, 1.7, 7.8, 8, 145, "1 cup");
        add("Mango", FRUITS, VEGAN, FRUIT, "BS", 60, 0.8, 15.0, 0.4, 1.6, 13.7, 1, 165, "1 cup");
        add("Grapes", FRUITS, VEGAN, FRUIT, "BS", 69, 0.7, 18.1, 0.2, 0.9, 15.5, 2, 150, "1 cup");
        add("Guava", FRUITS, VEGAN, FRUIT, "BS", 68, 2.6, 14.3, 1.0, 5.4, 8.9, 2, 100, "1 large");
        add("Pomegranate", FRUITS, VEGAN, FRUIT, "BS", 83, 1.7, 18.7, 1.2, 4.0, 13.7, 3, 85, "½ cup arils");
        add("Watermelon", FRUITS, VEGAN, FRUIT, "BS", 30, 0.6, 7.6, 0.2, 0.4, 6.2, 1, 150, "1 cup diced");
        add("Pineapple", FRUITS, VEGAN, FRUIT, "BS", 50, 0.5, 13.1, 0.1, 1.4, 9.9, 1, 165, "1 cup");
        add("Strawberries", FRUITS, VEGAN, FRUIT, "BS", 32, 0.7, 7.7, 0.3, 2.0, 4.9, 1, 150, "1 cup");
        add("Blueberries", FRUITS, VEGAN, FRUIT, "BS", 57, 0.7, 14.5, 0.3, 2.4, 10.0, 1, 150, "1 cup");
        add("Pear", FRUITS, VEGAN, FRUIT, "BS", 57, 0.4, 15.2, 0.1, 3.1, 9.8, 1, 180, "1 medium");
        add("Avocado", FRUITS, VEGAN, SNACK, "BS", 160, 2.0, 8.5, 14.7, 6.7, 0.7, 7, 75, "½ avocado");
        add("Dates, dried", FRUITS, VEGAN, SNACK, "S", 282, 2.5, 75.0, 0.4, 8.0, 63.0, 2, 24, "3 dates");
        add("Raisins", FRUITS, VEGAN, NONE, "BS", 299, 3.1, 79.2, 0.5, 3.7, 59.2, 11, 28, "1 small box");
        add("Fruit chaat", PREPARED, VEGAN, SNACK, "S", 62, 0.8, 14.0, 0.3, 2.0, 11.0, 150, 150, "1 bowl");

        // ---- Nuts and seeds ----------------------------------------------------------------------------------
        add("Almonds", NUTS_SEEDS, VEGAN, SNACK, "BS", 579, 21.2, 21.6, 49.9, 12.5, 4.4, 1, 28, "23 almonds", TREE_NUT);
        add("Walnuts", NUTS_SEEDS, VEGAN, SNACK, "BS", 654, 15.2, 13.7, 65.2, 6.7, 2.6, 2, 28, "1 handful", TREE_NUT);
        add("Cashews", NUTS_SEEDS, VEGAN, SNACK, "S", 553, 18.2, 30.2, 43.9, 3.3, 5.9, 12, 28, "18 cashews", TREE_NUT);
        add("Peanuts, dry roasted", NUTS_SEEDS, VEGAN, SNACK, "S", 585, 24.4, 21.3, 49.7, 8.0, 4.9, 6, 28, "1 handful", PEANUT);
        add("Peanut butter", NUTS_SEEDS, VEGAN, SNACK, "BS", 588, 25.0, 20.0, 50.0, 6.0, 9.2, 459, 32, "2 tbsp", PEANUT);
        add("Pumpkin seeds", NUTS_SEEDS, VEGAN, SNACK, "S", 559, 30.2, 10.7, 49.1, 6.0, 1.4, 7, 28, "1 handful");
        add("Sunflower seeds", NUTS_SEEDS, VEGAN, SNACK, "S", 584, 20.8, 20.0, 51.5, 8.6, 2.6, 9, 28, "1 handful");
        add("Chia seeds", NUTS_SEEDS, VEGAN, NONE, "BS", 486, 16.5, 42.1, 30.7, 34.4, 0, 16, 15, "1 tbsp");
        add("Flaxseeds", NUTS_SEEDS, VEGAN, NONE, "BS", 534, 18.3, 28.9, 42.2, 27.3, 1.6, 30, 10, "1 tbsp");

        // ---- Fats and oils -----------------------------------------------------------------------------------
        add("Olive oil", FATS_OILS, VEGAN, NONE, "LD", 884, 0, 0, 100, 0, 0, 2, 10, "2 tsp");
        add("Mustard oil", FATS_OILS, VEGAN, NONE, "LD", 884, 0, 0, 100, 0, 0, 0, 10, "2 tsp");
        add("Coconut oil", FATS_OILS, VEGAN, NONE, "LD", 892, 0, 0, 99, 0, 0, 0, 10, "2 tsp");

        // ---- Beverages ---------------------------------------------------------------------------------------
        add("Masala chai with milk and sugar", BEVERAGES, VEGETARIAN, NONE, "BS", 50, 1.6, 7.5, 1.5, 0, 7.0, 20, 150, "1 cup", DAIRY);
        add("Black coffee", BEVERAGES, VEGAN, NONE, "BS", 1, 0.1, 0, 0, 0, 0, 2, 240, "1 cup");
        add("Green tea", BEVERAGES, VEGAN, NONE, "BS", 1, 0.2, 0, 0, 0, 0, 1, 240, "1 cup");
        add("Coconut water", BEVERAGES, VEGAN, NONE, "S", 19, 0.7, 3.7, 0.2, 1.1, 2.6, 105, 240, "1 glass");
        add("Orange juice", BEVERAGES, VEGAN, NONE, "BS", 45, 0.7, 10.4, 0.2, 0.2, 8.4, 1, 250, "1 glass");
        add("Soy milk, unsweetened", BEVERAGES, VEGAN, PlanRole.DAIRY, "BS", 33, 2.9, 1.7, 1.6, 0.4, 1.0, 51, 250, "1 glass", SOY);
        add("Almond milk, unsweetened", BEVERAGES, VEGAN, PlanRole.DAIRY, "BS", 15, 0.6, 0.3, 1.2, 0.2, 0, 72, 250, "1 glass", TREE_NUT);
        add("Cola", BEVERAGES, VEGAN, NONE, "S", 42, 0, 10.6, 0, 0, 10.6, 4, 330, "1 can");

        // ---- Snacks ------------------------------------------------------------------------------------------
        add("Makhana (fox nuts), roasted", SNACKS, VEGAN, SNACK, "S", 347, 9.7, 76.9, 0.1, 7.6, 0, 1, 25, "1 bowl");
        add("Air-popped popcorn", SNACKS, VEGAN, SNACK, "S", 387, 12.9, 77.8, 4.5, 14.5, 0.9, 8, 24, "3 cups");
        add("Dhokla", SNACKS, VEGAN, SNACK, "BS", 160, 6.5, 24.0, 4.3, 2.0, 3.0, 450, 100, "4 pieces");
        add("Samosa", SNACKS, VEGAN, NONE, "S", 262, 4.5, 32.0, 13.0, 2.5, 2.0, 420, 75, "1 piece", GLUTEN);
        add("Potato chips", SNACKS, VEGAN, NONE, "S", 536, 7.0, 52.9, 34.6, 3.1, 0.3, 525, 28, "1 small pack");
        add("Digestive biscuits", SNACKS, VEGETARIAN, NONE, "S", 470, 7.0, 62.4, 21.0, 3.6, 16.6, 530, 30, "2 biscuits", GLUTEN, DAIRY);
        add("Granola bar", SNACKS, VEGAN, NONE, "S", 471, 10.0, 64.0, 20.0, 5.0, 29.0, 290, 25, "1 bar", GLUTEN, TREE_NUT);
        add("Vegetable sandwich", PREPARED, VEGETARIAN, NONE, "BS", 200, 6.0, 30.0, 6.5, 3.0, 4.0, 420, 150, "1 sandwich", GLUTEN, DAIRY);
        add("Pav bhaji", PREPARED, VEGETARIAN, NONE, "LD", 179, 4.0, 25.0, 7.0, 3.0, 4.0, 450, 250, "1 plate", GLUTEN, DAIRY);
        add("Vegetable biryani", PREPARED, VEGETARIAN, NONE, "LD", 150, 3.2, 22.0, 5.5, 1.8, 1.5, 380, 300, "1 plate", DAIRY);

        // ---- Sweets and desserts -----------------------------------------------------------------------------
        add("Gulab jamun", SWEETS, VEGETARIAN, NONE, "S", 323, 4.0, 50.0, 12.0, 0.5, 40.0, 60, 40, "1 piece", DAIRY, GLUTEN);
        add("Rice kheer", SWEETS, VEGETARIAN, NONE, "S", 124, 3.5, 19.0, 3.8, 0.2, 14.0, 50, 150, "1 bowl", DAIRY);
        add("Dark chocolate (70–85%)", SWEETS, VEGETARIAN, NONE, "S", 598, 7.8, 45.9, 42.6, 10.9, 24.0, 20, 20, "2 squares", DAIRY, SOY);
        add("Vanilla ice cream", SWEETS, VEGETARIAN, NONE, "S", 207, 3.5, 23.6, 11.0, 0.7, 21.2, 80, 66, "1 scoop", DAIRY);
        add("Honey", SWEETS, VEGETARIAN, NONE, "BS", 304, 0.3, 82.4, 0, 0.2, 82.1, 4, 21, "1 tbsp");
        add("Sugar", SWEETS, VEGAN, NONE, "BS", 387, 0, 100, 0, 0, 100, 1, 4, "1 tsp");
        add("Jaggery", SWEETS, VEGAN, NONE, "BS", 383, 0.4, 98.0, 0.1, 0, 85.0, 30, 10, "1 piece");
    }

    /** Fresh copies of every starter food (unsaved, without ids). */
    public static List<Food> foods() {
        List<Food> copies = new ArrayList<>();
        for (Food f : FOODS) copies.add(Food.fromDoc(0, f.toDoc()));
        return copies;
    }
}
