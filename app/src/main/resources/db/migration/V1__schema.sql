-- V1: Create schema for all 9 core entities

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'FRAUD_ANALYST', 'INVESTIGATOR', 'AUDITOR', 'SYSTEM_ACCOUNT')),
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- ============================================================
-- DEVICES
-- ============================================================
CREATE TABLE devices (
    id UUID PRIMARY KEY,
    fingerprint VARCHAR(255) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL,
    trusted BOOLEAN NOT NULL DEFAULT FALSE,
    first_seen TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_seen TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_devices_fingerprint ON devices(fingerprint);

-- ============================================================
-- MERCHANTS
-- ============================================================
CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    mcc VARCHAR(4),
    risk_tier VARCHAR(10) CHECK (risk_tier IN ('LOW', 'MEDIUM', 'HIGH')),
    country VARCHAR(2)
);

CREATE INDEX idx_merchants_mcc ON merchants(mcc);
CREATE INDEX idx_merchants_country ON merchants(country);

-- ============================================================
-- TRANSACTIONS
-- ============================================================
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    device_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    ip_address VARCHAR(45),
    country VARCHAR(2),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RECEIVED', 'SCORING', 'APPROVED', 'IN_REVIEW', 'DECLINED')) DEFAULT 'RECEIVED',
    idempotency_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_merchant_id ON transactions(merchant_id);
CREATE INDEX idx_transactions_device_id ON transactions(device_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_status ON transactions(status);

-- ============================================================
-- RISK SCORES
-- ============================================================
CREATE TABLE risk_scores (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    model_version_id UUID,
    ml_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    rules_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    aggregate_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('APPROVE', 'REVIEW', 'DECLINE')),
    degraded_mode BOOLEAN NOT NULL DEFAULT FALSE,
    reason_codes JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_risk_scores_transaction_id ON risk_scores(transaction_id);
CREATE INDEX idx_risk_scores_created_at ON risk_scores(created_at);
CREATE INDEX idx_risk_scores_decision ON risk_scores(decision);

-- ============================================================
-- FRAUD CASES
-- ============================================================
CREATE TABLE fraud_cases (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE,
    risk_score_id UUID,
    assignee_id UUID,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_REVIEW', 'RESOLVED_FRAUD', 'RESOLVED_LEGIT', 'ESCALATED', 'CLOSED')) DEFAULT 'OPEN',
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')) DEFAULT 'MEDIUM',
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sla_due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_fraud_cases_status ON fraud_cases(status);
CREATE INDEX idx_fraud_cases_severity ON fraud_cases(severity);
CREATE INDEX idx_fraud_cases_assignee_id ON fraud_cases(assignee_id);
CREATE INDEX idx_fraud_cases_opened_at ON fraud_cases(opened_at);
CREATE INDEX idx_fraud_cases_transaction_id ON fraud_cases(transaction_id);

-- ============================================================
-- FRAUD LABELS
-- ============================================================
CREATE TABLE fraud_labels (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    case_id UUID,
    analyst_id UUID NOT NULL,
    label VARCHAR(20) NOT NULL CHECK (label IN ('FRAUD', 'LEGITIMATE')),
    confidence DOUBLE PRECISION NOT NULL CHECK (confidence >= 0.0 AND confidence <= 1.0),
    reason TEXT,
    labeled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fraud_labels_transaction_id ON fraud_labels(transaction_id);
CREATE INDEX idx_fraud_labels_case_id ON fraud_labels(case_id);
CREATE INDEX idx_fraud_labels_analyst_id ON fraud_labels(analyst_id);

-- ============================================================
-- MODEL VERSIONS
-- ============================================================
CREATE TABLE model_versions (
    id UUID PRIMARY KEY,
    version VARCHAR(50) UNIQUE NOT NULL,
    mlflow_run_id VARCHAR(100),
    metrics JSONB,
    status VARCHAR(20) NOT NULL CHECK (status IN ('REGISTERED', 'APPROVED', 'DEPLOYED', 'ROLLED_BACK', 'ARCHIVED')) DEFAULT 'REGISTERED',
    deployed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_model_versions_status ON model_versions(status);

-- ============================================================
-- AUDIT EVENTS
-- ============================================================
CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID,
    before TEXT,
    after TEXT,
    correlation_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_events_target_type ON audit_events(target_type);
CREATE INDEX idx_audit_events_target_id ON audit_events(target_id);
CREATE INDEX idx_audit_events_created_at ON audit_events(created_at);
CREATE INDEX idx_audit_events_correlation_id ON audit_events(correlation_id);
