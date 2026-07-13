-- ============================================================
-- PATCH: Location + Delivery Partner support
-- Run this AFTER the original schema.sql
-- ============================================================

USE food_delivery_db;

-- 1. Add lat/lng + delivery coords to orders
ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS delivery_lat   DECIMAL(10,7) NULL,
  ADD COLUMN IF NOT EXISTS delivery_lng   DECIMAL(10,7) NULL,
  ADD COLUMN IF NOT EXISTS delivery_partner_id BIGINT NULL;

-- 2. Add DELIVERY_PARTNER role to users (extend enum safely via string column)
-- MySQL ENUM can be extended with ALTER TABLE
ALTER TABLE users
  MODIFY COLUMN role ENUM('CUSTOMER','ADMIN','DELIVERY_PARTNER') DEFAULT 'CUSTOMER';

-- 3. Add delivery partner location tracking
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS current_lat    DECIMAL(10,7) NULL,
  ADD COLUMN IF NOT EXISTS current_lng    DECIMAL(10,7) NULL,
  ADD COLUMN IF NOT EXISTS is_available   BOOLEAN DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS vehicle_type   VARCHAR(50) NULL;

-- 4. FK for delivery_partner_id (add after users table is ready)
-- We use a soft reference (no FK) to allow null
-- ALTER TABLE orders ADD CONSTRAINT fk_delivery_partner
--   FOREIGN KEY (delivery_partner_id) REFERENCES users(id);

-- 5. Seed a delivery partner account (password: partner123)
INSERT IGNORE INTO users (name, email, password, phone, role, vehicle_type, is_available)
VALUES
  ('Ravi Kumar',  'ravi@delivery.com',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', '9111111111', 'DELIVERY_PARTNER', 'Bike',   TRUE),
  ('Priya Nair',  'priya@delivery.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', '9222222222', 'DELIVERY_PARTNER', 'Scooter',TRUE);

