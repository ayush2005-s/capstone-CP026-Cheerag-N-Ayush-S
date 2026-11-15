package com.nutriplanner.repository;

import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.Keys;
import com.nutriplanner.storage.Database;
import com.nutriplanner.storage.EntityStore;

import java.util.List;
import java.util.Optional;

public final class FoodRepository {

    private final EntityStore<Food> store;
    private final EntityStore.Index<Food> byOwner;

    public FoodRepository(Database db) {
        store = new EntityStore<>(db, "foods", Food.MAPPER);
        byOwner = store.index("owner", f -> Keys.id(f.getOwnerId()));
    }

    public Optional<Food> findById(long id) {
        return store.findById(id);
    }

    public List<Food> findAll() {
        return store.findAll();
    }

    /** Foods managed by administrators, visible to everyone. */
    public List<Food> findGlobal() {
        return store.findByKey(byOwner, Keys.id(0));
    }

    public List<Food> findByOwner(long ownerId) {
        return store.findByKey(byOwner, Keys.id(ownerId));
    }

    public boolean globalNameExists(String name, long exceptId) {
        String wanted = Keys.lower(name);
        for (Food f : findGlobal()) {
            if (f.getId() != exceptId && Keys.lower(f.getName()).equals(wanted)) return true;
        }
        return false;
    }

    public long count() {
        return store.count();
    }

    public Food save(Food food) {
        return store.save(food);
    }

    public boolean delete(long id) {
        return store.delete(id);
    }

    public int deleteByOwner(long ownerId) {
        int n = 0;
        for (Food f : findByOwner(ownerId)) {
            if (store.delete(f.getId())) n++;
        }
        return n;
    }
}
