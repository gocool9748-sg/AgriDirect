package com.agrimarket.web;

import static com.agrimarket.web.Html.esc;

import com.agrimarket.dao.ProductDAO;
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
import com.agrimarket.web.WebServer.Req;
import com.agrimarket.web.WebServer.Result;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Buyer pages: browse and search produce, cart and checkout, my orders. */
final class BuyerPages {

    private static final ProductService PRODUCTS = new ProductService();
    private static final ProductDAO PRODUCT_DAO = new ProductDAO();
    private static final OrderService ORDERS = new OrderService();

    private BuyerPages() {
    }

    static void register(WebServer web) {
        web.get("/buyer", BuyerPages::browse);
        web.post("/buyer/cart/add", BuyerPages::addToCart);
        web.get("/buyer/cart", BuyerPages::cart);
        web.post("/buyer/cart/remove", BuyerPages::removeFromCart);
        web.post("/buyer/checkout", BuyerPages::checkout);
        web.get("/buyer/orders", BuyerPages::orders);
        web.post("/buyer/orders/cancel", BuyerPages::cancel);
    }

    private static Result page(Req r, String active, String body) {
        Buyer b = r.session.buyer;
        return Result.html(Html.page(r.session, "Hello, " + b.name(), "Buyer · " + b.city() + " · " + b.phone(), active,
                body));
    }

    // ---------------------------------------------------------------- browse

    private static Result browse(Req r) throws Exception {
        Session s = r.session;
        String q = r.param("q");
        String cat = r.param("category");
        String loc = r.param("location");
        String max = r.param("max");
        List<Category> categories = PRODUCTS.categories();
        Category category = null;
        for (Category c : categories) {
            if (String.valueOf(c.id()).equals(cat)) {
                category = c;
            }
        }

        StringBuilder b = new StringBuilder("<form class=\"toolbar\" method=\"get\" action=\"/buyer\">")
                .append("<div class=\"f\"><label for=\"q\">Product</label><input id=\"q\" name=\"q\" value=\"")
                .append(esc(q)).append("\" placeholder=\"e.g. tomato\"></div>")
                .append("<div class=\"f\"><label for=\"cat\">Category</label><select id=\"cat\" name=\"category\">")
                .append(Html.option("", "All categories", category == null));
        for (Category c : categories) {
            b.append(Html.option(String.valueOf(c.id()), c.name(), c.equals(category)));
        }
        b.append("</select></div>")
                .append("<div class=\"f\"><label for=\"loc\">Village / district</label><input id=\"loc\" name=\"location\" value=\"")
                .append(esc(loc)).append("\"></div>")
                .append("<div class=\"f\"><label for=\"max\">Max ₹/kg</label><input id=\"max\" class=\"small\" name=\"max\" inputmode=\"decimal\" value=\"")
                .append(esc(max)).append("\"></div>")
                .append("<button class=\"btn primary\" type=\"submit\">Search</button>")
                .append("<a class=\"btn\" href=\"/buyer\">Clear</a></form>");

        List<Product> found;
        try {
            found = PRODUCTS.search(q, category, loc, max);
        } catch (ValidationException e) {
            b.insert(0, "<div class=\"flash err\">" + esc(e.getMessage()) + "</div>");
            found = List.of();
        }
        List<String[]> rows = new ArrayList<>();
        for (Product p : found) {
            CartItem inCart = s.cart.get(p.id());
            String add = "<form class=\"inline\" method=\"post\" action=\"/buyer/cart/add\">" + Html.csrf(s)
                    + "<input type=\"hidden\" name=\"id\" value=\"" + p.id() + "\">"
                    + "<input type=\"hidden\" name=\"back\" value=\"" + esc(r.uri) + "\">"
                    + "<input class=\"small\" name=\"qty\" value=\"1\" inputmode=\"decimal\" aria-label=\"Quantity in kg\"> kg "
                    + "<button class=\"btn primary\" type=\"submit\">Add to cart</button></form>"
                    + (inCart == null ? "" : "<span class=\"muted\">" + Html.kg(inCart.quantityKg()) + " in cart</span>");
            rows.add(new String[] {esc(p.name()), esc(p.categoryName()), esc(p.farmerName()),
                esc(p.farmerVillage() + ", " + p.farmerDistrict()), Html.money(p.pricePerKg()), Html.kg(p.qtyAvailable()),
                esc(p.description()), add});
        }
        b.append(Html.card("Available produce, cheapest first", Html.table(new String[] {"Product", "Category", "Farmer",
            "Village / district", "Price / kg", "Available", "Description", "Order"}, rows,
            "Nothing matches your search. Try fewer filters.")));
        return page(r, "/buyer", b.toString());
    }

