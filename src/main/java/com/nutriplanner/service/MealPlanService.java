package com.nutriplanner.service;

import com.nutriplanner.domain.Food;
import com.nutriplanner.domain.MealPlanDay;
import com.nutriplanner.domain.MealType;
import com.nutriplanner.domain.Nutrients;
import com.nutriplanner.domain.PlanRole;
import com.nutriplanner.domain.PortionUnit;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.repository.Repositories;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/** Personalised meal plans: generating, adjusting, and logging planned meals into the diary. */
public final class MealPlanService {

    private final Repositories repos;
    private final FoodService foods;
    private final ProfileService profiles;
    private final DiaryService diary;
    private final Clock clock;
    private final Supplier<Random> randomSource;
    private final MealPlanGenerator generator = new MealPlanGenerator();

    public MealPlanService(Repositories repos, FoodService foods, ProfileService profiles, DiaryService diary, Clock clock) {
        this(repos, foods, profiles, diary, clock, () -> new Random(new SecureRandom().nextLong()));
    }

    public MealPlanService(Repositories repos, FoodService foods, ProfileService profiles, DiaryService diary, Clock clock,
                           Supplier<Random> randomSource) {
        this.repos = repos;
        this.foods = foods;
        this.profiles = profiles;
        this.diary = diary;
        this.clock = clock;
        this.randomSource = randomSource;
    }

    public static LocalDate weekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    // ---------------------------------------------------------------- generate

    public MealPlanDay generateDay(long userId, LocalDate date) {
        checkDate(date);
        Context ctx = context(userId);
        Set<Long> recent = recentFoodIds(userId, date);
        List<MealPlanDay.Item> items = generator.generateDay(ctx.targets, ctx.candidates, recent, randomSource.get());
        return save(userId, date, items, true);
    }

    public List<MealPlanDay> generateWeek(long userId, LocalDate anyDayOfWeek) {
        LocalDate start = weekStart(anyDayOfWeek);
        checkDate(start.plusDays(6));
        Context ctx = context(userId);
        Random random = randomSource.get();
        return repos.db.write(() -> {
            List<MealPlanDay> days = new ArrayList<>();
            Set<Long> previous = recentFoodIds(userId, start);
            for (int i = 0; i < 7; i++) {
                LocalDate date = start.plusDays(i);
                List<MealPlanDay.Item> items = generator.generateDay(ctx.targets, ctx.candidates, previous, random);
                days.add(save(userId, date, items, true));
                previous = new HashSet<>();
                for (MealPlanDay.Item item : items) previous.add(item.foodId());
            }
            return days;
        });
    }

    public MealPlanDay regenerateMeal(long userId, LocalDate date, MealType meal) {
        checkDate(date);
        Context ctx = context(userId);
        MealPlanDay plan = repos.mealPlans.findByUserAndDate(userId, date).orElse(null);
        List<MealPlanDay.Item> kept = new ArrayList<>();
        Set<Long> avoid = new HashSet<>();
        if (plan != null) {
            for (MealPlanDay.Item item : plan.getItems()) {
                if (item.meal() == meal) avoid.add(item.foodId());
                else kept.add(item);
            }
        }
        Set<Long> usedElsewhere = new HashSet<>();
        for (MealPlanDay.Item item : kept) usedElsewhere.add(item.foodId());
        Set<Long> skip = new HashSet<>(usedElsewhere);
        skip.addAll(avoid);
        List<MealPlanDay.Item> fresh = generator.generateMeal(meal, MealPlanGenerator.MealTarget.of(ctx.targets, meal),
                ctx.candidates, skip, recentFoodIds(userId, date), randomSource.get());
        List<MealPlanDay.Item> items = new ArrayList<>();
        for (MealType m : MealType.values()) {
            if (m == meal) items.addAll(fresh);
            else for (MealPlanDay.Item item : kept) if (item.meal() == m) items.add(item);
        }
        return save(userId, date, items, false);
    }

