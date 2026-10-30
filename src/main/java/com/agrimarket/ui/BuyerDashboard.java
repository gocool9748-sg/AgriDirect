package com.agrimarket.ui;

import com.agrimarket.model.Buyer;
import com.agrimarket.model.CartItem;
import com.agrimarket.model.Category;
import com.agrimarket.model.Order;
import com.agrimarket.model.Product;
import com.agrimarket.service.OrderService;
import com.agrimarket.service.ProductService;
import com.agrimarket.service.Validate;
import com.agrimarket.service.ValidationException;
import com.agrimarket.util.Format;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Buyer home: search the marketplace, manage the cart, place and track orders. */
public class BuyerDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    private final Buyer buyer;
    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();

    // browse
    private final JTextField searchText = new JTextField(14);
    private final JComboBox<Object> categoryBox = new JComboBox<>();
    private final JTextField locationText = new JTextField(10);
    private final JTextField maxPriceText = new JTextField(6);
    private final JTextField quantityText = new JTextField("1", 6);
    private final DefaultTableModel resultModel =
            UI.model("ID", "Product", "Category", "Farmer", "Village / district", "Price / kg", "Available", "Description");
    private final JTable resultTable = UI.table(resultModel);
    private final Map<Integer, Product> results = new LinkedHashMap<>();

    // cart
    private final Map<Integer, CartItem> cart = new LinkedHashMap<>();
    private final DefaultTableModel cartModel = UI.model("ID", "Product", "Farmer", "Price / kg", "Quantity", "Subtotal");
    private final JTable cartTable = UI.table(cartModel);
    private final JLabel cartTotal = UI.label("Total: ₹0.00", Font.BOLD, 18, UI.GREEN);
    private final JTextField address = new JTextField(36);
    private final JComboBox<String> payment = new JComboBox<>(new String[] {"Cash on delivery", "UPI on delivery"});
    private final JTabbedPane tabs = new JTabbedPane();

    // orders
    private final DefaultTableModel orderModel =
            UI.model("Order #", "Date", "Farmer", "Farmer phone", "Items", "Total", "Payment", "Status");
    private final JTable orderTable = UI.table(orderModel);

    public BuyerDashboard(Buyer buyer) {
        super("AgriDirect · Buyer · " + buyer.name());
        this.buyer = buyer;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        tabs.setFont(UI.font(Font.BOLD, 14));
        tabs.addTab("Browse produce", browseTab());
        tabs.addTab("Cart (0)", cartTab());
        tabs.addTab("My orders", ordersTab());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 2) {
                loadOrders();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UI.CREAM);
        root.add(UI.header("Hello, " + buyer.name(), "Buyer · " + buyer.city() + " · " + buyer.phone(), this::logout),
                BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        setSize(1240, 780);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);

        UI.attempt(this, () -> {
            categoryBox.addItem("All categories");
            for (Category c : productService.categories()) {
                categoryBox.addItem(c);
            }
        });
        search();
        loadOrders();
    }

    // ---------------- tabs ----------------

    private JPanel browseTab() {
        JPanel page = UI.page();
        JButton searchButton = UI.button("Search", true);
        JButton clear = UI.button("Clear", false);
        searchButton.addActionListener(e -> search());
        clear.addActionListener(e -> {
            searchText.setText("");
            locationText.setText("");
            maxPriceText.setText("");
            categoryBox.setSelectedIndex(0);
            search();
        });
        searchText.addActionListener(e -> search());
        page.add(UI.toolbar(new JLabel("Product"), searchText, new JLabel("Category"), categoryBox,
                new JLabel("Village / district"), locationText, new JLabel("Max ₹/kg"), maxPriceText,
                searchButton, clear), BorderLayout.NORTH);

        UI.widths(resultTable, 40, 120, 120, 110, 190, 90, 90, 260);
        page.add(UI.card("Available produce, cheapest first", UI.scroll(resultTable)), BorderLayout.CENTER);

        JButton addToCart = UI.button("Add to cart", true);
        addToCart.addActionListener(e -> addToCart());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setOpaque(false);
        bottom.add(new JLabel("Quantity (kg)"));
        bottom.add(quantityText);
        bottom.add(addToCart);
        page.add(bottom, BorderLayout.SOUTH);
        return page;
    }

    private JPanel cartTab() {
        JPanel page = UI.page();
        UI.widths(cartTable, 40, 180, 160, 110, 110, 120);
        JButton remove = UI.button("Remove item", false);
        remove.addActionListener(e -> {
            Integer id = UI.selectedId(cartTable);
            if (id == null) {
                UI.hint(this);
                return;
            }
            cart.remove(id);
            refreshCart();
        });
        page.add(UI.toolbar(remove), BorderLayout.NORTH);
        page.add(UI.card("Items in your cart", UI.scroll(cartTable)), BorderLayout.CENTER);

        JPanel checkout = new JPanel(new BorderLayout(12, 8));
        checkout.setOpaque(false);
        checkout.add(UI.form(new String[] {"Delivery address", "Payment"},
                new javax.swing.JComponent[] {address, payment}), BorderLayout.CENTER);
        JButton place = UI.button("Place order", true);
        place.addActionListener(e -> placeOrder());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        right.setOpaque(false);
        right.add(cartTotal);
        right.add(place);
        checkout.add(right, BorderLayout.EAST);
        checkout.add(UI.label("Items from different farmers become separate orders, so each farmer ships their own.",
                Font.PLAIN, 12, UI.MUTED), BorderLayout.SOUTH);
        page.add(checkout, BorderLayout.SOUTH);
        return page;
    }

    private JPanel ordersTab() {
        JPanel page = UI.page();
        JButton cancel = UI.button("Cancel order", false);
        JButton details = UI.button("View items", false);
        JButton refresh = UI.button("Refresh", false);
        cancel.addActionListener(e -> cancelOrder());
        details.addActionListener(e -> {
            Integer id = UI.selectedId(orderTable);
            if (id == null) {
                UI.hint(this);
                return;
            }
            UI.attempt(this, () -> OrderItemsDialog.show(this, id, orderService.items(id)));
        });
        refresh.addActionListener(e -> loadOrders());
        page.add(UI.toolbar(details, cancel, refresh), BorderLayout.NORTH);
        UI.widths(orderTable, 60, 140, 130, 110, 330, 100, 130, 100);
        UI.statusColumn(orderTable, 7);
        page.add(UI.card("Your orders", UI.scroll(orderTable)), BorderLayout.CENTER);
        return page;
    }

    // ---------------- actions ----------------

    private void search() {
        UI.attempt(this, () -> {
            Object sel = categoryBox.getSelectedItem();
            Category category = sel instanceof Category c ? c : null;
            List<Product> found = productService.search(searchText.getText(), category, locationText.getText(),
                    maxPriceText.getText());
            results.clear();
            resultModel.setRowCount(0);
            for (Product p : found) {
                results.put(p.id(), p);
                resultModel.addRow(new Object[] {p.id(), p.name(), p.categoryName(), p.farmerName(),
                    p.farmerVillage() + ", " + p.farmerDistrict(), Format.money(p.pricePerKg()),
                    Format.kg(p.qtyAvailable()), p.description()});
            }
        });
    }

    private void addToCart() {
        Integer id = UI.selectedId(resultTable);
        if (id == null) {
            UI.hint(this);
            return;
        }
        Product p = results.get(id);
        UI.attempt(this, () -> {
            BigDecimal qty = Validate.positiveAmount(quantityText.getText(), "Quantity");
            CartItem existing = cart.get(p.id());
            BigDecimal newQty = existing == null ? qty : existing.quantityKg().add(qty);
            if (newQty.compareTo(p.qtyAvailable()) > 0) {
                throw new ValidationException("Only " + Format.kg(p.qtyAvailable()) + " of " + p.name()
                        + " is available.");
            }
            cart.put(p.id(), new CartItem(p, newQty));
            refreshCart();
            UI.info(this, Format.kg(qty) + " of " + p.name() + " added to your cart.");
        });
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem i : cart.values()) {
            cartModel.addRow(new Object[] {i.product().id(), i.product().name(), i.product().farmerName(),
                Format.money(i.product().pricePerKg()), Format.kg(i.quantityKg()), Format.money(i.subtotal())});
            total = total.add(i.subtotal());
        }
        cartTotal.setText("Total: " + Format.money(total));
        tabs.setTitleAt(1, "Cart (" + cart.size() + ")");
    }

    private void placeOrder() {
        String mode = payment.getSelectedIndex() == 1 ? OrderService.UPI_ON_DELIVERY : OrderService.CASH_ON_DELIVERY;
        List<Integer> ids = new ArrayList<>();
        boolean ok = UI.attempt(this, () -> ids.addAll(
                orderService.placeOrder(buyer.id(), new ArrayList<>(cart.values()), address.getText(), mode)));
        if (ok) {
            cart.clear();
            refreshCart();
            search();
            loadOrders();
            UI.info(this, ids.size() == 1 ? "Order #" + ids.get(0) + " placed. The farmer will confirm it soon."
                    : ids.size() + " orders placed (one per farmer): "
                        + ids.stream().map(i -> "#" + i).collect(java.util.stream.Collectors.joining(", ")) + ".");
            tabs.setSelectedIndex(2);
        }
    }

    private void cancelOrder() {
        Integer id = UI.selectedId(orderTable);
        if (id == null) {
            UI.hint(this);
            return;
        }
        if (UI.confirm(this, "Cancel order #" + id + "?")
                && UI.attempt(this, () -> orderService.buyerCancel(id, buyer.id()))) {
            loadOrders();
            search();
        }
    }

    private void loadOrders() {
        UI.attempt(this, () -> {
            orderModel.setRowCount(0);
            for (Order o : orderService.buyerOrders(buyer.id())) {
                orderModel.addRow(new Object[] {o.id(), Format.dateTime(o.orderDate()), o.farmerName(), o.farmerPhone(),
                    o.itemsSummary(), Format.money(o.totalAmount()), o.paymentLabel(), o.status().name()});
            }
        });
    }

    private void logout() {
        if (!cart.isEmpty() && !UI.confirm(this, "Your cart will be emptied. Log out anyway?")) {
            return;
        }
        dispose();
        new LoginFrame().setVisible(true);
    }
}
