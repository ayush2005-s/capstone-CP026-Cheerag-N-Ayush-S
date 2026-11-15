package com.nutriplanner;

import com.nutriplanner.config.AppConfig;
import com.nutriplanner.domain.User;
import com.nutriplanner.repository.Repositories;
import com.nutriplanner.security.PasswordHasher;
import com.nutriplanner.seed.DataSeeder;
import com.nutriplanner.service.Services;
import com.nutriplanner.storage.Database;
import com.nutriplanner.web.Web;
import com.nutriplanner.web.controller.AdminController;
import com.nutriplanner.web.controller.AuthController;
import com.nutriplanner.web.controller.CalculatorController;
import com.nutriplanner.web.controller.DashboardController;
import com.nutriplanner.web.controller.DiaryController;
import com.nutriplanner.web.controller.DietPlanController;
import com.nutriplanner.web.controller.FoodController;
import com.nutriplanner.web.controller.PlannerController;
import com.nutriplanner.web.controller.ProfileController;
import com.nutriplanner.web.controller.ProgressController;
import com.nutriplanner.web.controller.PublicController;
import com.nutriplanner.web.controller.SettingsController;
import com.nutriplanner.web.http.HttpAuth;
import com.nutriplanner.web.http.RateLimiter;
import com.nutriplanner.web.http.Router;
import com.nutriplanner.web.http.SessionManager;
import com.nutriplanner.web.http.WebServer;
import com.nutriplanner.web.view.ErrorPage;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.logging.Logger;

/** Starts Nutri Planner: opens the database, seeds first-run data and serves the website. */
public final class NutriPlannerApplication implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(NutriPlannerApplication.class.getName());

    private final AppConfig config;
    private final Database database;
    private final Services services;
    private final WebServer server;
    private final SessionManager sessions;

    public NutriPlannerApplication(AppConfig config, Clock clock) {
        this(config, clock, Database.open(config.databaseFile()));
    }

    public NutriPlannerApplication(AppConfig config, Clock clock, Database database) {
        this.config = config;
        this.database = database;
        Repositories repos = new Repositories(database);
        this.services = new Services(repos, clock, new PasswordHasher(Math.max(1000, config.pbkdf2Iterations)));
        new DataSeeder(services).seed(config.adminInitialPassword, config.demoData);

        this.sessions = new SessionManager(clock, Duration.ofMinutes(config.sessionIdleMinutes));
        RateLimiter loginLimiter = new RateLimiter(20, Duration.ofMinutes(15), clock);
        Web web = new Web(services, config, new HttpAuth(sessions), loginLimiter);

        Router router = new Router();
        new PublicController(web).register(router);
        new AuthController(web).register(router);
        new DashboardController(web).register(router);
        new DiaryController(web).register(router);
        new FoodController(web).register(router);
        new PlannerController(web).register(router);
        new DietPlanController(web).register(router);
        new ProgressController(web).register(router);
        new CalculatorController(web).register(router);
        new ProfileController(web).register(router);
        new SettingsController(web).register(router);
        new AdminController(web).register(router);

        WebServer.Accounts accounts = new WebServer.Accounts() {
            @Override
            public User user(long id) {
                return repos.users.findById(id).orElse(null);
            }

            @Override
            public boolean onboardingComplete(long userId) {
                return services.profiles.profile(userId).isCompleted();
            }
        };
        this.server = new WebServer(router, sessions, accounts, new ErrorPage(web),
                new WebServer.Settings(config.host, config.port, config.threads, config.secureCookies, config.keystore, config.keystorePassword),
                loginLimiter);
    }

    public void start() throws IOException {
        server.start();
    }

    public int port() {
        return server.port();
    }

    public Services services() {
        return services;
    }

    public SessionManager sessions() {
        return sessions;
    }

    @Override
    public void close() {
        server.stop();
        database.close();
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %3$s: %5$s%6$s%n");
        AppConfig config = AppConfig.load();
        NutriPlannerApplication app = new NutriPlannerApplication(config, Clock.system(config.zone));
        app.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down Nutri Planner");
            app.close();
        }, "shutdown"));
        String scheme = config.https() ? "https" : "http";
        String host = config.host.equals("0.0.0.0") ? "localhost" : config.host;
        System.out.println();
        System.out.println("  Nutri Planner is running at " + scheme + "://" + host + ":" + app.port() + "/");
        System.out.println("  Data folder: " + config.dataDir.toAbsolutePath());
        System.out.println("  First admin login: " + DataSeeder.ADMIN_USERNAME + " / " + (config.adminInitialPassword.equals("Planner#Start1")
                ? "Planner#Start1 (you'll be asked to change it)" : "the password you configured"));
        if (config.demoData) System.out.println("  Demo member: " + DataSeeder.DEMO_USERNAME + " / " + DataSeeder.DEMO_PASSWORD);
        System.out.println("  Press Ctrl+C to stop.");
        System.out.println();
    }
}
