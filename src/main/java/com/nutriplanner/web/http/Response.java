package com.nutriplanner.web.http;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An HTTP response to send. Pages are never cached; static assets are cached by content hash. */
public final class Response {

    private final int status;
    private final String contentType;
    private final byte[] body;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final List<String> cookies = new ArrayList<>();

    private Response(int status, String contentType, byte[] body) {
        this.status = status;
        this.contentType = contentType;
        this.body = body;
    }

    public static Response html(int status, String html) {
        return new Response(status, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8))
                .header("Cache-Control", "no-store");
    }

    public static Response html(String html) {
        return html(200, html);
    }

    /** 303 See Other: the browser follows with a GET, so refreshing never re-submits a form. */
    public static Response redirect(String location) {
        return new Response(303, null, new byte[0]).header("Location", location).header("Cache-Control", "no-store");
    }

    public static Response bytes(String contentType, byte[] body, String cacheControl) {
        return new Response(200, contentType, body).header("Cache-Control", cacheControl);
    }

    public static Response text(int status, String text) {
        return new Response(status, "text/plain; charset=utf-8", text.getBytes(StandardCharsets.UTF_8))
                .header("Cache-Control", "no-store");
    }

    public static Response notModified() {
        return new Response(304, null, new byte[0]);
    }

    public Response header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public Response cookie(String setCookie) {
        cookies.add(setCookie);
        return this;
    }

    public int status() {
        return status;
    }

    public String contentType() {
        return contentType;
    }

    public byte[] body() {
        return body;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public List<String> cookies() {
        return cookies;
    }

    public boolean isCompressible() {
        return contentType != null && (contentType.startsWith("text/") || contentType.contains("svg")) && body.length > 1024;
    }
}
