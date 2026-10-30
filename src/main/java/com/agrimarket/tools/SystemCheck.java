package com.agrimarket.tools;

import com.agrimarket.dao.DashboardDAO;
import com.agrimarket.db.DBConnection;
import com.agrimarket.model.Buyer;
import com.agrimarket.model.CartItem;
import com.agrimarket.model.Category;
import com.agrimarket.model.Farmer;
import com.agrimarket.model.Order;
import com.agrimarket.model.OrderStatus;
import com.agrimarket.model.Product;
import com.agrimarket.service.AuthService;
import com.agrimarket.service.OrderService;
import com.agrimarket.service.ProductService;
import com.agrimarket.service.ValidationException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * End-to-end check of the whole back end without the GUI:
 * register, list, search, order (with transaction + rollback), status flow, cancel + restock, dashboards.
 * Run it against a test database:  java -cp "out:lib/*" com.agrimarket.tools.SystemCheck
 * It creates its own users (phone numbers starting 600000...) and removes them at the end.
 */
public final class SystemCheck {

    private static int passed;

    public static void main(String[] args) throws Exception {
        // Fresh copy of the project: create the database first, the same way the app does.
        if ("first-run".equals(com.agrimarket.db.DatabaseSetup.check())
                && !com.agrimarket.db.DatabaseSetup.autoSetup()) {
            throw new IllegalStateException("Could not create the database; run \"run.bat setup\" first.");
        }
        System.out.println("Database: " + DBConnection.url());
        cleanUp();
        AuthService auth = new AuthService();
        ProductService productService = new ProductService();
        OrderService orderService = new OrderService();
        DashboardDAO dashboard = new DashboardDAO();

        Farmer farmer = auth.registerFarmer("Test Farmer", "6000000001", "Testpuram", "Testdistrict", "secret1", "secret1");
        check(farmer.id() > 0, "farmer registered");
        Buyer buyer = auth.registerBuyer("Test Buyer", "6000000002", "Testcity", "secret2", "secret2");
        check(buyer.id() > 0, "buyer registered");
        check(expectError(() -> auth.registerFarmer("X", "6000000001", "v", "d", "secret1", "secret1")),
                "duplicate phone rejected");
        check(expectError(() -> auth.registerBuyer("X", "12345", "c", "secret1", "secret1")), "bad phone rejected");

        check(auth.loginFarmer("6000000001", "secret1").isPresent(), "farmer login with correct password");
        check(auth.loginFarmer("6000000001", "wrong").isEmpty(), "farmer login with wrong password fails");
        check(auth.loginBuyer("6000000002", "secret2").isPresent(), "buyer login");

        Category veg = productService.categories().stream()
                .filter(c -> c.name().equals("Vegetables")).findFirst().orElseThrow();
        int pid = productService.addProduct(farmer.id(), veg, "Testberry", "check item", "20", "50");
        check(pid > 0, "product added");
        check(expectError(() -> productService.addProduct(farmer.id(), veg, "Bad", "", "-5", "1")),
                "negative price rejected");

        List<Product> found = productService.search("Testberry", null, "Testdistrict", "25");
        check(found.size() == 1 && found.get(0).id() == pid, "search by name, location and max price");
        check(productService.search("Testberry", null, null, "10").isEmpty(), "max price filter excludes it");
        Product p = found.get(0);

        List<Integer> ids = orderService.placeOrder(buyer.id(), List.of(new CartItem(p, new BigDecimal("12.5"))),
                "1 Test Street", OrderService.UPI_ON_DELIVERY);
        check(ids.size() == 1, "order placed");
        int orderId = ids.get(0);
        check(stock(productService, farmer.id(), pid).compareTo(new BigDecimal("37.50")) == 0, "stock reduced to 37.5 kg");
        Order o = orderService.buyerOrders(buyer.id()).get(0);
        check(o.totalAmount().compareTo(new BigDecimal("250.00")) == 0, "order total = 12.5 kg x 20 = 250");

        check(expectError(() -> orderService.placeOrder(buyer.id(), List.of(new CartItem(p, new BigDecimal("100"))),
                "1 Test Street", OrderService.CASH_ON_DELIVERY)), "ordering more than stock rejected");
        check(stock(productService, farmer.id(), pid).compareTo(new BigDecimal("37.50")) == 0,
                "rejected order left stock unchanged");

        // Two-item cart where the SECOND item fails after the first one's stock was already reduced:
        // the transaction must roll the first reduction back.
        int pid2 = productService.addProduct(farmer.id(), veg, "Testleaf", "", "10", "3");
        Product p2 = productService.search("Testleaf", null, null, null).get(0);
        check(expectError(() -> orderService.placeOrder(buyer.id(), List.of(
                new CartItem(p, new BigDecimal("5")), new CartItem(p2, new BigDecimal("4"))),
                "1 Test Street", OrderService.CASH_ON_DELIVERY)), "cart with one short item rejected");
        check(stock(productService, farmer.id(), pid).compareTo(new BigDecimal("37.50")) == 0
                && stock(productService, farmer.id(), pid2).compareTo(new BigDecimal("3.00")) == 0,
                "transaction rolled back the first item's stock reduction");
        productService.setListed(pid2, farmer.id(), false);
        check(productService.search("Testleaf", null, null, null).isEmpty(), "removed listing hidden from search");

        check(expectError(() -> orderService.farmerUpdateStatus(orderId, farmer.id(), OrderStatus.DELIVERED)),
                "cannot skip from PLACED to DELIVERED");
        orderService.farmerUpdateStatus(orderId, farmer.id(), OrderStatus.CONFIRMED);
        orderService.farmerUpdateStatus(orderId, farmer.id(), OrderStatus.SHIPPED);
        check(expectError(() -> orderService.buyerCancel(orderId, buyer.id())), "cannot cancel a shipped order");
        orderService.farmerUpdateStatus(orderId, farmer.id(), OrderStatus.DELIVERED);
        check(orderService.farmerOrders(farmer.id(), OrderStatus.DELIVERED).size() == 1, "order delivered");

        int second = orderService.placeOrder(buyer.id(), List.of(new CartItem(p, new BigDecimal("7.5"))),
                "1 Test Street", OrderService.CASH_ON_DELIVERY).get(0);
        check(stock(productService, farmer.id(), pid).compareTo(new BigDecimal("30.00")) == 0, "second order reduced stock");
        orderService.buyerCancel(second, buyer.id());
        check(stock(productService, farmer.id(), pid).compareTo(new BigDecimal("37.50")) == 0, "cancel returned stock");

        DashboardDAO.FarmerStats fs = dashboard.farmerStats(farmer.id());
        check(fs.earnings30d().compareTo(new BigDecimal("250.00")) == 0, "farmer earnings = 250 (delivered only)");
        DashboardDAO.AdminStats as = dashboard.adminStats();
        check(as.farmers() >= 1 && as.buyers() >= 1, "admin stats: " + as);
        check(!dashboard.ordersByCategory().isEmpty(), "orders by category: " + dashboard.ordersByCategory());

        cleanUp();
        System.out.println("\nAll " + passed + " checks passed.");
    }

