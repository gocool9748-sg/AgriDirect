package com.agrimarket.web;

import static com.agrimarket.web.Html.esc;

import com.agrimarket.dao.DashboardDAO;
import com.agrimarket.dao.OrderDAO;
import com.agrimarket.model.Category;
import com.agrimarket.model.Order;
import com.agrimarket.service.ProductService;
import com.agrimarket.service.ValidationException;
import com.agrimarket.util.Format;
import com.agrimarket.web.WebServer.Req;
import com.agrimarket.web.WebServer.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Admin pages: dashboard, farmers, buyers, all orders, categories. */
final class AdminPages {

    private static final DashboardDAO DASHBOARD = new DashboardDAO();
    private static final OrderDAO ORDER_DAO = new OrderDAO();
    private static final ProductService PRODUCTS = new ProductService();

    private AdminPages() {
    }

    static void register(WebServer web) {
        web.get("/admin", AdminPages::dashboard);
        web.get("/admin/farmers", AdminPages::farmers);
        web.get("/admin/buyers", AdminPages::buyers);
        web.get("/admin/orders", AdminPages::orders);
        web.get("/admin/categories", r -> categories(r, null, null, null));
        web.post("/admin/categories/add", AdminPages::addCategory);
    }

    private static Result page(Req r, String active, String body) {
        return Result.html(Html.page(r.session, "Marketplace at a glance",
                "Signed in as admin · " + r.session.admin.username(), active, body));
    }

    private static Result dashboard(Req r) throws Exception {
        DashboardDAO.AdminStats s = DASHBOARD.adminStats();
        StringBuilder b = new StringBuilder("<div class=\"stats\">")
                .append(Html.stat("Farmers", String.valueOf(s.farmers()), false))
                .append(Html.stat("Buyers", String.valueOf(s.buyers()), false))
                .append(Html.stat("Active listings", String.valueOf(s.activeListings()), false))
                .append(Html.stat("Orders · 30 days", String.valueOf(s.orders30d()), false))
                .append(Html.stat("Open orders", String.valueOf(s.openOrders()), false))
                .append(Html.stat("Revenue · 30 days", Format.money(s.revenue30d()), true))
                .append("</div><div class=\"grid2\">");

        Map<String, Integer> byCategory = DASHBOARD.ordersByCategory();
        int most = byCategory.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        StringBuilder bars = new StringBuilder("<div class=\"bars\">");
        for (Map.Entry<String, Integer> e : byCategory.entrySet()) {
            int pct = Math.max(2, e.getValue() * 100 / Math.max(1, most));
            bars.append("<div class=\"row\"><span>").append(esc(e.getKey())).append("</span><div class=\"track\">")
                    .append("<div class=\"fill\" style=\"width:").append(pct).append("%\"></div></div><b>")
                    .append(e.getValue()).append("</b></div>");
        }
        if (byCategory.isEmpty()) {
            bars.append("<div class=\"empty\">No orders in the last 30 days.</div>");
        }
        bars.append("</div>");
        b.append(Html.card("Orders by category · last 30 days", bars.toString()));

        List<String[]> top = new ArrayList<>();
        for (DashboardDAO.TopProduct t : DASHBOARD.topProducts(8)) {
            top.add(new String[] {esc(t.product()), esc(t.farmer()), Html.kg(t.kgSold()), Html.money(t.revenue())});
        }
        b.append(Html.card("Top products by revenue", Html.table(new String[] {"Product", "Farmer", "Sold", "Revenue"},
                top, "No sales yet."))).append("</div>");

        b.append(Html.card("Recent orders", orderTable(ORDER_DAO.findRecent(15), false, "/admin")));
        return page(r, "/admin", b.toString());
    }

