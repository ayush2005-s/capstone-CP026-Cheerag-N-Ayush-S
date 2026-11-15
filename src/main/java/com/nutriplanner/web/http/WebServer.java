package com.nutriplanner.web.http;

import com.nutriplanner.domain.User;
import com.nutriplanner.security.AccessDeniedException;
import com.nutriplanner.service.NotFoundException;
import com.nutriplanner.service.ValidationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * The HTTP front door, built on the JDK's own HTTP server. For every request it loads the session, checks the CSRF
 * token on form posts, enforces the route's access rule, runs the handler, converts errors into friendly pages, and
 * adds security headers to every response.
 */
public final class WebServer {

    private static final Logger LOG = Logger.getLogger(WebServer.class.getName());

    public static final String CSRF_FIELD = "_csrf";
    public static final String BACK_FIELD = "_back";

    /** Renders an error page in the site's look. */
    public interface ErrorPages {
        Response render(Request request, int status, String title, String message);
    }

    /** Looks up accounts for the session. */
    public interface Accounts {
        User user(long id);

        boolean onboardingComplete(long userId);
    }

    /** Where the server listens and how cookies are protected. */
    public record Settings(String host, int port, int threads, boolean secureCookies, Path keystore, String keystorePassword) {
    }

    static final String CONTENT_SECURITY_POLICY = "default-src 'none'; style-src 'self'; img-src 'self' data:; "
            + "font-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'";

    private final Router router;
    private final SessionManager sessions;
    private final Accounts accounts;
    private final ErrorPages errors;
    private final Settings settings;
    private final String cookieName;
    private HttpServer server;
    private ExecutorService pool;
    private ScheduledExecutorService janitor;
    private final RateLimiter[] limiters;

    public WebServer(Router router, SessionManager sessions, Accounts accounts, ErrorPages errors, Settings settings,
                     RateLimiter... limiters) {
        this.router = router;
        this.sessions = sessions;
        this.accounts = accounts;
        this.errors = errors;
        this.settings = settings;
        this.cookieName = settings.secureCookies() ? "__Host-np_session" : "np_session";
        this.limiters = limiters;
    }

    public void start() throws IOException {
        InetSocketAddress address = new InetSocketAddress(settings.host(), settings.port());
        if (settings.keystore() != null) {
            HttpsServer https = HttpsServer.create(address, 0);
            https.setHttpsConfigurator(new HttpsConfigurator(sslContext(settings.keystore(), settings.keystorePassword())));
            server = https;
        } else {
            server = HttpServer.create(address, 0);
        }
        AtomicInteger counter = new AtomicInteger();
        pool = Executors.newFixedThreadPool(Math.max(2, settings.threads()), r -> {
            Thread t = new Thread(r, "http-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(pool);
        server.createContext("/", this::handle);
        server.start();
        janitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-janitor");
            t.setDaemon(true);
            return t;
        });
        janitor.scheduleAtFixedRate(() -> {
            sessions.purgeExpired();
            for (RateLimiter l : limiters) l.purge();
        }, 5, 5, TimeUnit.MINUTES);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) server.stop(1);
        if (pool != null) pool.shutdownNow();
        if (janitor != null) janitor.shutdownNow();
    }

    public String cookieName() {
        return cookieName;
    }

    // ---------------------------------------------------------------- dispatch

    private void handle(HttpExchange exchange) {
        Request request = null;
        Response response;
        try {
            request = Request.from(exchange);
            response = dispatch(request);
        } catch (Request.BodyTooLargeException e) {
            response = Response.text(413, "The submitted form is too large.");
        } catch (Throwable t) {
            String id = UUID.randomUUID().toString().substring(0, 8);
            LOG.log(Level.SEVERE, "Unhandled error " + id + " on " + exchange.getRequestURI(), t);
            response = request == null ? Response.text(500, "Something went wrong (error " + id + ").")
                    : safeError(request, 500, "Something went wrong",
                    "The server hit an unexpected problem. Try again, and if it keeps happening, mention error " + id + " to an administrator.");
        }
        write(exchange, request, response);
    }

    private Response dispatch(Request req) throws Exception {
        boolean stateless = req.path().startsWith("/assets/") || req.path().equals("/favicon.svg") || req.path().equals("/robots.txt");
        if (!stateless) attachSession(req);

        boolean[] wrongMethod = {false};
        Router.Match match = router.match(req.method(), req.path(), wrongMethod);
        if (match == null) {
            if (wrongMethod[0]) return Response.text(405, "Method not allowed").header("Allow", "GET, POST");
            return error(req, 404, "Page not found", "There's nothing at this address. Check the link, or use the menu to find your way.");
        }
        req.setPathVariables(match.variables());
        Router.Route route = match.route();

        if (req.isPost() && !csrfValid(req)) {
            return error(req, 403, "This form has expired",
                    "For your security, forms stop working after you've been away for a while. Go back, reload the page and try again.");
        }

        Response gate = checkAccess(req, route);
        if (gate != null) return gate;

        try {
            return route.handler().handle(req);
        } catch (NotFoundException e) {
            return error(req, 404, "Not found", e.getMessage());
        } catch (AccessDeniedException e) {
            return error(req, 403, "You don't have access to this", e.getMessage());
        } catch (ValidationException e) {
            if (!req.isPost()) return error(req, 400, "That didn't work", e.getMessage());
            req.session().error(e.getMessage());
            return Response.redirect(backTarget(req));
        }
    }

