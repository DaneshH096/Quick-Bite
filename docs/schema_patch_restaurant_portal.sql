-- ============================================================
-- PATCH: Restaurant-owner login, open/close status, and the
--        full order pipeline (PREPARED + PICKED_UP stages)
-- Run this AFTER schema.sql and schema_patch_location.sql
-- (spring.jpa.hibernate.ddl-auto=update will also add these columns
--  automatically on app startup, but this file documents the change
--  and is handy for manual/production DB migrations.)
-- ============================================================

USE food_delivery_db;

-- 1. Add RESTAURANT_OWNER to the users.role enum
ALTER TABLE users
  MODIFY COLUMN role ENUM('CUSTOMER','ADMIN','DELIVERY_PARTNER','RESTAURANT_OWNER') DEFAULT 'CUSTOMER';

-- 2. Link a restaurant to the user account that owns/manages it,
--    plus a live open/closed toggle separate from the admin's isActive switch.
ALTER TABLE restaurants
  ADD COLUMN IF NOT EXISTS owner_id BIGINT NULL,
  ADD COLUMN IF NOT EXISTS is_open  BOOLEAN DEFAULT TRUE;

ALTER TABLE restaurants
  ADD CONSTRAINT fk_restaurant_owner
    FOREIGN KEY (owner_id) REFERENCES users(id);

-- 3. Extend the order status pipeline with PREPARED (food ready, awaiting
--    a delivery partner) and PICKED_UP (partner has collected it, not yet
--    "out for delivery" to the customer).
ALTER TABLE orders
  MODIFY COLUMN status
    ENUM('PENDING','CONFIRMED','PREPARING','PREPARED','PICKED_UP',
         'OUT_FOR_DELIVERY','DELIVERED','CANCELLED') DEFAULT 'PENDING';

-- 4. Backfill: ddl-auto=update adds the new column to EXISTING rows as NULL,
--    not TRUE, even though the column has a DEFAULT TRUE for future inserts.
--    A null Boolean then breaks any Thymeleaf expression that negates it
--    directly inside ${...} (e.g. ${!r.isOpen}), so backfill it explicitly.
UPDATE restaurants SET is_open = TRUE WHERE is_open IS NULL;
