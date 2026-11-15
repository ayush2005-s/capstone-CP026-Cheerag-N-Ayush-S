package com.nutriplanner.service;

import com.nutriplanner.domain.AccountStatus;
import com.nutriplanner.domain.AuditEvent;
import com.nutriplanner.domain.AuditType;
import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.FoodLogEntry;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.Role;
import com.nutriplanner.domain.User;
import com.nutriplanner.repository.Repositories;
import com.nutriplanner.security.AccessDeniedException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Administrator operations. Every method checks the caller's role first, so access control does not depend on
 * the web layer alone.
 */
public final class AdminService {

    private static final char[] TEMP_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789".toCharArray();

    private final Repositories repos;
    private final FoodService foods;
    private final AccountService accounts;
    private final AuthService auth;
    private final AuditService audit;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AdminService(Repositories repos, FoodService foods, AccountService accounts, AuthService auth,
                        AuditService audit, Clock clock) {
        this.repos = repos;
        this.foods = foods;
        this.accounts = accounts;
        this.auth = auth;
        this.audit = audit;
        this.clock = clock;
    }

    public static void requireAdmin(User actor) {
        if (actor == null || !actor.isAdmin() || !actor.isActive()) {
            throw new AccessDeniedException("Only administrators can do this.");
        }
    }

    // ---------------------------------------------------------------- overview

    public record Overview(int users, int admins, int activeLast7Days, int disabled, int locked, int newLast30Days,
                           int globalFoods, int inactiveFoods, int customFoods, long diaryEntries, int entriesLast7Days,
                           Map<LocalDate, Integer> signups, Map<LocalDate, Integer> activity, List<AuditEvent> recentEvents) {
    }

