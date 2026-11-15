package com.nutriplanner.storage;

import com.nutriplanner.domain.FoodLogEntry;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.Nutrients;
import com.nutriplanner.domain.User;
import com.nutriplanner.repository.Repositories;
import com.nutriplanner.testkit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.nutriplanner.testkit.Assert.assertEquals;
import static com.nutriplanner.testkit.Assert.assertThrows;
import static com.nutriplanner.testkit.Assert.assertTrue;

public class DatabaseTest {

    private static User user(String name) {
        User u = new User();
        u.setUsername(name);
        u.setEmail(name + "@example.com");
        u.setFullName("Test " + name);
        u.setPasswordHash("x");
        u.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return u;
    }

    @Test
    void documentsRoundTrip() {
        Doc doc = new Doc().put("s", "héllo").put("n", 42).put("d", 1.5).put("b", true)
                .put("date", LocalDate.of(2026, 3, 1)).put("at", Instant.parse("2026-03-01T10:15:30.123Z"))
                .put("list", List.of("a", "b")).put("nested", new Doc().put("x", 1));
        Doc back = DocCodec.decode(DocCodec.encode(doc));
        assertEquals(doc, back);
        assertEquals(42L, back.getLong("n", 0));
        assertEquals(LocalDate.of(2026, 3, 1), back.getDate("date"));
        assertEquals(1L, back.getDoc("nested").getLong("x", 0));
    }

    @Test
    void uniqueIndexesRejectDuplicates() {
        try (Database db = Database.inMemory()) {
            Repositories repos = new Repositories(db);
            repos.users.insert(user("asha"));
            assertTrue(repos.users.findByUsername("ASHA").isPresent(), "lookups ignore case");
            assertThrows(DuplicateKeyException.class, () -> repos.users.insert(user("Asha")));
            assertEquals(1L, repos.users.count());
        }
    }

    @Test
    void failedTransactionsRollBack() {
        try (Database db = Database.inMemory()) {
            Repositories repos = new Repositories(db);
            assertThrows(IllegalStateException.class, () -> db.write(() -> {
                repos.users.insert(user("one"));
                repos.users.insert(user("two"));
                throw new IllegalStateException("boom");
            }));
            assertEquals(0L, repos.users.count());
            assertTrue(repos.users.findByUsername("one").isEmpty(), "index entries are rolled back too");
        }
    }

    @Test
    void rangeQueriesAndPersistenceAcrossRestarts() throws Exception {
        Path dir = Files.createTempDirectory("np-db");
        Path file = dir.resolve("test.mv.db");
        try (Database db = Database.open(file)) {
            Repositories repos = new Repositories(db);
            User u = repos.users.insert(user("ravi"));
            for (int day = 1; day <= 10; day++) {
                FoodLogEntry e = new FoodLogEntry();
                e.setUserId(u.getId());
                e.setDate(LocalDate.of(2026, 5, day));
                e.setMealType(MealType.LUNCH);
                e.setFoodName("Dal " + day);
                e.setNutrients(new Nutrients(100 * day, 1, 1, 1, 0, 0, 0));
                repos.foodLog.save(e);
            }
            List<FoodLogEntry> week = repos.foodLog.findByUserBetween(u.getId(), LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 9));
            assertEquals(7, week.size(), "inclusive date range");
            assertEquals(LocalDate.of(2026, 5, 3), week.get(0).getDate());
        }
        try (Database db = Database.open(file)) {
            Repositories repos = new Repositories(db);
            User u = repos.users.findByUsername("ravi").orElseThrow();
            assertEquals(10, repos.foodLog.findByUser(u.getId()).size(), "data survives a restart");
            assertEquals(500.0, repos.foodLog.findByUserAndDate(u.getId(), LocalDate.of(2026, 5, 5)).get(0).getNutrients().kcal());
        }
    }
}
