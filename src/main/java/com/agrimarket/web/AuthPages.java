package com.agrimarket.web;

import static com.agrimarket.web.Html.esc;

import com.agrimarket.dao.OrderDAO;
import com.agrimarket.db.DBConnection;
import com.agrimarket.model.Admin;
import com.agrimarket.model.Buyer;
import com.agrimarket.model.Farmer;
import com.agrimarket.model.OrderItem;
import com.agrimarket.service.AuthService;
import com.agrimarket.service.OrderService;
import com.agrimarket.service.ValidationException;
import com.agrimarket.web.WebServer.Req;
import com.agrimarket.web.WebServer.Result;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Login, registration, logout and the order-items page shared by all roles. */
final class AuthPages {

    private static final AuthService AUTH = new AuthService();
    private static final OrderService ORDERS = new OrderService();
    private static final OrderDAO ORDER_DAO = new OrderDAO();

    private AuthPages() {
    }

    static void register(WebServer web) {
        web.get("/", AuthPages::loginPage);
        web.post("/login", r -> login(web, r));
        web.post("/logout", r -> logout(web, r));
        web.get("/register", r -> registerPage(r, null, null));
        web.post("/register", AuthPages::registerSave);
        web.get("/order", AuthPages::orderItems);
    }

    // ---------------------------------------------------------------- login

    private static Result loginPage(Req r) {
        Session s = r.session;
        if (s.loggedIn()) {
            return Result.redirect(s.homePath());
        }
        String role = s.loginRole;
        StringBuilder b = new StringBuilder("<div class=\"login\"><div class=\"left\">");
        b.append("<div class=\"brand\">AGRIDIRECT</div><h1>Farmer-to-Buyer<br>Direct Marketplace</h1>")
                .append("<p>Farmers list produce at their own price.<br>Buyers search, order and track it directly.<br>")
                .append("No middlemen in between.</p>")
                .append("<div class=\"foot\">Java · JDBC · Database: ").append(esc(DBConnection.kind())).append("</div>")
                .append("</div><div class=\"right\">")
                .append("<h2>Welcome back</h2><div class=\"muted\">Log in to continue</div><br>")
                .append(Html.flash(s))
                .append("<form method=\"post\" action=\"/login\">").append(Html.csrf(s))
                .append("<label>I am a</label><div class=\"roles\">");
        for (String[] opt : new String[][] {{"farmer", "Farmer"}, {"buyer", "Buyer"}, {"admin", "Admin"}}) {
            b.append("<label><input type=\"radio\" name=\"role\" value=\"").append(opt[0]).append("\"")
                    .append(opt[0].equals(role) ? " checked" : "").append("> ").append(opt[1]).append("</label>");
        }
        b.append("</div>")
                .append("<div class=\"field\"><label for=\"id\">Mobile number (admin: username)</label>")
                .append("<input id=\"id\" name=\"id\" autocomplete=\"username\" required value=\"")
                .append(esc(s.loginId)).append("\"></div>")
                .append("<div class=\"field\"><label for=\"pw\">Password</label>")
                .append("<input id=\"pw\" type=\"password\" name=\"password\" autocomplete=\"current-password\" required></div>")
                .append("<button class=\"btn primary\" type=\"submit\">Log in</button> ")
                .append("<a class=\"btn\" href=\"/register?role=farmer\">Register as farmer</a> ")
                .append("<a class=\"btn\" href=\"/register?role=buyer\">Register as buyer</a>")
                .append("</form><br><p class=\"muted\">Sample logins:<br>")
                .append("Farmer 9840000001 · Buyer 9790000001 · password pass123")
                .append(WebServer.customAdminPassword ? "" : "<br>Admin admin · password admin123")
                .append("</p></div></div>");
        return Result.html(Html.bare("Login", b.toString()));
    }

    private static Result login(WebServer web, Req r) throws Exception {
        String role = r.param("role");
        String id = r.param("id").trim();
        String password = r.param("password");
        r.session.loginRole = role;
        r.session.loginId = id;

        Optional<Farmer> farmer = Optional.empty();
        Optional<Buyer> buyer = Optional.empty();
        Optional<Admin> admin = Optional.empty();
        switch (role) {
            case "buyer" -> buyer = AUTH.loginBuyer(id, password);
            case "admin" -> admin = AUTH.loginAdmin(id, password);
            default -> farmer = AUTH.loginFarmer(id, password);
        }
        if (farmer.isEmpty() && buyer.isEmpty() && admin.isEmpty()) {
            throw new ValidationException("Wrong login details. Check the role, number and password.");
        }
        Session s = web.replace(r.session);
        WebServer.SWITCHED.set(s);
        if (farmer.isPresent()) {
            s.role = Session.Role.FARMER;
            s.farmer = farmer.get();
        } else if (buyer.isPresent()) {
            s.role = Session.Role.BUYER;
            s.buyer = buyer.get();
        } else {
            s.role = Session.Role.ADMIN;
            s.admin = admin.get();
        }
        return Result.redirect(s.homePath());
    }

