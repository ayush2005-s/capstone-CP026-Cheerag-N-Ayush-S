package com.nutriplanner.service;

import com.nutriplanner.domain.WaterLog;
import com.nutriplanner.repository.Repositories;

import java.time.Clock;
import java.time.LocalDate;

/** Daily water intake, counted in 250 ml glasses. */
public final class WaterService {

    public static final int MAX_GLASSES = 40;

    private final Repositories repos;
    private final Clock clock;

    public WaterService(Repositories repos, Clock clock) {
        this.repos = repos;
        this.clock = clock;
    }

    public int glasses(long userId, LocalDate date) {
        return repos.water.findByUserAndDate(userId, date).map(WaterLog::getGlasses).orElse(0);
    }

    public int change(long userId, LocalDate date, int delta) {
        if (date == null || date.isAfter(LocalDate.now(clock))) {
            throw new ValidationException("You can only track water for today or earlier.");
        }
        return repos.db.write(() -> {
            WaterLog log = repos.water.findByUserAndDate(userId, date).orElseGet(() -> new WaterLog(userId, date, 0));
            log.setGlasses(Math.max(0, Math.min(MAX_GLASSES, log.getGlasses() + delta)));
            repos.water.save(log);
            return log.getGlasses();
        });
    }
}
