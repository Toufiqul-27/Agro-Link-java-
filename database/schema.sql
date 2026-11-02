-- AgroLink BD local development schema (MySQL 8.0.16+).
-- Safe for new databases. Back up any existing database first.
CREATE DATABASE IF NOT EXISTS agrolink_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE agrolink_db;

CREATE TABLE IF NOT EXISTS users (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    nid VARCHAR(32) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    location VARCHAR(255) NOT NULL,
    role ENUM('Farmer','Buyer','Arotdar','Admin') NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status ENUM('Active','Pending','Deactivated') NOT NULL DEFAULT 'Active',
    registered_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_nid (nid),
    UNIQUE KEY uq_users_phone (phone)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    farmer_id INT NOT NULL,
    crop_type VARCHAR(40) NOT NULL,
    crop_name VARCHAR(150) NOT NULL,
    price_per_kg DECIMAL(12,2) NOT NULL,
    stock_kg INT NOT NULL,
    harvest_date DATE,
    pickup_location VARCHAR(255) NOT NULL,
    description TEXT,
    CONSTRAINT fk_products_farmer FOREIGN KEY (farmer_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_products_stock CHECK (stock_kg >= 0),
    CONSTRAINT chk_products_price CHECK (price_per_kg > 0),
    INDEX idx_products_farmer (farmer_id),
    INDEX idx_products_type (crop_type)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity_kg INT NOT NULL,
    total_price DECIMAL(14,2) NOT NULL,
    status ENUM('Pending','Confirmed','Dispatched','Cancelled') NOT NULL DEFAULT 'Pending',
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT chk_orders_qty CHECK (quantity_kg > 0),
    CONSTRAINT chk_orders_total CHECK (total_price >= 0),
    INDEX idx_orders_buyer (buyer_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS trucks (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    route VARCHAR(255) NOT NULL,
    capacity_kg INT NOT NULL,
    booked_weight_kg INT NOT NULL DEFAULT 0,
    cost_per_kg DECIMAL(12,2) NOT NULL,
    departure_date DATE NOT NULL,
    status ENUM('Available','Unavailable','Completed') NOT NULL DEFAULT 'Available',
    CONSTRAINT chk_trucks_capacity CHECK (capacity_kg > 0),
    CONSTRAINT chk_trucks_booked CHECK (booked_weight_kg >= 0 AND booked_weight_kg <= capacity_kg),
    CONSTRAINT chk_trucks_price CHECK (cost_per_kg > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS truck_bookings (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    truck_id INT NOT NULL,
    weight_kg INT NOT NULL,
    cost_share DECIMAL(14,2) NOT NULL,
    pickup_location VARCHAR(255) NOT NULL,
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_truck FOREIGN KEY (truck_id)
        REFERENCES trucks(id) ON DELETE RESTRICT,
    CONSTRAINT chk_bookings_weight CHECK (weight_kg > 0),
    CONSTRAINT chk_bookings_cost CHECK (cost_share >= 0),
    INDEX idx_bookings_user (user_id)
) ENGINE=InnoDB;
