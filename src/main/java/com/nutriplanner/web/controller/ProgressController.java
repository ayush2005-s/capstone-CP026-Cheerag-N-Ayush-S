package com.nutriplanner.web.controller;

import com.nutriplanner.domain.Nutrients;
import com.nutriplanner.domain.Profile;
import com.nutriplanner.domain.WeightEntry;
import com.nutriplanner.service.NutritionCalculator;
import com.nutriplanner.service.NutritionTargets;
import com.nutriplanner.service.ValidationException;
import com.nutriplanner.service.WeightService;
import com.nutriplanner.web.Params;
import com.nutriplanner.web.ViewContext;
import com.nutriplanner.web.Web;
import com.nutriplanner.web.html.Element;
import com.nutriplanner.web.html.Html;
import com.nutriplanner.web.html.Node;
import com.nutriplanner.web.http.Request;
import com.nutriplanner.web.http.Response;
import com.nutriplanner.web.http.Router;
import com.nutriplanner.web.ui.Charts;
import com.nutriplanner.web.ui.FormState;
import com.nutriplanner.web.ui.Forms;
import com.nutriplanner.web.ui.Fmt;
import com.nutriplanner.web.ui.Icon;
import com.nutriplanner.web.ui.Ui;
import com.nutriplanner.web.view.Layout;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.nutriplanner.web.http.Router.Access.MEMBER;

/** Weight log, trend and goal projection, plus how intake has tracked against targets. */
public final class ProgressController {

    private final Web web;

    public ProgressController(Web web) {
        this.web = web;
    }

    public void register(Router r) {
        r.get("/progress", MEMBER, req -> page(req, FormState.empty(), 200));
        r.post("/progress/weight", MEMBER, this::log);
        r.post("/progress/weight/{id}/delete", MEMBER, this::delete);
    }

    private Response page(Request req, FormState st, int status) {
        ViewContext ctx = web.context(req);
        Profile profile = ctx.profile();
        NutritionTargets t = web.services.profiles.targets(ctx.userId()).orElseThrow();
        WeightService.WeightStats s = web.services.weights.stats(ctx.userId(), profile);
        List<WeightEntry> all = web.services.weights.history(ctx.userId());
        String range = new Params(req).text("range");
        if (!List.of("30", "90", "365", "all").contains(range)) range = "90";
        List<WeightEntry> shown = new ArrayList<>();
        LocalDate from = range.equals("all") ? LocalDate.MIN : ctx.today().minusDays(Long.parseLong(range));
        for (WeightEntry e : all) if (!e.getDate().isBefore(from)) shown.add(e);

        Element stats = Html.div("stats",
                Ui.stat("Starting weight", s.startKg() == null ? "–" : Fmt.one(s.startKg()), "kg", s.startDate() == null ? null : Fmt.mediumDate(s.startDate())),
                Ui.stat("Current weight", s.currentKg() == null ? "–" : Fmt.one(s.currentKg()), "kg", s.currentDate() == null ? null : Fmt.relative(s.currentDate(), ctx.today())),
                Ui.stat("Total change", s.totalChangeKg() == null ? "–" : Fmt.signed(s.totalChangeKg()), "kg", s.entries() + " weigh-ins"),
                Ui.stat("Weekly trend", s.weeklyTrendKg() == null ? "–" : Fmt.signed(s.weeklyTrendKg()), "kg", "Last eight weeks"),
                s.targetKg() == null ? Ui.stat("Goal", "Maintain", null, profile.getGoal().label())
                        : Ui.stat("To go", s.toGoKg() == null ? "–" : s.toGoKg() < 0.05 ? "Reached" : Fmt.one(s.toGoKg()),
                        s.toGoKg() != null && s.toGoKg() >= 0.05 ? "kg" : null, "Goal " + Fmt.one(s.targetKg()) + " kg"),
                Ui.stat("BMI", Fmt.one(t.bmi()), null, t.bmiCategory().label()));

        Node chart = shown.size() < 2
                ? Ui.empty(Icon.TREND, "Not enough weigh-ins for a chart", "Log your weight on two or more days in this range to see the line.", null)
                : Charts.weightLine(shown, s.targetKg());
        String finalRange = range;
        Element ranges = Ui.segmented(List.of(tabFor("30", "30 days", finalRange), tabFor("90", "90 days", finalRange),
                tabFor("365", "1 year", finalRange), tabFor("all", "All", finalRange)), "Range");
        Element weightPanel = Ui.panel("Weight", projection(s), ranges, chart);
        if (s.targetKg() != null && s.progressPercent() != null) {
            weightPanel.add(Html.div("macro mt-3", Html.div("macro-top", Html.span("macro-name", "Progress to goal"),
                    Html.span("macro-val", Math.round(s.progressPercent()) + "%")), Ui.meter(s.progressPercent(), 100, null)));
        }

        Map<LocalDate, Nutrients> days = web.services.diary.dailyTotals(ctx.userId(), ctx.today().minusDays(13), ctx.today());
        Map<LocalDate, Double> kcal = new LinkedHashMap<>();
        double sumK = 0, sumP = 0;
        int logged = 0;
        for (Map.Entry<LocalDate, Nutrients> e : days.entrySet()) {
            kcal.put(e.getKey(), e.getValue().kcal());
            if (e.getValue().kcal() > 0) {
                logged++;
                sumK += e.getValue().kcal();
                sumP += e.getValue().protein();
            }
        }
        Element intake = Ui.panel("Calories against target", "The last 14 days", null, Charts.dailyBars(kcal, t.calories(), ctx.today()),
                Html.div("stats mt-3",
                        Ui.stat("Days logged", Integer.toString(logged), "of 14", null),
                        Ui.stat("Average intake", logged == 0 ? "–" : Fmt.kcal(sumK / logged), "kcal", "Target " + Fmt.kcal(t.calories())),
                        Ui.stat("Average protein", logged == 0 ? "–" : Fmt.amount(sumP / logged), "g", "Target " + t.proteinG() + " g")));

        st.setIfAbsent("date", ctx.today().toString());
        if (s.currentKg() != null) st.setIfAbsent("weightKg", s.currentKg());
        Element form = Forms.post("/progress/weight", ctx.csrf()).cls("form").add(Forms.formMessage(st),
                Html.div("form-grid",
                        Forms.date(st, "date", "Date", null, true, ctx.today().minusYears(10).toString(), ctx.today().toString()),
                        Forms.number(st, "weightKg", "Weight", "kg", "0.1", "25", "350", null, true)),
                Forms.text(st, "note", "Note", "Optional, like “after breakfast”.", false),
                Forms.submit("Save weigh-in", "btn-primary", Icon.CHECK));
        Element logPanel = Ui.panel("Log your weight", "One entry per day. Saving again on the same day replaces it.", null, form);
        logPanel.id("log");

        Element history;
        if (all.isEmpty()) {
            history = Html.p("muted", "Your weigh-ins will appear here.");
        } else {
            List<WeightEntry> newest = new ArrayList<>(all);
            Collections.reverse(newest);
            Ui.Table table = new Ui.Table().caption("Weigh-ins").col("Date", false).col("Weight", true).col("Change", true).col("", false);
            for (int i = 0; i < Math.min(newest.size(), 15); i++) {
                WeightEntry e = newest.get(i);
                Double change = i + 1 < newest.size() ? e.getWeightKg() - newest.get(i + 1).getWeightKg() : null;
                Element date = Html.div(null, Html.text(Fmt.mediumDate(e.getDate())));
                if (!e.getNote().isBlank()) date.add(Html.span("cell-sub", e.getNote()));
                table.row(date, Html.text(Fmt.one(e.getWeightKg()) + " kg"),
                        Html.span(change == null || Math.abs(change) < 0.05 ? "muted" : change < 0 ? "good" : null, change == null ? "–" : Fmt.signed(change)),
                        Html.div("text-right", Forms.post("/progress/weight/" + e.getId() + "/delete", ctx.csrf(), "/progress").cls("inline")
                                .add(Ui.iconButton(Icon.TRASH, "Delete weigh-in of " + Fmt.mediumDate(e.getDate()), "btn-danger-ghost"))));
            }
            history = table.build();
        }
        return Layout.app(ctx, status, "Progress", Layout.Nav.PROGRESS,
                Ui.pageHead("Progress", "Weigh in at the same time of day, ideally in the morning, for the clearest trend."),
                Html.div("stack", stats,
                        Html.div("grid g-main",
                                Html.div("stack", weightPanel, intake),
                                Html.div("stack", logPanel, Ui.panel("Recent weigh-ins", null, null, history)))));
    }

