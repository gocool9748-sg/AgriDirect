package com.agrimarket.ui;

import com.agrimarket.db.DatabaseSetup;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * Shown when the app cannot reach its database: asks for the MySQL login, then creates the
 * agri_market database (tables + optional demo data) and saves the settings to db.properties.
 */
public class SetupDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField host = new JTextField("127.0.0.1", 16);
    private final JTextField port = new JTextField("3306", 6);
    private final JTextField user = new JTextField("root", 16);
    private final JPasswordField password = new JPasswordField(16);
    private final JCheckBox sample = new JCheckBox("Add demo farmers, buyers, products and orders", true);
    private final JLabel status = UI.label(" ", Font.PLAIN, 13, UI.MUTED);
    private final JButton create = UI.button("Create database & start", true);
    private final JButton useExisting = UI.button("Use existing database", false);
    private final JButton builtIn = UI.button("Use the built-in database", false);
    private boolean ready;

    /** Switches to the built-in database (no MySQL needed), creating it the first time. */
    private void useBuiltIn() {
        boolean withSample = sample.isSelected();
        if (UI.attempt(this, () -> {
            if (DatabaseSetup.embeddedExists()) {
                DatabaseSetup.useEmbedded();
            } else {
                DatabaseSetup.installEmbedded(withSample);
            }
        })) {
            ready = true;
            dispose();
        }
    }

    public SetupDialog(String problem, String currentUser) {
        super((java.awt.Frame) null, "AgriDirect · Database setup", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        if (currentUser != null && !currentUser.isBlank()) {
            user.setText(currentUser);
        }

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UI.CREAM);
        root.setBorder(new EmptyBorder(22, 26, 20, 26));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(UI.label("Connect to MySQL", Font.BOLD, 22, UI.GREEN));
        top.add(Box.createVerticalStrut(6));
        top.add(UI.label("<html><div style='width:380px'>Enter the MySQL login you set when installing MySQL. "
                + "AgriDirect will create its database (<b>agri_market</b>) and remember these settings.</div></html>",
                Font.PLAIN, 13, UI.MUTED));
        if (problem != null) {
            top.add(Box.createVerticalStrut(8));
            JLabel p = UI.label("<html><div style='width:380px'>Could not connect: " + escape(problem) + "</div></html>",
                    Font.PLAIN, 12, UI.RED);
            top.add(p);
        }
        root.add(top, BorderLayout.NORTH);

        for (JTextField f : new JTextField[] {host, port, user, password}) {
            f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UI.BORDER),
                    new EmptyBorder(5, 7, 5, 7)));
        }
        sample.setOpaque(false);
        JPanel form = UI.form(new String[] {"MySQL host", "Port", "Username", "Password", ""},
                new JComponent[] {host, port, user, password, sample});
        JPanel middle = new JPanel(new BorderLayout());
        middle.setOpaque(false);
        middle.add(form, BorderLayout.NORTH);
        middle.add(status, BorderLayout.SOUTH);
        root.add(middle, BorderLayout.CENTER);

        create.addActionListener(e -> run(true));
        useExisting.addActionListener(e -> run(false));
        builtIn.addActionListener(e -> useBuiltIn());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(useExisting);
        buttons.add(create);
        JPanel noMysql = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        noMysql.setOpaque(false);
        noMysql.add(UI.label("No MySQL?  ", Font.PLAIN, 13, UI.MUTED));
        noMysql.add(builtIn);
        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(noMysql, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(create);
        pack();
        setLocationRelativeTo(null);
        // An owner-less dialog has no taskbar button on Windows, so make sure it opens in front.
        setAlwaysOnTop(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                toFront();
                password.requestFocusInWindow();
                setAlwaysOnTop(false);
            }
        });
        password.requestFocusInWindow();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void run(boolean createDb) {
        String h = host.getText().trim();
        String u = user.getText().trim();
        String pw = new String(password.getPassword());
        int p;
        try {
            p = Integer.parseInt(port.getText().trim());
        } catch (NumberFormatException e) {
            status.setForeground(UI.RED);
            status.setText("Port must be a number, usually 3306.");
            return;
        }
        boolean withSample = sample.isSelected();
        create.setEnabled(false);
        useExisting.setEnabled(false);
        status.setForeground(UI.MUTED);
        status.setText(createDb ? "Creating the database…" : "Checking the database…");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    boolean exists = DatabaseSetup.databaseExists(h, p, u, pw);
                    if (createDb) {
                        if (exists && !askOnScreen(
                                "An AgriDirect database already exists on this server.\n"
                                + "Re-creating it DELETES all its farmers, buyers, products and orders.\n\n"
                                + "Re-create it? (Choose No, then \"Use existing database\" to keep it.)")) {
                            return "Kept the existing database. Click \"Use existing database\".";
                        }
                        DatabaseSetup.install(h, p, u, pw, withSample);
                    } else {
                        if (!exists) {
                            return "No AgriDirect database on this server yet. Click \"Create database & start\".";
                        }
                        DatabaseSetup.useExisting(h, p, u, pw);
                    }
                    return null;
                } catch (Exception e) {
                    String m = e.getMessage() == null ? e.toString() : e.getMessage();
                    if (m.contains("Access denied")) {
                        return "Wrong MySQL username or password.";
                    }
                    if (m.contains("Connection refused") || m.contains("connect")) {
                        return "MySQL is not running on " + h + ":" + p + ". Start the MySQL service and try again.";
                    }
                    return m;
                }
            }

            @Override
            protected void done() {
                String error;
                try {
                    error = get();
                } catch (Exception e) {
                    error = e.toString();
                }
                if (error == null) {
                    ready = true;
                    dispose();
                } else {
                    status.setForeground(UI.RED);
                    status.setText("<html><div style='width:380px'>" + escape(error) + "</div></html>");
                    create.setEnabled(true);
                    useExisting.setEnabled(true);
                }
            }
        }.execute();
    }

    /** Shows a yes/no question on the Swing thread from a background thread. */
    private boolean askOnScreen(String question) throws Exception {
        boolean[] answer = new boolean[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> answer[0] = UI.confirm(this, question));
        return answer[0];
    }

    /** True once the database is ready and the settings are saved. */
    public boolean isReady() {
        return ready;
    }
}
