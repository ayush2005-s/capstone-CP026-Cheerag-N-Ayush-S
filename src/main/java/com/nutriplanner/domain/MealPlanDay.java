package com.nutriplanner.domain;

import com.nutriplanner.storage.Doc;
import com.nutriplanner.storage.EntityMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The planned meals of one user for one day. */
public final class MealPlanDay {

    public static final EntityMapper<MealPlanDay> MAPPER =
            EntityMapper.of(MealPlanDay::getId, MealPlanDay::setId, MealPlanDay::toDoc, MealPlanDay::fromDoc);

    /** One food in a planned meal. */
    public record Item(MealType meal, long foodId, double servings) {
        Doc toDoc() {
            return new Doc().put("meal", meal).put("foodId", foodId).put("servings", servings);
        }

        static Item fromDoc(Doc d) {
            return new Item(d.getEnum("meal", MealType.class, MealType.LUNCH), d.getLong("foodId", 0),
                    d.getDouble("servings", 1));
        }

        public Item withServings(double value) {
            return new Item(meal, foodId, value);
        }
    }

    private long id;
    private long userId;
    private LocalDate date;
    private List<Item> items = new ArrayList<>();
    private Instant generatedAt;
    private Instant updatedAt;

    public List<Item> itemsFor(MealType meal) {
        List<Item> out = new ArrayList<>();
        for (Item item : items) {
            if (item.meal() == meal) out.add(item);
        }
        return out;
    }

    public Doc toDoc() {
        List<Doc> docs = new ArrayList<>();
        for (Item item : items) docs.add(item.toDoc());
        return new Doc().put("userId", userId).put("date", date).put("items", docs)
                .put("generatedAt", generatedAt).put("updatedAt", updatedAt);
    }

    public static MealPlanDay fromDoc(long id, Doc d) {
        MealPlanDay p = new MealPlanDay();
        p.id = id;
        p.userId = d.getLong("userId", 0);
        p.date = d.getDate("date");
        for (Doc item : d.getDocList("items")) p.items.add(Item.fromDoc(item));
        p.generatedAt = d.getInstant("generatedAt");
        p.updatedAt = d.getInstant("updatedAt");
        return p;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = new ArrayList<>(items); }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
