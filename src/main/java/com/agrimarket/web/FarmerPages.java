package com.agrimarket.web;

import static com.agrimarket.web.Html.esc;

import com.agrimarket.dao.DashboardDAO;
import com.agrimarket.dao.OrderDAO;
import com.agrimarket.model.Category;
import com.agrimarket.model.Farmer;
import com.agrimarket.model.Order;
import com.agrimarket.model.OrderStatus;
import com.agrimarket.model.Product;
import com.agrimarket.service.OrderService;
import com.agrimarket.service.ProductService;
import com.agrimarket.service.ValidationException;
import com.agrimarket.util.Format;
import com.agrimarket.web.WebServer.Req;
import com.agrimarket.web.WebServer.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Farmer pages: overview, my products (add / edit / stock / remove), orders received. */
final class FarmerPages {

    private static final ProductService PRODUCTS = new ProductService();
    private static final OrderService ORDERS = new OrderService();
    private static final OrderDAO ORDER_DAO = new OrderDAO();
    private static final DashboardDAO DASHBOARD = new DashboardDAO();

    private FarmerPages() {
    }

    static void register(WebServer web) {
        web.get("/farmer", FarmerPages::overview);
        web.get("/farmer/products", r -> products(r, null, null));
        web.post("/farmer/products/save", FarmerPages::saveProduct);
        web.post("/farmer/products/stock", FarmerPages::updateStock);
        web.post("/farmer/products/toggle", FarmerPages::toggle);
        web.get("/farmer/orders", FarmerPages::orders);
        web.post("/farmer/orders/status", FarmerPages::changeStatus);
    }

    private static String subtitle(Farmer f) {
        return "Farmer dashboard · " + f.village() + ", " + f.district() + " · " + f.phone();
    }

    private static Result page(Req r, String active, String body) {
        Farmer f = r.session.farmer;
        return Result.html(Html.page(r.session, "Welcome, " + f.name(), subtitle(f), active, body));
    }

    // ---------------------------------------------------------------- overview

    private static Result overview(Req r) throws Exception {
        Farmer f = r.session.farmer;
        DashboardDAO.FarmerStats st = DASHBOARD.farmerStats(f.id());
        StringBuilder b = new StringBuilder("<div class=\"stats\">")
                .append(Html.stat("Earnings · last 30 days", Format.money(st.earnings30d()), true))
                .append(Html.stat("Orders to act on", String.valueOf(st.pendingOrders()), false))
                .append(Html.stat("Active listings", String.valueOf(st.activeListings()), false))
                .append(Html.stat("Low / out of stock", String.valueOf(st.lowStock()), false))
                .append(Html.stat("Sold · last 30 days", Format.kg(st.kgSold30d()), false))
                .append("</div>");
        List<String[]> rows = new ArrayList<>();
        for (Order o : ORDER_DAO.findRecentForFarmer(f.id(), 12)) {
            rows.add(new String[] {"#" + o.id(), Html.date(o.orderDate()), esc(o.buyerName()), esc(o.itemsSummary()),
                Html.money(o.totalAmount()), Html.badge(o.status().name()), AuthPages.itemsLink(o.id(), "/farmer")});
        }
        b.append(Html.card("Recent orders", Html.table(
                new String[] {"Order #", "Date", "Buyer", "Items", "Total", "Status", ""}, rows,
                "No orders yet. Add products so buyers can find them.")));
        if (st.pendingOrders() > 0) {
            b.append("<a class=\"btn primary\" href=\"/farmer/orders\">Act on ").append(st.pendingOrders())
                    .append(" new order").append(st.pendingOrders() == 1 ? "" : "s").append("</a>");
        }
        return page(r, "/farmer", b.toString());
    }

    // ---------------------------------------------------------------- products

