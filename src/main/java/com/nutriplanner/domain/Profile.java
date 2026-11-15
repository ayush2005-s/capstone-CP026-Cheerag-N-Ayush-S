package com.nutriplanner.domain;

import com.nutriplanner.storage.Doc;
import com.nutriplanner.storage.EntityMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.EnumSet;

/** Body measurements, goals and dietary preferences of one user. */
public final class Profile {

    public static final EntityMapper<Profile> MAPPER =
            EntityMapper.of(Profile::getId, Profile::setId, Profile::toDoc, Profile::fromDoc);

    private long id;
    private long userId;
    private Sex sex = Sex.FEMALE;
    private LocalDate birthDate;
    private double heightCm;
    private double weightKg;
    private Double bodyFatPercent;
    private ActivityLevel activityLevel = ActivityLevel.LIGHT;
    private Goal goal = Goal.MAINTAIN;
    private Double targetWeightKg;
    private double weeklyChangeKg;
    private DietType dietType = DietType.VEGETARIAN;
    private EnumSet<Allergen> allergens = EnumSet.noneOf(Allergen.class);
    private EnumSet<DietaryRestriction> restrictions = EnumSet.noneOf(DietaryRestriction.class);
    private BmrFormula bmrFormula = BmrFormula.MIFFLIN_ST_JEOR;
    private Integer calorieOverride;
    private ThemePreference theme = ThemePreference.SYSTEM;
    private int onboardingStep;
    private boolean completed;
    private Instant updatedAt;

    public Profile() {
    }

    public Profile(long userId) {
        this.userId = userId;
    }

    public int age(LocalDate today) {
        return birthDate == null ? 0 : Period.between(birthDate, today).getYears();
    }

    public boolean hasBodyMetrics() {
        return birthDate != null && heightCm > 0 && weightKg > 0;
    }

    public Doc toDoc() {
        return new Doc()
                .put("userId", userId)
                .put("sex", sex)
                .put("birthDate", birthDate)
                .put("heightCm", heightCm)
                .put("weightKg", weightKg)
                .put("bodyFatPercent", bodyFatPercent)
                .put("activityLevel", activityLevel)
                .put("goal", goal)
                .put("targetWeightKg", targetWeightKg)
                .put("weeklyChangeKg", weeklyChangeKg)
                .put("dietType", dietType)
                .put("allergens", allergens)
                .put("restrictions", restrictions)
                .put("bmrFormula", bmrFormula)
                .put("calorieOverride", calorieOverride)
                .put("theme", theme)
                .put("onboardingStep", onboardingStep)
                .put("completed", completed)
                .put("updatedAt", updatedAt);
    }

    public static Profile fromDoc(long id, Doc d) {
        Profile p = new Profile();
        p.id = id;
        p.userId = d.getLong("userId", 0);
        p.sex = d.getEnum("sex", Sex.class, Sex.FEMALE);
        p.birthDate = d.getDate("birthDate");
        p.heightCm = d.getDouble("heightCm", 0);
        p.weightKg = d.getDouble("weightKg", 0);
        p.bodyFatPercent = d.getDoubleOrNull("bodyFatPercent");
        p.activityLevel = d.getEnum("activityLevel", ActivityLevel.class, ActivityLevel.LIGHT);
        p.goal = d.getEnum("goal", Goal.class, Goal.MAINTAIN);
        p.targetWeightKg = d.getDoubleOrNull("targetWeightKg");
        p.weeklyChangeKg = d.getDouble("weeklyChangeKg", 0);
        p.dietType = d.getEnum("dietType", DietType.class, DietType.VEGETARIAN);
        p.allergens = d.getEnumSet("allergens", Allergen.class);
        p.restrictions = d.getEnumSet("restrictions", DietaryRestriction.class);
        p.bmrFormula = d.getEnum("bmrFormula", BmrFormula.class, BmrFormula.MIFFLIN_ST_JEOR);
        p.calorieOverride = d.getIntOrNull("calorieOverride");
        p.theme = d.getEnum("theme", ThemePreference.class, ThemePreference.SYSTEM);
        p.onboardingStep = d.getInt("onboardingStep", 0);
        p.completed = d.getBool("completed", false);
        p.updatedAt = d.getInstant("updatedAt");
        return p;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public Sex getSex() { return sex; }
    public void setSex(Sex sex) { this.sex = sex; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }
    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }
    public Double getBodyFatPercent() { return bodyFatPercent; }
    public void setBodyFatPercent(Double bodyFatPercent) { this.bodyFatPercent = bodyFatPercent; }
    public ActivityLevel getActivityLevel() { return activityLevel; }
    public void setActivityLevel(ActivityLevel activityLevel) { this.activityLevel = activityLevel; }
    public Goal getGoal() { return goal; }
    public void setGoal(Goal goal) { this.goal = goal; }
    public Double getTargetWeightKg() { return targetWeightKg; }
    public void setTargetWeightKg(Double targetWeightKg) { this.targetWeightKg = targetWeightKg; }
    public double getWeeklyChangeKg() { return weeklyChangeKg; }
    public void setWeeklyChangeKg(double weeklyChangeKg) { this.weeklyChangeKg = weeklyChangeKg; }
    public DietType getDietType() { return dietType; }
    public void setDietType(DietType dietType) { this.dietType = dietType; }
    public EnumSet<Allergen> getAllergens() { return EnumSet.copyOf(allergens.isEmpty() ? EnumSet.noneOf(Allergen.class) : allergens); }
    public void setAllergens(java.util.Set<Allergen> allergens) { this.allergens = allergens.isEmpty() ? EnumSet.noneOf(Allergen.class) : EnumSet.copyOf(allergens); }
    public EnumSet<DietaryRestriction> getRestrictions() { return restrictions.isEmpty() ? EnumSet.noneOf(DietaryRestriction.class) : EnumSet.copyOf(restrictions); }
    public void setRestrictions(java.util.Set<DietaryRestriction> restrictions) { this.restrictions = restrictions.isEmpty() ? EnumSet.noneOf(DietaryRestriction.class) : EnumSet.copyOf(restrictions); }
    public BmrFormula getBmrFormula() { return bmrFormula; }
    public void setBmrFormula(BmrFormula bmrFormula) { this.bmrFormula = bmrFormula; }
    public Integer getCalorieOverride() { return calorieOverride; }
    public void setCalorieOverride(Integer calorieOverride) { this.calorieOverride = calorieOverride; }
    public ThemePreference getTheme() { return theme; }
    public void setTheme(ThemePreference theme) { this.theme = theme; }
    public int getOnboardingStep() { return onboardingStep; }
    public void setOnboardingStep(int onboardingStep) { this.onboardingStep = onboardingStep; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
