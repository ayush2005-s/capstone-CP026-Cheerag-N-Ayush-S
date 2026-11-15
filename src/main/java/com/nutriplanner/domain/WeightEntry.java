package com.nutriplanner.domain;

import com.nutriplanner.storage.Doc;
import com.nutriplanner.storage.EntityMapper;

import java.time.Instant;
import java.time.LocalDate;

/** A body-weight measurement. One per user per day. */
public final class WeightEntry {

    public static final EntityMapper<WeightEntry> MAPPER =
            EntityMapper.of(WeightEntry::getId, WeightEntry::setId, WeightEntry::toDoc, WeightEntry::fromDoc);

    private long id;
    private long userId;
    private LocalDate date;
    private double weightKg;
    private String note = "";
    private Instant createdAt;

    public WeightEntry() {
    }

    public WeightEntry(long userId, LocalDate date, double weightKg, String note) {
        this.userId = userId;
        this.date = date;
        this.weightKg = weightKg;
        this.note = note == null ? "" : note;
    }

    public Doc toDoc() {
        return new Doc().put("userId", userId).put("date", date).put("weightKg", weightKg)
                .put("note", note).put("createdAt", createdAt);
    }

    public static WeightEntry fromDoc(long id, Doc d) {
        WeightEntry w = new WeightEntry();
        w.id = id;
        w.userId = d.getLong("userId", 0);
        w.date = d.getDate("date");
        w.weightKg = d.getDouble("weightKg", 0);
        w.note = d.getString("note", "");
        w.createdAt = d.getInstant("createdAt");
        return w;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note == null ? "" : note; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
