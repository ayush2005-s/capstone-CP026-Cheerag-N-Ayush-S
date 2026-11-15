package com.nutriplanner.service;

import com.nutriplanner.domain.AccountStatus;
import com.nutriplanner.domain.DietType;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.PortionUnit;
import com.nutriplanner.domain.Role;
import com.nutriplanner.domain.User;
import com.nutriplanner.security.AccessDeniedException;
import com.nutriplanner.testkit.Test;
import com.nutriplanner.testkit.TestServices;

import java.util.Set;

import static com.nutriplanner.testkit.Assert.assertEquals;
import static com.nutriplanner.testkit.Assert.assertThrows;
import static com.nutriplanner.testkit.Assert.assertTrue;

public class AdminServiceTest extends TestServices {

    @Test
    void membersCannotUseAdminFunctions() {
        User member = member("ravi", DietType.VEGETARIAN, Set.of());
        assertThrows(AccessDeniedException.class, () -> services.admin.overview(member));
        assertThrows(AccessDeniedException.class, () -> services.admin.users(member, "", null, null, 1, 20));
        assertThrows(AccessDeniedException.class, () -> services.admin.setRole(member, member.getId(), Role.ADMIN, "ip"));
        assertThrows(AccessDeniedException.class, () -> services.admin.deleteFood(member, 1, "ip"));
    }

    @Test
    void protectsTheLastAdministrator() {
        User admin = admin();
        assertThrows(ValidationException.class, () -> services.admin.setRole(admin, admin.getId(), Role.USER, "ip"));
        assertThrows(ValidationException.class, () -> services.admin.setStatus(admin, admin.getId(), AccountStatus.DISABLED, "ip"));
        assertThrows(ValidationException.class, () -> services.admin.deleteUser(admin, admin.getId(), "admin", "ip"));
    }

    @Test
    void managesMembers() {
        User admin = admin();
        User member = member("ravi", DietType.VEGETARIAN, Set.of());
        services.admin.setRole(admin, member.getId(), Role.ADMIN, "ip");
        assertTrue(repos.users.findById(member.getId()).orElseThrow().isAdmin(), "promoted");
        services.admin.setRole(admin, member.getId(), Role.USER, "ip");

        String temporary = services.admin.resetPassword(admin, member.getId(), "ip");
        AuthService.LoginResult login = services.auth.login("ravi", temporary, "ip");
        assertTrue(login.success(), "temporary password works");
        assertTrue(login.user().isMustChangePassword(), "and must be changed");

        assertEquals(1, services.admin.users(admin, "ravi", null, null, 1, 20).total(), "search by username");
        assertEquals(1, services.admin.users(admin, "", Role.ADMIN, null, 1, 20).total(), "filter by role");
    }

    @Test
    void deletingAUserRemovesAllTheirData() {
        User admin = admin();
        User member = member("ravi", DietType.VEGETARIAN, Set.of());
        Food apple = repos.foods.findAll().stream().filter(f -> f.getName().equals("Apple")).findFirst().orElseThrow();
        services.diary.add(member.getId(), TODAY, MealType.SNACK, apple.getId(), 1.0, PortionUnit.SERVING);
        services.plans.generateDay(member.getId(), TODAY);
        services.water.change(member.getId(), TODAY, 3);
        assertThrows(ValidationException.class, () -> services.admin.deleteUser(admin, member.getId(), "wrong", "ip"));
        services.admin.deleteUser(admin, member.getId(), "ravi", "ip");
        assertTrue(repos.users.findById(member.getId()).isEmpty(), "user removed");
        assertTrue(repos.foodLog.findByUser(member.getId()).isEmpty(), "diary removed");
        assertTrue(repos.weights.findByUser(member.getId()).isEmpty(), "weights removed");
        assertTrue(repos.mealPlans.findByUser(member.getId()).isEmpty(), "plans removed");
        assertTrue(repos.profiles.findByUserId(member.getId()).isEmpty(), "profile removed");
    }

    @Test
    void managesGlobalFoods() {
        User admin = admin();
        Food apple = repos.foods.findAll().stream().filter(f -> f.getName().equals("Apple")).findFirst().orElseThrow();
        FoodService.FoodInput input = FoodService.toInput(apple);
        assertThrows(ValidationException.class, () -> services.admin.saveFood(admin, null, input, "ip"));
        FoodService.FoodInput renamed = new FoodService.FoodInput("Green apple", input.category(), input.diet(), input.allergens(),
                input.mealTypes(), input.planRole(), 58.0, 0.4, 13.6, 0.2, 2.8, 9.6, 1.0, 180.0, "1 medium");
        Food created = services.admin.saveFood(admin, null, renamed, "ip");
        assertTrue(created.isGlobal(), "admin foods are global");
        User member = member("ravi", DietType.VEGETARIAN, Set.of());
        services.admin.setFoodActive(admin, created.getId(), false, "ip");
        assertThrows(NotFoundException.class, () -> services.foods.visibleFood(member.getId(), created.getId()));
        services.admin.setFoodActive(admin, created.getId(), true, "ip");
        assertEquals(created.getId(), services.foods.visibleFood(member.getId(), created.getId()).getId());
        services.admin.deleteFood(admin, created.getId(), "ip");
        assertTrue(repos.foods.findById(created.getId()).isEmpty(), "deleted");
        assertTrue(services.admin.audit(admin, null, "Green apple", 1, 50).total() >= 3, "food changes are audited");
    }
}
