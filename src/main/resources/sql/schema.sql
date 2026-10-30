-- =====================================================================
-- Farmer-to-Buyer Direct Marketplace (AgriDirect) - database schema
-- MySQL 8.0+  (also works on MariaDB 10.5+)
-- Run:  mysql -u root -p < sql/schema.sql
-- WARNING: re-running this file drops and recreates all tables.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS agri_market
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE agri_market;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS order_items, orders, products, categories, admins, buyers, farmers;
SET FOREIGN_KEY_CHECKS = 1;

-- Sellers
CREATE TABLE farmers (
  farmer_id      INT AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(100) NOT NULL,
  phone          VARCHAR(15)  NOT NULL UNIQUE,
  village        VARCHAR(100) NOT NULL,
  district       VARCHAR(100) NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- Customers
CREATE TABLE buyers (
  buyer_id       INT AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(100) NOT NULL,
  phone          VARCHAR(15)  NOT NULL UNIQUE,
  city           VARCHAR(100) NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- Platform administrators
CREATE TABLE admins (
  admin_id       INT AUTO_INCREMENT PRIMARY KEY,
  username       VARCHAR(50)  NOT NULL UNIQUE,
  password_hash  VARCHAR(255) NOT NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

CREATE TABLE categories (
  category_id    INT AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(50)  NOT NULL UNIQUE,
  description    VARCHAR(255)
) ENGINE = InnoDB;

-- Listings: each product belongs to exactly one farmer
CREATE TABLE products (
  product_id     INT AUTO_INCREMENT PRIMARY KEY,
  farmer_id      INT           NOT NULL,
  category_id    INT           NOT NULL,
  name           VARCHAR(80)   NOT NULL,
  description    VARCHAR(255),
  price_per_kg   DECIMAL(8,2)  NOT NULL,
  qty_available  DECIMAL(10,2) NOT NULL DEFAULT 0,
  is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_product_price CHECK (price_per_kg > 0),
  CONSTRAINT chk_product_qty   CHECK (qty_available >= 0),
  CONSTRAINT fk_product_farmer   FOREIGN KEY (farmer_id)   REFERENCES farmers(farmer_id),
  CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(category_id),
  INDEX idx_product_name (name),
  INDEX idx_product_active (is_active, qty_available)
) ENGINE = InnoDB;

-- One order = one buyer buying from one farmer
-- (a cart with several farmers becomes several orders, placed in one transaction)
CREATE TABLE orders (
  order_id         INT AUTO_INCREMENT PRIMARY KEY,
  buyer_id         INT           NOT NULL,
  farmer_id        INT           NOT NULL,
  order_date       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total_amount     DECIMAL(10,2) NOT NULL,
  status           ENUM('PLACED','CONFIRMED','SHIPPED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'PLACED',
  payment_mode     ENUM('CASH_ON_DELIVERY','UPI_ON_DELIVERY') NOT NULL DEFAULT 'CASH_ON_DELIVERY',
  delivery_address VARCHAR(255)  NOT NULL,
  updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_buyer  FOREIGN KEY (buyer_id)  REFERENCES buyers(buyer_id),
  CONSTRAINT fk_order_farmer FOREIGN KEY (farmer_id) REFERENCES farmers(farmer_id),
  INDEX idx_order_status (status),
  INDEX idx_order_date (order_date)
) ENGINE = InnoDB;

-- Line items. unit_price is copied at order time so later price changes don't alter history.
CREATE TABLE order_items (
  item_id      INT AUTO_INCREMENT PRIMARY KEY,
  order_id     INT           NOT NULL,
  product_id   INT           NOT NULL,
  quantity_kg  DECIMAL(10,2) NOT NULL,
  unit_price   DECIMAL(8,2)  NOT NULL,
  CONSTRAINT chk_item_qty CHECK (quantity_kg > 0),
  CONSTRAINT fk_item_order   FOREIGN KEY (order_id)   REFERENCES orders(order_id) ON DELETE CASCADE,
  CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES products(product_id)
) ENGINE = InnoDB;
