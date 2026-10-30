package com.agrimarket.web;

import com.agrimarket.util.Format;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Page layout and small HTML building blocks shared by every web page. Everything printed is escaped. */
final class Html {

    private Html() {
    }

    static final String CSS = """
            :root{--green:#1F3A2B;--green2:#2A4A38;--cream:#F6F1E4;--card:#FFFBF2;--sand:#EDE5D0;--orange:#D9822B;
              --orange-dark:#A3560F;--muted:#4E5D52;--border:#DCD3BC;--leaf:#4F7A3A;--light:#C9D6C4;--red:#B03A2E}
            *{box-sizing:border-box}
            body{margin:0;font-family:"Segoe UI",system-ui,-apple-system,Roboto,Arial,sans-serif;background:var(--cream);
              color:var(--green);font-size:15px}
            a{color:var(--orange-dark)}
            header.bar{background:var(--green);color:var(--cream);padding:14px 28px;display:flex;align-items:center;
              justify-content:space-between;gap:16px;flex-wrap:wrap}
            header.bar .brand{color:var(--orange);font-weight:700;font-size:12px;letter-spacing:.04em}
            header.bar h1{margin:2px 0 0;font-size:22px}
            header.bar .sub{color:var(--light);font-size:13px}
            nav.tabs{background:var(--sand);border-bottom:1px solid var(--border);padding:0 22px;display:flex;gap:4px;
              flex-wrap:wrap}
            nav.tabs a{display:inline-block;padding:11px 16px;text-decoration:none;color:var(--muted);font-weight:600;
              border-bottom:3px solid transparent}
            nav.tabs a.on{color:var(--green);border-bottom-color:var(--orange)}
            main{padding:20px 26px;max-width:1400px;margin:0 auto}
            .card{background:var(--card);border:1px solid var(--border);border-radius:8px;padding:16px 18px;margin-bottom:18px}
            .card h2{font-size:16px;margin:0 0 12px}
            .stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:12px;margin-bottom:18px}
            .stat{background:var(--card);border:1px solid var(--border);border-radius:8px;padding:16px 20px}
            .stat .cap{color:var(--muted);font-size:13px}
            .stat .val{font-size:28px;font-weight:700;margin-top:6px}
            .stat.hi{background:var(--green);border-color:var(--green)}
            .stat.hi .cap{color:var(--light)}
            .stat.hi .val{color:#E8A04A}
            .grid2{display:grid;grid-template-columns:repeat(auto-fit,minmax(420px,1fr));gap:18px}
            .tablewrap{overflow-x:auto}
            table{border-collapse:collapse;width:100%;background:var(--card)}
            th{background:#E4DCC6;text-align:left;font-size:13px;padding:8px 10px;white-space:nowrap}
            td{padding:7px 10px;border-top:1px solid #EAE2CF;vertical-align:middle}
            tr:nth-child(even) td{background:#F8F3E8}
            td.num{white-space:nowrap}
            .empty{color:var(--muted);padding:14px 4px}
            .btn{display:inline-block;font:inherit;font-weight:600;font-size:13px;padding:7px 14px;border-radius:6px;
              border:1px solid var(--border);background:var(--card);color:var(--green);cursor:pointer;text-decoration:none}
            .btn.primary{background:var(--orange-dark);border-color:var(--orange-dark);color:#fff}
            .btn.danger{color:var(--red)}
            .btn:hover{filter:brightness(.95)}
            header.bar .btn{background:var(--card)}
            form.inline{display:inline-flex;gap:6px;align-items:center;margin:2px 4px 2px 0}
            input,select,textarea{font:inherit;padding:7px 9px;border:1px solid var(--border);border-radius:5px;
              background:#fff;color:var(--green)}
            input.small{width:80px}
            label{font-weight:600;color:var(--muted);font-size:13px}
            .form{display:grid;grid-template-columns:max-content minmax(220px,420px);gap:10px 14px;align-items:center}
            .form label{text-align:right}
            .toolbar{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;margin-bottom:14px}
            .toolbar .f{display:flex;flex-direction:column;gap:4px}
            .flash{padding:11px 16px;border-radius:6px;margin-bottom:16px;font-weight:600}
            .flash.ok{background:#E3EEDA;color:#2F5A21;border:1px solid #BCD3A9}
            .flash.err{background:#F6DEDA;color:var(--red);border:1px solid #E5B7AF}
            .badge{font-weight:700;font-size:12px;letter-spacing:.02em}
            .s-DELIVERED,.s-Active{color:var(--leaf)}
            .s-CANCELLED,.s-Removed,.s-Out{color:var(--red)}
            .s-PLACED,.s-Low{color:var(--orange-dark)}
            .muted{color:var(--muted);font-size:13px}
            .bars .row{display:grid;grid-template-columns:150px 1fr 40px;gap:10px;align-items:center;margin:8px 0}
            .bars .track{background:var(--sand);border-radius:4px;height:22px}
            .bars .fill{background:var(--leaf);height:22px;border-radius:4px}
            .total{font-size:20px;font-weight:700}
            .login{display:grid;grid-template-columns:1fr 1fr;min-height:100vh}
            .login .left{background:var(--green);color:var(--cream);padding:80px 56px 40px;display:flex;flex-direction:column}
            .login .left .brand{color:var(--orange);font-weight:700;letter-spacing:.06em}
            .login .left h1{font-size:38px;line-height:1.2;margin:16px 0}
            .login .left p{color:var(--light);font-size:16px;line-height:1.6}
            .login .left .foot{margin-top:auto;color:var(--light);font-size:13px}
            .login .right{padding:70px 56px;max-width:560px}
            .login h2{font-size:28px;margin:0}
            .login .field{display:flex;flex-direction:column;gap:5px;margin:14px 0}
            .login .field input{max-width:380px;font-size:16px;padding:9px 11px}
            .roles{display:flex;gap:18px;margin-top:6px}
            .roles label{font-weight:400;font-size:15px;color:var(--green)}
            @media (max-width:800px){.login{grid-template-columns:1fr}.login .left{padding:30px}.login .right{padding:30px}
              .form{grid-template-columns:1fr}.form label{text-align:left}.grid2{grid-template-columns:1fr}}
            """;

