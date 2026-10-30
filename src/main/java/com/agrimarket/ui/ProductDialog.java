package com.agrimarket.ui;

import com.agrimarket.model.Category;
import com.agrimarket.model.Product;
import com.agrimarket.service.ProductService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** Add or edit one of the farmer's listings. */
public class ProductDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final ProductService service = new ProductService();
    private final int farmerId;
    private final Product existing;
    private final JComboBox<Category> category;
    private final JTextField name = new JTextField(24);
    private final JTextField description = new JTextField(24);
    private final JTextField price = new JTextField(10);
    private final JTextField qty = new JTextField(10);
    private boolean saved;

    public ProductDialog(JFrame owner, int farmerId, List<Category> categories, Product existing) {
        super(owner, existing == null ? "Add a product" : "Edit product", true);
        this.farmerId = farmerId;
        this.existing = existing;
        this.category = new JComboBox<>(categories.toArray(new Category[0]));

        if (existing != null) {
            for (Category c : categories) {
                if (c.id() == existing.categoryId()) {
                    category.setSelectedItem(c);
                }
            }
            name.setText(existing.name());
            description.setText(existing.description() == null ? "" : existing.description());
            price.setText(existing.pricePerKg().toPlainString());
            qty.setText(existing.qtyAvailable().stripTrailingZeros().toPlainString());
        }

        JPanel root = UI.page();
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        root.add(UI.sectionTitle(existing == null ? "New listing" : "Edit listing"), BorderLayout.NORTH);
        root.add(UI.form(
                new String[] {"Category", "Product name", "Description", "Price per kg (₹)", "Quantity available (kg)"},
                new JComponent[] {category, name, description, price, qty}), BorderLayout.CENTER);
        JButton save = UI.button("Save", true);
        JButton cancel = UI.button("Cancel", false);
        save.addActionListener(e -> save());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(save);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(save);
        pack();
        setLocationRelativeTo(owner);
    }

    private void save() {
        UI.attempt(this, () -> {
            Category c = (Category) category.getSelectedItem();
            if (existing == null) {
                service.addProduct(farmerId, c, name.getText(), description.getText(), price.getText(), qty.getText());
            } else {
                service.updateProduct(existing.id(), farmerId, c, name.getText(), description.getText(),
                        price.getText(), qty.getText());
            }
            saved = true;
            dispose();
        });
    }

    public boolean wasSaved() {
        return saved;
    }
}