    private static BigDecimal stock(ProductService s, int farmerId, int productId) throws Exception {
        return s.farmerProducts(farmerId).stream().filter(x -> x.id() == productId).findFirst()
                .orElseThrow().qtyAvailable();
    }

    private interface Body {
        void run() throws Exception;
    }

    private static boolean expectError(Body body) throws Exception {
        try {
            body.run();
            return false;
        } catch (ValidationException e) {
            System.out.println("    (expected) " + e.getMessage());
            return true;
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError("FAILED: " + what);
        }
        passed++;
        System.out.println("  ok  " + what);
    }

    private static void cleanUp() throws Exception {
        String[] sql = {
            "DELETE FROM order_items WHERE order_id IN (SELECT order_id FROM orders WHERE buyer_id IN "
                + "(SELECT buyer_id FROM buyers WHERE phone LIKE '600000%'))",
            "DELETE FROM orders WHERE buyer_id IN (SELECT buyer_id FROM buyers WHERE phone LIKE '600000%')",
            "DELETE FROM products WHERE farmer_id IN (SELECT farmer_id FROM farmers WHERE phone LIKE '600000%')",
            "DELETE FROM farmers WHERE phone LIKE '600000%'",
            "DELETE FROM buyers WHERE phone LIKE '600000%'"
        };
        try (Connection c = DBConnection.get()) {
            for (String s : sql) {
                try (PreparedStatement ps = c.prepareStatement(s)) {
                    ps.executeUpdate();
                }
            }
        }
    }
}