    public Overview overview(User actor) {
        requireAdmin(actor);
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);
        ZoneId zone = clock.getZone();
        List<User> users = repos.users.findAll();
        int admins = 0, active7 = 0, disabled = 0, locked = 0, new30 = 0;
        Map<LocalDate, Integer> signups = new LinkedHashMap<>();
        for (int i = 13; i >= 0; i--) signups.put(today.minusDays(i), 0);
        for (User u : users) {
            if (u.isAdmin()) admins++;
            if (u.getLastLoginAt() != null && u.getLastLoginAt().isAfter(now.minus(7, ChronoUnit.DAYS))) active7++;
            if (!u.isActive()) disabled++;
            if (u.isLocked(now)) locked++;
            if (u.getCreatedAt() != null) {
                if (u.getCreatedAt().isAfter(now.minus(30, ChronoUnit.DAYS))) new30++;
                signups.computeIfPresent(LocalDate.ofInstant(u.getCreatedAt(), zone), (d, c) -> c + 1);
            }
        }
        int global = 0, inactive = 0, custom = 0;
        for (Food f : repos.foods.findAll()) {
            if (f.isGlobal()) {
                global++;
                if (!f.isActive()) inactive++;
            } else {
                custom++;
            }
        }
        Map<LocalDate, Integer> activity = new LinkedHashMap<>();
        for (int i = 13; i >= 0; i--) activity.put(today.minusDays(i), 0);
        int last7 = 0;
        for (FoodLogEntry e : repos.foodLog.findAll()) {
            activity.computeIfPresent(e.getDate(), (d, c) -> c + 1);
            if (!e.getDate().isBefore(today.minusDays(6))) last7++;
        }
        List<AuditEvent> recent = audit.newestFirst();
        return new Overview(users.size(), admins, active7, disabled, locked, new30, global, inactive, custom,
                repos.foodLog.count(), last7, signups, activity, recent.subList(0, Math.min(8, recent.size())));
    }

    // ------------------------------------------------------------------- users

    public Page<User> users(User actor, String text, Role role, AccountStatus status, int page, int pageSize) {
        requireAdmin(actor);
        String q = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
        List<User> out = new ArrayList<>();
        for (User u : repos.users.findAll()) {
            if (role != null && u.getRole() != role) continue;
            if (status != null && u.getStatus() != status) continue;
            if (!q.isEmpty() && !(u.getUsername().toLowerCase(Locale.ROOT).contains(q)
                    || u.getEmail().toLowerCase(Locale.ROOT).contains(q)
                    || u.getFullName().toLowerCase(Locale.ROOT).contains(q))) continue;
            out.add(u);
        }
        out.sort(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return Page.of(out, page, pageSize);
    }

    public record UserDetail(User user, Profile profile, int diaryEntries, int weightEntries, int customFoods,
                             LocalDate lastLogged) {
    }

    public UserDetail user(User actor, long userId) {
        requireAdmin(actor);
        User user = find(userId);
        List<FoodLogEntry> entries = repos.foodLog.findByUser(userId);
        LocalDate last = entries.stream().map(FoodLogEntry::getDate).max(Comparator.naturalOrder()).orElse(null);
        return new UserDetail(user, repos.profiles.findByUserId(userId).orElse(null), entries.size(),
                repos.weights.findByUser(userId).size(), repos.foods.findByOwner(userId).size(), last);
    }

    public void setStatus(User actor, long userId, AccountStatus status, String ip) {
        requireAdmin(actor);
        if (status == null) throw new ValidationException("Choose a status.");
        User user = find(userId);
        if (user.getId() == actor.getId()) throw new ValidationException("You can't disable your own account.");
        if (status == AccountStatus.DISABLED && user.isAdmin() && repos.users.countAdmins(true) <= 1) {
            throw new ValidationException("This is the only active administrator.");
        }
        user.setStatus(status);
        repos.users.update(user);
        audit.record(AuditType.ADMIN_USER_UPDATED, actor, user.getUsername() + ": status set to " + status.label(), ip);
    }

    public void setRole(User actor, long userId, Role role, String ip) {
        requireAdmin(actor);
        if (role == null) throw new ValidationException("Choose a role.");
        User user = find(userId);
        if (user.getId() == actor.getId() && role != Role.ADMIN) {
            throw new ValidationException("You can't remove your own administrator role.");
        }
        if (user.isAdmin() && role != Role.ADMIN && repos.users.countAdmins(true) <= 1) {
            throw new ValidationException("There must always be at least one administrator.");
        }
        user.setRole(role);
        repos.users.update(user);
        audit.record(AuditType.ADMIN_USER_UPDATED, actor, user.getUsername() + ": role set to " + role.label(), ip);
    }

    public void unlock(User actor, long userId, String ip) {
        requireAdmin(actor);
        User user = find(userId);
        user.setLockedUntil(null);
        user.setFailedLogins(0);
        repos.users.update(user);
        audit.record(AuditType.ADMIN_USER_UPDATED, actor, user.getUsername() + ": unlocked", ip);
    }

    /** Sets a random temporary password, which the user must change at their next login. */
    public String resetPassword(User actor, long userId, String ip) {
        requireAdmin(actor);
        User user = find(userId);
        String temporary = temporaryPassword();
        user.setPasswordHash(auth.hashPassword(temporary));
        user.setMustChangePassword(true);
        user.setLockedUntil(null);
        user.setFailedLogins(0);
        user.setPasswordChangedAt(clock.instant());
        repos.users.update(user);
        audit.record(AuditType.ADMIN_PASSWORD_RESET, actor, user.getUsername(), ip);
        return temporary;
    }

    public void deleteUser(User actor, long userId, String confirmUsername, String ip) {
        requireAdmin(actor);
        User user = find(userId);
        if (user.getId() == actor.getId()) throw new ValidationException("Delete your own account from Settings instead.");
        if (confirmUsername == null || !confirmUsername.strip().equalsIgnoreCase(user.getUsername())) {
            throw ValidationException.field("confirmUsername", "Type the username exactly to confirm.");
        }
        if (user.isAdmin() && repos.users.countAdmins(true) <= 1) {
            throw new ValidationException("There must always be at least one administrator.");
        }
        accounts.purge(userId);
        audit.record(AuditType.ADMIN_USER_DELETED, actor, user.getUsername(), ip);
    }

    // ------------------------------------------------------------------- foods

    public Food saveFood(User actor, Long foodId, FoodService.FoodInput input, String ip) {
        requireAdmin(actor);
        Food saved = foods.saveGlobal(foodId, input);
        audit.record(foodId == null ? AuditType.ADMIN_FOOD_CREATED : AuditType.ADMIN_FOOD_UPDATED, actor, saved.getName(), ip);
        return saved;
    }

    public void setFoodActive(User actor, long foodId, boolean active, String ip) {
        requireAdmin(actor);
        Food food = repos.foods.findById(foodId).filter(Food::isGlobal)
                .orElseThrow(() -> new NotFoundException("That food doesn't exist."));
        food.setActive(active);
        food.setUpdatedAt(clock.instant());
        repos.foods.save(food);
        audit.record(AuditType.ADMIN_FOOD_UPDATED, actor, food.getName() + (active ? ": restored" : ": hidden from members"), ip);
    }

    public void deleteFood(User actor, long foodId, String ip) {
        requireAdmin(actor);
        Food food = repos.foods.findById(foodId).filter(Food::isGlobal)
                .orElseThrow(() -> new NotFoundException("That food doesn't exist."));
        repos.foods.delete(food.getId());
        audit.record(AuditType.ADMIN_FOOD_DELETED, actor, food.getName(), ip);
    }

    // ------------------------------------------------------------------- audit

    public Page<AuditEvent> audit(User actor, AuditType type, String text, int page, int pageSize) {
        requireAdmin(actor);
        String q = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
        List<AuditEvent> out = new ArrayList<>();
        for (AuditEvent e : audit.newestFirst()) {
            if (type != null && e.getType() != type) continue;
            if (!q.isEmpty() && !(e.getActorName().toLowerCase(Locale.ROOT).contains(q)
                    || e.getDetail().toLowerCase(Locale.ROOT).contains(q)
                    || e.getIp().contains(q))) continue;
            out.add(e);
        }
        return Page.of(out, page, pageSize);
    }

    // ----------------------------------------------------------------- helpers

    private User find(long userId) {
        return repos.users.findById(userId).orElseThrow(() -> new NotFoundException("That user doesn't exist."));
    }

    String temporaryPassword() {
        StringBuilder sb = new StringBuilder();
        for (int group = 0; group < 3; group++) {
            if (group > 0) sb.append('-');
            for (int i = 0; i < 4; i++) sb.append(TEMP_ALPHABET[random.nextInt(TEMP_ALPHABET.length)]);
        }
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        sb.append(2 + random.nextInt(8));
        return sb.toString();
    }
}