    /** Replaces one planned food with a different food that plays the same part, keeping calories similar. */
    public MealPlanDay swapItem(long userId, LocalDate date, int index) {
        MealPlanDay plan = ownPlan(userId, date);
        MealPlanDay.Item item = item(plan, index);
        Context ctx = context(userId);
        Food current = repos.foods.findById(item.foodId()).orElse(null);
        PlanRole role = current == null ? PlanRole.NONE : current.getPlanRole();
        Set<Long> inMeal = new HashSet<>();
        for (MealPlanDay.Item i : plan.itemsFor(item.meal())) inMeal.add(i.foodId());
        List<Food> options = new ArrayList<>();
        for (Food f : ctx.candidates) {
            if (inMeal.contains(f.getId()) || !f.suitsMeal(item.meal())) continue;
            if (role != PlanRole.NONE ? f.getPlanRole() == role : current != null && f.getCategory() == current.getCategory()) {
                options.add(f);
            }
        }
        if (options.isEmpty()) {
            throw new ValidationException("There are no other suitable foods for this part of the meal.");
        }
        Food replacement = options.get(randomSource.get().nextInt(options.size()));
        double kcal = current == null ? 0 : current.perServing().kcal() * item.servings();
        double perServing = replacement.perServing().kcal();
        double servings = perServing <= 0 ? 1 : Math.round(kcal / perServing * 2) / 2.0;
        PlanRole r = replacement.getPlanRole() == PlanRole.NONE ? PlanRole.SNACK : replacement.getPlanRole();
        servings = Math.max(0.5, Math.min(r.maxServings(), servings));
        List<MealPlanDay.Item> items = new ArrayList<>(plan.getItems());
        items.set(index, new MealPlanDay.Item(item.meal(), replacement.getId(), servings));
        return save(userId, date, items, false);
    }

    public MealPlanDay removeItem(long userId, LocalDate date, int index) {
        MealPlanDay plan = ownPlan(userId, date);
        item(plan, index);
        List<MealPlanDay.Item> items = new ArrayList<>(plan.getItems());
        items.remove(index);
        return save(userId, date, items, false);
    }

    public MealPlanDay updateServings(long userId, LocalDate date, int index, Double servings) {
        if (servings == null || servings <= 0 || servings > 20) {
            throw ValidationException.field("servings", "Enter between 0.25 and 20 servings.");
        }
        MealPlanDay plan = ownPlan(userId, date);
        MealPlanDay.Item item = item(plan, index);
        List<MealPlanDay.Item> items = new ArrayList<>(plan.getItems());
        items.set(index, item.withServings(Math.round(servings * 4) / 4.0));
        return save(userId, date, items, false);
    }