    /** @param values form values to show again after a mistake (null for a fresh form) */
    private static Result products(Req r, Map<String, String> values, String error) throws Exception {
        Session s = r.session;
        Farmer f = s.farmer;
        List<Product> mine = PRODUCTS.farmerProducts(f.id());
        List<Category> categories = PRODUCTS.categories();

        // The add/edit form: ?edit=ID fills it with that product.
        Product editing = null;
        String editParam = values != null ? values.getOrDefault("id", "") : r.param("edit");
        if (!editParam.isBlank()) {
            for (Product p : mine) {
                if (String.valueOf(p.id()).equals(editParam.trim())) {
                    editing = p;
                }
            }
        }
        String name = values != null ? values.get("name") : editing == null ? "" : editing.name();
        String desc = values != null ? values.get("description") : editing == null ? "" : editing.description();
        String price = values != null ? esc(values.get("price")) : editing == null ? "" : Html.plain(editing.pricePerKg());
        String qty = values != null ? esc(values.get("qty")) : editing == null ? "" : Html.plain(editing.qtyAvailable());
        String cat = values != null ? values.get("category") : editing == null ? "" : String.valueOf(editing.categoryId());

        StringBuilder form = new StringBuilder();
        if (error != null) {
            form.append("<div class=\"flash err\">").append(esc(error)).append("</div>");
        }
        form.append("<form method=\"post\" action=\"/farmer/products/save\" class=\"form\">").append(Html.csrf(s));
        if (editing != null) {
            form.append("<input type=\"hidden\" name=\"id\" value=\"").append(editing.id()).append("\">");
        }
        form.append("<label for=\"category\">Category</label><select id=\"category\" name=\"category\" required>")
                .append(Html.option("", "Choose…", cat == null || cat.isBlank()));
        for (Category c : categories) {
            form.append(Html.option(String.valueOf(c.id()), c.name(), String.valueOf(c.id()).equals(cat)));
        }
        form.append("</select>")
                .append("<label for=\"pname\">Product name</label><input id=\"pname\" name=\"name\" maxlength=\"80\" required value=\"")
                .append(esc(name)).append("\">")
                .append("<label for=\"pdesc\">Description</label><input id=\"pdesc\" name=\"description\" maxlength=\"255\" value=\"")
                .append(esc(desc)).append("\">")
                .append("<label for=\"price\">Price per kg (₹)</label><input id=\"price\" name=\"price\" inputmode=\"decimal\" required value=\"")
                .append(price).append("\">")
                .append("<label for=\"qty\">Quantity available (kg)</label><input id=\"qty\" name=\"qty\" inputmode=\"decimal\" required value=\"")
                .append(qty).append("\">")
                .append("<span></span><div><button class=\"btn primary\" type=\"submit\">")
                .append(editing == null ? "Add product" : "Save changes").append("</button>");
        if (editing != null) {
            form.append(" <a class=\"btn\" href=\"/farmer/products\">Cancel</a>");
        }
        form.append("</div></form>");

        List<String[]> rows = new ArrayList<>();
        for (Product p : mine) {
            String actions = "<a class=\"btn\" href=\"/farmer/products?edit=" + p.id() + "\">Edit</a>"
                    + "<form class=\"inline\" method=\"post\" action=\"/farmer/products/stock\">" + Html.csrf(s)
                    + "<input type=\"hidden\" name=\"id\" value=\"" + p.id() + "\">"
                    + "<input type=\"hidden\" name=\"back\" value=\"/farmer/products\">"
                    + "<input class=\"small\" name=\"qty\" inputmode=\"decimal\" aria-label=\"New stock in kg\" value=\""
                    + Html.plain(p.qtyAvailable()) + "\"><button class=\"btn\" type=\"submit\">Update stock</button></form>"
                    + (p.active()
                        ? Html.postButton(s, "/farmer/products/toggle", "Remove", "btn danger",
                            "Remove " + p.name() + " from the marketplace? Past orders are kept.",
                            "id", String.valueOf(p.id()), "list", "false", "back", "/farmer/products")
                        : Html.postButton(s, "/farmer/products/toggle", "Relist", "btn", null,
                            "id", String.valueOf(p.id()), "list", "true", "back", "/farmer/products"));
            rows.add(new String[] {String.valueOf(p.id()), esc(p.name()), esc(p.categoryName()), Html.money(p.pricePerKg()),
                Html.kg(p.qtyAvailable()), Html.badge(p.stockStatus()), esc(p.description()), actions});
        }
        String body = Html.card(editing == null ? "Add a product" : "Edit " + editing.name(), form.toString())
                + Html.card("My listings", Html.table(
                        new String[] {"ID", "Product", "Category", "Price / kg", "Available", "Status", "Description", ""},
                        rows, "You have no products yet. Add your first one above."));
        return page(r, "/farmer/products", body);
    }

    private static Result saveProduct(Req r) throws Exception {
        Session s = r.session;
        try {
            Category category = null;
            for (Category c : PRODUCTS.categories()) {
                if (String.valueOf(c.id()).equals(r.param("category"))) {
                    category = c;
                }
            }
            String id = r.param("id");
            if (id.isBlank()) {
                PRODUCTS.addProduct(s.farmer.id(), category, r.param("name"), r.param("description"), r.param("price"),
                        r.param("qty"));
                s.ok(r.param("name").trim() + " is now listed for buyers.");
            } else {
                PRODUCTS.updateProduct(r.intParam("id"), s.farmer.id(), category, r.param("name"),
                        r.param("description"), r.param("price"), r.param("qty"));
                s.ok("Saved " + r.param("name").trim() + ".");
            }
            return Result.redirect("/farmer/products");
        } catch (ValidationException e) {
            return products(r, r.form, e.getMessage());
        }
    }

