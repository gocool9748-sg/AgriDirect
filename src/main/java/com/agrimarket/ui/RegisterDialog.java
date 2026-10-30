package com.agrimarket.ui;

import com.agrimarket.service.AuthService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** Registration form for a farmer or a buyer. */
public class RegisterDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final AuthService auth = new AuthService();
    private final boolean farmer;
    private final JTextField name = new JTextField(22);
    private final JTextField phone = new JTextField(22);
    private final JTextField village = new JTextField(22);
    private final JTextField district = new JTextField(22);
    private final JTextField city = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JPasswordField confirm = new JPasswordField(22);
    private String registeredPhone;

    public RegisterDialog(JFrame owner, boolean farmer) {
        super(owner, farmer ? "Register as a farmer" : "Register as a buyer", true);
        this.farmer = farmer;

        JPanel root = UI.page();
        root.add(UI.sectionTitle(farmer ? "New farmer account" : "New buyer account"), BorderLayout.NORTH);
        if (farmer) {
            root.add(UI.form(
                    new String[] {"Full name", "Mobile number", "Village", "District", "Password", "Confirm password"},
                    new JComponent[] {name, phone, village, district, password, confirm}), BorderLayout.CENTER);
        } else {
            root.add(UI.form(
                    new String[] {"Full name / business", "Mobile number", "City", "Password", "Confirm password"},
                    new JComponent[] {name, phone, city, password, confirm}), BorderLayout.CENTER);
        }
        JButton save = UI.button("Create account", true);
        JButton cancel = UI.button("Cancel", false);
        save.addActionListener(e -> save());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(save);
        root.add(buttons, BorderLayout.SOUTH);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        setContentPane(root);
        getRootPane().setDefaultButton(save);
        pack();
        setLocationRelativeTo(owner);
    }

    private void save() {
        String pw = new String(password.getPassword());
        String pw2 = new String(confirm.getPassword());
        UI.attempt(this, () -> {
            if (farmer) {
                registeredPhone = auth.registerFarmer(name.getText(), phone.getText(), village.getText(),
                        district.getText(), pw, pw2).phone();
            } else {
                registeredPhone = auth.registerBuyer(name.getText(), phone.getText(), city.getText(), pw, pw2).phone();
            }
            dispose();
        });
    }

    /** The phone number of the new account, or null if the dialog was cancelled. */
    public String registeredPhone() {
        return registeredPhone;
    }
}