    private static Result logout(WebServer web, Req r) {
        String role = r.session.role == Session.Role.BUYER ? "buyer"
                : r.session.role == Session.Role.ADMIN ? "admin" : "farmer";
        Session s = web.replace(r.session);
        s.loginRole = role;
        s.ok("You have logged out.");
        WebServer.SWITCHED.set(s);
        return Result.redirect("/");
    }

    // ---------------------------------------------------------------- register

    private static Result registerPage(Req r, String error, java.util.Map<String, String> values) {
        Session s = r.session;
        if (s.loggedIn()) {
            return Result.redirect(s.homePath());
        }
        boolean farmer = !"buyer".equals(r.param("role"));
        java.util.Map<String, String> v = values == null ? java.util.Map.of() : values;
        StringBuilder b = new StringBuilder("<main style=\"max-width:720px\"><section class=\"card\">");
        b.append("<h2>Create a ").append(farmer ? "farmer" : "buyer").append(" account</h2>");
        if (error != null) {
            b.append("<div class=\"flash err\">").append(esc(error)).append("</div>");
        }
        b.append("<form method=\"post\" action=\"/register\" class=\"form\">").append(Html.csrf(s))
                .append("<input type=\"hidden\" name=\"role\" value=\"").append(farmer ? "farmer" : "buyer").append("\">");
        field(b, "Full name", "name", v, "text", 100);
        field(b, "Mobile number", "phone", v, "tel", 15);
        if (farmer) {
            field(b, "Village", "village", v, "text", 100);
            field(b, "District", "district", v, "text", 100);
        } else {
            field(b, "City", "city", v, "text", 100);
        }
        field(b, "Password (6+ characters)", "password", java.util.Map.of(), "password", 100);
        field(b, "Confirm password", "confirm", java.util.Map.of(), "password", 100);
        b.append("<span></span><div><button class=\"btn primary\" type=\"submit\">Create account</button> ")
                .append("<a class=\"btn\" href=\"/\">Back to login</a></div></form>")
                .append("<p class=\"muted\">").append(farmer ? "Buying instead? " : "Selling instead? ")
                .append("<a href=\"/register?role=").append(farmer ? "buyer" : "farmer").append("\">Register as a ")
                .append(farmer ? "buyer" : "farmer").append("</a></p></section></main>");
        return Result.html(Html.bare("Register", b.toString()));
    }

    private static void field(StringBuilder b, String label, String name, java.util.Map<String, String> v, String type,
                              int max) {
        b.append("<label for=\"").append(name).append("\">").append(esc(label)).append("</label>")
                .append("<input id=\"").append(name).append("\" name=\"").append(name).append("\" type=\"").append(type)
                .append("\" maxlength=\"").append(max).append("\" required value=\"").append(esc(v.get(name)))
                .append("\">");
    }

    private static Result registerSave(Req r) throws Exception {
        boolean farmer = !"buyer".equals(r.param("role"));
        try {
            String phone;
            if (farmer) {
                phone = AUTH.registerFarmer(r.param("name"), r.param("phone"), r.param("village"), r.param("district"),
                        r.param("password"), r.param("confirm")).phone();
            } else {
                phone = AUTH.registerBuyer(r.param("name"), r.param("phone"), r.param("city"), r.param("password"),
                        r.param("confirm")).phone();
            }
            r.session.loginRole = farmer ? "farmer" : "buyer";
            r.session.loginId = phone;
            r.session.ok("Account created. You can log in now.");
            return Result.redirect("/");
        } catch (ValidationException e) {
            return registerPage(r, e.getMessage(), r.form);
        }
    }

    // ---------------------------------------------------------------- order items (all roles)

    private static Result orderItems(Req r) throws Exception {
        Session s = r.session;
        int id = r.intParam("id");
        int[] owners = ORDER_DAO.owners(id);
        boolean mine = owners != null && (s.role == Session.Role.ADMIN
                || (s.role == Session.Role.BUYER && owners[0] == s.buyer.id())
                || (s.role == Session.Role.FARMER && owners[1] == s.farmer.id()));
        if (!mine) {
            throw new ValidationException("Order #" + id + " was not found.");
        }
        List<OrderItem> items = ORDERS.items(id);
        List<String[]> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem i : items) {
            rows.add(new String[] {esc(i.productName()), Html.kg(i.quantityKg()), Html.money(i.unitPrice()),
                Html.money(i.subtotal())});
            total = total.add(i.subtotal());
        }
        String back = r.back(s.homePath());
        String body = Html.card("Items in order #" + id,
                Html.table(new String[] {"Product", "Quantity", "Price / kg", "Subtotal"}, rows, "No items.")
                + "<p class=\"total\">Total: " + Html.money(total) + "</p>"
                + "<a class=\"btn\" href=\"" + esc(back) + "\">Back</a>");
        return Result.html(Html.page(s, "Order #" + id, "Price per kg is the price at the time of ordering", "", body));
    }

    /** Link to the order-items page, returning to the given page. */
    static String itemsLink(int orderId, String back) {
        return "<a class=\"btn\" href=\"/order?id=" + orderId + "&amp;back=" + esc(java.net.URLEncoder.encode(back,
                java.nio.charset.StandardCharsets.UTF_8)) + "\">Items</a>";
    }
}
