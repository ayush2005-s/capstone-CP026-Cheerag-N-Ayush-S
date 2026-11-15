package com.nutriplanner.repository;

import com.nutriplanner.domain.Keys;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.storage.Database;
import com.nutriplanner.storage.EntityStore;

import java.util.Optional;

public final class ProfileRepository {

    private final EntityStore<Profile> store;
    private final EntityStore.Index<Profile> byUser;

    public ProfileRepository(Database db) {
        store = new EntityStore<>(db, "profiles", Profile.MAPPER);
        byUser = store.uniqueIndex("user", p -> Keys.id(p.getUserId()));
    }

    public Optional<Profile> findByUserId(long userId) {
        return store.findUnique(byUser, Keys.id(userId));
    }

    public Profile save(Profile profile) {
        return store.save(profile);
    }

    public void deleteByUser(long userId) {
        findByUserId(userId).ifPresent(p -> store.delete(p.getId()));
    }
}
