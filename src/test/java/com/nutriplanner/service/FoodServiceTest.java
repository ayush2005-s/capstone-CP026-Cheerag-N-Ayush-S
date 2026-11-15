package com.nutriplanner.service;

import com.nutriplanner.domain.Allergen;
import com.nutriplanner.domain.DietType;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodCategory;
import com.nutriplanner.domain.FoodDiet;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.PlanRole;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.User;
import com.nutriplanner.seed.FoodCatalog;
import com.nutriplanner.testkit.Test;
import com.nutriplanner.testkit.TestServices;

import java.util.EnumSet;
import java.util.Set;

import static com.nutriplanner.testkit.Assert.assertEquals;
import static com.nutriplanner.testkit.Assert.assertFalse;
import static com.nutriplanner.testkit.Assert.assertNotEmpty;
import static com.nutriplanner.testkit.Assert.assertThrows;
import static com.nutriplanner.testkit.Assert.assertTrue;

public class FoodServiceTest extends TestServices {

    @Test
    void everyCatalogFoodPassesValidation() {
        User u = member("checker", DietType.NON_VEGETARIAN, Set.of());
        int count = 0;
        for (Food f : FoodCatalog.foods()) {
            try {
                services.foods.createCustom(u.getId(), FoodService.toInput(f));
                count++;
            } catch (ValidationException e) {
                throw new AssertionError(f.getName() + " is invalid: " + e.fieldErrors());
            }
        }
        assertTrue(count >= 150, "catalog has at least 150 foods, has " + count);
    }

    @Test
    void searchFiltersByDietAllergyAndText() {
        User vegan = member("vegan", DietType.VEGAN, EnumSet.of(Allergen.PEANUT));
        Profile p = services.profiles.profile(vegan.getId());
        FoodService.Query suitable = new FoodService.Query("", null, null, null, true, false, FoodService.Sort.NAME, 1, 500);
        for (Food f : services.foods.search(vegan.getId(), p, suitable).items()) {
            assertEquals(FoodDiet.VEGAN, f.getDiet(), f.getName() + " should be vegan");
            assertFalse(f.getAllergens().contains(Allergen.PEANUT), f.getName() + " contains peanuts");
        }
        FoodService.Query paneer = new FoodService.Query("paneer", null, null, null, false, false, FoodService.Sort.NAME, 1, 50);
        Page<Food> found = services.foods.search(vegan.getId(), p, paneer);
        assertTrue(found.total() >= 4, "text search finds paneer dishes");
        assertTrue(found.items().get(0).getName().toLowerCase().startsWith("paneer"), "prefix matches come first");

        FoodService.Query fruit = new FoodService.Query("", FoodCategory.FRUITS, null, MealType.SNACK, false, false, FoodService.Sort.KCAL_ASC, 1, 50);
        Page<Food> fruits = services.foods.search(vegan.getId(), p, fruit);
        assertNotEmpty(fruits.items(), "fruit for snacks");
        for (int i = 1; i < fruits.items().size(); i++) {
            assertTrue(fruits.items().get(i - 1).perServing().kcal() <= fruits.items().get(i).perServing().kcal(), "sorted by calories");
        }
    }

    @Test
    void suitabilityExplainsWhy() {
        User u = member("veg", DietType.VEGETARIAN, EnumSet.of(Allergen.DAIRY));
        Profile p = services.profiles.profile(u.getId());
        Food chicken = byName("Chicken curry");
        Food paneer = byName("Palak paneer");
        Food dal = byName("Dal tadka (toor dal)");
        assertFalse(FoodService.assess(chicken, p).suitable(), "chicken is not vegetarian");
        assertTrue(FoodService.assess(paneer, p).issues().contains("Contains milk and dairy"), "dairy allergy reason");
        assertTrue(FoodService.assess(dal, p).suitable(), "dal suits a vegetarian with a dairy allergy");
    }

    @Test
    void customFoodsArePrivateToTheirOwner() {
        User a = member("alpha", DietType.VEGETARIAN, Set.of());
        User b = member("bravo", DietType.VEGETARIAN, Set.of());
        Food mine = services.foods.createCustom(a.getId(), new FoodService.FoodInput("Grandma's thepla", FoodCategory.BREADS,
                FoodDiet.VEGETARIAN, EnumSet.of(Allergen.GLUTEN), EnumSet.of(MealType.BREAKFAST), PlanRole.NONE,
                300.0, 8.0, 42.0, 11.0, 5.0, 2.0, 380.0, 50.0, "1 thepla"));
        assertEquals(mine.getId(), services.foods.visibleFood(a.getId(), mine.getId()).getId());
        assertThrows(NotFoundException.class, () -> services.foods.visibleFood(b.getId(), mine.getId()));
        assertThrows(NotFoundException.class, () -> services.foods.deleteCustom(b.getId(), mine.getId()));
        services.foods.deleteCustom(a.getId(), mine.getId());
        assertThrows(NotFoundException.class, () -> services.foods.visibleFood(a.getId(), mine.getId()));
    }

    @Test
    void rejectsImpossibleNutrition() {
        User u = member("cook", DietType.VEGETARIAN, Set.of());
        ValidationException e = assertThrows(ValidationException.class, () -> services.foods.createCustom(u.getId(),
                new FoodService.FoodInput("Mystery bar", FoodCategory.SNACKS, FoodDiet.VEGAN, Set.of(), Set.of(), PlanRole.NONE,
                        50.0, 30.0, 40.0, 20.0, 50.0, 60.0, 10.0, 40.0, "1 bar")));
        assertTrue(e.fieldErrors().containsKey("kcal"), "calories must match macros");
        assertTrue(e.fieldErrors().containsKey("fiber"), "fibre can't exceed carbs");
        assertTrue(e.fieldErrors().containsKey("sugar"), "sugar can't exceed carbs");
    }

    Food byName(String name) {
        return repos.foods.findAll().stream().filter(f -> f.getName().equals(name)).findFirst().orElseThrow();
    }
}
