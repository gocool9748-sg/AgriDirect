package com.agrimarket.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens JDBC connections to MySQL. Settings are read from db.properties in the
 * folder the app is started from (or the file named by -Dagri.config=...).
 * The JDBC driver registers itself automatically, so Class.forName is not needed.
 */
public final class DBConnection {

    public static final String OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true";
    private static final Properties CONFIG = load();

    private DBConnection() {
    }

    private static Path file() {
        return Paths.get(System.getProperty("agri.config", "db.properties"));
    }

    private static Properties load() {
        Properties p = new Properties();
        p.setProperty("db.url", "jdbc:mysql://127.0.0.1:3306/agri_market" + OPTIONS);
        p.setProperty("db.user", "root");
        p.setProperty("db.password", "");
        Path file = file();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("Could not read " + file.toAbsolutePath() + ": " + e.getMessage());
            }
        }
        // Environment variables win (used when deploying to a server).
        override(p, "db.url", "DB_URL");
        override(p, "db.user", "DB_USER");
        override(p, "db.password", "DB_PASSWORD");
        return p;
    }

    private static void override(Properties p, String key, String env) {
        String value = System.getenv(env);
        if (value != null && !value.isBlank()) {
            p.setProperty(key, value);
        }
    }

    public static synchronized Connection get() throws SQLException {
        return DriverManager.getConnection(
                withKeepOpen(CONFIG.getProperty("db.url")),
                CONFIG.getProperty("db.user"),
                CONFIG.getProperty("db.password"));
    }

    /**
     * Built-in database: keep it open while the program runs (DB_CLOSE_DELAY=-1). Otherwise it is
     * re-opened for every query, and when the website and the desktop app run at the same time the
     * second one fails with "Lock file recently modified". With it, the first program to open the
     * database serves it (AUTO_SERVER) and the other connects to that one.
     */
    static String withKeepOpen(String url) {
        if (url != null && url.startsWith("jdbc:h2:") && !url.toUpperCase().contains("DB_CLOSE_DELAY")) {
            return url + ";DB_CLOSE_DELAY=-1";
        }
        return url;
    }

    public static synchronized String url() {
        return CONFIG.getProperty("db.url");
    }

    /** "MySQL" or "built-in database", for showing on screen. */
    public static synchronized String kind() {
        return CONFIG.getProperty("db.url").startsWith("jdbc:h2:") ? "built-in database" : "MySQL";
    }

    public static synchronized String user() {
        return CONFIG.getProperty("db.user");
    }

    /** Switches to new settings and saves them to db.properties for next time. */
    public static synchronized void configure(String url, String user, String password) throws IOException {
        CONFIG.setProperty("db.url", url);
        CONFIG.setProperty("db.user", user);
        CONFIG.setProperty("db.password", password == null ? "" : password);
        try (OutputStream out = Files.newOutputStream(file())) {
            CONFIG.store(out, "MySQL connection settings for AgriDirect (written by the setup screen)");
        }
    }
}
