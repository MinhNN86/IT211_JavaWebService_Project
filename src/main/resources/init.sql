-- Badminton Booking System - MySQL 8 schema
-- Safe to run repeatedly: existing tables and data are preserved.
-- For an existing BIGINT users.id schema, run migrate_users_id_to_uuid.sql once.

CREATE DATABASE IF NOT EXISTS badminton_booking_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE badminton_booking_db;

CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT (UUID()),
    full_name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER',
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'MANAGER', 'CUSTOMER'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS courts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NULL,
    address VARCHAR(255) NOT NULL,
    price_per_hour DECIMAL(12, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    image_url VARCHAR(255) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_courts_status_price (status, price_per_hour),
    KEY idx_courts_name (name),
    CONSTRAINT chk_courts_price CHECK (price_per_hour > 0),
    CONSTRAINT chk_courts_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS time_slots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    start_time TIME(6) NOT NULL,
    end_time TIME(6) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_time_slots_start_end (start_time, end_time),
    CONSTRAINT chk_time_slots_time CHECK (start_time < end_time),
    CONSTRAINT chk_time_slots_price CHECK (price > 0)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    court_id BIGINT NOT NULL,
    time_slot_id BIGINT NOT NULL,
    booking_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    note VARCHAR(255) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_bookings_customer (customer_id),
    KEY idx_bookings_availability (court_id, booking_date, time_slot_id, status),
    KEY idx_bookings_time_slot (time_slot_id),
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_court FOREIGN KEY (court_id) REFERENCES courts (id),
    CONSTRAINT fk_bookings_time_slot FOREIGN KEY (time_slot_id) REFERENCES time_slots (id),
    CONSTRAINT chk_bookings_status CHECK (
        status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED', 'COMPLETED')
    )
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(1000) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_token (token),
    KEY idx_refresh_tokens_user (user_id),
    KEY idx_refresh_tokens_expiry (expiry_date),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NULL,
    action VARCHAR(255) NOT NULL,
    message VARCHAR(2000) NULL,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_audit_logs_username (username),
    KEY idx_audit_logs_created_at (created_at)
) ENGINE = InnoDB;

-- Demo users are created by DataInitializer when the application starts.
-- Default credentials: admin/123456, manager/123456, customer/123456.
