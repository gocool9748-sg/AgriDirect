package com.agrimarket.web;

import com.agrimarket.db.DBConnection;
import com.agrimarket.db.DatabaseSetup;
import com.agrimarket.service.ValidationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * AgriDirect in the browser. A small web server built into Java (com.sun.net.httpserver), so nothing
 * extra is installed: the pages call the same services, DAOs and database as the desktop app.
 *
 *   web.bat            start it and open http://localhost:8080
 *   web.bat lan        also allow other computers on the same Wi-Fi/LAN to open it
 *   -Dport=9090        use another port
 */
public final class WebServer {

    static final String COOKIE = "AGRISESSION";
    private static final long SESSION_IDLE_MS = 8L * 60 * 60 * 1000;
    private static final int MAX_BODY = 64 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** True when ADMIN_PASSWORD replaced the demo admin password (then the login page doesn't show it). */
    static volatile boolean customAdminPassword;

    /** A page handler. */
    interface Handler {
        Result handle(Req r) throws Exception;
    }

    /** What a handler returns: a page or a redirect. */
    record Result(int status, String html, String location) {
        static Result html(String html) {
            return new Result(200, html, null);
        }

        static Result redirect(String location) {
            return new Result(303, null, location);
        }
    }

    /** The parts of an HTTP request the pages need. */
    static final class Req {
        final String method;
        final String path;
        final Map<String, String> query;
        final Map<String, String> form;
        final Session session;
        final String uri;

        Req(String method, String path, String uri, Map<String, String> query, Map<String, String> form,
            Session session) {
            this.method = method;
            this.path = path;
            this.uri = uri;
            this.query = query;
            this.form = form;
            this.session = session;
        }

        /** Form field first, then query string; never null. */
        String param(String name) {
            String v = form.get(name);
            if (v == null) {
                v = query.get(name);
            }
            return v == null ? "" : v;
        }

        int intParam(String name) {
            try {
                return Integer.parseInt(param(name).trim());
            } catch (NumberFormatException e) {
                throw new ValidationException("Something was missing from the form. Please try again.");
            }
        }

        /** Where to go back to after a POST: the form's "back" field if it is a local path, else the default. */
        String back(String fallback) {
            String b = param("back");
            return b.startsWith("/") && !b.startsWith("//") && !b.contains("\\") ? b : fallback;
        }
    }

    private final Map<String, Handler> routes = new HashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private WebServer() {
        AuthPages.register(this);
        FarmerPages.register(this);
        BuyerPages.register(this);
        AdminPages.register(this);
    }

    void get(String path, Handler h) {
        routes.put("GET " + path, h);
    }

    void post(String path, Handler h) {
        routes.put("POST " + path, h);
    }

    public static void main(String[] args) throws IOException {
        List<String> options = Arrays.asList(args);
        boolean lan = options.contains("lan");
        int port = Integer.parseInt(System.getProperty("port", envOr("PORT", "8080")));

        System.out.println("AgriDirect website - starting...");
        String problem = DatabaseSetup.check();
        if ("first-run".equals(problem)) {
            System.out.println("First run: creating the database with demo data...");
            problem = DatabaseSetup.autoSetup() ? DatabaseSetup.check() : "could not create the database.";
        }
        if (problem != null) {
            System.err.println("Cannot open the database: " + problem);
            System.err.println("Fix it with \"run.bat setup\" (choose the built-in database), then start the website again.");
            System.exit(1);
        }
        System.out.println("Database: " + DBConnection.kind());

        // On a public server, replace the demo admin password (admin123) with a private one.
        String adminPassword = System.getenv("ADMIN_PASSWORD");
        if (adminPassword != null && !adminPassword.isBlank()) {
            try {
                new com.agrimarket.dao.AdminDAO().updatePassword("admin",
                        com.agrimarket.util.PasswordUtil.hash(adminPassword.trim()));
                customAdminPassword = true;
                System.out.println("Admin password set from ADMIN_PASSWORD.");
            } catch (SQLException e) {
                System.err.println("Could not set the admin password: " + e.getMessage());
            }
        }
        // HOST=0.0.0.0 (set in the Dockerfile) listens on every network interface, like "lan".
        lan = lan || "0.0.0.0".equals(System.getenv("HOST"));

        InetSocketAddress address = lan ? new InetSocketAddress(port)
                : new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        HttpServer server;
        try {
            server = HttpServer.create(address, 0);
        } catch (BindException e) {
            System.err.println("Port " + port + " is already in use. AgriDirect is probably already running:");
            System.err.println("open http://localhost:" + port + " in your browser, or close the other window first.");
            if (options.contains("open")) {
                openBrowser("http://localhost:" + port + "/");
            }
            System.exit(2);
            return;
        }
        WebServer app = new WebServer();
        server.createContext("/", app::serve);
        server.setExecutor(Executors.newFixedThreadPool(16));
        server.start();

        String url = "http://localhost:" + port + "/";
        System.out.println();
        System.out.println("  AgriDirect is running:  " + url);
        if (lan) {
            System.out.println("  Other computers on your network: http://" + InetAddress.getLocalHost().getHostAddress()
                    + ":" + port + "/  (allow Java in Windows Firewall if asked)");
        }
        System.out.println("  Keep this window open while you use the website. Close it (or press Ctrl+C) to stop.");
        System.out.println();
        if (options.contains("open")) {
            openBrowser(url);
        }
    }

    private static String envOr(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception | Error ignored) {
            // the URL is printed in the window; the user can open it by hand
        }
    }

    // ------------------------------------------------------------------ request handling