    private static Result addToCart(Req r) throws Exception {
        Session s = r.session;
        int id = r.intParam("id");
        BigDecimal qty = Validate.positiveAmount(r.param("qty"), "Quantity");
        Optional<Product> found = PRODUCT_DAO.findById(id);
        if (found.isEmpty() || !found.get().active() || found.get().qtyAvailable().signum() <= 0) {
            throw new ValidationException("That product is no longer available.");
        }
        Product p = found.get();
        synchronized (s) {
            CartItem existing = s.cart.get(p.id());
            BigDecimal newQty = existing == null ? qty : existing.quantityKg().add(qty);
            if (newQty.compareTo(p.qtyAvailable()) > 0) {
                throw new ValidationException("Only " + Format.kg(p.qtyAvailable()) + " of " + p.name() + " is available.");
            }
            s.cart.put(p.id(), new CartItem(p, newQty));
        }
        s.ok(Format.kg(qty) + " of " + p.name() + " added to your cart.");
        return Result.redirect(r.back("/buyer"));
    }

    // ---------------------------------------------------------------- cart

    private static Result cart(Req r) {
        Session s = r.session;
        List<String[]> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        synchronized (s) {
            for (CartItem i : s.cart.values()) {
                Product p = i.product();
                rows.add(new String[] {esc(p.name()), esc(p.farmerName()), Html.money(p.pricePerKg()),
                    Html.kg(i.quantityKg()), Html.money(i.subtotal()),
                    Html.postButton(s, "/buyer/cart/remove", "Remove", "btn danger", null, "id", String.valueOf(p.id()),
                        "back", "/buyer/cart")});
                total = total.add(i.subtotal());
            }
        }
        StringBuilder b = new StringBuilder(Html.card("Items in your cart", Html.table(
                new String[] {"Product", "Farmer", "Price / kg", "Quantity", "Subtotal", ""}, rows,
                "Your cart is empty. Go to Browse produce to add items.")));
        if (!rows.isEmpty()) {
            b.append(Html.card("Checkout", "<p class=\"total\">Total: " + Html.money(total) + "</p>"
                    + "<form method=\"post\" action=\"/buyer/checkout\" class=\"form\">" + Html.csrf(s)
                    + "<input type=\"hidden\" name=\"back\" value=\"/buyer/cart\">"
                    + "<label for=\"addr\">Delivery address</label><input id=\"addr\" name=\"address\" maxlength=\"255\" required>"
                    + "<label for=\"pay\">Payment</label><select id=\"pay\" name=\"payment\">"
                    + Html.option(OrderService.CASH_ON_DELIVERY, "Cash on delivery", true)
                    + Html.option(OrderService.UPI_ON_DELIVERY, "UPI on delivery", false) + "</select>"
                    + "<span></span><div><button class=\"btn primary\" type=\"submit\">Place order</button></div></form>"
                    + "<p class=\"muted\">Items from different farmers become separate orders, so each farmer ships "
                    + "their own. You pay the farmer's own price; nothing is added in between.</p>"));
        }
        return page(r, "/buyer/cart", b.toString());
    }

    private static Result removeFromCart(Req r) {
        Session s = r.session;
        synchronized (s) {
            s.cart.remove(r.intParam("id"));
        }
        return Result.redirect("/buyer/cart");
    }

    private static Result checkout(Req r) throws Exception {
        Session s = r.session;
        List<CartItem> items;
        synchronized (s) {
            items = new ArrayList<>(s.cart.values());
        }
        List<Integer> ids = ORDERS.placeOrder(s.buyer.id(), items, r.param("address"), r.param("payment"));
        synchronized (s) {
            s.cart.clear();
        }
        s.ok(ids.size() == 1 ? "Order #" + ids.get(0) + " placed. The farmer will confirm it soon."
                : ids.size() + " orders placed (one per farmer): #" + ids);
        return Result.redirect("/buyer/orders");
    }

    // ---------------------------------------------------------------- orders

    private static Result orders(Req r) throws Exception {
        Session s = r.session;
        List<String[]> rows = new ArrayList<>();
        for (Order o : ORDERS.buyerOrders(s.buyer.id())) {
            String act = AuthPages.itemsLink(o.id(), "/buyer/orders")
                    + (o.status().buyerCanCancel() ? Html.postButton(s, "/buyer/orders/cancel", "Cancel", "btn danger",
                        "Cancel order #" + o.id() + "?", "id", String.valueOf(o.id()), "back", "/buyer/orders") : "");
            rows.add(new String[] {"#" + o.id(), Html.date(o.orderDate()), esc(o.farmerName()), esc(o.farmerPhone()),
                esc(o.itemsSummary()), Html.money(o.totalAmount()), esc(o.paymentLabel()), Html.badge(o.status().name()),
                act});
        }
        return page(r, "/buyer/orders", Html.card("Your orders", Html.table(new String[] {"Order #", "Date", "Farmer",
            "Farmer phone", "Items", "Total", "Payment", "Status", ""}, rows, "You have not ordered anything yet.")));
    }

    private static Result cancel(Req r) throws Exception {
        int id = r.intParam("id");
        ORDERS.buyerCancel(id, r.session.buyer.id());
        r.session.ok("Order #" + id + " cancelled.");
        return Result.redirect("/buyer/orders");
    }
}
