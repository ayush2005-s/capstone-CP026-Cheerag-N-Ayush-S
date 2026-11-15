package com.nutriplanner.service;

import com.nutriplanner.domain.Goal;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.WeightEntry;
import com.nutriplanner.repository.Repositories;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Weight log and progress statistics. The latest entry is always mirrored into the profile's current weight. */
public final class WeightService {

    public static final double MIN_KG = 25;
    public static final double MAX_KG = 350;

    private final Repositories repos;
    private final Clock clock;

    public WeightService(Repositories repos, Clock clock) {
        this.repos = repos;
        this.clock = clock;
    }

    public WeightEntry log(long userId, LocalDate date, Double weightKg, String note) {
        LocalDate today = LocalDate.now(clock);
        ValidationException.Errors errors = new ValidationException.Errors();
        if (date == null) errors.add("date", "Choose the date of the measurement.");
        else if (date.isAfter(today)) errors.add("date", "The date can't be in the future.");
        else if (date.isBefore(today.minusYears(10))) errors.add("date", "Choose a date within the last 10 years.");
        if (weightKg == null) errors.add("weightKg", "Enter your weight in kilograms.");
        else if (weightKg < MIN_KG || weightKg > MAX_KG) errors.add("weightKg", "Enter a weight between 25 and 350 kg.");
        String cleanNote = note == null ? "" : note.strip();
        if (cleanNote.length() > 120) errors.add("note", "Keep the note under 120 characters.");
        errors.throwIfAny();

        double rounded = Math.round(weightKg * 10) / 10.0;
        return repos.db.write(() -> {
            WeightEntry entry = repos.weights.findByUserAndDate(userId, date).orElseGet(() -> {
                WeightEntry e = new WeightEntry(userId, date, rounded, cleanNote);
                e.setCreatedAt(clock.instant());
                return e;
            });
            entry.setWeightKg(rounded);
            entry.setNote(cleanNote);
            repos.weights.save(entry);
            syncProfileWeight(userId);
            return entry;
        });
    }

    public void delete(long userId, long entryId) {
        repos.db.write(() -> {
            WeightEntry entry = repos.weights.findById(entryId)
                    .filter(e -> e.getUserId() == userId)
                    .orElseThrow(() -> new NotFoundException("That weight entry doesn't exist."));
            repos.weights.delete(entry.getId());
            syncProfileWeight(userId);
        });
    }

    /** All entries, oldest first. */
    public List<WeightEntry> history(long userId) {
        return repos.weights.findByUser(userId);
    }

    public Optional<WeightEntry> latest(long userId) {
        List<WeightEntry> all = history(userId);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(all.size() - 1));
    }

    private void syncProfileWeight(long userId) {
        latest(userId).ifPresent(latest -> repos.profiles.findByUserId(userId).ifPresent(p -> {
            if (Math.abs(p.getWeightKg() - latest.getWeightKg()) > 0.001) {
                p.setWeightKg(latest.getWeightKg());
                p.setUpdatedAt(clock.instant());
                repos.profiles.save(p);
            }
        }));
    }

    // ------------------------------------------------------------------- stats

    public record WeightStats(int entries, Double startKg, LocalDate startDate, Double currentKg, LocalDate currentDate,
                              Double lowestKg, Double highestKg, Double totalChangeKg, Double change30dKg,
                              Double weeklyTrendKg, Double targetKg, Double toGoKg, Double progressPercent,
                              LocalDate projectedDate) {
        public boolean hasData() {
            return entries > 0;
        }
    }

    public WeightStats stats(long userId, Profile profile) {
        List<WeightEntry> all = history(userId);
        LocalDate today = LocalDate.now(clock);
        Double target = profile == null || profile.getGoal() == Goal.MAINTAIN ? null : profile.getTargetWeightKg();
        if (all.isEmpty()) {
            return new WeightStats(0, null, null, profile == null ? null : nonZero(profile.getWeightKg()), null,
                    null, null, null, null, null, target, null, null, null);
        }
        WeightEntry first = all.get(0);
        WeightEntry last = all.get(all.size() - 1);
        double lowest = Double.MAX_VALUE, highest = -Double.MAX_VALUE;
        for (WeightEntry e : all) {
            lowest = Math.min(lowest, e.getWeightKg());
            highest = Math.max(highest, e.getWeightKg());
        }
        Double change30 = null;
        LocalDate cutoff = last.getDate().minusDays(30);
        for (WeightEntry e : all) {
            if (!e.getDate().isAfter(cutoff)) change30 = last.getWeightKg() - e.getWeightKg();
        }
        if (change30 == null && all.size() > 1) change30 = last.getWeightKg() - first.getWeightKg();

        Double trend = weeklyTrend(all, today);
        Double toGo = null, progress = null;
        LocalDate projected = null;
        if (target != null) {
            toGo = Math.abs(last.getWeightKg() - target);
            double totalDistance = Math.abs(first.getWeightKg() - target);
            double moved = profile.getGoal() == Goal.LOSE_WEIGHT
                    ? first.getWeightKg() - last.getWeightKg()
                    : last.getWeightKg() - first.getWeightKg();
            progress = totalDistance < 0.05 ? 100.0 : Math.max(0, Math.min(100, moved / totalDistance * 100));
            boolean reached = profile.getGoal() == Goal.LOSE_WEIGHT ? last.getWeightKg() <= target : last.getWeightKg() >= target;
            if (reached) {
                toGo = 0.0;
                progress = 100.0;
            } else if (trend != null) {
                double needed = target - last.getWeightKg();
                if (Math.abs(trend) > 0.02 && Math.signum(needed) == Math.signum(trend)) {
                    long days = Math.round(needed / trend * 7);
                    if (days < 3650) projected = last.getDate().plusDays(days);
                }
            }
        }
        return new WeightStats(all.size(), first.getWeightKg(), first.getDate(), last.getWeightKg(), last.getDate(),
                lowest, highest, last.getWeightKg() - first.getWeightKg(), change30, trend, target, toGo, progress, projected);
    }

    /** Least-squares slope of the last eight weeks of entries, in kg per week. */
    static Double weeklyTrend(List<WeightEntry> all, LocalDate today) {
        LocalDate from = today.minusDays(56);
        List<WeightEntry> recent = new ArrayList<>();
        for (WeightEntry e : all) {
            if (!e.getDate().isBefore(from)) recent.add(e);
        }
        if (recent.size() < 3) return null;
        LocalDate origin = recent.get(0).getDate();
        long span = ChronoUnit.DAYS.between(origin, recent.get(recent.size() - 1).getDate());
        if (span < 7) return null;
        double n = recent.size(), sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (WeightEntry e : recent) {
            double x = ChronoUnit.DAYS.between(origin, e.getDate());
            double y = e.getWeightKg();
            sx += x;
            sy += y;
            sxx += x * x;
            sxy += x * y;
        }
        double denominator = n * sxx - sx * sx;
        if (denominator == 0) return null;
        double slopePerDay = (n * sxy - sx * sy) / denominator;
        return slopePerDay * 7;
    }

    private static Double nonZero(double v) {
        return v > 0 ? v : null;
    }
}
