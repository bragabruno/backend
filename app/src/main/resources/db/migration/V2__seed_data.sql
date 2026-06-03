-- V2: Seed demo data

-- ============================================================
-- USERS (passwords are BCrypt hashes of "password123")
-- ============================================================
INSERT INTO users (id, username, email, password_hash, role, status, created_at, updated_at, version) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'admin', 'admin@frauddetection.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 'ACTIVE', NOW(), NOW(), 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'analyst1', 'analyst1@frauddetection.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'FRAUD_ANALYST', 'ACTIVE', NOW(), NOW(), 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'analyst2', 'analyst2@frauddetection.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'FRAUD_ANALYST', 'ACTIVE', NOW(), NOW(), 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'investigator1', 'investigator1@frauddetection.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'INVESTIGATOR', 'ACTIVE', NOW(), NOW(), 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'auditor1', 'auditor1@frauddetection.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUDITOR', 'ACTIVE', NOW(), NOW(), 0),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'system', 'system@frauddetection.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'SYSTEM_ACCOUNT', 'ACTIVE', NOW(), NOW(), 0);

-- ============================================================
-- MERCHANTS
-- ============================================================
INSERT INTO merchants (id, name, mcc, risk_tier, country) VALUES
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Amazon', '5942', 'LOW', 'US'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'Walmart', '5411', 'LOW', 'US'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'Target', '5331', 'LOW', 'US'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'Best Buy', '5732', 'LOW', 'US'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'Apple Store', '5732', 'LOW', 'US'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'Unknown Shop', '5999', 'MEDIUM', 'US'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'Foreign Electronics', '5732', 'HIGH', 'NG'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'Online Gaming', '7995', 'HIGH', 'RU'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'Gas Station', '5541', 'LOW', 'US'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'Jewelry Store', '5944', 'MEDIUM', 'US');

-- ============================================================
-- DEVICES
-- ============================================================
INSERT INTO devices (id, fingerprint, type, trusted, first_seen, last_seen) VALUES
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'fp-iphone-001', 'MOBILE', true, NOW() - INTERVAL '90 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'fp-android-001', 'MOBILE', true, NOW() - INTERVAL '60 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'fp-chrome-001', 'DESKTOP', true, NOW() - INTERVAL '120 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'fp-safari-001', 'DESKTOP', false, NOW() - INTERVAL '5 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a05', 'fp-firefox-001', 'DESKTOP', false, NOW() - INTERVAL '2 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a06', 'fp-china-001', 'MOBILE', false, NOW() - INTERVAL '1 day', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a07', 'fp-nigeria-001', 'DESKTOP', false, NOW() - INTERVAL '1 day', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a08', 'fp-russia-001', 'DESKTOP', false, NOW() - INTERVAL '3 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a09', 'fp-tablet-001', 'TABLET', true, NOW() - INTERVAL '45 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a10', 'fp-unknown-001', 'UNKNOWN', false, NOW() - INTERVAL '1 hour', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'fp-iphone-002', 'MOBILE', false, NOW() - INTERVAL '10 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'fp-android-002', 'MOBILE', false, NOW() - INTERVAL '7 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'fp-chrome-002', 'DESKTOP', false, NOW() - INTERVAL '15 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'fp-safari-002', 'DESKTOP', true, NOW() - INTERVAL '30 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'fp-edge-001', 'DESKTOP', false, NOW() - INTERVAL '4 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'fp-opera-001', 'DESKTOP', false, NOW() - INTERVAL '2 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a17', 'fp-iphone-003', 'MOBILE', true, NOW() - INTERVAL '180 days', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a18', 'fp-android-003', 'MOBILE', false, NOW() - INTERVAL '6 hours', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a19', 'fp-chrome-003', 'DESKTOP', false, NOW() - INTERVAL '30 minutes', NOW()),
    ('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a20', 'fp-bot-001', 'UNKNOWN', false, NOW() - INTERVAL '5 minutes', NOW());
