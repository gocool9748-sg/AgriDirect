package com.agrimarket.ui;

import com.agrimarket.model.OrderItem;
import com.agrimarket.util.Format;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Read-only list of the line items in an order. */
public final class OrderItemsDialog {

    private OrderItemsDialog() {
    }

    public static void show(Component parent, int orderId, List<OrderItem> items) {
        DefaultTableModel model = UI.model("Product", "Quantity", "Unit price", "Subtotal");
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem i : items) {
            model.addRow(new Object[] {i.productName(), Format.kg(i.quantityKg()), Format.money(i.unitPrice()),
                Format.money(i.subtotal())});
            total = total.add(i.subtotal());
        }
        JTable table = UI.table(model);
        JPanel page = UI.page();
        page.add(UI.sectionTitle("Order #" + orderId), BorderLayout.NORTH);
        page.add(UI.scroll(table), BorderLayout.CENTER);
        JLabel totalLabel = UI.label("Total: " + Format.money(total), Font.BOLD, 16, UI.GREEN);
        page.add(totalLabel, BorderLayout.SOUTH);

        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(parent), "Order #" + orderId,
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        d.setContentPane(page);
        d.setSize(new Dimension(560, 320));
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }
}
