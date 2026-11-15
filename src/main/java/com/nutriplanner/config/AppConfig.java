package com.nutriplanner.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Properties;

/**
 * Runtime settings. Each setting is read, in order of precedence, from a JVM system property
 * ({@code -Dnutri.port=9090}), an environment variable ({@code NUTRI_PORT=9090}), a {@code nutriplanner.properties}
 * file in the working directory or data directory, and finally a built-in default.
 */
public final class AppConfig {

    public final String host;
    public final int port;
    public final Path dataDir;
    public final ZoneId zone;
    public final String adminInitialPassword;
    public final boolean demoData;
    public final boolean showDemoLogin;
    public final int pbkdf2Iterations;
    public final int sessionIdleMinutes;
    public final int threads;
    public final Path keystore;
    public final String keystorePassword;
    public final boolean secureCookies;

    private final Properties file;
    private final boolean fileWins;

    private AppConfig(Properties file, boolean fileWins) {
        this.file = file;
        this.fileWins = fileWins;
        this.host = get("nutri.host", "127.0.0.1");
        this.port = getInt("nutri.port", 8080);
        this.dataDir = Paths.get(get("nutri.data", Paths.get(System.getProperty("user.home"), ".nutriplanner").toString()));
        String zoneName = get("nutri.zone", "");
        this.zone = zoneName.isBlank() ? ZoneId.systemDefault() : ZoneId.of(zoneName);
        this.adminInitialPassword = get("nutri.admin.password", "Planner#Start1");
        this.demoData = getBool("nutri.demo", true);
        this.showDemoLogin = getBool("nutri.demo.hint", demoData);
        this.pbkdf2Iterations = getInt("nutri.pbkdf2.iterations", 210_000);
        this.sessionIdleMinutes = getInt("nutri.session.idle.minutes", 30);
        this.threads = getInt("nutri.threads", 16);
        String ks = get("nutri.https.keystore", "");
        this.keystore = ks.isBlank() ? null : Paths.get(ks);
        this.keystorePassword = get("nutri.https.password", "");
        this.secureCookies = getBool("nutri.cookies.secure", keystore != null);
    }

    public static AppConfig load() {
        Properties props = new Properties();
        loadInto(props, Paths.get("nutriplanner.properties"));
        String data = System.getProperty("nutri.data", System.getenv("NUTRI_DATA"));
        Path dataDir = data == null ? Paths.get(System.getProperty("user.home"), ".nutriplanner") : Paths.get(data);
        loadInto(props, dataDir.resolve("nutriplanner.properties"));
        return new AppConfig(props, false);
    }

    /** Configuration for tests: a private data directory and the given overrides. */
    public static AppConfig forTesting(Path dataDir, Properties overrides) {
        Properties props = new Properties();
        props.setProperty("nutri.data", dataDir.toString());
        props.setProperty("nutri.port", "0");
        props.setProperty("nutri.demo", "false");
        props.setProperty("nutri.pbkdf2.iterations", "1000");
        props.putAll(overrides);
        return new AppConfig(props, true);
    }

    public boolean https() {
        return keystore != null;
    }

    public Path databaseFile() {
        return dataDir.resolve("nutriplanner.mv.db");
    }

    private static void loadInto(Properties props, Path file) {
        if (!Files.isRegularFile(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            Properties loaded = new Properties();
            loaded.load(in);
            props.putAll(loaded);
        } catch (IOException e) {
            System.err.println("[config] Could not read " + file + ": " + e.getMessage());
        }
    }

    private String get(String key, String fallback) {
        if (fileWins && file.containsKey(key)) return file.getProperty(key);
        String sys = System.getProperty(key);
        if (sys != null) return sys;
        String env = System.getenv(key.toUpperCase(Locale.ROOT).replace('.', '_'));
        if (env != null) return env;
        return file.getProperty(key, fallback);
    }

    private int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean getBool(String key, boolean fallback) {
        String v = get(key, Boolean.toString(fallback)).trim().toLowerCase(Locale.ROOT);
        return v.equals("true") || v.equals("yes") || v.equals("1");
    }
}
