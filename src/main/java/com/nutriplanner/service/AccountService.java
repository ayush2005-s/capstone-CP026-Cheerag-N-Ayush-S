package com.nutriplanner.service;

import com.nutriplanner.domain.AuditType;
import com.nutriplanner.domain.User;
import com.nutriplanner.repository.Repositories;

/** Deleting accounts together with every record that belongs to them. */
public final class AccountService {

    private final Repositories repos;
    private final AuthService auth;
    private final AuditService audit;

    public AccountService(Repositories repos, AuthService auth, AuditService audit) {
        this.repos = repos;
        this.auth = auth;
        this.audit = audit;
    }

    /** A member deletes their own account. Requires their password and the word DELETE. */
    public void deleteOwnAccount(User user, String password, String confirmation, String ip) {
        ValidationException.Errors errors = new ValidationException.Errors();
        if (!auth.verifyPassword(user, password)) errors.add("password", "Your password is incorrect.");
        if (confirmation == null || !confirmation.strip().equals("DELETE")) {
            errors.add("confirmation", "Type DELETE in capital letters to confirm.");
        }
        if (user.isAdmin() && repos.users.countAdmins(true) <= 1) {
            errors.add("confirmation", "You're the only administrator. Make someone else an administrator first.");
        }
        errors.throwIfAny();
        purge(user.getId());
        audit.record(AuditType.ACCOUNT_DELETED, 0, user.getUsername(), "Deleted by the account owner", ip);
    }

    /** Removes the user and all their data in one transaction. */
    void purge(long userId) {
        repos.db.write(() -> {
            repos.foodLog.deleteByUser(userId);
            repos.weights.deleteByUser(userId);
            repos.mealPlans.deleteByUser(userId);
            repos.water.deleteByUser(userId);
            repos.foods.deleteByOwner(userId);
            repos.profiles.deleteByUser(userId);
            repos.users.delete(userId);
        });
    }
}
