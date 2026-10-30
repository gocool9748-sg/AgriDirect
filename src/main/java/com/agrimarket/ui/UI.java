package com.agrimarket.ui;

import com.agrimarket.service.ValidationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Shared colours, fonts and small widgets so every screen looks the same. */
public final class UI {

    public static final Color GREEN = new Color(0x1F3A2B);
    public static final Color GREEN_SOFT = new Color(0x2A4A38);
    public static final Color CREAM = new Color(0xF6F1E4);
    public static final Color CARD = new Color(0xFFFBF2);
    public static final Color SAND = new Color(0xEDE5D0);
    public static final Color ORANGE = new Color(0xD9822B);
    public static final Color ORANGE_DARK = new Color(0xA3560F);
    public static final Color MUTED = new Color(0x4E5D52);
    public static final Color BORDER = new Color(0xDCD3BC);
    public static final Color LEAF = new Color(0x4F7A3A);
    public static final Color LIGHT_TEXT = new Color(0xC9D6C4);
    public static final Color RED = new Color(0xB03A2E);

    private UI() {
    }

    public static Font font(int style, int size) {
        return new Font(Font.SANS_SERIF, style, size);
    }

    public static void installLookAndFeel() {
        UIManager.put("control", CREAM);
        UIManager.put("nimbusBase", GREEN);
        UIManager.put("nimbusBlueGrey", new Color(0x8A9A8C));
        UIManager.put("nimbusFocus", ORANGE);
        UIManager.put("nimbusSelectionBackground", GREEN_SOFT);
        UIManager.put("nimbusLightBackground", CARD);
        UIManager.put("text", GREEN);
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
        UIManager.put("Table.alternateRowColor", new Color(0xF8F3E8));
        UIManager.getLookAndFeelDefaults().put("defaultFont", font(Font.PLAIN, 14));
    }

    // ---------- layout pieces ----------

    /** Dark green bar at the top of every dashboard. */
    public static JPanel header(String title, String subtitle, Runnable onLogout) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(GREEN);
        bar.setBorder(new EmptyBorder(14, 24, 14, 24));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label("AgriDirect · Farmer-to-Buyer Marketplace", Font.BOLD, 12, ORANGE));
        text.add(Box.createVerticalStrut(2));
        text.add(label(title, Font.BOLD, 22, CREAM));
        text.add(label(subtitle, Font.PLAIN, 13, LIGHT_TEXT));
        bar.add(text, BorderLayout.WEST);

        if (onLogout != null) {
            JButton logout = button("Log out", false);
            logout.addActionListener(e -> onLogout.run());
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
            right.setOpaque(false);
            right.add(logout);
            bar.add(right, BorderLayout.EAST);
        }
        return bar;
    }

    public static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    public static JLabel sectionTitle(String text) {
        JLabel l = label(text, Font.BOLD, 16, GREEN);
        l.setBorder(new EmptyBorder(4, 0, 8, 0));
        return l;
    }

    public static JButton button(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setFont(font(Font.BOLD, 13));
        b.setBackground(primary ? ORANGE_DARK : CARD);
        b.setForeground(primary ? Color.WHITE : GREEN);
        b.setBorder(new CompoundBorder(new LineBorder(primary ? ORANGE_DARK : BORDER, 1, true),
                new EmptyBorder(8, 16, 8, 16)));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** A dashboard number tile. Keep the returned value label to update it later. */
    public static JPanel statCard(String caption, JLabel value, boolean highlight) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(highlight ? GREEN : CARD);
        card.setBorder(new CompoundBorder(new LineBorder(highlight ? GREEN : BORDER, 1, true),
                new EmptyBorder(16, 20, 16, 20)));
        JLabel cap = label(caption, Font.PLAIN, 13, highlight ? LIGHT_TEXT : MUTED);
        value.setFont(font(Font.BOLD, 28));
        value.setForeground(highlight ? new Color(0xE8A04A) : GREEN);
        card.add(cap);
        card.add(Box.createVerticalStrut(6));
        card.add(value);
        return card;
    }

    /** A white rounded panel with a title, holding any content. */
    public static JPanel card(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD);
        p.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(14, 16, 14, 16)));
        if (title != null) {
            p.add(sectionTitle(title), BorderLayout.NORTH);
        }
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    public static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(16, 16));
        p.setBackground(CREAM);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        return p;
    }

    public static JPanel toolbar(JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        for (JComponent c : items) {
            p.add(c);
        }
        return p;
    }

    /** Two-column label / field form. */
    public static JPanel form(String[] labels, JComponent[] fields) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 8);
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0;
            g.gridy = i;
            g.anchor = GridBagConstraints.LINE_END;
            g.fill = GridBagConstraints.NONE;
            g.weightx = 0;
            p.add(label(labels[i], Font.BOLD, 13, MUTED), g);
            g.gridx = 1;
            g.anchor = GridBagConstraints.LINE_START;
            g.fill = GridBagConstraints.HORIZONTAL;
            g.weightx = 1;
            p.add(fields[i], g);
        }
        return p;
    }

    // ---------- tables ----------

    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                for (int r = 0; r < getRowCount(); r++) {
                    Object v = getValueAt(r, column);
                    if (v != null) {
                        return v.getClass();
                    }
                }
                return Object.class;
            }
        };
    }

    public static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(30);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(0xEAE2CF));
        t.getTableHeader().setFont(font(Font.BOLD, 13));
        t.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        t.setDefaultRenderer(Integer.class, left);
        return t;
    }

    public static JScrollPane scroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(CARD);
        return sp;
    }

    /** Colours a status column (order status or stock status) so it reads at a glance. */
    public static void statusColumn(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                setFont(font(Font.BOLD, 13));
                if (!selected) {
                    String s = String.valueOf(value);
                    switch (s) {
                        case "DELIVERED", "Active" -> setForeground(LEAF);
                        case "CANCELLED", "Out of stock", "Removed" -> setForeground(RED);
                        case "Low stock", "PLACED" -> setForeground(ORANGE_DARK);
                        default -> setForeground(GREEN);
                    }
                }
                return c;
            }
        });
    }

    /** The id in column 0 of the selected row, or null when nothing is selected. */
    public static Integer selectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return (Integer) table.getModel().getValueAt(table.convertRowIndexToModel(row), 0);
    }

    public static void widths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // ---------- messages ----------

    @FunctionalInterface
    public interface Action {
        void run() throws Exception;
    }

    /** Runs an action and turns any error into a friendly dialog. Returns true if it succeeded. */
    public static boolean attempt(Component parent, Action action) {
        try {
            action.run();
            return true;
        } catch (ValidationException e) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Please check", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent, "Database error: " + e.getMessage(), "Database error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, "Something went wrong: " + e, "Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "AgriDirect", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    public static void hint(Component parent) {
        info(parent, "Select a row in the table first.");
    }
}
