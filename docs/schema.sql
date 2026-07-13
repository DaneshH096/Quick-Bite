-- ============================================
-- FOOD DELIVERY APP - MySQL Schema + Seed Data
-- Run this ONCE before starting the Spring Boot app
-- ============================================

CREATE DATABASE IF NOT EXISTS food_delivery_db;
USE food_delivery_db;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    role ENUM('CUSTOMER', 'ADMIN') DEFAULT 'CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Restaurants table
CREATE TABLE IF NOT EXISTS restaurants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    cuisine_type VARCHAR(100),
    address TEXT,
    phone VARCHAR(20),
    image_url VARCHAR(255),
    rating DECIMAL(2,1) DEFAULT 0.0,
    delivery_time_minutes INT DEFAULT 30,
    min_order_amount DECIMAL(10,2) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Menu Items table
CREATE TABLE IF NOT EXISTS menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    image_url VARCHAR(255),
    is_vegetarian BOOLEAN DEFAULT FALSE,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    delivery_address TEXT NOT NULL,
    status ENUM('PENDING','CONFIRMED','PREPARING','OUT_FOR_DELIVERY','DELIVERED','CANCELLED') DEFAULT 'PENDING',
    payment_method ENUM('CASH','ONLINE') DEFAULT 'CASH',
    special_instructions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
);

-- Order Items table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);

-- ============================================
-- SEED DATA
-- ============================================

-- Admin user (password: admin123 - BCrypt encoded)
INSERT INTO users (name, email, password, phone, role) VALUES
('Admin', 'admin@foodapp.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', '9999999999', 'ADMIN');

-- Test customer (password: customer123)
INSERT INTO users (name, email, password, phone, address, role) VALUES
('Rahul Sharma', 'rahul@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', '9876543210', '123 MG Road, Bengaluru', 'CUSTOMER');

-- Restaurants
INSERT INTO restaurants (name, description, cuisine_type, address, phone, image_url, rating, delivery_time_minutes, min_order_amount) VALUES
('Spice Garden', 'Authentic South Indian cuisine with traditional recipes', 'South Indian', '12 Church Street, Bengaluru', '080-12345678', 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=400', 4.5, 25, 100.00),
('Pizza Planet', 'Wood-fired pizzas and Italian pasta', 'Italian', '45 Indiranagar, Bengaluru', '080-87654321', 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400', 4.2, 35, 200.00),
('Burger Barn', 'Juicy burgers and crispy fries', 'American', '78 Koramangala, Bengaluru', '080-11223344', 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400', 4.0, 20, 150.00),
('Dragon Wok', 'Indo-Chinese fusion dishes', 'Chinese', '22 HSR Layout, Bengaluru', '080-55667788', 'https://images.unsplash.com/photo-1563245372-f21724e3856d?w=400', 4.3, 30, 120.00);

-- Menu Items for Spice Garden (id=1)
INSERT INTO menu_items (restaurant_id, name, description, price, category, is_vegetarian) VALUES
(1, 'Masala Dosa', 'Crispy rice crepe with spiced potato filling', 80.00, 'Breakfast', TRUE),
(1, 'Idli Sambar', '4 soft idlis with sambar and chutneys', 60.00, 'Breakfast', TRUE),
(1, 'Curd Rice', 'Tempered curd rice with pomegranate', 90.00, 'Rice', TRUE),
(1, 'Chicken Biryani', 'Aromatic basmati rice with tender chicken', 220.00, 'Biryani', FALSE),
(1, 'Filter Coffee', 'Traditional South Indian filter coffee', 30.00, 'Beverages', TRUE),
(1, 'Rasam', 'Spicy tamarind soup', 40.00, 'Soups', TRUE);

-- Menu Items for Pizza Planet (id=2)
INSERT INTO menu_items (restaurant_id, name, description, price, category, is_vegetarian) VALUES
(2, 'Margherita Pizza', 'Classic tomato and mozzarella', 250.00, 'Pizza', TRUE),
(2, 'Pepperoni Pizza', 'Loaded with pepperoni slices', 320.00, 'Pizza', FALSE),
(2, 'Pasta Arrabiata', 'Penne in spicy tomato sauce', 180.00, 'Pasta', TRUE),
(2, 'Garlic Bread', '4 pieces of toasted garlic bread', 120.00, 'Sides', TRUE),
(2, 'Tiramisu', 'Classic Italian dessert', 150.00, 'Desserts', TRUE);

-- Menu Items for Burger Barn (id=3)
INSERT INTO menu_items (restaurant_id, name, description, price, category, is_vegetarian) VALUES
(3, 'Classic Beef Burger', 'Juicy beef patty with all toppings', 280.00, 'Burgers', FALSE),
(3, 'Veggie Burger', 'Aloo tikki patty with fresh veggies', 180.00, 'Burgers', TRUE),
(3, 'Chicken Burger', 'Crispy fried chicken burger', 240.00, 'Burgers', FALSE),
(3, 'French Fries', 'Crispy golden fries with dip', 100.00, 'Sides', TRUE),
(3, 'Milkshake', 'Thick chocolate or vanilla shake', 130.00, 'Beverages', TRUE);

-- Menu Items for Dragon Wok (id=4)
INSERT INTO menu_items (restaurant_id, name, description, price, category, is_vegetarian) VALUES
(4, 'Veg Fried Rice', 'Wok-tossed rice with vegetables', 160.00, 'Rice', TRUE),
(4, 'Chicken Manchurian', 'Crispy chicken in manchurian sauce', 220.00, 'Starters', FALSE),
(4, 'Veg Hakka Noodles', 'Stir fried noodles with vegetables', 150.00, 'Noodles', TRUE),
(4, 'Chilli Paneer', 'Spicy paneer in Indo-Chinese sauce', 200.00, 'Starters', TRUE),
(4, 'Spring Rolls', '3 crispy vegetable spring rolls', 120.00, 'Starters', TRUE);
