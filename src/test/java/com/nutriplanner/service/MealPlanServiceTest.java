package com.nutriplanner.service;

import com.nutriplanner.domain.Allergen;
import com.nutriplanner.domain.DietType;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.MealPlanDay;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.User;
import com.nutriplanner.testkit.Test;
import com.nutriplanner.testkit.TestServices;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.nutriplanner.testkit.Assert.assertEquals;
import static com.nutriplanner.testkit.Assert.assertFalse;
import static com.nutriplanner.testkit.Assert.assertThrows;
import static com.nutriplanner.testkit.Assert.assertTrue;

public class MealPlanServiceTest extends TestServices {

    private void checkWeek(DietType diet, Set<Allergen> allergens) {
        User u = member("u" + diet.ordinal() + allergens.size(), diet, allergens);
        Profile p = services.profiles.profile(u.getId());
        NutritionTargets t = services.profiles.targets(u.getId()).orElseThrow();
        List<MealPlanDay> week = services.plans.generateWeek(u.getId(), TODAY);
        assertEquals(7, week.size());
        for (MealPlanService.PlannedDay day : services.plans.week(u.getId(), TODAY)) {
            assertFalse(day.isEmpty(), diet + ": " + day.date() + " has food");
            for (MealType m : MealType.values()) {
                assertFalse(day.meals().get(m).isEmpty(), diet + ": " + m + " on " + day.date() + " has food");
                for (MealPlanService.PlannedItem item : day.meals().get(m)) {
                    Food f = item.food();
                    assertTrue(FoodService.assess(f, p).suitable(), diet + " plan contains unsuitable " + f.getName());
                    assertTrue(f.suitsMeal(m), f.getName() + " planned for " + m);
                }
            }
            double kcal = day.total().kcal();
            assertTrue(Math.abs(kcal - t.calories()) <= t.calories() * 0.12,
                    diet + " " + day.date() + " planned " + Math.round(kcal) + " kcal vs target " + t.calories());
            assertTrue(day.total().protein() >= t.proteinG() * 0.70,
                    diet + " " + day.date() + " protein " + Math.round(day.total().protein()) + " g vs " + t.proteinG());
        }
    }

    @Test
    void plansRespectEveryDietAndHitTargets() {
        checkWeek(DietType.NON_VEGETARIAN, Set.of());
        checkWeek(DietType.EGGETARIAN, Set.of());
        checkWeek(DietType.VEGETARIAN, Set.of());
        checkWeek(DietType.VEGAN, Set.of());
    }

    @Test
    void plansRespectAllergies() {
        checkWeek(DietType.VEGAN, EnumSet.of(Allergen.SOY, Allergen.GLUTEN));
        checkWeek(DietType.VEGETARIAN, EnumSet.of(Allergen.DAIRY, Allergen.PEANUT, Allergen.TREE_NUT));
        checkWeek(DietType.NON_VEGETARIAN, EnumSet.of(Allergen.FISH, Allergen.SHELLFISH, Allergen.EGG));
    }

    @Test
    void editsAndLogsPlannedMeals() {
        User u = member("planner", DietType.EGGETARIAN, Set.of());
        services.plans.generateDay(u.getId(), TODAY);
        MealPlanService.PlannedDay before = services.plans.day(u.getId(), TODAY);
        List<MealPlanService.PlannedItem> lunch = before.meals().get(MealType.LUNCH);
        MealPlanService.PlannedItem first = lunch.get(0);

        services.plans.swapItem(u.getId(), TODAY, first.index());
        MealPlanService.PlannedItem swapped = services.plans.day(u.getId(), TODAY).meals().get(MealType.LUNCH).get(0);
        assertTrue(swapped.food().getId() != first.food().getId(), "swap picks a different food");
        assertEquals(first.food().getPlanRole(), swapped.food().getPlanRole(), "swap keeps the role on the plate");

        List<MealPlanService.PlannedItem> breakfast = services.plans.day(u.getId(), TODAY).meals().get(MealType.BREAKFAST);
        services.plans.regenerateMeal(u.getId(), TODAY, MealType.DINNER);
        List<MealPlanService.PlannedItem> breakfastAfter = services.plans.day(u.getId(), TODAY).meals().get(MealType.BREAKFAST);
        assertEquals(breakfast.size(), breakfastAfter.size(), "other meals are kept");

        services.plans.updateServings(u.getId(), TODAY, 0, 2.5);
        assertEquals(2.5, repos.mealPlans.findByUserAndDate(u.getId(), TODAY).orElseThrow().getItems().get(0).servings());

        int logged = services.plans.logMeal(u.getId(), TODAY, MealType.BREAKFAST);
        assertEquals(breakfastAfter.size(), logged, "every breakfast item is logged");
        assertEquals(logged, services.diary.day(u.getId(), TODAY).meals().get(MealType.BREAKFAST).size());

        services.plans.generateDay(u.getId(), TODAY.plusDays(2));
        assertThrows(ValidationException.class, () -> services.plans.logMeal(u.getId(), TODAY.plusDays(2), MealType.LUNCH));
    }

    @Test
    void requiresCompletedProfile() {
        User u = services.auth.createAccount("New Person", "newbie", "newbie@example.com", "Secret#123",
                com.nutriplanner.domain.Role.USER, false);
        assertThrows(ValidationException.class, () -> services.plans.generateDay(u.getId(), TODAY));
    }
}
