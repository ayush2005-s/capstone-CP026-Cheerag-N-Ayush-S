# Nutri Planner

A nutrition and diet-planning website written entirely in Java. It works out your calories and macros from your body
and goal, plans a week of meals that respect your diet and allergies, keeps a food diary that adds up as you eat, and
tracks your weight towards a goal. Administrators manage members and the shared food database.

- **Language:** Java only (Java 17+). There are no HTML, CSS, JavaScript, SQL or template files in this project.
- **Dependencies:** a JDK, plus one library that ships in `lib/` (the H2 storage engine, itself pure Java).
- **Tests:** 45 automated tests, including end-to-end tests that drive the real web server over HTTP.

---

## Quick start

You need **JDK 17 or newer** (`java -version` to check). No Maven, Gradle or Node is required.

```
cd nutri-planner
java Build.java          # compile, run all tests, build dist/nutri-planner.jar
java -jar dist/nutri-planner.jar
```

Then open **http://localhost:8080**.

Other build commands:

| Command | What it does |
|---|---|
| `java Build.java` | Compile, run the tests, package `dist/nutri-planner.jar` |
| `java Build.java run` | Package (skipping tests) and start the server |
| `java Build.java test` | Compile and run the test suite |
| `java Build.java package` | Build the jar without running tests |
| `java Build.java clean` | Delete `build/` and `dist/` |

