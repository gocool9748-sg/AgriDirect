package com.agrimarket.ui;

import com.agrimarket.dao.DashboardDAO;
import com.agrimarket.dao.OrderDAO;
import com.agrimarket.model.Farmer;
import com.agrimarket.model.Order;
import com.agrimarket.model.OrderStatus;
import com.agrimarket.model.Product;
import com.agrimarket.service.OrderService;
import com.agrimarket.service.ProductService;
import com.agrimarket.util.Format;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Farmer home: overview numbers, product listings and incoming orders. */
public class FarmerDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    private final Farmer farmer;
    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();
    private final DashboardDAO dashboard = new DashboardDAO();
    private final OrderDAO orderDao = new OrderDAO();

    private final JLabel earnings = new JLabel("-");
    private final JLabel pending = new JLabel("-");
    private final JLabel listings = new JLabel("-");
    private final JLabel lowStock = new JLabel("-");
    private final JLabel kgSold = new JLabel("-");

    private final DefaultTableModel recentModel = UI.model("Order #", "Date", "Buyer", "Items", "Total", "Status");
    private final DefaultTableModel productModel =
            UI.model("ID", "Product", "Category", "Price / kg", "Stock", "Status", "Description");
    private final DefaultTableModel orderModel =
            UI.model("Order #", "Date", "Buyer", "Phone", "Items", "Total", "Payment", "Deliver to", "Status");
    private final JTable recentTable = UI.table(recentModel);
    private final JTable productTable = UI.table(productModel);
    private final JTable orderTable = UI.table(orderModel);
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[] {"All orders", "PLACED", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"});
    private List<Product> products = List.of();

    public FarmerDashboard(Farmer farmer) {
        super("AgriDirect · Farmer · " + farmer.name());
        this.farmer = farmer;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UI.font(java.awt.Font.BOLD, 14));
        tabs.addTab("Overview", overviewTab());
        tabs.addTab("My products", productsTab());
        tabs.addTab("Orders received", ordersTab());
        tabs.addChangeListener(e -> refreshAll());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UI.CREAM);
        root.add(UI.header("Welcome, " + farmer.name(),
                "Farmer dashboard · " + farmer.village() + ", " + farmer.district() + " · " + farmer.phone(),
                this::logout), BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        setSize(1240, 780);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        refreshAll();
    }

    // ---------------- tabs ----------------

    private JPanel overviewTab() {
        JPanel page = UI.page();
        JPanel stats = new JPanel(new GridLayout(1, 5, 14, 0));
        stats.setOpaque(false);
        stats.add(UI.statCard("Earnings · last 30 days", earnings, true));
        stats.add(UI.statCard("Orders to act on", pending, false));
        stats.add(UI.statCard("Active listings", listings, false));
        stats.add(UI.statCard("Low / out of stock", lowStock, false));
        stats.add(UI.statCard("Sold · last 30 days", kgSold, false));
        page.add(stats, BorderLayout.NORTH);
        UI.widths(recentTable, 70, 150, 160, 380, 110, 110);
        UI.statusColumn(recentTable, 5);
        page.add(UI.card("Recent orders", UI.scroll(recentTable)), BorderLayout.CENTER);
        return page;
    }

    private JPanel productsTab() {
        JPanel page = UI.page();
        JButton add = UI.button("+ Add product", true);
        JButton edit = UI.button("Edit", false);
        JButton stock = UI.button("Update stock", false);
        JButton toggle = UI.button("Remove / relist", false);
        JButton refresh = UI.button("Refresh", false);
        add.addActionListener(e -> addProduct());
        edit.addActionListener(e -> editProduct());
        stock.addActionListener(e -> updateStock());
        toggle.addActionListener(e -> toggleListing());
        refresh.addActionListener(e -> refreshAll());
        page.add(UI.toolbar(add, edit, stock, toggle, refresh), BorderLayout.NORTH);
        UI.widths(productTable, 50, 160, 140, 100, 100, 110, 360);
        UI.statusColumn(productTable, 5);
        productTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editProduct();
                }
            }
        });
        page.add(UI.card("Your listings (double-click to edit)", UI.scroll(productTable)), BorderLayout.CENTER);
        return page;
    }

    private JPanel ordersTab() {
        JPanel page = UI.page();
        JButton confirm = UI.button("Confirm", true);
        JButton ship = UI.button("Mark shipped", false);
        JButton deliver = UI.button("Mark delivered", false);
        JButton reject = UI.button("Reject / cancel", false);
        JButton details = UI.button("View items", false);
        confirm.addActionListener(e -> changeStatus(OrderStatus.CONFIRMED));
        ship.addActionListener(e -> changeStatus(OrderStatus.SHIPPED));
        deliver.addActionListener(e -> changeStatus(OrderStatus.DELIVERED));
        reject.addActionListener(e -> changeStatus(OrderStatus.CANCELLED));
        details.addActionListener(e -> showItems());
        statusFilter.addActionListener(e -> loadOrders());
        page.add(UI.toolbar(new JLabel("Show:"), statusFilter, confirm, ship, deliver, reject, details),
                BorderLayout.NORTH);
        UI.widths(orderTable, 60, 140, 140, 100, 280, 90, 120, 220, 100);
        UI.statusColumn(orderTable, 8);
        page.add(UI.card("Orders for your products", UI.scroll(orderTable)), BorderLayout.CENTER);
        return page;
    }

    // ---------------- data ----------------

    private void refreshAll() {
        UI.attempt(this, () -> {
            DashboardDAO.FarmerStats s = dashboard.farmerStats(farmer.id());
            earnings.setText(Format.money(s.earnings30d()));
            pending.setText(String.valueOf(s.pendingOrders()));
            listings.setText(String.valueOf(s.activeListings()));
            lowStock.setText(String.valueOf(s.lowStock()));
            kgSold.setText(Format.kg(s.kgSold30d()));

            recentModel.setRowCount(0);
            for (Order o : orderDao.findRecentForFarmer(farmer.id(), 12)) {
                recentModel.addRow(new Object[] {o.id(), Format.dateTime(o.orderDate()), o.buyerName(),
                    o.itemsSummary(), Format.money(o.totalAmount()), o.status().name()});
            }

            products = productService.farmerProducts(farmer.id());
            productModel.setRowCount(0);
            for (Product p : products) {
                productModel.addRow(new Object[] {p.id(), p.name(), p.categoryName(), Format.money(p.pricePerKg()),
                    Format.kg(p.qtyAvailable()), p.stockStatus(), p.description()});
            }
            loadOrders();
        });
    }

    private void loadOrders() {
        UI.attempt(this, () -> {
            int idx = statusFilter.getSelectedIndex();
            OrderStatus filter = idx <= 0 ? null : OrderStatus.values()[idx - 1];
            orderModel.setRowCount(0);
            for (Order o : orderService.farmerOrders(farmer.id(), filter)) {
                orderModel.addRow(new Object[] {o.id(), Format.dateTime(o.orderDate()), o.buyerName(), o.buyerPhone(),
                    o.itemsSummary(), Format.money(o.totalAmount()), o.paymentLabel(), o.deliveryAddress(),
                    o.status().name()});
            }
        });
    }

    private Product selectedProduct() {
        Integer id = UI.selectedId(productTable);
        if (id == null) {
            return null;
        }
        return products.stream().filter(p -> p.id() == id).findFirst().orElse(null);
    }

    // ---------------- actions ----------------

    private void addProduct() {
        UI.attempt(this, () -> {
            ProductDialog d = new ProductDialog(this, farmer.id(), productService.categories(), null);
            d.setVisible(true);
            if (d.wasSaved()) {
                refreshAll();
            }
        });
    }

    private void editProduct() {
        Product p = selectedProduct();
        if (p == null) {
            UI.hint(this);
            return;
        }
        UI.attempt(this, () -> {
            ProductDialog d = new ProductDialog(this, farmer.id(), productService.categories(), p);
            d.setVisible(true);
            if (d.wasSaved()) {
                refreshAll();
            }
        });
    }

    private void updateStock() {
        Product p = selectedProduct();
        if (p == null) {
            UI.hint(this);
            return;
        }
        String value = JOptionPane.showInputDialog(this,
                "New quantity available for " + p.name() + " (kg):", p.qtyAvailable().stripTrailingZeros().toPlainString());
        if (value != null && UI.attempt(this, () -> productService.updateStock(p.id(), farmer.id(), value))) {
            refreshAll();
        }
    }

    private void toggleListing() {
        Product p = selectedProduct();
        if (p == null) {
            UI.hint(this);
            return;
        }
        boolean relist = !p.active();
        String question = relist ? "List " + p.name() + " for sale again?"
                : "Remove " + p.name() + " from the marketplace? Past orders are kept.";
        if (UI.confirm(this, question)
                && UI.attempt(this, () -> productService.setListed(p.id(), farmer.id(), relist))) {
            refreshAll();
        }
    }

    private void changeStatus(OrderStatus next) {
        Integer id = UI.selectedId(orderTable);
        if (id == null) {
            UI.hint(this);
            return;
        }
        if (next == OrderStatus.CANCELLED
                && !UI.confirm(this, "Reject order #" + id + "? The stock will be added back to your listing.")) {
            return;
        }
        if (UI.attempt(this, () -> orderService.farmerUpdateStatus(id, farmer.id(), next))) {
            refreshAll();
        }
    }

    private void showItems() {
        Integer id = UI.selectedId(orderTable);
        if (id == null) {
            UI.hint(this);
            return;
        }
        UI.attempt(this, () -> OrderItemsDialog.show(this, id, orderService.items(id)));
    }

    private void logout() {
        dispose();
        new LoginFrame().setVisible(true);
    }
}