    private static Ui.Tab tabFor(String value, String label, String current) {
        return new Ui.Tab("/progress?range=" + value, label, value.equals(current));
    }

    private static String projection(WeightService.WeightStats s) {
        if (!s.hasData()) return "No weigh-ins yet";
        if (s.targetKg() == null) return "You're maintaining. Aim to stay within a kilo or so of " + Fmt.one(s.currentKg()) + " kg.";
        if (s.toGoKg() != null && s.toGoKg() < 0.05) return "You've reached your goal of " + Fmt.one(s.targetKg()) + " kg.";
        if (s.projectedDate() != null) return "At your current pace you'll reach " + Fmt.one(s.targetKg()) + " kg around " + Fmt.mediumDate(s.projectedDate()) + ".";
        if (s.weeklyTrendKg() == null) return "Keep logging for a week or two and a projection will appear.";
        return "The trend isn't heading towards " + Fmt.one(s.targetKg()) + " kg yet. Check your diary against your targets.";
    }

    private Response log(Request req) {
        Params p = new Params(req);
        try {
            LocalDate date = p.date("date");
            Double weight = p.decimal("weightKg");
            if (p.hasErrors()) throw new ValidationException(p.errors());
            WeightEntry e = web.services.weights.log(req.user().getId(), date, weight, p.text("note"));
            Profile profile = web.services.profiles.profile(req.user().getId());
            String bmi = profile.getHeightCm() > 0 ? " Your BMI is now " + Fmt.one(NutritionCalculator.bmi(profile.getWeightKg(), profile.getHeightCm())) + "." : "";
            req.session().success("Saved " + Fmt.one(e.getWeightKg()) + " kg for " + Fmt.mediumDate(e.getDate()) + "." + bmi);
            return Response.redirect("/progress");
        } catch (ValidationException e) {
            return page(req, FormState.from(req).errors(e.fieldErrors()).message(e.fieldErrors().isEmpty() ? e.getMessage() : null), 422);
        }
    }

    private Response delete(Request req) {
        web.services.weights.delete(req.user().getId(), req.pathId("id"));
        req.session().success("Deleted the weigh-in.");
        return Response.redirect("/progress");
    }
}
