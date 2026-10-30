package com.agrimarket;

import com.agrimarket.db.DBConnection;
import com.agrimarket.db.DatabaseSetup;
import com.agrimarket.ui.LoginFrame;
import com.agrimarket.ui.SetupDialog;
import com.agrimarket.ui.UI;
import javax.swing.SwingUtilities;

/**
 * Entry point.
 * First run: uses a local MySQL (root, no password) if one is running, otherwise the built-in
 * database, creating the tables and demo data automatically. If the saved database can't be
 * reached later, the setup window lets the user fix it. "run.bat setup" opens the setup window.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        boolean forceSetup = args.length > 0 && args[0].equalsIgnoreCase("setup");
        SwingUtilities.invokeLater(() -> {
            UI.installLookAndFeel();
            String problem = forceSetup ? "setup" : checkDatabase();
            if ("first-run".equals(problem) && autoSetup()) {
                problem = null;
            }
            if (problem != null) {
                String shown = problem.equals("first-run") || problem.equals("setup") ? null : problem;
                SetupDialog setup = new SetupDialog(shown, DBConnection.user());
                setup.setVisible(true);
                if (!setup.isReady()) {
                    System.exit(0);
                }
            }
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
            login.toFront();
            login.requestFocus();
        });
    }

    /** First run: local MySQL without a password if available, else the built-in database. */
    private static boolean autoSetup() {
        return DatabaseSetup.autoSetup();
    }

    /** null if the database is ready, otherwise a short reason. */
    private static String checkDatabase() {
        return DatabaseSetup.check();
    }
}