    private void serve(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod().toUpperCase();
            if (method.equals("HEAD")) {
                method = "GET";
            }
            URI uri = ex.getRequestURI();
            String path = uri.getPath() == null ? "/" : uri.getPath();
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            if (path.equals("/favicon.ico")) {
                send(ex, new Result(404, "", null), null);
                return;
            }
            Map<String, String> query = parse(uri.getRawQuery());
            Map<String, String> form = Map.of();
            if (method.equals("POST")) {
                form = parse(readBody(ex));
            }
            Session session = session(ex);
            String raw = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
            Req r = new Req(method, path, raw, query, form, session);
            send(ex, dispatch(r), session);
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, new Result(500, Html.bare("Error", "<main><div class=\"card\"><h2>Something went wrong</h2>"
                    + "<p>" + Html.esc(e.toString()) + "</p><p><a href=\"/\">Back to AgriDirect</a></p></div></main>"),
                    null), null);
        } finally {
            ex.close();
        }
    }

    private Result dispatch(Req r) throws Exception {
        Session s = r.session;
        Handler h = routes.get(r.method + " " + r.path);
        if (h == null) {
            return new Result(404, Html.bare("Not found", "<main><div class=\"card\"><h2>Page not found</h2>"
                    + "<p><a href=\"" + s.homePath() + "\">Back to AgriDirect</a></p></div></main>"), null);
        }
        // Only the right role may open its own pages.
        if (!allowed(s, r.path)) {
            if (!s.loggedIn()) {
                s.error("Please log in first.");
            }
            return Result.redirect(s.loggedIn() ? s.homePath() : "/");
        }
        if (r.method.equals("POST") && !s.csrf.equals(r.form.get("csrf"))) {
            s.error("That form had expired. Please try again.");
            return Result.redirect(s.homePath());
        }
        try {
            return h.handle(r);
        } catch (ValidationException e) {
            s.error(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            s.error("Database error: " + e.getMessage());
        }
        // After a form error: go back to the page the form was on, where the message is shown.
        if (r.method.equals("POST")) {
            return Result.redirect(r.back(s.homePath()));
        }
        String back = "<p><a class=\"btn\" href=\"" + s.homePath() + "\">Back</a></p>";
        return s.loggedIn()
                ? Result.html(Html.page(s, "Could not open this page", "", "", back))
                : Result.html(Html.bare("Error", "<main>" + Html.flash(s) + back + "</main>"));
    }

    private static boolean allowed(Session s, String path) {
        if (path.equals("/farmer") || path.startsWith("/farmer/")) {
            return s.role == Session.Role.FARMER;
        }
        if (path.equals("/buyer") || path.startsWith("/buyer/")) {
            return s.role == Session.Role.BUYER;
        }
        if (path.equals("/admin") || path.startsWith("/admin/")) {
            return s.role == Session.Role.ADMIN;
        }
        if (path.equals("/order")) {
            return s.loggedIn();
        }
        return true;
    }

    // ------------------------------------------------------------------ sessions

    private Session session(HttpExchange ex) {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(x -> now - x.lastSeen > SESSION_IDLE_MS);
        String id = cookie(ex, COOKIE);
        Session s = id == null ? null : sessions.get(id);
        if (s == null) {
            s = newSession();
        }
        s.lastSeen = now;
        return s;
    }

    Session newSession() {
        Session s = new Session(token(), token());
        sessions.put(s.id, s);
        return s;
    }

    /** Logging in or out starts a fresh session (so an old session id can't be reused). */
    Session replace(Session old) {
        sessions.remove(old.id);
        return newSession();
    }

    private static String token() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String cookie(HttpExchange ex, String name) {
        List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) {
            return null;
        }
        for (String header : headers) {
            for (String part : header.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals(name)) {
                    return kv[1];
                }
            }
        }
        return null;
    }

    /** The session a handler switched to (login/logout), so the response sets the new cookie. */
    static final ThreadLocal<Session> SWITCHED = new ThreadLocal<>();

    // ------------------------------------------------------------------ responses

    private static void send(HttpExchange ex, Result res, Session session) throws IOException {
        Session cookieSession = SWITCHED.get();
        SWITCHED.remove();
        if (cookieSession == null) {
            cookieSession = session;
        }
        var h = ex.getResponseHeaders();
        if (cookieSession != null) {
            // Behind an HTTPS host (Render, Railway...), only send the cookie over HTTPS.
            boolean https = "https".equalsIgnoreCase(ex.getRequestHeaders().getFirst("X-Forwarded-Proto"));
            h.add("Set-Cookie", COOKIE + "=" + cookieSession.id + "; Path=/; HttpOnly; SameSite=Lax"
                    + (https ? "; Secure" : ""));
        }
        h.set("X-Content-Type-Options", "nosniff");
        h.set("X-Frame-Options", "DENY");
        h.set("Referrer-Policy", "same-origin");
        h.set("Cache-Control", "no-store");
        if (res.location() != null) {
            h.set("Location", res.location());
            ex.sendResponseHeaders(res.status(), -1);
            return;
        }
        byte[] body = (res.html() == null ? "" : res.html()).getBytes(StandardCharsets.UTF_8);
        h.set("Content-Type", "text/html; charset=utf-8");
        if (ex.getRequestMethod().equalsIgnoreCase("HEAD")) {
            ex.sendResponseHeaders(res.status(), -1);
            return;
        }
        ex.sendResponseHeaders(res.status(), body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = ex.getResponseBody()) {
                out.write(body);
            }
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] data = in.readNBytes(MAX_BODY + 1);
            if (data.length > MAX_BODY) {
                throw new IOException("Form too large");
            }
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    /** Parses a=1&b=two (query string or form body). */
    static Map<String, String> parse(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            String[] kv = pair.split("=", 2);
            try {
                String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                map.putIfAbsent(k, v);
            } catch (IllegalArgumentException ignored) {
                // skip a badly encoded field
            }
        }
        return map;
    }
}
