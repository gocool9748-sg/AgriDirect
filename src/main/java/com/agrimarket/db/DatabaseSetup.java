package com.agrimarket.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * First-run setup: connects to the MySQL server, creates the agri_market database by running
 * sql/schema.sql (and optionally sql/sample_data.sql), then saves the settings to db.properties.
 */
public final class DatabaseSetup {

    private DatabaseSetup() {
    }

    /** Server URL without a database, e.g. jdbc:mysql://localhost:3306/ */
    public static String serverUrl(String host, int port) {
        return "jdbc:mysql://" + host + ":" + port + "/" + DBConnection.OPTIONS + "&connectTimeout=4000";
    }

    // ------------------------------------------------------------------
    // Built-in database (H2, stored in the project's "data" folder).
    // Used automatically when no MySQL server is available, so the app always works.
    // ------------------------------------------------------------------

    public static final String EMBEDDED_URL =
            "jdbc:h2:./data/agri_market;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE";

    public static boolean embeddedExists() throws SQLException {
        try (Connection c = DriverManager.getConnection(DBConnection.withKeepOpen(EMBEDDED_URL), "sa", "");
             ResultSet rs = c.getMetaData().getTables(null, null, "farmers", null)) {
            return rs.next();
        }
    }

    /** Creates the tables (and optionally demo data) in the built-in database and switches to it. */
    public static void installEmbedded(boolean withSampleData) throws SQLException, IOException {
        try (Connection c = DriverManager.getConnection(DBConnection.withKeepOpen(EMBEDDED_URL), "sa", "");
             Statement st = c.createStatement()) {
            for (String sql : statements(read("schema.sql"))) {
                String head = sql.toUpperCase().replaceAll("\\s+", " ");
                if (head.startsWith("CREATE DATABASE") || head.startsWith("USE ")
                        || head.startsWith("SET FOREIGN_KEY_CHECKS")) {
                    continue; // MySQL-only housekeeping
                }
                if (head.startsWith("DROP TABLE")) {
                    sql = sql + " CASCADE";
                }
                st.execute(sql);
            }
            if (withSampleData) {
                for (String sql : statements(read("sample_data.sql"))) {
                    if (!sql.toUpperCase().startsWith("USE ")) {
                        st.execute(sql);
                    }
                }
            }
            // keep auto-numbering after the sample rows
            for (String t : new String[] {"farmers:farmer_id", "buyers:buyer_id", "admins:admin_id",
                "categories:category_id", "products:product_id", "orders:order_id", "order_items:item_id"}) {
                String[] p = t.split(":");
                try (ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(" + p[1] + "), 0) + 1 FROM " + p[0])) {
                    rs.next();
                    st.execute("ALTER TABLE " + p[0] + " ALTER COLUMN " + p[1] + " RESTART WITH " + rs.getLong(1));
                }
            }
        }
        DBConnection.configure(EMBEDDED_URL, "sa", "");
    }

    public static void useEmbedded() throws IOException {
        DBConnection.configure(EMBEDDED_URL, "sa", "");
    }

    public static String databaseUrl(String host, int port) {
        return "jdbc:mysql://" + host + ":" + port + "/agri_market" + DBConnection.OPTIONS;
    }

    /** True if the agri_market database already has the farmers table. */
    public static boolean databaseExists(String host, int port, String user, String password) throws SQLException {
        try (Connection c = DriverManager.getConnection(serverUrl(host, port), user, password);
             ResultSet rs = c.getMetaData().getTables("agri_market", null, "farmers", null)) {
            return rs.next();
        }
    }

    /**
     * Creates (or re-creates) the database. WARNING: schema.sql drops existing tables.
     * @param withSampleData also load sql/sample_data.sql (demo farmers, buyers, orders)
     */
    public static void install(String host, int port, String user, String password, boolean withSampleData)
            throws SQLException, IOException {
        try (Connection c = DriverManager.getConnection(serverUrl(host, port), user, password);
             Statement st = c.createStatement()) {
            for (String sql : statements(read("schema.sql"))) {
                st.execute(sql);
            }
            if (withSampleData) {
                for (String sql : statements(read("sample_data.sql"))) {
                    st.execute(sql);
                }
            }
        }
        DBConnection.configure(databaseUrl(host, port), user, password);
    }

    /** Uses an existing database without changing it. */
    public static void useExisting(String host, int port, String user, String password) throws IOException {
        DBConnection.configure(databaseUrl(host, port), user, password);
    }

    /**
     * null if the saved database is ready, "first-run" if nothing is set up yet,
     * otherwise a short reason to show the user. Used by both the desktop app and the website.
     */
    public static String check() {
        if (!Files.exists(Paths.get(System.getProperty("agri.config", "db.properties")))) {
            return "first-run";
        }
        try (Connection c = DBConnection.get()) {
            DatabaseMetaData md = c.getMetaData();
            try (ResultSet rs = md.getTables(c.getCatalog(), null, "farmers", null)) {
                return rs.next() ? null : "the AgriDirect tables have not been created yet.";
            }
        } catch (SQLException e) {
            String m = e.getMessage() == null ? "" : e.getMessage();
            if (m.contains("Unknown database")) {
                return "first-run";
            }
            if (m.contains("Access denied")) {
                return "wrong MySQL username or password in db.properties.";
            }
            if (m.contains("onnect") || m.contains("refused")) {
                return "MySQL is not running. Start it, or use the built-in database.";
            }
            return m;
        }
    }

    /** First run: local MySQL without a password if available, else the built-in database. */
    public static boolean autoSetup() {
        try {
            if (databaseExists("127.0.0.1", 3306, "root", "")) {
                useExisting("127.0.0.1", 3306, "root", "");
            } else {
                install("127.0.0.1", 3306, "root", "", true);
            }
            return true;
        } catch (Exception mysqlProblem) {
            try {
                if (embeddedExists()) {
                    useEmbedded();
                } else {
                    installEmbedded(true);
                }
                return true;
            } catch (Exception embeddedProblem) {
                embeddedProblem.printStackTrace();
                return false;
            }
        }
    }

    /** Reads sql/<name> from the project folder, or from inside the jar. */
    static String read(String name) throws IOException {
        Path file = Paths.get("sql", name);
        if (Files.exists(file)) {
            return Files.readString(file, StandardCharsets.UTF_8);
        }
        try (InputStream in = DatabaseSetup.class.getResourceAsStream("/sql/" + name)) {
            if (in == null) {
                throw new IOException("Cannot find sql/" + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Splits a SQL script into statements: drops -- comments, splits on ; outside quotes. */
    static List<String> statements(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quote = 0;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (!inQuote && c == '-' && i + 1 < script.length() && script.charAt(i + 1) == '-') {
                while (i < script.length() && script.charAt(i) != '\n') {
                    i++;
                }
                current.append('\n');
                continue;
            }
            if (inQuote) {
                current.append(c);
                if (c == '\\' && i + 1 < script.length()) {
                    current.append(script.charAt(++i));
                } else if (c == quote) {
                    inQuote = false;
                }
                continue;
            }
            if (c == '\'' || c == '"' || c == '`') {
                inQuote = true;
                quote = c;
                current.append(c);
            } else if (c == ';') {
                add(out, current);
            } else {
                current.append(c);
            }
        }
        add(out, current);
        return out;
    }

    private static void add(List<String> out, StringBuilder sb) {
        String s = sb.toString().trim();
        if (!s.isEmpty()) {
            out.add(s);
        }
        sb.setLength(0);
    }
}