    static String esc(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&#39;");
                default -> b.append(c);
            }
        }
        return b.toString();
    }

    static String money(BigDecimal amount) {
        return esc(Format.money(amount));
    }

    static String kg(BigDecimal qty) {
        return esc(Format.kg(qty));
    }

    static String date(LocalDateTime d) {
        return esc(Format.dateTime(d));
    }

    /** Plain number for an input box, e.g. 12.5 rather than 12.50. */
    static String plain(BigDecimal value) {
        return value == null ? "" : esc(value.stripTrailingZeros().toPlainString());
    }

    private static String head(String title) {
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + esc(title) + " · AgriDirect</title><style>" + CSS + "</style></head><body>";
    }

    /** A page without the logged-in header (login, register, errors). */
    static String bare(String title, String body) {
        return head(title) + body + "</body></html>";
    }

    /** A logged-in page: green header with logout, tab bar, flash message, then the body. */
    static String page(Session s, String title, String subtitle, String activePath, String body) {
        StringBuilder b = new StringBuilder(head(title));
        b.append("<header class=\"bar\"><div><div class=\"brand\">AgriDirect · Farmer-to-Buyer Marketplace</div>")
                .append("<h1>").append(esc(title)).append("</h1><div class=\"sub\">").append(esc(subtitle))
                .append("</div></div>");
        String confirm = s.role == Session.Role.BUYER && !s.cart.isEmpty()
                ? "Your cart will be emptied. Log out anyway?" : null;
        b.append(postButton(s, "/logout", "Log out", "btn", confirm)).append("</header>");
        b.append("<nav class=\"tabs\">");
        for (String[] tab : tabs(s)) {
            b.append("<a href=\"").append(tab[0]).append("\"").append(tab[0].equals(activePath) ? " class=\"on\"" : "")
                    .append(">").append(esc(tab[1])).append("</a>");
        }
        b.append("</nav><main>").append(flash(s)).append(body).append("</main></body></html>");
        return b.toString();
    }

    private static List<String[]> tabs(Session s) {
        if (s.role == null) {
            return List.of();
        }
        return switch (s.role) {
            case FARMER -> List.of(new String[] {"/farmer", "Overview"}, new String[] {"/farmer/products", "My products"},
                    new String[] {"/farmer/orders", "Orders received"});
            case BUYER -> List.of(new String[] {"/buyer", "Browse produce"},
                    new String[] {"/buyer/cart", "Cart (" + s.cart.size() + ")"},
                    new String[] {"/buyer/orders", "My orders"});
            case ADMIN -> List.of(new String[] {"/admin", "Dashboard"}, new String[] {"/admin/farmers", "Farmers"},
                    new String[] {"/admin/buyers", "Buyers"}, new String[] {"/admin/orders", "All orders"},
                    new String[] {"/admin/categories", "Categories"});
        };
    }

    /** Shows and clears the one-time message. */
    static String flash(Session s) {
        StringBuilder b = new StringBuilder();
        if (s.flashError != null) {
            b.append("<div class=\"flash err\">").append(esc(s.flashError)).append("</div>");
            s.flashError = null;
        }
        if (s.flashOk != null) {
            b.append("<div class=\"flash ok\">").append(esc(s.flashOk)).append("</div>");
            s.flashOk = null;
        }
        return b.toString();
    }

    static String csrf(Session s) {
        return "<input type=\"hidden\" name=\"csrf\" value=\"" + esc(s.csrf) + "\">";
    }

    /** A one-button form that POSTs to action with the given hidden name/value pairs. */
    static String postButton(Session s, String action, String label, String cls, String confirm, String... hidden) {
        StringBuilder b = new StringBuilder("<form class=\"inline\" method=\"post\" action=\"").append(esc(action))
                .append("\"");
        if (confirm != null) {
            b.append(" onsubmit=\"return confirm('").append(esc(confirm.replace("\\", "\\\\").replace("'", "\\'")))
                    .append("')\"");
        }
        b.append(">").append(csrf(s));
        for (int i = 0; i + 1 < hidden.length; i += 2) {
            b.append("<input type=\"hidden\" name=\"").append(esc(hidden[i])).append("\" value=\"")
                    .append(esc(hidden[i + 1])).append("\">");
        }
        b.append("<button class=\"").append(esc(cls)).append("\" type=\"submit\">").append(esc(label))
                .append("</button></form>");
        return b.toString();
    }

    static String card(String title, String inner) {
        return "<section class=\"card\">" + (title == null ? "" : "<h2>" + esc(title) + "</h2>") + inner + "</section>";
    }

    static String stat(String caption, String value, boolean highlight) {
        return "<div class=\"stat" + (highlight ? " hi" : "") + "\"><div class=\"cap\">" + esc(caption)
                + "</div><div class=\"val\">" + esc(value) + "</div></div>";
    }

    /** Coloured status text, e.g. DELIVERED in green, CANCELLED in red. */
    static String badge(String status) {
        String key = status == null ? "" : status.split(" ")[0];
        return "<span class=\"badge s-" + esc(key) + "\">" + esc(status) + "</span>";
    }

    /** A table; each row is a list of cells that are already HTML. */
    static String table(String[] headers, List<String[]> rows, String emptyText) {
        if (rows.isEmpty()) {
            return "<div class=\"empty\">" + esc(emptyText) + "</div>";
        }
        StringBuilder b = new StringBuilder("<div class=\"tablewrap\"><table><thead><tr>");
        for (String h : headers) {
            b.append("<th>").append(esc(h)).append("</th>");
        }
        b.append("</tr></thead><tbody>");
        for (String[] row : rows) {
            b.append("<tr>");
            for (String cell : row) {
                b.append("<td>").append(cell == null ? "" : cell).append("</td>");
            }
            b.append("</tr>");
        }
        return b.append("</tbody></table></div>").toString();
    }

    static String option(String value, String label, boolean selected) {
        return "<option value=\"" + esc(value) + "\"" + (selected ? " selected" : "") + ">" + esc(label) + "</option>";
    }
}
