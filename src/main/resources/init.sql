-- Badminton Booking System
-- MySQL 8+ initialization schema for the current application model.
-- Safe to run repeatedly: existing tables and data are preserved.

CREATE DATABASE IF NOT EXISTS badminton_booking_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE badminton_booking_db;

-- ---------------------------------------------------------------------------
-- Users and authentication
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    role VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER',
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role_active (role, is_active),

    CONSTRAINT chk_users_role
        CHECK (role IN ('ADMIN', 'MANAGER', 'CUSTOMER'))
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
    KEY idx_refresh_tokens_expiry_revoked (expiry_date, revoked),

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- Courts, managers, images, and time slots
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS courts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000) NULL,
    address VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,

    PRIMARY KEY (id),
    KEY idx_courts_status (status),
    KEY idx_courts_name (name),

    CONSTRAINT chk_courts_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS court_managers (
    court_id BIGINT NOT NULL,
    manager_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,

    PRIMARY KEY (court_id, manager_id),
    KEY idx_court_managers_manager (manager_id),

    CONSTRAINT fk_court_managers_court
        FOREIGN KEY (court_id) REFERENCES courts (id) ON DELETE CASCADE,
    CONSTRAINT fk_court_managers_manager
        FOREIGN KEY (manager_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS court_images (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    court_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    url VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_court_images_url (url),
    KEY idx_court_images_court_created (court_id, created_at),

    CONSTRAINT fk_court_images_court
        FOREIGN KEY (court_id) REFERENCES courts (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS time_slots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    court_id BIGINT NOT NULL,
    start_time TIME(6) NOT NULL,
    end_time TIME(6) NOT NULL,
    price INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    UNIQUE KEY uk_time_slots_court_start_end (court_id, start_time, end_time),
    KEY idx_time_slots_court_active_start (court_id, active, start_time),

    CONSTRAINT fk_time_slots_court
        FOREIGN KEY (court_id) REFERENCES courts (id) ON DELETE CASCADE,
    CONSTRAINT chk_time_slots_time
        CHECK (start_time < end_time)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- Bookings
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    court_id BIGINT NOT NULL,
    booking_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    note VARCHAR(255) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,

    PRIMARY KEY (id),
    KEY idx_bookings_customer_created (customer_id, created_at),
    KEY idx_bookings_court_date_status (court_id, booking_date, status),

    CONSTRAINT fk_bookings_customer
        FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_court
        FOREIGN KEY (court_id) REFERENCES courts (id),
    CONSTRAINT chk_bookings_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED', 'COMPLETED'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS booking_time_slots (
    booking_id BIGINT NOT NULL,
    time_slot_id BIGINT NOT NULL,

    PRIMARY KEY (booking_id, time_slot_id),
    KEY idx_booking_time_slots_time_slot (time_slot_id),

    CONSTRAINT fk_booking_time_slots_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_time_slots_time_slot
        FOREIGN KEY (time_slot_id) REFERENCES time_slots (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- Audit
-- ---------------------------------------------------------------------------

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
