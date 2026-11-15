package com.nutriplanner.web.http;

import com.nutriplanner.domain.User;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A parsed HTTP request with its session and the logged-in user (if any). */
public final class Request {

    public static final int MAX_BODY_BYTES = 256 * 1024;

    private final HttpExchange exchange;
    private final String method;
    private final String path;
    private final String rawQuery;
    private final Map<String, List<String>> query;
    private final Map<String, List<String>> form;
    private final Map<String, String> cookies;
    private final String clientIp;
    private Map<String, String> pathVariables = Map.of();
    private Session session;
    private User user;

    private Request(HttpExchange exchange, String method, String path, String rawQuery, Map<String, List<String>> query,
                    Map<String, List<String>> form, Map<String, String> cookies, String clientIp) {
        this.exchange = exchange;
        this.method = method;
        this.path = path;
        this.rawQuery = rawQuery;
        this.query = query;
        this.form = form;
        this.cookies = cookies;
        this.clientIp = clientIp;
    }

    static Request from(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
        URI uri = exchange.getRequestURI();
        String path = uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath();
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        Map<String, List<String>> query = parseEncoded(uri.getRawQuery());
        Map<String, List<String>> form = Map.of();
        if (method.equals("POST")) {
            String type = exchange.getRequestHeaders().getFirst("Content-Type");
            if (type != null && type.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded")) {
                form = parseEncoded(readBody(exchange.getRequestBody()));
            }
        }
        String ip = exchange.getRemoteAddress() == null ? "" : exchange.getRemoteAddress().getAddress().getHostAddress();
        return new Request(exchange, method, path, uri.getRawQuery(), query, form,
                parseCookies(exchange.getRequestHeaders().get("Cookie")), ip);
    }

    private static String readBody(InputStream in) throws IOException {
        byte[] buffer = in.readNBytes(MAX_BODY_BYTES + 1);
        if (buffer.length > MAX_BODY_BYTES) throw new BodyTooLargeException();
        return new String(buffer, StandardCharsets.UTF_8);
    }

    static final class BodyTooLargeException extends IOException {
        BodyTooLargeException() {
            super("Request body too large");
        }
    }

    static Map<String, List<String>> parseEncoded(String raw) {
        if (raw == null || raw.isEmpty()) return Map.of();
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String key = decode(eq < 0 ? pair : pair.substring(0, eq));
            String value = eq < 0 ? "" : decode(pair.substring(eq + 1));
            out.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return out;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static Map<String, String> parseCookies(List<String> headers) {
        if (headers == null) return Map.of();
        Map<String, String> out = new HashMap<>();
        for (String header : headers) {
            for (String part : header.split(";")) {
                int eq = part.indexOf('=');
                if (eq > 0) out.putIfAbsent(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ access

    public String method() {
        return method;
    }

    public boolean isPost() {
        return method.equals("POST");
    }

    public String path() {
        return path;
    }

    /** Path plus query string, for redirecting back to the same page. */
    public String pathWithQuery() {
        return rawQuery == null || rawQuery.isEmpty() ? path : path + "?" + rawQuery;
    }

    /** A form field (for POST) or query parameter, or null. */
    public String param(String name) {
        List<String> values = form.get(name);
        if (values == null || values.isEmpty()) values = query.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    public String param(String name, String fallback) {
        String v = param(name);
        return v == null ? fallback : v;
    }

    /** All values of a repeated field, such as a group of checkboxes. */
    public List<String> params(String name) {
        List<String> values = form.get(name);
        if (values == null) values = query.get(name);
        return values == null ? List.of() : Collections.unmodifiableList(values);
    }

    public Map<String, List<String>> formFields() {
        return form;
    }

    public Map<String, List<String>> queryFields() {
        return query;
    }

    public String header(String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    public String cookie(String name) {
        return cookies.get(name);
    }

    public String clientIp() {
        return clientIp;
    }

    public String pathVar(String name) {
        return pathVariables.get(name);
    }

    public long pathId(String name) {
        try {
            return Long.parseLong(pathVariables.get(name));
        } catch (NumberFormatException | NullPointerException e) {
            throw new com.nutriplanner.service.NotFoundException("Not found");
        }
    }

    void setPathVariables(Map<String, String> vars) {
        this.pathVariables = vars;
    }

    public Session session() {
        return session;
    }

    void setSession(Session session) {
        this.session = session;
    }

    public User user() {
        return user;
    }

    void setUser(User user) {
        this.user = user;
    }

    public boolean acceptsGzip() {
        String ae = header("Accept-Encoding");
        return ae != null && ae.toLowerCase(Locale.ROOT).contains("gzip");
    }
}
