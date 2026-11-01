-- Sentinel Fraud Detection Engine - Database Schema

CREATE DATABASE IF NOT EXISTS sentinel_db;
USE sentinel_db;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    role ENUM('EMPLOYEE', 'MANAGER', 'ADMIN') NOT NULL DEFAULT 'EMPLOYEE'
);

CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    location VARCHAR(100) NOT NULL,
    txn_time DATETIME NOT NULL,
    risk_score DECIMAL(5,2) DEFAULT 0,
    flagged BOOLEAN DEFAULT FALSE,
    flag_reason VARCHAR(255),
    INDEX idx_account (account_id),
    INDEX idx_flagged (flagged)
);

-- Sample seed data
INSERT INTO users (username, role) VALUES
    ('alice', 'EMPLOYEE'),
    ('bob', 'MANAGER'),
    ('carol', 'ADMIN');
