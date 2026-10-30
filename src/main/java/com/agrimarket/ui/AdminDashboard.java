package com.agrimarket.ui;

import com.agrimarket.dao.DashboardDAO;
import com.agrimarket.dao.OrderDAO;
import com.agrimarket.model.Admin;
import com.agrimarket.model.Category;
import com.agrimarket.model.Order;
import com.agrimarket.service.ProductService;
import com.agrimarket.util.Format;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Admin dashboard: platform-wide KPIs, a category chart, users, orders and categories. */
public class AdminDashboard extends JFrame {

    private static final long serialVersionUID = 1L;

    private final DashboardDAO dashboard = new DashboardDAO();
    private final OrderDAO orderDao = new OrderDAO();
    private final ProductService productService = new ProductService();

    private final JLabel farmers = new JLabel("-");
    private final JLabel buyers = new JLabel("-");
    private final JLabel listings = new JLabel("-");
    private final JLabel orders = new JLabel("-");
    private final JLabel open = new JLabel("-");
    private final JLabel revenue = new JLabel("-");
    private final BarChartPanel chart = new BarChartPanel("No orders in the last 30 days yet.");

    private final DefaultTableModel recentModel = UI.model("Order #", "Date", "Buyer", "Farmer", "Items", "Total", "Status");
    private final DefaultTableModel topModel = UI.model("Product", "Farmer", "Sold", "Revenue");
    private final DefaultTableModel farmerModel =
            UI.model("ID", "Name", "Phone", "Village", "District", "Listings", "Orders", "Earned (delivered)", "Joined");
    private final DefaultTableModel buyerModel = UI.model("ID", "Name", "Phone", "City", "Orders", "Spent", "Joined");
    private final DefaultTableModel allOrdersModel =
            UI.model("Order #", "Date", "Buyer", "Farmer", "Items", "Total", "Payment", "Status");
    private final DefaultTableModel categoryModel = UI.model("ID", "Category", "Description");
    private final JTable recentTable = UI.table(recentModel);

    public AdminDashboard(Admin admin) {
        super("AgriDirect · Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UI.font(Font.BOLD, 14));
        tabs.addTab("Dashboard", overviewTab());
        tabs.addTab("Farmers", tablePage("Registered farmers", farmerModel, -1));
        tabs.addTab("Buyers", tablePage("Registered buyers", buyerModel, -1));
        tabs.addTab("All orders", tablePage("Every order on the platform", allOrdersModel, 7));
        tabs.addTab("Categories", categoriesTab());
        tabs.addChangeListener(e -> refreshAll());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UI.CREAM);
        root.add(UI.header("Marketplace at a glance", "Signed in as admin · " + admin.username(), this::logout),
                BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1050, 660));
        setLocationRelativeTo(null);
        refreshAll();
    }

    private JPanel overviewTab() {
        JPanel page = UI.page();
        JPanel stats = new JPanel(new GridLayout(1, 6, 12, 0));
        stats.setOpaque(false);
        stats.add(UI.statCard("Farmers", farmers, false));
        stats.add(UI.statCard("Buyers", buyers, false));
        stats.add(UI.statCard("Active listings", listings, false));
        stats.add(UI.statCard("Orders · 30 days", orders, false));
        stats.add(UI.statCard("Open orders", open, false));
        stats.add(UI.statCard("Revenue · 30 days", revenue, true));
        page.add(stats, BorderLayout.NORTH);

        JPanel middle = new JPanel(new GridLayout(1, 2, 16, 0));
        middle.setOpaque(false);
        middle.add(UI.card("Orders by category · last 30 days", chart));
        JTable top = UI.table(topModel);
        middle.add(UI.card("Top products by revenue", UI.scroll(top)));

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 16));
        center.setOpaque(false);
        center.add(middle);
        UI.widths(recentTable, 60, 140, 150, 120, 330, 100, 100);
        UI.statusColumn(recentTable, 6);
        center.add(UI.card("Recent orders", UI.scroll(recentTable)));
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private JPanel tablePage(String title, DefaultTableModel model, int statusColumn) {
        JPanel page = UI.page();
        JTable t = UI.table(model);
        if (statusColumn >= 0) {
            UI.statusColumn(t, statusColumn);
        }
        JButton refresh = UI.button("Refresh", false);
        refresh.addActionListener(e -> refreshAll());
        page.add(UI.toolbar(refresh), BorderLayout.NORTH);
        page.add(UI.card(title, UI.scroll(t)), BorderLayout.CENTER);
        return page;
    }

    private JPanel categoriesTab() {
        JPanel page = UI.page();
        JTextField name = new JTextField(16);
        JTextField desc = new JTextField(28);
        JButton add = UI.button("Add category", true);
        add.addActionListener(e -> {
            if (UI.attempt(this, () -> productService.addCategory(name.getText(), desc.getText()))) {
                name.setText("");
                desc.setText("");
                refreshAll();
            }
        });
        JPanel form = UI.form(new String[] {"New category", "Description"}, new JComponent[] {name, desc});
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(form, BorderLayout.WEST);
        top.add(UI.toolbar(add), BorderLayout.SOUTH);
        page.add(top, BorderLayout.NORTH);
        page.add(UI.card("Product categories", UI.scroll(UI.table(categoryModel))), BorderLayout.CENTER);
        return page;
    }

    private void refreshAll() {
        UI.attempt(this, () -> {
            DashboardDAO.AdminStats s = dashboard.adminStats();
            farmers.setText(String.valueOf(s.farmers()));
            buyers.setText(String.valueOf(s.buyers()));
            listings.setText(String.valueOf(s.activeListings()));
            orders.setText(String.valueOf(s.orders30d()));
            open.setText(String.valueOf(s.openOrders()));
            revenue.setText(Format.money(s.revenue30d()));
            chart.setData(dashboard.ordersByCategory());

            topModel.setRowCount(0);
            for (DashboardDAO.TopProduct t : dashboard.topProducts(8)) {
                topModel.addRow(new Object[] {t.product(), t.farmer(), Format.kg(t.kgSold()), Format.money(t.revenue())});
            }
            recentModel.setRowCount(0);
            for (Order o : orderDao.findRecent(15)) {
                recentModel.addRow(new Object[] {o.id(), Format.dateTime(o.orderDate()), o.buyerName(), o.farmerName(),
                    o.itemsSummary(), Format.money(o.totalAmount()), o.status().name()});
            }
            farmerModel.setRowCount(0);
            for (DashboardDAO.FarmerRow f : dashboard.farmers()) {
                farmerModel.addRow(new Object[] {f.id(), f.name(), f.phone(), f.village(), f.district(), f.listings(),
                    f.orders(), Format.money(f.earnings()), Format.dateTime(f.joined())});
            }
            buyerModel.setRowCount(0);
            for (DashboardDAO.BuyerRow b : dashboard.buyers()) {
                buyerModel.addRow(new Object[] {b.id(), b.name(), b.phone(), b.city(), b.orders(), Format.money(b.spent()),
                    Format.dateTime(b.joined())});
            }
            allOrdersModel.setRowCount(0);
            for (Order o : orderDao.findAll()) {
                allOrdersModel.addRow(new Object[] {o.id(), Format.dateTime(o.orderDate()), o.buyerName(), o.farmerName(),
                    o.itemsSummary(), Format.money(o.totalAmount()), o.paymentLabel(), o.status().name()});
            }
            categoryModel.setRowCount(0);
            for (Category c : productService.categories()) {
                categoryModel.addRow(new Object[] {c.id(), c.name(), c.description()});
            }
        });
    }

    private void logout() {
        dispose();
        new LoginFrame().setVisible(true);
    }
}
