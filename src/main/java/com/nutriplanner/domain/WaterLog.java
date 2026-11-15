package com.nutriplanner.domain;

import com.nutriplanner.storage.Doc;
import com.nutriplanner.storage.EntityMapper;

import java.time.LocalDate;

/** Glasses of water drunk on a day (one glass is 250 ml). */
public final class WaterLog {

    public static final int GLASS_ML = 250;

    public static final EntityMapper<WaterLog> MAPPER =
            EntityMapper.of(WaterLog::getId, WaterLog::setId, WaterLog::toDoc, WaterLog::fromDoc);

    private long id;
    private long userId;
    private LocalDate date;
    private int glasses;

    public WaterLog() {
    }

    public WaterLog(long userId, LocalDate date, int glasses) {
        this.userId = userId;
        this.date = date;
        this.glasses = glasses;
    }

    public int millilitres() {
        return glasses * GLASS_ML;
    }

    public Doc toDoc() {
        return new Doc().put("userId", userId).put("date", date).put("glasses", glasses);
    }

    public static WaterLog fromDoc(long id, Doc d) {
        WaterLog w = new WaterLog();
        w.id = id;
        w.userId = d.getLong("userId", 0);
        w.date = d.getDate("date");
        w.glasses = d.getInt("glasses", 0);
        return w;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public int getGlasses() { return glasses; }
    public void setGlasses(int glasses) { this.glasses = glasses; }
}
