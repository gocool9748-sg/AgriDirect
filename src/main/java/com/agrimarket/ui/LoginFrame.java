package com.agrimarket.ui;

import com.agrimarket.model.Admin;
import com.agrimarket.model.Buyer;
import com.agrimarket.model.Farmer;
import com.agrimarket.service.AuthService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** First screen: choose Farmer / Buyer / Admin and log in, or register. */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final AuthService auth = new AuthService();
    private final JRadioButton farmerRole = new JRadioButton("Farmer", true);
    private final JRadioButton buyerRole = new JRadioButton("Buyer");
    private final JRadioButton adminRole = new JRadioButton("Admin");
    private final JLabel idLabel = UI.label("Mobile number", Font.BOLD, 13, UI.MUTED);
    private final JTextField idField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JButton registerButton = UI.button("Create an account", false);

    public LoginFrame() {
        super("AgriDirect · Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));
        add(brandPanel());
        add(formPanel());
        setSize(new Dimension(920, 560));
        setMinimumSize(new Dimension(820, 520));
        setLocationRelativeTo(null);
    }

    private JPanel brandPanel() {
        JPanel p = new JPanel();
        p.setBackground(UI.GREEN);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(70, 48, 48, 40));
        p.add(UI.label("AGRIDIRECT", Font.BOLD, 14, UI.ORANGE));
        p.add(Box.createVerticalStrut(14));
        p.add(UI.label("<html>Farmer-to-Buyer<br>Direct Marketplace</html>", Font.BOLD, 32, UI.CREAM));
        p.add(Box.createVerticalStrut(18));
        p.add(UI.label("<html>Farmers list produce at their own price.<br>Buyers search, order and track it directly.<br>"
                + "No middlemen in between.</html>", Font.PLAIN, 15, UI.LIGHT_TEXT));
        p.add(Box.createVerticalGlue());
        p.add(UI.label("Java · JDBC · Database: " + com.agrimarket.db.DBConnection.kind(), Font.PLAIN, 13,
                UI.LIGHT_TEXT));
        return p;
    }

    private JPanel formPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UI.CREAM);
        outer.setBorder(new EmptyBorder(60, 48, 40, 48));

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = UI.label("Welcome back", Font.BOLD, 26, UI.GREEN);
        title.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        JLabel sub = UI.label("Log in to continue", Font.PLAIN, 14, UI.MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        p.add(sub);
        p.add(Box.createVerticalStrut(22));

        JLabel roleLabel = UI.label("I am a", Font.BOLD, 13, UI.MUTED);
        roleLabel.setAlignmentX(LEFT_ALIGNMENT);
        p.add(roleLabel);
        ButtonGroup group = new ButtonGroup();
        JPanel roles = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        roles.setOpaque(false);
        roles.setAlignmentX(LEFT_ALIGNMENT);
        for (JRadioButton r : new JRadioButton[] {farmerRole, buyerRole, adminRole}) {
            r.setOpaque(false);
            r.setFont(UI.font(Font.PLAIN, 14));
            r.addActionListener(e -> roleChanged());
            group.add(r);
            roles.add(r);
            roles.add(Box.createHorizontalStrut(14));
        }
        p.add(roles);
        p.add(Box.createVerticalStrut(12));

        idLabel.setAlignmentX(LEFT_ALIGNMENT);
        p.add(idLabel);
        p.add(Box.createVerticalStrut(4));
        p.add(field(idField));
        p.add(Box.createVerticalStrut(12));
        JLabel pw = UI.label("Password", Font.BOLD, 13, UI.MUTED);
        pw.setAlignmentX(LEFT_ALIGNMENT);
        p.add(pw);
        p.add(Box.createVerticalStrut(4));
        p.add(field(passwordField));
        p.add(Box.createVerticalStrut(20));

        JButton login = UI.button("Log in", true);
        login.addActionListener(e -> login());
        getRootPane().setDefaultButton(login);
        registerButton.addActionListener(e -> register());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(login);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(registerButton);
        p.add(buttons);
        p.add(Box.createVerticalStrut(24));

        JLabel demo = UI.label("<html>Sample logins (after loading sample_data.sql):<br>"
                + "Farmer 9840000001 · Buyer 9790000001 · password pass123<br>"
                + "Admin admin · password admin123</html>", Font.PLAIN, 12, UI.MUTED);
        demo.setAlignmentX(LEFT_ALIGNMENT);
        p.add(demo);

        outer.add(p, BorderLayout.NORTH);
        return outer;
    }

    private static JPanel field(javax.swing.JComponent c) {
        c.setFont(UI.font(Font.PLAIN, 15));
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UI.BORDER),
                new EmptyBorder(6, 8, 6, 8)));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(360, 40));
        wrap.add(c);
        return wrap;
    }

    private void roleChanged() {
        boolean admin = adminRole.isSelected();
        idLabel.setText(admin ? "Username" : "Mobile number");
        registerButton.setEnabled(!admin);
    }

    private void login() {
        String id = idField.getText();
        String password = new String(passwordField.getPassword());
        UI.attempt(this, () -> {
            if (farmerRole.isSelected()) {
                Optional<Farmer> f = auth.loginFarmer(id, password);
                if (f.isPresent()) {
                    open(new FarmerDashboard(f.get()));
                    return;
                }
            } else if (buyerRole.isSelected()) {
                Optional<Buyer> b = auth.loginBuyer(id, password);
                if (b.isPresent()) {
                    open(new BuyerDashboard(b.get()));
                    return;
                }
            } else {
                Optional<Admin> a = auth.loginAdmin(id, password);
                if (a.isPresent()) {
                    open(new AdminDashboard(a.get()));
                    return;
                }
            }
            passwordField.setText("");
            throw new com.agrimarket.service.ValidationException("Wrong login details. Check the role, number and password.");
        });
    }

    private void open(JFrame dashboard) {
        dashboard.setVisible(true);
        dispose();
    }

    private void register() {
        RegisterDialog dialog = new RegisterDialog(this, farmerRole.isSelected());
        dialog.setVisible(true);
        if (dialog.registeredPhone() != null) {
            idField.setText(dialog.registeredPhone());
            passwordField.setText("");
            passwordField.requestFocusInWindow();
            UI.info(this, "Account created. You can log in now.");
        }
    }
}
