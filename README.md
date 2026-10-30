# AgriDirect — Farmer-to-Buyer Direct Marketplace (Java)

A **100% Java** project: a website **and** a desktop app (Java Swing), both on JDBC + a built-in database (H2, or MySQL if you have it). No frameworks, nothing to install except Java.
Farmers sell directly to buyers — no middlemen.

- **Farmer:** register → log in → add products → set price per kg & stock → confirm / ship / deliver / reject orders → see earnings dashboard.
- **Buyer:** register → log in → search (product, category, village/district, max price) → add to cart → place order (cash/UPI on delivery) → track or cancel.
- **Admin:** dashboard with farmers, buyers, listings, orders, revenue, orders-by-category bar chart, top products, recent orders; lists of all farmers, buyers, orders; add categories.

Screenshots are in `screenshots/`.

---

## Run it

**You need:** Java 17 or newer (<https://adoptium.net>). **Nothing else — no MySQL to install.**

Double-click **`run.bat`**. The first time, the app creates its database by itself — all tables plus demo farmers, buyers, products and orders — in the file `data/agri_market.mv.db` inside this folder, then shows the login screen. From then on, all data is saved there.

The built-in database is **H2**, a database engine written in Java that runs inside the app (embedded) and is driven through **JDBC** with the same SQL as MySQL — so the code, DAOs and transactions are identical. To reset to fresh demo data, close the app and delete the `data` folder and `db.properties`.

### Run it as a website

Double-click **`web.bat`** (or `run.bat web`). A black window opens and your browser opens **<http://localhost:8080>** — log in with the same accounts as below. Keep the black window open while you use the site; close it to stop the website.

- Same features as the desktop app: farmers add products, update stock and handle orders; buyers search, fill a cart and place / cancel orders; the admin sees the dashboard, farmers, buyers, all orders and categories.
- Same database: the website and the desktop app can run at the same time and see each other's changes.
- **On a phone or another laptop:** run `web.bat lan`, allow Java in the Windows Firewall prompt, then open the address it prints (e.g. `http://192.168.1.5:8080`) on any device on the same Wi-Fi.
- Another port: `java -Dport=9090 -cp "AgriDirect.jar;lib\*" com.agrimarket.web.WebServer open`.

The web server is the one built into Java (`com.sun.net.httpserver`), so there is still nothing extra to install. Pages are plain HTML forms; every action goes through the same Service → DAO → JDBC code as the desktop app, with a login session per browser, role checks on every page and a CSRF token on every form.

### Put it online (share a link with friends)

The repository is ready for **[Render](https://render.com)** (free plan, no card needed):

1. Sign in to Render with GitHub → **New → Blueprint** → choose this repository → **Apply**.
   (Render reads `render.yaml` and builds the `Dockerfile`.)
2. After a few minutes you get a link like `https://agridirect-xxxx.onrender.com` — send it to anyone.
3. The admin password online is **not** `admin123`: Render generates one. Find it under the service's
   **Environment** tab (`ADMIN_PASSWORD`). The demo farmer and buyer logins (`pass123`) still work.

Good to know about the free plan: the site sleeps after 15 minutes without visitors, so the first visit
afterwards takes about a minute; and its disk is temporary, so data added online resets to the demo data
whenever the site restarts or redeploys. For data that stays, point it at a hosted MySQL database by
setting `DB_URL`, `DB_USER` and `DB_PASSWORD` in Render's Environment tab (create its tables first by
running `sql/schema.sql` and `sql/sample_data.sql` on it).

Any other Docker host works too: `docker build -t agridirect . && docker run -p 8080:8080 agridirect`.

### Optional: use MySQL instead

If MySQL is running on this PC with user `root` and no password, the first start uses it automatically. With a password, run `run.bat setup` and enter it in the setup window (it creates the `agri_market` database from `sql/schema.sql`). No MySQL at all but want one? `run.bat mysql` downloads and starts a portable MySQL (about 300 MB), then run `run.bat setup`.

### Change the code

The ready-to-run `AgriDirect.jar` is built from `src/`. After editing the code, run **`build.bat`** (needs a JDK) to rebuild it.

### In an IDE

The project is a standard Maven project (`pom.xml`), so it opens directly (Maven downloads the official MySQL Connector/J).

- **IntelliJ IDEA:** File → Open → choose the `AgriDirect-Java` folder → open `src/main/java/com/agrimarket/Main.java` → click ▶ Run.
- **Eclipse:** File → Import → Maven → Existing Maven Projects → choose the folder → right-click `Main.java` → Run As → Java Application.
- **NetBeans:** File → Open Project → choose the folder → right-click the project → Run (main class `com.agrimarket.Main`).
- **VS Code:** install *Extension Pack for Java*, open the folder, open `Main.java`, click *Run*.

Run from the project folder (the IDE default) so the app finds `db.properties`; on first run the setup window creates it.

### Logins (from `sample_data.sql`)

| Role | Login | Password |
|---|---|---|
| Farmer | `9840000001` (Murugan K) … `9840000005` | `pass123` |
| Buyer | `9790000001` (Priya S) … `9790000004` | `pass123` |
| Admin | `admin` | `admin123` |

New farmers and buyers can register from the login screen ("Create an account").

### Use it on several computers (a real marketplace demo)

All computers share one MySQL database:

1. On the computer with MySQL, allow other machines to connect (create a user, e.g. in Workbench:
   `CREATE USER 'agri'@'%' IDENTIFIED BY 'agri123'; GRANT ALL ON agri_market.* TO 'agri'@'%';`) and allow port 3306 in Windows Firewall.
2. On every other computer, set `db.properties` to that computer's IP:
   ```properties
   db.url=jdbc:mysql://192.168.1.5:3306/agri_market?useSSL=false&allowPublicKeyRetrieval=true
   db.user=agri
   db.password=agri123
   ```
3. A farmer lists a product on one laptop → a buyer sees it and orders on another → the farmer gets the order.

### Self-test

`run.bat check` runs `SystemCheck`: 27 automatic checks of registration, login, listing, search, ordering, stock reduction, transaction rollback, the order status flow, cancel + restock and the dashboard numbers. It uses its own test accounts and deletes them afterwards.

---

## Project structure

```
AgriDirect-Java/
├── sql/
│   ├── schema.sql              tables, keys, constraints, indexes
│   └── sample_data.sql         demo farmers, buyers, products, orders
├── src/main/java/com/agrimarket/
│   ├── Main.java               entry point (checks DB, opens login)
│   ├── db/DBConnection.java    JDBC connection from db.properties
│   ├── model/                  Farmer, Buyer, Admin, Category, Product, Order, OrderItem, CartItem, OrderStatus
│   ├── dao/                    FarmerDAO, BuyerDAO, AdminDAO, CategoryDAO, ProductDAO, OrderDAO, DashboardDAO
│   ├── service/                AuthService, ProductService, OrderService, Validate
│   ├── ui/                     LoginFrame, RegisterDialog, FarmerDashboard, BuyerDashboard, AdminDashboard,
│   │                           ProductDialog, OrderItemsDialog, BarChartPanel, UI (shared styling)
│   ├── web/                    WebServer (built-in HTTP server, sessions), Html (layout),
│   │                           AuthPages, FarmerPages, BuyerPages, AdminPages
│   ├── util/                   PasswordUtil (password hashing), Format (₹, kg, dates)
│   └── tools/SystemCheck.java  end-to-end test
├── AgriDirect.jar           ready-to-run app (built from src/)
├── web.bat                  starts the website (http://localhost:8080)
├── Dockerfile, render.yaml  put the website online (Render or any Docker host)
├── run.bat, stop-mysql.bat, build.bat, run.sh, pom.xml
├── tools/start-mysql.ps1    starts MySQL (downloads portable MySQL the first time)
└── lib/                        JDBC driver (included)
```

**Layers:** Web pages / Swing UI → Service (rules, validation, transactions) → DAO (SQL) → JDBC → database.

## Database

| Table | Keys | Notes |
|---|---|---|
| `farmers` | farmer_id PK, phone UNIQUE | village, district, password_hash |
| `buyers` | buyer_id PK, phone UNIQUE | city, password_hash |
| `admins` | admin_id PK, username UNIQUE | |
| `categories` | category_id PK, name UNIQUE | |
| `products` | product_id PK, farmer_id FK, category_id FK | price_per_kg > 0, qty_available ≥ 0, is_active |
| `orders` | order_id PK, buyer_id FK, farmer_id FK | status ENUM, payment_mode, delivery_address |
| `order_items` | item_id PK, order_id FK, product_id FK | quantity_kg, unit_price copied at order time |

## Points to explain in the viva

- **No middleman:** the buyer pays exactly the farmer's `price_per_kg`; nothing is added in between.
- **JDBC:** `DriverManager.getConnection()` → `PreparedStatement` with `?` parameters → `ResultSet`; try-with-resources closes everything.
- **Transactions:** placing an order uses `setAutoCommit(false)`, locks the product rows (`SELECT … FOR UPDATE`), checks stock, reduces it, inserts the order and items, then `commit()`. Any failure → `rollback()`, so stock is never oversold.
- **One order per farmer:** a cart with items from several farmers becomes several orders, so each farmer ships their own.
- **Order lifecycle:** `PLACED → CONFIRMED → SHIPPED → DELIVERED`; the farmer can reject and the buyer can cancel before shipping, which returns the stock.
- **Security:** PreparedStatement everywhere (no SQL injection); passwords stored as salted PBKDF2 hashes, never plain text.
- **Price history:** `order_items.unit_price` keeps the price at order time.
- **Soft delete:** removing a listing hides it (`is_active = FALSE`) because past orders still refer to it.

## Future scope

Online UPI payments, SMS alerts, Tamil/Hindi interface, product photos, ratings and reviews, price-trend charts.