    private static String orderTable(List<Order> orders, boolean payment, String back) {
        List<String[]> rows = new ArrayList<>();
        for (Order o : orders) {
            List<String> cells = new ArrayList<>(List.of("#" + o.id(), Html.date(o.orderDate()), esc(o.buyerName()),
                    esc(o.farmerName()), esc(o.itemsSummary()), Html.money(o.totalAmount())));
            if (payment) {
                cells.add(esc(o.paymentLabel()));
            }
            cells.add(Html.badge(o.status().name()));
            cells.add(AuthPages.itemsLink(o.id(), back));
            rows.add(cells.toArray(new String[0]));
        }
        String[] headers = payment
                ? new String[] {"Order #", "Date", "Buyer", "Farmer", "Items", "Total", "Payment", "Status", ""}
                : new String[] {"Order #", "Date", "Buyer", "Farmer", "Items", "Total", "Status", ""};
        return Html.table(headers, rows, "No orders yet.");
    }

    private static Result farmers(Req r) throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (DashboardDAO.FarmerRow f : DASHBOARD.farmers()) {
            rows.add(new String[] {String.valueOf(f.id()), esc(f.name()), esc(f.phone()), esc(f.village()),
                esc(f.district()), String.valueOf(f.listings()), String.valueOf(f.orders()), Html.money(f.earnings()),
                Html.date(f.joined())});
        }
        return page(r, "/admin/farmers", Html.card("Registered farmers", Html.table(new String[] {"ID", "Name", "Phone",
            "Village", "District", "Listings", "Orders", "Earnings (delivered)", "Joined"}, rows, "No farmers yet.")));
    }

    private static Result buyers(Req r) throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (DashboardDAO.BuyerRow b : DASHBOARD.buyers()) {
            rows.add(new String[] {String.valueOf(b.id()), esc(b.name()), esc(b.phone()), esc(b.city()),
                String.valueOf(b.orders()), Html.money(b.spent()), Html.date(b.joined())});
        }
        return page(r, "/admin/buyers", Html.card("Registered buyers", Html.table(new String[] {"ID", "Name", "Phone",
            "City", "Orders", "Spent", "Joined"}, rows, "No buyers yet.")));
    }

    private static Result orders(Req r) throws Exception {
        return page(r, "/admin/orders", Html.card("Every order on the platform",
                orderTable(ORDER_DAO.findAll(), true, "/admin/orders")));
    }

    private static Result categories(Req r, String error, String name, String description) throws Exception {
        Session s = r.session;
        StringBuilder b = new StringBuilder();
        if (error != null) {
            b.append("<div class=\"flash err\">").append(esc(error)).append("</div>");
        }
        b.append("<form method=\"post\" action=\"/admin/categories/add\" class=\"form\">").append(Html.csrf(s))
                .append("<label for=\"cname\">New category</label><input id=\"cname\" name=\"name\" maxlength=\"50\" required value=\"")
                .append(esc(name)).append("\">")
                .append("<label for=\"cdesc\">Description</label><input id=\"cdesc\" name=\"description\" maxlength=\"255\" value=\"")
                .append(esc(description)).append("\">")
                .append("<span></span><div><button class=\"btn primary\" type=\"submit\">Add category</button></div></form>");
        List<String[]> rows = new ArrayList<>();
        for (Category c : PRODUCTS.categories()) {
            rows.add(new String[] {String.valueOf(c.id()), esc(c.name()), esc(c.description())});
        }
        return page(r, "/admin/categories", Html.card("Add a category", b.toString())
                + Html.card("Product categories", Html.table(new String[] {"ID", "Name", "Description"}, rows,
                        "No categories yet.")));
    }

    private static Result addCategory(Req r) throws Exception {
        try {
            PRODUCTS.addCategory(r.param("name"), r.param("description"));
        } catch (ValidationException e) {
            return categories(r, e.getMessage(), r.param("name"), r.param("description"));
        }
        r.session.ok("Category \"" + r.param("name").trim() + "\" added.");
        return Result.redirect("/admin/categories");
    }
}