    public MealPlanDay addItem(long userId, LocalDate date, MealType meal, long foodId, Double servings) {
        checkDate(date);
        if (meal == null) throw ValidationException.field("meal", "Choose a meal.");
        if (servings == null || servings <= 0 || servings > 20) {
            throw ValidationException.field("quantity", "Enter between 0.25 and 20 servings.");
        }
        Food food = foods.visibleFood(userId, foodId);
        MealPlanDay plan = repos.mealPlans.findByUserAndDate(userId, date).orElse(null);
        List<MealPlanDay.Item> items = plan == null ? new ArrayList<>() : new ArrayList<>(plan.getItems());
        int insertAt = items.size();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).meal().ordinal() > meal.ordinal()) {
                insertAt = i;
                break;
            }
        }
        items.add(insertAt, new MealPlanDay.Item(meal, food.getId(), Math.round(servings * 4) / 4.0));
        return save(userId, date, items, false);
    }

    public void clearDay(long userId, LocalDate date) {
        repos.mealPlans.findByUserAndDate(userId, date).ifPresent(p -> repos.mealPlans.delete(p.getId()));
    }

    // -------------------------------------------------------------- to diary

    /** Copies one planned meal into the diary. Returns the number of foods logged. */
    public int logMeal(long userId, LocalDate date, MealType meal) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new ValidationException("You can log this meal on " + date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ".");
        }
        MealPlanDay plan = ownPlan(userId, date);
        List<MealPlanDay.Item> items = plan.itemsFor(meal);
        if (items.isEmpty()) throw new ValidationException("There's nothing planned for " + meal.label().toLowerCase(Locale.ROOT) + ".");
        return repos.db.write(() -> {
            int logged = 0;
            for (MealPlanDay.Item item : items) {
                if (repos.foods.findById(item.foodId()).filter(f -> f.visibleTo(userId)).isEmpty()) continue;
                diary.add(userId, date, meal, item.foodId(), item.servings(), PortionUnit.SERVING);
                logged++;
            }
            return logged;
        });
    }

    public int logDay(long userId, LocalDate date) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new ValidationException("You can log a planned day on the day itself or later.");
        }
        MealPlanDay plan = ownPlan(userId, date);
        if (plan.getItems().isEmpty()) throw new ValidationException("There's nothing planned for this day.");
        return repos.db.write(() -> {
            int logged = 0;
            for (MealType meal : MealType.values()) {
                if (!plan.itemsFor(meal).isEmpty()) logged += logMeal(userId, date, meal);
            }
            return logged;
        });
    }

    // ----------------------------------------------------------------- reading

    public record PlannedItem(int index, MealPlanDay.Item item, Food food, Nutrients nutrients) {
        public boolean available() {
            return food != null;
        }
    }

    public record PlannedDay(LocalDate date, boolean exists, Map<MealType, List<PlannedItem>> meals,
                             Map<MealType, Nutrients> mealTotals, Nutrients total) {
        public boolean isEmpty() {
            return meals.values().stream().allMatch(List::isEmpty);
        }
    }

    public PlannedDay day(long userId, LocalDate date) {
        Optional<MealPlanDay> plan = repos.mealPlans.findByUserAndDate(userId, date);
        return toView(userId, date, plan.orElse(null), new HashMap<>());
    }

    public List<PlannedDay> week(long userId, LocalDate anyDay) {
        LocalDate start = weekStart(anyDay);
        Map<LocalDate, MealPlanDay> plans = new HashMap<>();
        for (MealPlanDay p : repos.mealPlans.findByUserBetween(userId, start, start.plusDays(6))) plans.put(p.getDate(), p);
        Map<Long, Optional<Food>> cache = new HashMap<>();
        List<PlannedDay> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            days.add(toView(userId, d, plans.get(d), cache));
        }
        return days;
    }

    private PlannedDay toView(long userId, LocalDate date, MealPlanDay plan, Map<Long, Optional<Food>> cache) {
        Map<MealType, List<PlannedItem>> meals = new EnumMap<>(MealType.class);
        Map<MealType, Nutrients> totals = new EnumMap<>(MealType.class);
        for (MealType m : MealType.values()) {
            meals.put(m, new ArrayList<>());
            totals.put(m, Nutrients.ZERO);
        }
        Nutrients total = Nutrients.ZERO;
        if (plan != null) {
            List<MealPlanDay.Item> items = plan.getItems();
            for (int i = 0; i < items.size(); i++) {
                MealPlanDay.Item item = items.get(i);
                Food food = cache.computeIfAbsent(item.foodId(), id -> repos.foods.findById(id).filter(f -> f.visibleTo(userId))).orElse(null);
                Nutrients n = food == null ? Nutrients.ZERO : food.perServing().scale(item.servings());
                meals.get(item.meal()).add(new PlannedItem(i, item, food, n));
                totals.put(item.meal(), totals.get(item.meal()).plus(n));
                total = total.plus(n);
            }
        }
        return new PlannedDay(date, plan != null, meals, totals, total);
    }

    // ---------------------------------------------------------------- helpers

    private record Context(Profile profile, NutritionTargets targets, List<Food> candidates) {
    }

    private Context context(long userId) {
        Profile profile = profiles.profile(userId);
        if (!profile.isCompleted() || !profile.hasBodyMetrics()) {
            throw new ValidationException("Finish setting up your profile so plans can match your body and goals.");
        }
        NutritionTargets targets = NutritionCalculator.targetsFor(profile, LocalDate.now(clock));
        List<Food> candidates = foods.planCandidates(userId, profile);
        if (candidates.isEmpty()) {
            throw new ValidationException("No foods in the database match your diet and allergies yet.");
        }
        return new Context(profile, targets, candidates);
    }

    private void checkDate(LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        if (date == null || date.isBefore(today.minusDays(60)) || date.isAfter(today.plusDays(90))) {
            throw new ValidationException("Plans can be made from 60 days ago up to 90 days ahead.");
        }
    }

    private MealPlanDay ownPlan(long userId, LocalDate date) {
        return repos.mealPlans.findByUserAndDate(userId, date)
                .orElseThrow(() -> new NotFoundException("There's no plan for that day yet."));
    }

    private static MealPlanDay.Item item(MealPlanDay plan, int index) {
        if (index < 0 || index >= plan.getItems().size()) throw new NotFoundException("That planned food no longer exists.");
        return plan.getItems().get(index);
    }

    private Set<Long> recentFoodIds(long userId, LocalDate date) {
        Set<Long> ids = new HashSet<>();
        repos.mealPlans.findByUserAndDate(userId, date.minusDays(1))
                .ifPresent(p -> p.getItems().forEach(i -> ids.add(i.foodId())));
        return ids;
    }

    private MealPlanDay save(long userId, LocalDate date, List<MealPlanDay.Item> items, boolean generated) {
        return repos.db.write(() -> {
            MealPlanDay plan = repos.mealPlans.findByUserAndDate(userId, date).orElseGet(() -> {
                MealPlanDay p = new MealPlanDay();
                p.setUserId(userId);
                p.setDate(date);
                return p;
            });
            plan.setItems(items);
            if (generated || plan.getGeneratedAt() == null) plan.setGeneratedAt(clock.instant());
            plan.setUpdatedAt(clock.instant());
            return repos.mealPlans.save(plan);
        });
    }
}
