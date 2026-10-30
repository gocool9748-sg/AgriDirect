-- =====================================================================
-- Sample data for demos and testing. Run after schema.sql:
--   mysql -u root -p < sql/sample_data.sql
--
-- Logins (all sample farmers and buyers use password: pass123)
--   Admin  : username admin       password admin123
--   Farmer : phone 9840000001     (Murugan K)
--   Buyer  : phone 9790000001     (Priya S)
-- Order dates are relative to today, so the dashboards always show data.
-- =====================================================================
USE agri_market;

SET @pw    = 'pbkdf2$65536$XhfD9ekfH83Wm1Bgzc1QLw==$0WT9ZgwHqhId+TIwVwhYNnsRWwEVRJwKsbZfI0fI1WQ=';
SET @admin = 'pbkdf2$65536$2hW6tj3Gy8R0GS7RlkPOOQ==$Uk9ukT7kKCxdaFvBo6wC2fNDGHxvH3p1NgHqtANTxoo=';

INSERT INTO admins (username, password_hash) VALUES ('admin', @admin);

INSERT INTO categories (category_id, name, description) VALUES
 (1, 'Vegetables',         'Fresh seasonal vegetables'),
 (2, 'Fruits',             'Fresh fruits'),
 (3, 'Grains',             'Rice, millets and cereals'),
 (4, 'Pulses & Oilseeds',  'Dals, gram and groundnut'),
 (5, 'Dairy',              'Milk products'),
 (6, 'Spices',             'Whole and dried spices');

INSERT INTO farmers (farmer_id, name, phone, village, district, password_hash, created_at) VALUES
 (1, 'Murugan K',  '9840000001', 'Kilpennathur', 'Tiruvannamalai', @pw, NOW() - INTERVAL '60' DAY),
 (2, 'Lakshmi R',  '9840000002', 'Gingee',       'Villupuram',     @pw, NOW() - INTERVAL '55' DAY),
 (3, 'Senthil P',  '9840000003', 'Orathanadu',   'Thanjavur',      @pw, NOW() - INTERVAL '50' DAY),
 (4, 'Kavitha S',  '9840000004', 'Hosur',        'Krishnagiri',    @pw, NOW() - INTERVAL '40' DAY),
 (5, 'Ramesh V',   '9840000005', 'Omalur',       'Salem',          @pw, NOW() - INTERVAL '35' DAY);

INSERT INTO buyers (buyer_id, name, phone, city, password_hash, created_at) VALUES
 (1, 'Priya S',                '9790000001', 'Chennai',        @pw, NOW() - INTERVAL '45' DAY),
 (2, 'Arun Kumar',             '9790000002', 'Vellore',        @pw, NOW() - INTERVAL '40' DAY),
 (3, 'Fatima B',               '9790000003', 'Tiruvannamalai', @pw, NOW() - INTERVAL '30' DAY),
 (4, 'Saravana Kitchen (Hotel)','9790000004', 'Chennai',       @pw, NOW() - INTERVAL '28' DAY);

INSERT INTO products (product_id, farmer_id, category_id, name, description, price_per_kg, qty_available) VALUES
 ( 1, 1, 1, 'Tomato',           'Country tomato, harvested this week', 28.00, 320.00),
 ( 2, 1, 1, 'Onion',            'Small onion (sambar onion)',          34.00,   0.00),
 ( 3, 1, 1, 'Brinjal',          'Purple brinjal',                      40.00,  85.00),
 ( 4, 1, 2, 'Banana',           'Poovan banana',                       30.00,   8.00),
 ( 5, 2, 3, 'Ponni Rice',       'Raw ponni rice, 2025 harvest',        58.00, 600.00),
 ( 6, 2, 4, 'Groundnut',        'Shelled groundnut',                   95.00, 150.00),
 ( 7, 2, 4, 'Green Gram',       'Whole moong',                        110.00, 120.00),
 ( 8, 3, 2, 'Mango',            'Banganapalli mango',                  70.00, 200.00),
 ( 9, 3, 2, 'Guava',            'Pink guava',                          50.00,  60.00),
 (10, 3, 4, 'Black Gram',       'Whole urad',                         120.00,  80.00),
 (11, 4, 1, 'Carrot',           'Ooty-type carrot',                    45.00, 140.00),
 (12, 4, 1, 'Beans',            'French beans',                        60.00,  70.00),
 (13, 4, 5, 'Paneer',           'Fresh paneer, made to order',        380.00,  15.00),
 (14, 5, 6, 'Turmeric',         'Salem turmeric, dried fingers',      140.00, 100.00),
 (15, 5, 1, 'Tapioca',          'Fresh tapioca root',                  25.00, 400.00),
 (16, 5, 3, 'Ragi',             'Finger millet',                       48.00, 250.00);

