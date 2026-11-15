package com.nutriplanner.repository;

import com.nutriplanner.domain.Keys;
import com.nutriplanner.domain.Role;
import com.nutriplanner.domain.User;
import com.nutriplanner.storage.Database;
import com.nutriplanner.storage.EntityStore;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class UserRepository {

    private final EntityStore<User> store;
    private final EntityStore.Index<User> byUsername;
    private final EntityStore.Index<User> byEmail;

    public UserRepository(Database db) {
        store = new EntityStore<>(db, "users", User.MAPPER);
        byUsername = store.uniqueIndex("username", u -> Keys.lower(u.getUsername()));
        byEmail = store.uniqueIndex("email", u -> Keys.lower(u.getEmail()));
    }

    public Optional<User> findById(long id) {
        return store.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return username == null ? Optional.empty() : store.findUnique(byUsername, Keys.lower(username));
    }

    public Optional<User> findByEmail(String email) {
        return email == null ? Optional.empty() : store.findUnique(byEmail, Keys.lower(email));
    }

    /** Finds an account by username or email address. */
    public Optional<User> findByLogin(String login) {
        if (login == null || login.isBlank()) return Optional.empty();
        return login.contains("@") ? findByEmail(login) : findByUsername(login);
    }

    public List<User> findAll() {
        List<User> users = store.findAll();
        users.sort(Comparator.comparing(User::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())));
        return users;
    }

    public long countAdmins(boolean activeOnly) {
        return store.find(u -> u.getRole() == Role.ADMIN && (!activeOnly || u.isActive())).size();
    }

    public long count() {
        return store.count();
    }

    public User insert(User user) {
        return store.insert(user);
    }

    public User update(User user) {
        return store.update(user);
    }

    public boolean delete(long id) {
        return store.delete(id);
    }
}