`Build.java` is itself a Java program (run with the JDK's single-file launcher), so even the build tooling is Java.
A prebuilt `nutri-planner.jar` is included in the delivery: `java -jar nutri-planner.jar` runs it directly.

### Accounts created on first run

| Account | Username | Password | Notes |
|---|---|---|---|
| Administrator | `admin` | `Planner#Start1` | You must choose a new password at first login. |
| Demo member | `demo` | `Healthy#2026` | Eight weeks of weigh-ins, three weeks of diary entries and a planned week. |

Turn the demo account off with `-Dnutri.demo=false`. Set a different first admin password with
`-Dnutri.admin.password=...`. Both only apply when the database is first created.

---

## Features

**Accounts and security**
- Registration with validation, login by username or email, logout, automatic logout after 30 idle minutes.
- Passwords hashed with salted PBKDF2-HMAC-SHA256 (210,000 iterations), upgraded automatically if the work factor rises.
- Account lockout for 15 minutes after 5 failed logins, plus a per-address limit of 20 attempts per 15 minutes.
- Role-based access (member and administrator), enforced in the web layer *and* again in the service layer.
- CSRF tokens on every form, same-origin checks, session-id rotation at login, HttpOnly + SameSite cookies.
- Strict Content-Security-Policy (no scripts allowed at all), clickjacking and MIME-sniffing protection.
- All output is escaped, so user input can never inject markup.
- Audit log of logins, failed attempts, lockouts, password changes and every admin action.
- Members can delete their own account and every record attached to it.

**Personal profile and calculators**
- Onboarding in three steps: body measurements, goal and activity, diet and allergies.
- **BMI** with WHO categories, healthy weight range and a visual scale.
- **BMR** with Mifflin–St Jeor (default), revised Harris–Benedict, or Katch–McArdle (when body fat is known).
- **TDEE** with the five standard activity factors (1.2 to 1.9).
- **Daily calorie target** for losing weight, maintaining, gaining weight or building muscle, at a chosen weekly pace.
  Deficits are capped at 25% of maintenance and never below 1,200 kcal (women) or 1,500 kcal (men).
- **Protein, carbohydrate, fat, fibre and water targets.**
- Calculators work in metric or imperial units and are open to visitors without an account.
- Optional custom calorie target (for example one set by a dietitian).

**Diet types, allergies and restrictions**
- Vegetarian, vegan, eggetarian and non-vegetarian diets.
- Nine allergens: gluten, dairy, egg, peanut, tree nuts, soy, fish, shellfish and sesame.
- Low-sodium, low-sugar, low-carb and low-fat restrictions (the last two also change the macro split).
- Every food is checked against your profile and shows why it doesn't fit, if it doesn't.

**Food database**
- 153 foods with nutrition per 100 g and a typical serving, with a strong selection of Indian dishes
  (dal, roti, idli, dosa, poha, paneer, rajma, chole, sambar and more) alongside international staples.
- Search, filter by category, diet and meal, sort by calories, protein or protein density, per serving or per 100 g.
- Indian-style veg / non-veg marks on every food.
- Members can add private custom foods; nutrition is validated (calories must match the macros).

**Meal planning**
- Generates a day or a whole week of breakfast, lunch, dinner and snacks, portioned to your calorie and macro
  targets, using only foods that fit your diet, allergies and restrictions, and varying foods between days.
- Swap any dish for another that plays the same part on the plate, change portions, add or remove foods,
  get new ideas for one meal, and log a planned meal or a whole day to the diary in one step.
- A written **diet plan** page: targets, how they split across meals, the best protein, fibre and fat sources
  for you, what your plan avoids, and guidance specific to your goal and diet.

**Food diary and tracking**
- Log foods by servings or grams for any meal of any past day; edit amounts, move entries between meals, delete.
- Running totals against targets for calories, protein, carbs, fat and fibre, with sugar and sodium.
- Copy a whole previous day, water tracking in glasses, and a logging streak.
- Weight log with trend per week (least squares over eight weeks), progress to goal and a projected goal date.
- Charts: the day as a plate, calories per day against target, weight over time, macro energy split, BMI scale.

**Dashboard** – today's plate, macros, the week's calories, weight trend, water, today's planned meals with one-tap
logging, and personal suggestions (for example protein-rich foods that fit your diet when you're short).

**Admin panel** – usage overview with charts, member search and management (disable, re-enable, unlock, promote,
reset password with a one-time temporary password, delete with confirmation), the shared food database
(add, edit, hide, delete) and the audit log. The last administrator can never be removed or disabled.

**Design** – a calm, food-inspired palette (spinach-green ink, jade for actions, and one food colour per macronutrient:
beetroot for protein, turmeric for carbs, blueberry for fat, whole-grain brown for fibre), Bricolage Grotesque for
headings and numbers with Inter for text, light and dark themes, and a responsive layout down to phone size with
a slide-out menu. Pages work with JavaScript turned off, use visible keyboard focus and respect reduced motion.

---

## How "Java only" works for a website

Browsers can only display HTML and CSS, so a website has to send them. In Nutri Planner that output is *generated by
Java code at runtime*; nothing is hand-written in those languages:

- Pages are trees of Java objects (`Element`, `Ui.panel(...)`, `Forms.number(...)`, `Charts.plate(...)`) that are
  rendered with automatic escaping (`web/html`, `web/ui`).
- The stylesheet is built by a Java `StyleSheet` builder from design tokens (`web/style/AppStyles.java`) and served
  from `/assets/app.css`.
- Charts and icons are vector drawings built with Java calls (`web/ui/Charts.java`, `web/ui/Icon.java`).
- There is **no JavaScript**, not even generated: interactive pieces like the mobile menu, confirmation dialogs and
  inline editing use plain form and link behaviour.
- There is **no SQL**: data is stored with H2's MVStore engine through a Java API (`storage/`).
- The build script (`Build.java`) and the test framework (`src/test/.../testkit`) are Java too.

The only non-Java files are data and documentation: two font files with their licences, the H2 library jar,
this README, `pom.xml` (optional, for IDE import) and an example settings file.

---

## Configuration

Settings are read from, in order: JVM system properties (`-Dnutri.port=9090`), environment variables
(`NUTRI_PORT=9090`), a `nutriplanner.properties` file in the working directory or data directory, then defaults.
See `nutriplanner.properties.example`.

| Setting | Default | Meaning |
|---|---|---|
| `nutri.port` | `8080` | HTTP port |
| `nutri.host` | `127.0.0.1` | Address to listen on. Use `0.0.0.0` to allow other devices on your network. |
| `nutri.data` | `~/.nutriplanner` | Folder for the database file |
| `nutri.zone` | system zone | Time zone used for "today", e.g. `Asia/Kolkata` |
| `nutri.demo` | `true` | Create the demo member on first run |
| `nutri.demo.hint` | same as `nutri.demo` | Show the demo login on the login page |
| `nutri.admin.password` | `Planner#Start1` | Password of the first administrator (changed at first login) |
| `nutri.session.idle.minutes` | `30` | Automatic logout after inactivity |
| `nutri.pbkdf2.iterations` | `210000` | Password hashing work factor |
| `nutri.threads` | `16` | Request-handling threads |
| `nutri.https.keystore` | none | PKCS#12 keystore; when set, the server speaks HTTPS and sets HSTS and Secure cookies |
| `nutri.https.password` | none | Keystore password |

To serve over HTTPS, create a keystore with the JDK's `keytool`, for example
`keytool -genkeypair -alias nutri -keyalg EC -storetype PKCS12 -keystore nutri.p12 -dname CN=localhost`,
then run with `-Dnutri.https.keystore=nutri.p12 -Dnutri.https.password=...`.

To back up, stop the server and copy `nutriplanner.mv.db` from the data folder. To start over, delete it.

---

## Architecture

```
 Browser ──HTTP──► WebServer (JDK HttpServer)
                     │  session · CSRF · access rules · security headers · gzip
                     ▼
                   Router ──► Controllers (web/controller)
                                  │ build pages from              ▲
                                  │ Ui / Forms / Charts           │ Response (generated page)
                                  ▼                               │
                               Services (service)  ── rules, calculations, meal planning, authorisation
                                  ▼
                               Repositories (repository)
                                  ▼
                               EntityStore + Doc codec (storage) ──► H2 MVStore file
```

```
src/main/java/com/nutriplanner
├── NutriPlannerApplication.java   start-up and wiring
├── config/        AppConfig (settings)
├── domain/        entities (User, Profile, Food, FoodLogEntry, WeightEntry, MealPlanDay, ...) and enums
├── storage/       Database (transactions), EntityStore (ids + secondary indexes), Doc / DocCodec
├── repository/    one repository per entity, with index-backed queries
├── security/      PasswordHasher (PBKDF2), PasswordPolicy, AccessDeniedException
├── service/       NutritionCalculator, AuthService, ProfileService, FoodService, DiaryService,
│                  WeightService, WaterService, MealPlanGenerator, MealPlanService, DietGuideService,
│                  DashboardService, AccountService, AdminService, AuditService
├── seed/          FoodCatalog (153 foods), DataSeeder (first run)
└── web/
    ├── http/      WebServer, Router, Request, Response, SessionManager, RateLimiter, HttpAuth
    ├── html/      Element, Text, Fragment, Escape, Svg
    ├── style/     Theme tokens, StyleSheet builder, AppStyles
    ├── ui/        Ui components, Forms, FormState, Charts, Icon, Fmt
    ├── view/      Layout, ProfileForms, FoodForm, FoodPicker, ErrorPage
    └── controller/ Public, Auth, Dashboard, Diary, Food, Planner, DietPlan, Progress,
                    Calculator, Profile, Settings, Admin
```

### Database design

The database is a single file managed by H2's **MVStore**, an embedded, append-only, crash-safe storage engine.
Every write runs inside `Database.write(...)`: changes commit atomically when it returns and roll back if it throws.
Records are stored as self-describing binary documents, so fields can be added later without migrations.

| Collection | Key fields | Indexes |
|---|---|---|
| `users` | username, email, password hash, role, status, failed logins, lock time | unique `username`, unique `email` (case-insensitive) |
| `profiles` | user id, sex, birth date, height, weight, body fat, activity, goal, target, pace, diet, allergens, restrictions, formula, theme | unique `user` |
| `foods` | name, category, diet, allergens, meal types, planner role, nutrients per 100 g, serving, owner (0 = shared), visible | `owner` |
| `food_log` | user, date, meal, food id, food name and nutrient snapshot, quantity, unit, grams | `user + date` (range queries by day) |
| `weights` | user, date, kg, note | unique `user + date` |
| `meal_plans` | user, date, list of (meal, food, servings) | unique `user + date` |
| `water` | user, date, glasses | unique `user + date` |
| `audit` | time, event type, account, detail, IP address | (chronological by id; capped at 20,000 events) |

Diary entries keep a snapshot of the food's name and nutrients, so history stays correct if a food is later edited
or deleted. Deleting an account removes every record that belongs to it in one transaction.

---

## The nutrition science

| Calculation | Method |
|---|---|
| BMI | weight (kg) / height (m)², WHO adult categories; healthy range = BMI 18.5–24.9 |
| BMR | Mifflin–St Jeor: 10·kg + 6.25·cm − 5·age + 5 (men) or − 161 (women). Revised Harris–Benedict (Roza & Shizgal, 1984). Katch–McArdle: 370 + 21.6 · lean mass |
| TDEE | BMR × 1.2 / 1.375 / 1.55 / 1.725 / 1.9 |
| Weight change | 7,700 kcal per kg. Losing: deficit = pace × 7,700 / 7, capped at 25% of TDEE, floor 1,200 / 1,500 kcal. Gaining: matching surplus. Building muscle: +10% of TDEE (200–400 kcal) |
| Protein | 1.6 g/kg losing, 1.2 maintaining, 1.4 gaining, 2.0 building muscle; uses the weight at BMI 25 when BMI is above 25; at most 35% of calories |
| Fat | 27% losing, 30% maintaining/gaining, 25% building muscle; 20% on low-fat |
| Carbohydrate | the remaining calories; at most 26% of calories on low-carb |
| Fibre | 14 g per 1,000 kcal |
| Water | 35 ml per kg plus 0–1,000 ml for activity |
| Weight trend | least-squares slope of the last eight weeks of weigh-ins |

Food values are per 100 g as eaten, from USDA FoodData Central and the Indian Food Composition Tables (2017),
with typical home recipes for Indian dishes. Nutri Planner gives general guidance, not medical advice.

### How meal plans are generated

Each meal has a plate template: breakfast = staple + protein + optional fruit; lunch = staple + protein + vegetable
+ optional curd or buttermilk; dinner = staple + protein + vegetable; snacks = snack + optional fruit. For each
meal the generator makes 36 attempts: it picks one suitable food per slot (preferring foods not eaten the day
before), then searches portions in half-serving steps to minimise the weighted error against the meal's share of
calories (25 / 35 / 30 / 10%) and macros. The best attempt wins. Only foods that already passed the diet, allergy and
restriction checks are considered, and the tests verify each generated day lands within 12% of the calorie target
for every diet type and several allergy combinations.

---

## Tests

`java Build.java test` runs 45 tests with a small built-in test runner (no external test library):

- **NutritionCalculatorTest** – BMI, all three BMR equations, TDEE, calorie targets and safety limits, macro splits.
- **DatabaseTest** – document encoding, unique indexes, rollback, range queries, persistence across restarts.
- **AuthServiceTest** – registration rules, login, lockout and expiry, disabled accounts, password changes.
- **FoodServiceTest** – every catalogue food passes validation, search and filters, suitability, private custom foods.
- **DiaryAndWeightTest** – servings vs grams, totals, ownership checks, copying days, streaks, weight trend and projection.
- **MealPlanServiceTest** – plans for every diet and allergy combination respect restrictions and hit targets; swaps, edits, logging.
- **AdminServiceTest** – members can't call admin functions, last-admin protection, password reset, cascading delete.
- **WebFlowTest** – starts the real server and drives it over HTTP: security headers, sign-up to onboarding to
  diary, CSRF and origin checks, access control, inline validation errors, XSS escaping, the admin flow,
  account lockout, planner actions, custom foods, profile updates, themes and logout.

---

## Using an IDE or Maven

`pom.xml` is provided so the project opens directly in IntelliJ IDEA, Eclipse or VS Code. `mvn package` builds a runnable
jar at `target/nutri-planner.jar` (Maven downloads H2 from Maven Central). The verified build path is `java Build.java`;
the tests use the project's own runner, so run them with `java Build.java test`.

In an IDE, run `com.nutriplanner.NutriPlannerApplication` with the working directory set to the project folder.

---

## Licences and credits

- Inter (Rasmus Andersson) and Bricolage Grotesque (Mathieu Triay): SIL Open Font License 1.1, licence texts in
  `src/main/resources/assets/fonts/`.
- H2 Database Engine (MVStore): dual-licensed MPL 2.0 / EPL 1.0.
- Icon shapes follow the style of the Feather icon set (ISC licence).
- Nutrition data: USDA FoodData Central (public domain) and the Indian Food Composition Tables, NIN 2017.