    private static Result updateStock(Req r) throws Exception {
        PRODUCTS.updateStock(r.intParam("id"), r.session.farmer.id(), r.param("qty"));
        r.session.ok("Stock updated to " + r.param("qty").trim() + " kg.");
        return Result.redirect("/farmer/products");
    }

    private static Result toggle(Req r) throws Exception {
        boolean list = "true".equals(r.param("list"));
        PRODUCTS.setListed(r.intParam("id"), r.session.farmer.id(), list);
        r.session.ok(list ? "Listed for sale again." : "Removed from the marketplace. Past orders are kept.");
        return Result.redirect("/farmer/products");
    }

    // ---------------------------------------------------------------- orders

    private static Result orders(Req r) throws Exception {
        Session s = r.session;
        OrderStatus filter = null;
        try {
            if (!r.param("status").isBlank()) {
                filter = OrderStatus.valueOf(r.param("status"));
            }
        } catch (IllegalArgumentException ignored) {
            // unknown filter: show all
        }
        StringBuilder tabs = new StringBuilder("<div class=\"toolbar\">");
        tabs.append("<a class=\"btn").append(filter == null ? " primary" : "").append("\" href=\"/farmer/orders\">All</a>");
        for (OrderStatus st : OrderStatus.values()) {
            tabs.append("<a class=\"btn").append(st == filter ? " primary" : "").append("\" href=\"/farmer/orders?status=")
                    .append(st.name()).append("\">").append(st.name().charAt(0)).append(st.name().substring(1).toLowerCase())
                    .append("</a>");
        }
        tabs.append("</div>");

        String here = filter == null ? "/farmer/orders" : "/farmer/orders?status=" + filter.name();
        List<String[]> rows = new ArrayList<>();
        for (Order o : ORDERS.farmerOrders(s.farmer.id(), filter)) {
            StringBuilder act = new StringBuilder(AuthPages.itemsLink(o.id(), here));
            String id = String.valueOf(o.id());
            switch (o.status()) {
                case PLACED -> act.append(status(s, id, "CONFIRMED", "Confirm", "btn primary", null, here))
                        .append(status(s, id, "CANCELLED", "Reject", "btn danger",
                                "Reject order #" + id + "? The stock will be added back to your listing.", here));
                case CONFIRMED -> act.append(status(s, id, "SHIPPED", "Mark shipped", "btn primary", null, here))
                        .append(status(s, id, "CANCELLED", "Reject", "btn danger",
                                "Reject order #" + id + "? The stock will be added back to your listing.", here));
                case SHIPPED -> act.append(status(s, id, "DELIVERED", "Mark delivered", "btn primary", null, here));
                default -> {
                }
            }
            rows.add(new String[] {"#" + o.id(), Html.date(o.orderDate()), esc(o.buyerName()), esc(o.buyerPhone()),
                esc(o.itemsSummary()), Html.money(o.totalAmount()), esc(o.paymentLabel()), esc(o.deliveryAddress()),
                Html.badge(o.status().name()), act.toString()});
        }
        String body = tabs + Html.card("Orders from buyers", Html.table(new String[] {"Order #", "Date", "Buyer", "Phone",
            "Items", "Total", "Payment", "Deliver to", "Status", ""}, rows, "No orders here."));
        return page(r, "/farmer/orders", body);
    }

    private static String status(Session s, String id, String next, String label, String cls, String confirm,
                                 String back) {
        return Html.postButton(s, "/farmer/orders/status", label, cls, confirm, "id", id, "next", next, "back", back);
    }

    private static Result changeStatus(Req r) throws Exception {
        OrderStatus next;
        try {
            next = OrderStatus.valueOf(r.param("next"));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown order status.");
        }
        int id = r.intParam("id");
        ORDERS.farmerUpdateStatus(id, r.session.farmer.id(), next);
        r.session.ok(next == OrderStatus.CANCELLED ? "Order #" + id + " rejected; the stock is back in your listing."
                : "Order #" + id + " is now " + next + ".");
        return Result.redirect(r.back("/farmer/orders"));
    }
}