INSERT INTO orders (order_id, buyer_id, farmer_id, order_date, total_amount, status, payment_mode, delivery_address) VALUES
 ( 1, 1, 1, NOW() - INTERVAL '20' DAY,  480.00, 'DELIVERED', 'UPI_ON_DELIVERY',  '12 Lake View Rd, Anna Nagar, Chennai'),
 ( 2, 4, 2, NOW() - INTERVAL '18' DAY, 5800.00, 'DELIVERED', 'CASH_ON_DELIVERY', 'Saravana Kitchen, T. Nagar, Chennai'),
 ( 3, 2, 3, NOW() - INTERVAL '15' DAY, 1400.00, 'DELIVERED', 'UPI_ON_DELIVERY',  '4 Officers Line, Vellore'),
 ( 4, 3, 1, NOW() - INTERVAL '12' DAY, 1270.00, 'DELIVERED', 'CASH_ON_DELIVERY', '22 Car Street, Tiruvannamalai'),
 ( 5, 4, 4, NOW() - INTERVAL '9' DAY,  1950.00, 'SHIPPED',   'CASH_ON_DELIVERY', 'Saravana Kitchen, T. Nagar, Chennai'),
 ( 6, 1, 5, NOW() - INTERVAL '6' DAY,   760.00, 'CONFIRMED', 'UPI_ON_DELIVERY',  '12 Lake View Rd, Anna Nagar, Chennai'),
 ( 7, 2, 2, NOW() - INTERVAL '4' DAY,   550.00, 'PLACED',    'CASH_ON_DELIVERY', '4 Officers Line, Vellore'),
 ( 8, 3, 3, NOW() - INTERVAL '3' DAY,   200.00, 'CANCELLED', 'CASH_ON_DELIVERY', '22 Car Street, Tiruvannamalai'),
 ( 9, 4, 1, NOW() - INTERVAL '2' DAY,  1400.00, 'PLACED',    'CASH_ON_DELIVERY', 'Saravana Kitchen, T. Nagar, Chennai'),
 (10, 1, 4, NOW() - INTERVAL '1' DAY,   760.00, 'PLACED',    'UPI_ON_DELIVERY',  '12 Lake View Rd, Anna Nagar, Chennai'),
 (11, 2, 1, NOW() - INTERVAL '1' DAY,   180.00, 'CONFIRMED', 'CASH_ON_DELIVERY', '4 Officers Line, Vellore');

INSERT INTO order_items (order_id, product_id, quantity_kg, unit_price) VALUES
 ( 1,  1,  10.00,  28.00), ( 1,  3,  5.00,  40.00),
 ( 2,  5, 100.00,  58.00),
 ( 3,  8,  20.00,  70.00),
 ( 4,  2,  25.00,  34.00), ( 4,  1, 15.00,  28.00),
 ( 5, 11,  30.00,  45.00), ( 5, 12, 10.00,  60.00),
 ( 6, 14,   2.00, 140.00), ( 6, 16, 10.00,  48.00),
 ( 7,  7,   5.00, 110.00),
 ( 8,  9,   4.00,  50.00),
 ( 9,  1,  50.00,  28.00),
 (10, 13,   2.00, 380.00),
 (11,  4,   6.00,  30.00);