    private void attachSession(Request req) {
        Session session = sessions.find(req.cookie(cookieName));
        if (session == null) session = sessions.create();
        req.setSession(session);
        if (session.isAuthenticated()) {
            User user = accounts.user(session.userId());
            if (user == null || !user.isActive()) {
                sessions.invalidate(session);
                session = sessions.create();
                req.setSession(session);
                if (user != null) session.error("Your account has been disabled. Contact an administrator.");
            } else {
                req.setUser(user);
            }
        }
    }

    private Response checkAccess(Request req, Router.Route route) {
        User user = req.user();
        switch (route.access()) {
            case PUBLIC:
                return null;
            case GUEST:
                return user == null ? null : Response.redirect(user.isAdmin() && !accounts.onboardingComplete(user.getId()) ? "/admin" : "/dashboard");
            default:
                break;
        }
        if (user == null) {
            String next = req.isPost() ? "" : "?next=" + java.net.URLEncoder.encode(req.pathWithQuery(), StandardCharsets.UTF_8);
            req.session().info("Log in to continue.");
            return Response.redirect("/login" + next);
        }
        if (user.isMustChangePassword() && !route.allowedWhilePasswordChangeRequired()) {
            return Response.redirect("/settings/password");
        }
        if (route.access() == Router.Access.ADMIN && !user.isAdmin()) {
            return error(req, 403, "Administrators only", "This part of Nutri Planner is only for administrators.");
        }
        if (route.access() == Router.Access.MEMBER && !accounts.onboardingComplete(user.getId())) {
            return Response.redirect("/onboarding");
        }
        return null;
    }

    private boolean csrfValid(Request req) {
        String origin = req.header("Origin");
        String host = req.header("Host");
        if (origin != null && !origin.equals("null") && host != null) {
            try {
                URI o = URI.create(origin);
                String originHost = o.getPort() == -1 ? o.getHost() : o.getHost() + ":" + o.getPort();
                if (!host.equalsIgnoreCase(originHost)) return false;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        String sent = req.param(CSRF_FIELD);
        String expected = req.session() == null ? null : req.session().csrfToken();
        if (sent == null || expected == null) return false;
        return MessageDigest.isEqual(sent.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }

    /** Where to send the browser after a failed form action: the form's own page if it said so. */
    private static String backTarget(Request req) {
        String back = safePath(req.param(BACK_FIELD));
        if (back != null) return back;
        String referer = req.header("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                String host = req.header("Host");
                String refHost = uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
                if (host != null && host.equalsIgnoreCase(refHost)) {
                    String path = safePath(uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()));
                    if (path != null) return path;
                }
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return "/dashboard";
    }

    /** Accepts only local paths, so redirects can't be abused to send people to other sites. */
    public static String safePath(String path) {
        if (path == null || path.isEmpty() || !path.startsWith("/") || path.startsWith("//") || path.contains("\\")) return null;
        for (int i = 0; i < path.length(); i++) {
            if (Character.isISOControl(path.charAt(i))) return null;
        }
        return path;
    }

    private Response error(Request req, int status, String title, String message) {
        return safeError(req, status, title, message);
    }

    private Response safeError(Request req, int status, String title, String message) {
        try {
            return errors.render(req, status, title, message);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Error page failed", e);
            return Response.text(status, title + ". " + message);
        }
    }

    // ------------------------------------------------------------------ output

    private void write(HttpExchange exchange, Request request, Response response) {
        try (exchange) {
            var headers = exchange.getResponseHeaders();
            headers.set("Content-Security-Policy", CONTENT_SECURITY_POLICY);
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("X-Frame-Options", "DENY");
            headers.set("Referrer-Policy", "same-origin");
            headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            headers.set("Cross-Origin-Opener-Policy", "same-origin");
            if (settings.keystore() != null) headers.set("Strict-Transport-Security", "max-age=31536000");
            if (response.contentType() != null) headers.set("Content-Type", response.contentType());
            for (Map.Entry<String, String> h : response.headers().entrySet()) headers.set(h.getKey(), h.getValue());
            for (String c : response.cookies()) headers.add("Set-Cookie", c);
            if (request != null && request.session() != null && !request.session().id().equals(request.cookie(cookieName))) {
                headers.add("Set-Cookie", sessionCookie(request.session().id()));
            }
            byte[] body = response.body();
            if (request != null && request.acceptsGzip() && response.isCompressible()) {
                body = gzip(body);
                headers.set("Content-Encoding", "gzip");
                headers.set("Vary", "Accept-Encoding");
            }
            boolean head = request != null && request.method().equals("HEAD");
            if (response.status() == 304 || response.status() == 303 || body.length == 0 || head) {
                exchange.sendResponseHeaders(response.status(), -1);
            } else {
                exchange.sendResponseHeaders(response.status(), body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "Client went away", e);
        }
    }

    private String sessionCookie(String id) {
        return cookieName + "=" + id + "; Path=/; HttpOnly; SameSite=Lax" + (settings.secureCookies() ? "; Secure" : "");
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(data.length / 3 + 64);
        try (GZIPOutputStream gz = new GZIPOutputStream(bytes)) {
            gz.write(data);
        }
        return bytes.toByteArray();
    }

    private static SSLContext sslContext(Path keystore, String password) throws IOException {
        try (InputStream in = Files.newInputStream(keystore)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(in, password.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password.toCharArray());
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), null, null);
            return ctx;
        } catch (java.security.GeneralSecurityException e) {
            throw new IOException("Cannot load the HTTPS keystore " + keystore + ": " + e.getMessage(), e);
        }
    }
}
