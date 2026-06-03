# Backend — Spring Boot 3.5 / Java 21 Backlog

**Repo:** `backend` (polyrepo: `github.com/bragabruno/backend.git`)
**Package root:** `com.bragdev.frauddetection`
**Java:** 21 · **Spring Boot:** 3.5.14 · **Gradle:** Kotlin DSL
**Owner:** BragDev LLC

> This backlog continues the platform tickets in `../../docs/TICKETS.md` (FRAUD-001–161)
> and the ML tickets in `../../ml-service/docs/TICKETS.md` (FRAUD-162–207). Backend
> tickets start at **FRAUD-208** to avoid id collisions in the same Linear workspace.
>
> This file is **self-contained**: API contract, data model, state machines, and auth spec
> are embedded below so an implementing agent does not need to read other repos.

---

## Summary

| Epic | Title | Tickets | Primary skills |
|---|---|---|---|
| **EPIC-31** | Infrastructure & Domain Model | FRAUD-208–214 | Docker, Makefile, Testcontainers, Flyway, JPA, MapStruct |
| **EPIC-32** | Security (JWT / RBAC) | FRAUD-215–220 | Spring Security, RS256, OAuth |
| **EPIC-33** | Transaction & Event Service | FRAUD-221–225 | Kafka, outbox, idempotency |
| **EPIC-34** | Rules & Fraud Engine | FRAUD-226–229 | Rules SPI, scoring, Resilience4j |
| **EPIC-35** | Case Management (critical path) | FRAUD-230–234 | SSE, state machine, workflow |
| **EPIC-36** | Observability, Admin & CI | FRAUD-235–236 | Actuator, Prometheus, GitHub Actions |

---

## Pre-existing Infrastructure

The following already exist in the repo (from platform tickets FRAUD-001/002):

- **Docker Compose** (`docker-compose.yml`): Postgres 16 (port 5432), Redis 7 (port 6379), Kafka KRaft (port 9092)
- **`.env`** file with all local connection strings
- **Spring profiles**: `local`, `dev`, `staging`, `prod` (4 profiles)
- **Gradle multi-module build**: 7 modules — `app`, `common`, `transactions`, `rules`, `fraud-engine`, `case-management`, `security`
- **Only existing code**: `FrauddetectionApplication.java` (15 lines); all module `build.gradle.kts` files are empty (0 bytes) except `app/`
- **Version catalog** (`gradle/libs.versions.toml`): Spring Boot 3.5.14, Lombok 1.18.36, ErrorProne 2.23.0, PostgreSQL 42.7.2

---

## API Contract

All endpoints are prefixed `/api`. Authentication via `Authorization: Bearer <JWT>` unless noted.

### Auth Endpoints

#### `POST /api/auth/login`
```
Request:
{
  "username": "string",
  "password": "string"
}

Response 200:
{
  "accessToken": "string (JWT, 15min TTL)",
  "refreshToken": "string (JWT, 24h TTL)",
  "role": "ADMIN | FRAUD_ANALYST | INVESTIGATOR | AUDITOR | SYSTEM_ACCOUNT",
  "userId": "uuid",
  "username": "string"
}

Response 401:
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid credentials"
}
```

#### `POST /api/auth/refresh`
```
Request:
{
  "refreshToken": "string"
}

Response 200:
{
  "accessToken": "string (new JWT, 15min TTL)",
  "refreshToken": "string (new refresh, 24h TTL)"
}

Response 401:
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid or expired refresh token"
}
```

#### `POST /api/auth/logout`
```
Request:
{
  "refreshToken": "string"
}

Response 204 (no content)
```

### Transaction Endpoints

#### `POST /api/transactions`

Requires role: `SYSTEM_ACCOUNT` or `ADMIN`.

```
Request:
{
  "userId": "uuid",
  "merchantId": "uuid",
  "deviceId": "uuid",
  "amount": 1500.00,
  "currency": "USD",
  "ipAddress": "192.168.1.1",
  "country": "US",
  "idempotencyKey": "string (unique per client attempt)"
}

Response 201:
{
  "id": "uuid",
  "userId": "uuid",
  "merchantId": "uuid",
  "deviceId": "uuid",
  "amount": 1500.00,
  "currency": "USD",
  "ipAddress": "192.168.1.1",
  "country": "US",
  "status": "RECEIVED",
  "idempotencyKey": "string",
  "createdAt": "2026-06-02T12:00:00Z"
}

Response 400 (validation error):
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "errors": [
    { "field": "amount", "message": "must be positive" },
    { "field": "currency", "message": "must be ISO 4217" }
  ]
}
```

#### `GET /api/transactions/{id}`
Requires role: `ADMIN`, `FRAUD_ANALYST`, `INVESTIGATOR`, `AUDITOR`.

```
Response 200:
{
  "id": "uuid",
  "userId": "uuid",
  "merchantId": "uuid",
  "deviceId": "uuid",
  "amount": 1500.00,
  "currency": "USD",
  "ipAddress": "192.168.1.1",
  "country": "US",
  "status": "IN_REVIEW",
  "idempotencyKey": "string",
  "createdAt": "2026-06-02T12:00:00Z",
  "latestRiskScore": { ... }
}
```

#### `GET /api/transactions?page=0&size=20&status=IN_REVIEW&userId=uuid`

Paginated list with optional filters. Same role requirements as GET by id.

```
Response 200:
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 142,
  "totalPages": 8
}
```

### Case Management Endpoints

#### `GET /api/cases?page=0&size=20&status=OPEN&severity=HIGH&assigneeId=uuid`

Requires role: `ADMIN`, `FRAUD_ANALYST`, `INVESTIGATOR`, `AUDITOR` (read-only for AUDITOR).

```
Response 200:
{
  "content": [
    {
      "id": "uuid",
      "transactionId": "uuid",
      "riskScoreId": "uuid",
      "assigneeId": "uuid | null",
      "status": "OPEN",
      "severity": "HIGH",
      "openedAt": "2026-06-02T12:00:00Z",
      "slaDueAt": "2026-06-03T12:00:00Z",
      "resolvedAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 47,
  "totalPages": 3
}
```

#### `GET /api/cases/{id}`
```
Response 200:
{
  "id": "uuid",
  "transactionId": "uuid",
  "riskScoreId": "uuid",
  "assigneeId": "uuid | null",
  "status": "IN_REVIEW",
  "severity": "CRITICAL",
  "openedAt": "2026-06-02T12:00:00Z",
  "slaDueAt": "2026-06-03T12:00:00Z",
  "resolvedAt": null,
  "transaction": { ... },
  "riskScore": {
    "id": "uuid",
    "transactionId": "uuid",
    "mlScore": 0.87,
    "rulesScore": 0.65,
    "aggregateScore": 0.79,
    "decision": "REVIEW",
    "degradedMode": false,
    "reasonCodes": ["HIGH_AMOUNT", "NEW_DEVICE", "FOREIGN_COUNTRY"],
    "modelVersion": "v2.3.1",
    "createdAt": "2026-06-02T12:00:01Z"
  },
  "notes": [ ... ],
  "labels": [ ... ]
}
```

#### `PUT /api/cases/{id}/assign`
Requires role: `ADMIN`, `FRAUD_ANALYST`, `INVESTIGATOR`.

```
Request:
{
  "assigneeId": "uuid"
}

Response 200:
{
  "id": "uuid",
  "assigneeId": "uuid",
  "status": "ASSIGNED",
  ...
}
```

#### `PUT /api/cases/{id}/status`
Requires role: `ADMIN`, `FRAUD_ANALYST` (IN_REVIEW can escalate), `INVESTIGATOR` (ESCALATED can resolve).

```
Request:
{
  "status": "IN_REVIEW",
  "note": "Starting investigation"
}

Response 200: Case with updated status
```

Valid transitions (enforced server-side):
- OPEN → ASSIGNED
- ASSIGNED → IN_REVIEW
- IN_REVIEW → RESOLVED_FRAUD, RESOLVED_LEGIT, ESCALATED
- ESCALATED → IN_REVIEW, RESOLVED_FRAUD, RESOLVED_LEGIT
- RESOLVED_FRAUD → CLOSED
- RESOLVED_LEGIT → CLOSED

#### `GET /api/cases/{id}/notes`
```
Response 200:
[
  {
    "id": "uuid",
    "caseId": "uuid",
    "authorId": "uuid",
    "content": "string",
    "createdAt": "2026-06-02T14:30:00Z"
  }
]
```

#### `POST /api/cases/{id}/notes`
Requires role: `ADMIN`, `FRAUD_ANALYST`, `INVESTIGATOR`.

```
Request:
{
  "content": "Transaction matches known fraud pattern — multiple devices in 24h."
}

Response 201:
{
  "id": "uuid",
  "caseId": "uuid",
  "authorId": "uuid",
  "content": "string",
  "createdAt": "2026-06-02T14:30:00Z"
}
```

#### `POST /api/cases/{id}/labels`
Requires role: `ADMIN`, `FRAUD_ANALYST`, `INVESTIGATOR`.

```
Request:
{
  "label": "FRAUD",
  "confidence": 0.95,
  "reason": "Confirmed card-testing pattern"
}

Response 201:
{
  "id": "uuid",
  "transactionId": "uuid",
  "caseId": "uuid",
  "analystId": "uuid",
  "label": "FRAUD",
  "confidence": 0.95,
  "reason": "Confirmed card-testing pattern",
  "labeledAt": "2026-06-02T14:35:00Z"
}
```

#### `GET /api/cases/stream` (SSE)

Server-Sent Events stream for real-time case queue updates.

```
Event types:
- case_created    : new case opened
- case_assigned   : case assigned to analyst
- case_updated    : status or severity change
- case_resolved   : case closed/resolved

Event payload (all types):
{
  "eventType": "case_created",
  "caseId": "uuid",
  "transactionId": "uuid",
  "status": "OPEN",
  "severity": "HIGH",
  "assigneeId": null,
  "openedAt": "2026-06-02T12:00:00Z",
  "timestamp": "2026-06-02T12:00:00.123Z"
}
```

Client connects: `GET /api/cases/stream` with `Accept: text/event-stream` and `Authorization: Bearer <JWT>`.

### Risk Score Endpoints

#### `GET /api/risk-scores/{transactionId}`

Requires role: `ADMIN`, `FRAUD_ANALYST`, `INVESTIGATOR`, `AUDITOR`.

```
Response 200:
{
  "id": "uuid",
  "transactionId": "uuid",
  "modelVersionId": "uuid",
  "mlScore": 0.87,
  "rulesScore": 0.65,
  "aggregateScore": 0.79,
  "decision": "REVIEW",
  "degradedMode": false,
  "reasonCodes": ["HIGH_AMOUNT", "NEW_DEVICE", "FOREIGN_COUNTRY"],
  "createdAt": "2026-06-02T12:00:01Z"
}
```

### Admin Endpoints

#### `GET /api/admin/users?page=0&size=20`

Requires role: `ADMIN`.

```
Response 200:
{
  "content": [
    {
      "id": "uuid",
      "username": "jdoe",
      "email": "jdoe@example.com",
      "role": "FRAUD_ANALYST",
      "status": "ACTIVE",
      "createdAt": "2026-01-15T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1
}
```

#### `POST /api/admin/users`

Requires role: `ADMIN`.

```
Request:
{
  "username": "jdoe",
  "email": "jdoe@example.com",
  "password": "SecureP@ss1",
  "role": "FRAUD_ANALYST"
}

Response 201:
{
  "id": "uuid",
  "username": "jdoe",
  "email": "jdoe@example.com",
  "role": "FRAUD_ANALYST",
  "status": "ACTIVE",
  "createdAt": "2026-06-02T12:00:00Z"
}
```

#### `GET /api/admin/rules`

Requires role: `ADMIN`.

```
Response 200:
[
  {
    "id": "uuid",
    "name": "velocity_5m",
    "type": "VELOCITY",
    "enabled": true,
    "weight": 0.3,
    "threshold": 5,
    "description": "Flags 5+ transactions in 5 minutes",
    "updatedAt": "2026-05-28T10:00:00Z"
  }
]
```

#### `PUT /api/admin/rules/{id}`

Requires role: `ADMIN`.

```
Request:
{
  "enabled": true,
  "weight": 0.35,
  "threshold": 4
}

Response 200: updated rule
```

### Health / Observability

#### `GET /actuator/health`
```
Response 200:
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "kafka": { "status": "UP" }
  }
}
```

#### `GET /actuator/prometheus`
Prometheus metrics including: `http_server_requests_seconds`, `fraud_scoring_duration_seconds`, `fraud_decisions_total{decision}`, `kafka_consumer_lag_seconds`.

---

## Error Format

All error responses use RFC 7807 `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Case with id 550e8400-e29b-41d4-a716-446655440000 not found"
}
```

Validation errors (400) include a field-level breakdown:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "errors": [
    { "field": "amount", "message": "must be positive" },
    { "field": "currency", "message": "must be ISO 4217" }
  ]
}
```

---

## Data Model

### Entities (9 core tables)

```java
// ---- com.bragdev.frauddetection.common.model.User ----
@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Role role;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}

// ---- com.bragdev.frauddetection.common.model.Transaction ----
@Entity @Table(name = "transactions")
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private UUID merchantId;
    @Column(nullable = false) private UUID deviceId;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(length = 3, nullable = false) private String currency;
    private String ipAddress;
    private String country;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TransactionStatus status;
    @Column(unique = true) private String idempotencyKey;
    private Instant createdAt;
}

// ---- com.bragdev.frauddetection.common.model.Device ----
@Entity @Table(name = "devices")
public class Device {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, nullable = false) private String fingerprint;
    @Column(nullable = false) private String type;
    private boolean trusted;
    private Instant firstSeen;
    private Instant lastSeen;
}

// ---- com.bragdev.frauddetection.common.model.Merchant ----
@Entity @Table(name = "merchants")
public class Merchant {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private String name;
    @Column(length = 4) private String mcc;
    @Enumerated(EnumType.STRING) private RiskTier riskTier;
    private String country;
}

// ---- com.bragdev.frauddetection.common.model.RiskScore ----
@Entity @Table(name = "risk_scores")
public class RiskScore {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID transactionId;
    @Column(nullable = false) private UUID modelVersionId;
    @Column(nullable = false) private double mlScore;
    @Column(nullable = false) private double rulesScore;
    @Column(nullable = false) private double aggregateScore;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Decision decision;
    @Column(nullable = false) private boolean degradedMode;
    @Column(columnDefinition = "jsonb") private List<String> reasonCodes;
    private Instant createdAt;
}

// ---- com.bragdev.frauddetection.common.model.FraudCase ----
@Entity @Table(name = "fraud_cases")
public class FraudCase {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID transactionId;
    private UUID riskScoreId;
    private UUID assigneeId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private CaseStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Severity severity;
    private Instant openedAt;
    private Instant slaDueAt;
    private Instant resolvedAt;
}

// ---- com.bragdev.frauddetection.common.model.FraudLabel ----
@Entity @Table(name = "fraud_labels")
public class FraudLabel {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID transactionId;
    private UUID caseId;
    @Column(nullable = false) private UUID analystId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private LabelType label;
    @Column(nullable = false) private double confidence;
    @Column(columnDefinition = "TEXT") private String reason;
    private Instant labeledAt;
}

// ---- com.bragdev.frauddetection.common.model.ModelVersion ----
@Entity @Table(name = "model_versions")
public class ModelVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, nullable = false) private String version;
    private String mlflowRunId;
    @Column(columnDefinition = "jsonb") private Map<String, Double> metrics;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ModelStatus status;
    private Instant deployedAt;
    private Instant createdAt;
}

// ---- com.bragdev.frauddetection.common.model.AuditEvent ----
@Entity @Table(name = "audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private String actor;
    @Column(nullable = false) private String action;
    @Column(nullable = false) private String targetType;
    private UUID targetId;
    @Column(columnDefinition = "jsonb") private String before;
    @Column(columnDefinition = "jsonb") private String after;
    private String correlationId;
    private Instant createdAt;
}
```

### Enums

```java
public enum Role { ADMIN, FRAUD_ANALYST, INVESTIGATOR, AUDITOR, SYSTEM_ACCOUNT }
public enum UserStatus { ACTIVE, DISABLED }
public enum TransactionStatus { RECEIVED, SCORING, APPROVED, IN_REVIEW, DECLINED }
public enum Decision { APPROVE, REVIEW, DECLINE }
public enum CaseStatus { OPEN, ASSIGNED, IN_REVIEW, RESOLVED_FRAUD, RESOLVED_LEGIT, ESCALATED, CLOSED }
public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
public enum LabelType { FRAUD, LEGITIMATE }
public enum ModelStatus { REGISTERED, APPROVED, DEPLOYED, ROLLED_BACK, ARCHIVED }
public enum RiskTier { LOW, MEDIUM, HIGH }
```

### Indexes

- `transactions`: composite indexes on `user_id`, `merchant_id`, `device_id`, `created_at`; unique on `idempotency_key`
- `risk_scores`: indexes on `transaction_id`, `created_at`
- `fraud_cases`: indexes on `status`, `severity`, `assignee_id`, `opened_at`
- `fraud_labels`: index on `transaction_id`, `case_id`
- `audit_events`: index on `target_type`, `target_id`, `created_at`

---

## State Machines

### FraudCase Lifecycle

```
OPEN ──assign──▶ ASSIGNED ──start review──▶ IN_REVIEW ──escalate──▶ ESCALATED
                                            │                      │
                                            ├─label FRAUD──▶ RESOLVED_FRAUD ──finalize──▶ CLOSED
                                            ├─label LEGIT──▶ RESOLVED_LEGIT ──finalize──▶ CLOSED
                                            │                      │
                                     ESCALATED ◀────────────────────┘
                                     ESCALATED ──investigator──▶ IN_REVIEW
                                     ESCALATED ──resolve──▶ RESOLVED_FRAUD | RESOLVED_LEGIT
```

Every transition writes an `AuditEvent` (actor, action, before/after, correlation ID).

### Transaction Status

```
RECEIVED ──kafka consumed──▶ SCORING ──decision──▶ APPROVED | IN_REVIEW | DECLINED
IN_REVIEW ──analyst label──▶ APPROVED | DECLINED
```

### ModelVersion Lifecycle

```
REGISTERED ──gate passed──▶ APPROVED ──deploy──▶ DEPLOYED ──supersede──▶ ARCHIVED
DEPLOYED ──regression──▶ ROLLED_BACK ──restore──▶ DEPLOYED
```

---

## Auth & RBAC Specification

### JWT Claims

```json
{
  "sub": "uuid (userId)",
  "username": "jdoe",
  "role": "FRAUD_ANALYST",
  "iat": 1717334400,
  "exp": 1717334900
}
```

- Signing: RS256 (asymmetric key pair)
- Access token TTL: 15 minutes
- Refresh token TTL: 24 hours
- Refresh token rotation: new refresh issued on every refresh

### Role → Permission Matrix

| Endpoint | ADMIN | FRAUD_ANALYST | INVESTIGATOR | AUDITOR | SYSTEM_ACCOUNT |
|---|:---:|:---:|:---:|:---:|:---:|
| `POST /api/transactions` | ✅ | — | — | — | ✅ |
| `GET /api/transactions/*` | ✅ | ✅ | ✅ | ✅ | — |
| `GET /api/cases` | ✅ | ✅ | ✅ | ✅ (read) | — |
| `GET /api/cases/{id}` | ✅ | ✅ | ✅ | ✅ (read) | — |
| `PUT /api/cases/{id}/assign` | ✅ | ✅ | ✅ | — | — |
| `PUT /api/cases/{id}/status` | ✅ | ✅ | ✅ | — | — |
| `POST /api/cases/{id}/notes` | ✅ | ✅ | ✅ | — | — |
| `POST /api/cases/{id}/labels` | ✅ | ✅ | ✅ | — | — |
| `GET /api/cases/stream` | ✅ | ✅ | ✅ | ✅ (read) | — |
| `GET /api/risk-scores/*` | ✅ | ✅ | ✅ | ✅ | — |
| `GET /api/admin/users` | ✅ | — | — | — | — |
| `POST /api/admin/users` | ✅ | — | — | — | — |
| `GET /api/admin/rules` | ✅ | — | — | — | — |
| `PUT /api/admin/rules/{id}` | ✅ | — | — | — | — |

---

## Kafka Topics

7 domain topics (matching platform spec):

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `transactions.created` | Transaction Service | Fraud Engine | New transaction ingested |
| `fraud.scored` | Fraud Engine | Case Mgmt, ML | Scoring complete |
| `fraud.review.required` | Fraud Engine | Case Mgmt | Case needs analyst review |
| `fraud.confirmed` | Case Mgmt | ML | Analyst confirmed fraud |
| `fraud.falsepositive` | Case Mgmt | ML | Analyst marked false positive |
| `fraud.model.deployed` | ML | Fraud Engine, ML | New model version live |
| `fraud.retraining.requested` | Drift Monitor / ML | ML | Retraining trigger |

---

# PHASE 10 — BACKEND

## EPIC-31 — Infrastructure & Domain Model
*Phase 10 · Lead: BE/DO · Depends on: pre-existing scaffold (FRAUD-001/002)*

Docker Compose hardening, app Dockerfile, Makefile, Testcontainers, Flyway migrations, JPA entities, DTOs, and the shared error model. **Do infra tickets (FRAUD-208–210) first; domain model follows.**

### FRAUD-208 — Docker Compose hardening + app Dockerfile + healthchecks
**Type:** Infrastructure · **Epic:** EPIC-31 · **Complexity:** M · **Owner:** DO
**Description:** Harden the existing `docker-compose.yml`: add healthchecks to all services (Postgres, Redis, Kafka), add `depends_on` with health conditions so the app starts only when infra is ready, add a `Dockerfile` for the Spring Boot app (multi-stage build: `eclipse-temurin:21-jre-alpine`), and add a `app` service that builds from the Dockerfile with `.env` file binding. Expose port 8080. Add Kafka topic auto-creation via init container or script.
**Business Value:** One-command local stack with the app itself containerized; consistent with production.
**Acceptance Criteria:**
- `docker compose up --build` brings Postgres, Redis, Kafka, and the app healthy.
- App healthcheck passes at `/actuator/health`.
- Kafka topic auto-creation for the 7 domain topics.
- `.env` loaded by the app container.
- Data persists via named volumes.
**Technical Notes:** Use `HEALTHCHECK` + `curl` for the app; `pg_isready` for Postgres; `redis-cli ping` for Redis; `kafka-broker-api-versions` for Kafka. Kafka topics can be created via an init container running `kafka-topics --create` or a startup script.
**Dependencies:** —

### FRAUD-209 — Makefile / task runner
**Type:** Infrastructure · **Epic:** EPIC-31 · **Complexity:** S · **Owner:** DO
**Description:** Create a `Makefile` at the backend repo root with targets: `up` (docker compose up -d), `down` (docker compose down), `build` (./gradlew build), `test` (./gradlew test), `lint` (./gradlew check), `seed` (run Flyway seed migration), `reset` (docker compose down -v && docker compose up -d), `logs` (docker compose logs -f), `db` (connect to Postgres via psql), `redis-cli` (connect to Redis).
**Business Value:** Frictionless dev loop; a single command to bring up, test, or reset the environment.
**Acceptance Criteria:**
- `make up` brings all services healthy.
- `make test` runs unit + integration tests.
- `make reset` cleans volumes and re-creates the stack.
- Each target has a `help` comment.
**Technical Notes:** Use `.PHONY` for all targets. Include a `help` target that prints available commands.
**Dependencies:** FRAUD-208

### FRAUD-210 — Testcontainers for integration tests
**Type:** Technical Task · **Epic:** EPIC-31 · **Complexity:** M · **Owner:** BE
**Description:** Configure Testcontainers for Postgres, Redis, and Kafka so integration tests run against real infrastructure without manual setup. Create shared test base class or `@TestConfiguration` that starts containers once per test class. Wire into `src/test/resources/application-test.yml` that overrides the datasource, Redis, and Kafka URLs with container-provided values.
**Business Value:** Fast, isolated integration tests that run identically in CI and locally.
**Acceptance Criteria:**
- `./gradlew test` starts Postgres, Redis, and Kafka containers automatically.
- Test profile (`SPRING_PROFILES_ACTIVE=test`) uses container URLs.
- Tests clean up containers after the suite completes.
- No manual `docker compose up` required to run integration tests.
**Technical Notes:** Use `org.testcontainers:testcontainers-bom`. Postgres module: `org.testcontainers:postgresql`. Redis module: `org.testcontainers:redis`. Kafka module: `org.testcontainers:kafka`. Configure in `build.gradle.kts` for the `app` module. Use `@Testcontainers` + `@Container` annotations.
**Dependencies:** FRAUD-208

### FRAUD-211 — Flyway baseline & 9-entity migrations
**Type:** Infrastructure · **Epic:** EPIC-31 · **Complexity:** M · **Owner:** BE
**Description:** Create Flyway migrations for all 9 core entities from the domain model (`docs/diagrams/03-domain-model.md`): `users`, `transactions`, `devices`, `merchants`, `risk_scores`, `fraud_cases`, `fraud_labels`, `model_versions`, `audit_events`. Include all indexes, constraints, and enum types. Seed demo data: 1 admin, 2 analysts, 1 investigator, 1 auditor, 10 merchants, 20 devices.
**Business Value:** Schema is the foundation every feature builds on; seed data enables immediate manual testing.
**Acceptance Criteria:**
- `./gradlew flywayMigrate` applies all migrations cleanly to a fresh Postgres.
- `./gradlew flywayValidate` passes in CI.
- All 9 tables created with correct columns, types, constraints, and indexes.
- Seed migration inserts demo users and merchants.
**Technical Notes:** UUIDv7 for all PKs (time-sortable). Precise decimal for money (`numeric(19,4)`). `jsonb` for `reason_codes`, `metrics`, `before`/`after`. Enum columns as `varchar` with check constraints. Add CI step that runs Flyway validate against an ephemeral Testcontainers Postgres.
**Dependencies:** FRAUD-210

### FRAUD-212 — JPA entities & Spring Data repositories
**Type:** Story · **Epic:** EPIC-31 · **Complexity:** L · **Owner:** BE
**Description:** Implement JPA entities for all 9 domain objects with correct mappings, enums (using `@Enumerated(EnumType.STRING)`), relationships, and audit columns. Create Spring Data `JpaRepository` interfaces with custom query methods: case queue queries (`findByStatusOrderBySeverityDescOpenedAtAsc`), transaction lookups by user/merchant/device, latest risk score by transaction, label history by case.
**Business Value:** Type-safe persistence layer every service depends on.
**Acceptance Criteria:**
- All 9 entities compile and map to the Flyway schema without errors.
- Repository integration tests pass against Testcontainers Postgres.
- Custom query methods return correct results for seeded data.
**Technical Notes:** UUIDv7 generation strategy. `@Version` on mutable entities for optimistic locking. `@CreatedDate`/`@LastModifiedDate` via JPA auditing. Lombok for boilerplate.
**Dependencies:** FRAUD-211

### FRAUD-213 — Common module: error model, DTOs, MapStruct, pagination
**Type:** Story · **Epic:** EPIC-31 · **Complexity:** M · **Owner:** BE
**Description:** Build the `common` module with: (1) RFC 7807 `ProblemDetail` error response model via `@RestControllerAdvice`; (2) request/response DTOs for all endpoints with Jakarta Validation annotations; (3) MapStruct mappers for entity↔DTO conversion; (4) generic pagination envelope (`PageResponse<T>`); (5) correlation-ID filter (MDC + response header).
**Business Value:** Consistent API surface and error handling across all modules.
**Acceptance Criteria:**
- Validation errors return RFC 7807 with field-level breakdown.
- All DTOs have Jakarta Validation constraints matching the API contract.
- MapStruct mappers compile and round-trip entity↔DTO without data loss.
- Pagination envelope works for any entity type.
- Correlation ID propagated from request header to response and MDC.
**Technical Notes:** Centralized `ApiExceptionHandler` in `common`. Shared `MapStructConfig` with `componentModel = "spring"`.
**Dependencies:** FRAUD-212

### FRAUD-214 — OpenAPI / springdoc baseline & API docs
**Type:** Technical Task · **Epic:** EPIC-31 · **Complexity:** S · **Owner:** BE
**Description:** Wire springdoc-openapi to auto-generate and serve the API spec and Swagger UI. Group endpoints by domain tag (auth, transactions, cases, risk-scores, admin). Include JWT security scheme in the spec.
**Business Value:** Contract-first APIs enable frontend and QA to work against a stable spec.
**Acceptance Criteria:**
- `/v3/api-docs` and Swagger UI available at `/swagger-ui.html` in non-prod profiles.
- All endpoint groups appear as tagged sections.
- JWT bearer auth scheme documented.
**Technical Notes:** Add springdoc dependency to `app/build.gradle.kts`. Configure in `application-local/dev`. Suppress in `prod` via profile.
**Dependencies:** FRAUD-213

---

## EPIC-32 — Security (JWT / RBAC)
*Phase 10 · Lead: BE · Depends on: EPIC-31*

RS256 JWT authentication, 5-role RBAC, and endpoint authorization.

### FRAUD-215 — RS256 JWT authentication (login, refresh, logout)
**Type:** Story · **Epic:** EPIC-32 · **Complexity:** L · **Owner:** BE
**Description:** Implement JWT issuance and validation with Spring Security: login endpoint validates credentials against DB password hash and returns signed access + refresh tokens; refresh endpoint rotates tokens; logout invalidates refresh token. Use RS256 (asymmetric) signing with key pair from env vars. Access token TTL 15min, refresh 24h.
**Business Value:** Stateless, secure authentication across all platform APIs.
**Acceptance Criteria:**
- `POST /api/auth/login` returns `{accessToken, refreshToken, role, userId, username}` on valid credentials, 401 on invalid.
- `POST /api/auth/refresh` returns new token pair; old refresh invalidated.
- `POST /api/auth/logout` invalidates the refresh token.
- Expired/tampered tokens return 401 with RFC 7807 body.
**Technical Notes:** RSA key pair loaded from env (`JWT_PUBLIC_KEY`, `JWT_PRIVATE_KEY`) or generated on startup in local profile. Store refresh tokens in DB or Redis for revocation.
**Dependencies:** FRAUD-212

### FRAUD-216 — RBAC model & 5 role definitions
**Type:** Story · **Epic:** EPIC-32 · **Complexity:** M · **Owner:** BE
**Description:** Define the `Role` enum and map each role to a documented permission set per the role→capability matrix. Implement `UserDetailsService` loading roles from DB into Spring Security `GrantedAuthority`. Embed role in JWT claims so authorization decisions are stateless.
**Business Value:** Least-privilege access aligned to job function.
**Acceptance Criteria:**
- All 5 roles defined and assignable.
- JWT token contains `role` claim; `UserDetailsService` populates authorities from DB.
- Role→capability matrix documented and testable.
**Technical Notes:** Role stored as `varchar` in DB (maps to enum).
**Dependencies:** FRAUD-215

### FRAUD-217 — Endpoint authorization matrix
**Type:** Story · **Epic:** EPIC-32 · **Complexity:** M · **Owner:** BE
**Description:** Apply `@PreAuthorize` annotations across all current and planned APIs per the role→permission matrix. Secured endpoints reject unauthorized roles (403) and unauthenticated calls (401).
**Business Value:** Prevents privilege escalation and unauthorized data access.
**Acceptance Criteria:**
- Every endpoint has a `@PreAuthorize` annotation matching the role matrix.
- Integration tests verify: ADMIN accesses all, AUDITOR can only read, SYSTEM_ACCOUNT accesses transaction ingest only.
- Missing auth returns 401; wrong role returns 403.
**Dependencies:** FRAUD-216

### FRAUD-218 — System account (service-to-service) authentication
**Type:** Story · **Epic:** EPIC-32 · **Complexity:** M · **Owner:** BE
**Description:** Provide a `SYSTEM_ACCOUNT` identity for internal calls (ML service scoring callbacks). Client-credentials-style flow: service authenticates with a pre-shared key, receives a scoped JWT with `SYSTEM_ACCOUNT` role.
**Business Value:** Secures internal traffic without human credentials.
**Acceptance Criteria:**
- Service token issued with `SYSTEM_ACCOUNT` role; short-lived (5min TTL).
- Token scoped to system permissions only.
- Tests verify system token cannot access case management or admin endpoints.
**Dependencies:** FRAUD-216

### FRAUD-219 — Security context propagation (MDC + Kafka headers)
**Type:** Technical Task · **Epic:** EPIC-32 · **Complexity:** M · **Owner:** BE
**Description:** Propagate authenticated principal + correlation ID through synchronous REST calls (via servlet filter → MDC) and into Kafka message headers. Ensure correlation ID is available in consumers and audit events.
**Business Value:** End-to-end attribution and audit across async boundaries.
**Acceptance Criteria:**
- Every REST request has a correlation ID in response header `X-Correlation-ID` and in MDC.
- Every Kafka message includes `X-Correlation-ID` and `X-User-Id` headers.
- Consumers can access principal and correlation ID in their handler context.
**Dependencies:** FRAUD-215

### FRAUD-220 — User CRUD + seed data
**Type:** Story · **Epic:** EPIC-32 · **Complexity:** S · **Owner:** BE
**Description:** Implement `GET /api/admin/users` (paginated, admin-only) and `POST /api/admin/users` with BCrypt password hashing. Seed data Flyway migration creates demo users: 1 admin, 2 analysts, 1 investigator, 1 auditor, 1 system account.
**Business Value:** Enables immediate testing of role-based access and demo scenarios.
**Acceptance Criteria:**
- `POST /api/admin/users` creates a user with hashed password; returns 201 (no password hash in response).
- `GET /api/admin/users` returns paginated user list; accessible only by ADMIN role.
- Seed migration inserts 6 users with known credentials.
**Technical Notes:** BCrypt with strength 10. Never return password hash in DTOs.
**Dependencies:** FRAUD-217

---

## EPIC-33 — Transaction & Event Service
*Phase 10 · Lead: BE · Depends on: EPIC-31, EPIC-32*

Receive, validate, persist, and emit transaction events.

### FRAUD-221 — POST /transactions (ingest, validate, persist, idempotency)
**Type:** Story · **Epic:** EPIC-33 · **Complexity:** L · **Owner:** BE
**Description:** Implement `POST /api/transactions` to accept a transaction, validate it (amount > 0, ISO 4217 currency, required refs), persist with idempotency key (duplicate returns original 201), and trigger async event publication via transactional outbox.
**Business Value:** The front door for all transaction data; idempotency prevents double-processing.
**Acceptance Criteria:**
- Valid request returns 201 with created transaction DTO.
- Invalid request returns 400 with field-level RFC 7807 errors.
- Duplicate `idempotencyKey` returns original response.
- Idempotency key stored with TTL in Redis for in-flight dedup.
- Transaction persisted and outbox row written in the same DB transaction.
**Technical Notes:** Service layer handles idempotency check. Outbox row written in the same JPA transaction.
**Dependencies:** FRAUD-213, FRAUD-217

### FRAUD-222 — GET /transactions (paginated list + single by ID)
**Type:** Story · **Epic:** EPIC-33 · **Complexity:** M · **Owner:** BE
**Description:** Implement `GET /api/transactions/{id}` (returns transaction + latest risk score) and `GET /api/transactions` (paginated with filters: status, userId, merchantId, date range).
**Business Value:** Analysts and services inspect transaction state.
**Acceptance Criteria:**
- `GET /{id}` returns 200 with transaction + latest risk score, 404 if not found.
- `GET /?status=IN_REVIEW` returns paginated results matching filters.
- RBAC enforced: all authenticated roles can read.
- Response uses `PageResponse<TransactionDto>` envelope.
**Technical Notes:** Left-join latest `RiskScore` on single lookup. Use Spring Data `Specification` for dynamic filtering.
**Dependencies:** FRAUD-221

### FRAUD-223 — Transactional outbox pattern
**Type:** Story · **Epic:** EPIC-33 · **Complexity:** L · **Owner:** BE
**Description:** Implement the outbox pattern: domain writes and outbox rows committed atomically. A polling relay publishes unsent rows to Kafka and marks them published. Idempotent re-publish on relay restart.
**Business Value:** Eliminates lost or phantom events from dual writes.
**Acceptance Criteria:**
- Domain write + outbox row committed atomically.
- Relay publishes unsent rows to Kafka; marks `published = true`.
- Relay restart does not cause duplicate publication.
- Outbox table: `id, aggregate_type, aggregate_id, event_type, payload, published, created_at`.
**Technical Notes:** Polling relay with `@Scheduled(fixedDelay)` in local profile. Include correlation ID in Kafka headers from MDC.
**Dependencies:** FRAUD-221

### FRAUD-224 — Kafka consumer framework + DLQ
**Type:** Story · **Epic:** EPIC-33 · **Complexity:** M · **Owner:** BE
**Description:** Standardize Kafka consumers with error handling, exponential backoff retry, and dead-letter topics. Provision DLQ per consumer group. Capture original headers + exception in DLQ.
**Business Value:** Resilient async processing; failures contained and recoverable.
**Acceptance Criteria:**
- Transient failures retried with backoff (3 attempts).
- Permanent failures routed to DLQ with original payload + exception.
- DLQ topic naming: `<original-topic>.DLQ`.
- Consumer concurrency configurable per listener.
**Technical Notes:** Spring Kafka `DefaultErrorHandler` + `ExponentialBackOff`.
**Dependencies:** FRAUD-223

### FRAUD-225 — Transaction event schema & `transactions.created`
**Type:** Technical Task · **Epic:** EPIC-33 · **Complexity:** S · **Owner:** BE
**Description:** Define the versioned event schema for `transactions.created` with event envelope (id, type, version, correlationId, occurredAt) + payload (full Transaction DTO).
**Business Value:** Stable, evolvable contracts between producers and consumers.
**Acceptance Criteria:**
- Schema defined as a Java record/DTO with version.
- Events published with envelope + payload + headers.
**Dependencies:** FRAUD-223

---

## EPIC-34 — Rules & Fraud Engine
*Phase 10 · Lead: BE · Depends on: EPIC-33*

Deterministic rules, feature extraction, risk aggregation, and the decision engine.

### FRAUD-226 — Rules engine SPI + core rules
**Type:** Story · **Epic:** EPIC-34 · **Complexity:** L · **Owner:** BE
**Description:** Build the rule SPI (`Rule` interface with `evaluate(context) → RuleOutcome`), execution pipeline, and core rules: velocity (5m/24h from Redis), device (new/untrusted), geo (impossible travel, foreign country), merchant (risk tier/MCC), amount (threshold vs baseline).
**Business Value:** Extensible, deterministic fraud detection.
**Acceptance Criteria:**
- `Rule` SPI interface defined; rules pluggable via Spring beans.
- Each rule returns `RuleOutcome(score, reasonCode, details)`.
- 5 core rules implemented with unit tests.
**Technical Notes:** Start with a simple typed SPI. Rule configs stored in DB, cacheable.
**Dependencies:** FRAUD-212, FRAUD-224

### FRAUD-227 — Feature extraction service
**Type:** Story · **Epic:** EPIC-34 · **Complexity:** L · **Owner:** BE
**Description:** Compute the feature vector for a transaction: velocity counters (Redis: 5m, 24h), device features, merchant features, geo features, account age, failed attempts. Output matches the ML service feature contract (34 features).
**Business Value:** Consistent features shared by rules and ML.
**Acceptance Criteria:**
- Feature vector computed within 50ms p99.
- Redis counters increment on each transaction.
- Feature map matches `ml_service/features/contract.py`.
- Unit tests verify deterministic output.
**Technical Notes:** Redis sorted sets for velocity. Feature keys MUST match the ML contract — this is the parity boundary.
**Dependencies:** FRAUD-226

### FRAUD-228 — Decision engine (combine rules + ML → decision)
**Type:** Story · **Epic:** EPIC-34 · **Complexity:** M · **Owner:** BE
**Description:** Combine rules score + (optional) ML score, apply configurable thresholds, and output a final `Decision` (APPROVE/REVIEW/DECLINE) with component scores and reason codes. Persist as `RiskScore`.
**Business Value:** Turns signals into actionable, consistent outcomes.
**Acceptance Criteria:**
- `aggregateScore = w_ml * mlScore + w_rules * rulesScore` (configurable weights).
- Thresholds configurable: APPROVE < 0.3, REVIEW 0.3–0.7, DECLINE > 0.7.
- Persists `RiskScore` with all component scores and model version.
**Technical Notes:** Strategy pattern for policy. Default `WeightedCombinationPolicy`.
**Dependencies:** FRAUD-226, FRAUD-227

### FRAUD-229 — ML client with circuit breaker (Resilience4j)
**Type:** Story · **Epic:** EPIC-34 · **Complexity:** M · **Owner:** BE
**Description:** HTTP client that calls `POST /predict` on the ML service. Wrapped with Resilience4j circuit breaker + retry + timeout. On open circuit or timeout, fallback to rules-only with `degradedMode = true`.
**Business Value:** Keeps fraud decisions flowing during ML outages.
**Acceptance Criteria:**
- Success: ML returns `fraudProbability` + `factors` + `modelVersion`.
- Failure: 5 consecutive failures open circuit; fallback returns `degradedMode = true`.
- Timeout: 2s default.
- Circuit state visible via Actuator health.
**Technical Notes:** `@CircuitBreaker` + `@Fallback` annotations. ML base URL configurable.
**Dependencies:** FRAUD-228

---

## EPIC-35 — Case Management
*Phase 10 · Lead: BE · Depends on: EPIC-34*

Case lifecycle, assignment, notes, labels, and real-time SSE stream. **Critical path for the frontend.**

### FRAUD-230 — Case entity + state machine (lifecycle)
**Type:** Story · **Epic:** EPIC-35 · **Complexity:** L · **Owner:** BE
**Description:** Implement `FraudCase` entity services with full state machine enforcing legal transitions. Every transition validates the current state, applies the change, and writes an `AuditEvent`. Auto-create a case when `fraud.review.required` Kafka event is consumed (idempotent on `transactionId`).
**Business Value:** Legally defensible case lifecycle.
**Acceptance Criteria:**
- Only legal transitions succeed; illegal ones return 409.
- Every transition writes an `AuditEvent`.
- Kafka consumer `fraud.review.required` → auto-create `FraudCase(status=OPEN)`.
- Idempotent: duplicate `transactionId` returns existing case.
- SLA timer: `slaDueAt = openedAt + 24h`.
**Technical Notes:** `CaseStatus.transitionFrom(target)` guard. `CaseService` encapsulates all transitions.
**Dependencies:** FRAUD-212, FRAUD-224

### FRAUD-231 — GET /cases (paginated queue) + GET /cases/{id}
**Type:** Story · **Epic:** EPIC-35 · **Complexity:** M · **Owner:** BE
**Description:** Implement `GET /api/cases` (paginated, filterable, sorted by severity DESC then openedAt ASC) and `GET /api/cases/{id}` (full detail: case + transaction + risk score + notes + labels).
**Business Value:** The primary view for analysts.
**Acceptance Criteria:**
- Queue endpoint returns cases sorted by severity DESC, openedAt ASC.
- Filters: status, severity, assignee.
- Detail endpoint returns embedded transaction, risk score, notes, labels.
- Response: `PageResponse<CaseSummaryDto>` for queue; `CaseDetailDto` for single.
**Technical Notes:** Spring Data `Specification` for dynamic filters.
**Dependencies:** FRAUD-230

### FRAUD-232 — PUT /cases/{id}/assign + PUT /cases/{id}/status
**Type:** Story · **Epic:** EPIC-35 · **Complexity:** M · **Owner:** BE
**Description:** Implement `PUT /api/cases/{id}/assign` (set/clear assignee, OPEN → ASSIGNED) and `PUT /api/cases/{id}/status` (legal transitions with audit). Both write `AuditEvent`.
**Business Value:** Analysts claim and progress cases; every action is traceable.
**Acceptance Criteria:**
- `PUT /assign` sets `assigneeId` and transitions OPEN → ASSIGNED.
- `PUT /status` with valid transition updates status and writes audit.
- Invalid transitions return 409.
- SSE event emitted after each change.
**Dependencies:** FRAUD-230

### FRAUD-233 — Case notes + FraudLabel endpoints
**Type:** Story · **Epic:** EPIC-35 · **Complexity:** M · **Owner:** BE
**Description:** Implement `GET/POST /api/cases/{id}/notes` and `POST /api/cases/{id}/labels`. Labels are append-only and trigger case resolution + Kafka events (`fraud.confirmed` or `fraud.falsepositive`).
**Business Value:** Investigation notes and labels close the feedback loop.
**Acceptance Criteria:**
- Notes listed chronologically; new note returns 201.
- Label submission validates enum, sets confidence and reason.
- Label triggers case status transition to RESOLVED.
- Label triggers Kafka event via outbox.
**Dependencies:** FRAUD-230, FRAUD-223

### FRAUD-234 — SSE /cases/stream (real-time case updates)
**Type:** Story · **Epic:** EPIC-35 · **Complexity:** M · **Owner:** BE
**Description:** Implement `GET /api/cases/stream` as SSE endpoint. Event types: `case_created`, `case_assigned`, `case_updated`, `case_resolved`. Heartbeat every 30s. JWT-authenticated.
**Business Value:** Real-time case queue for analysts.
**Acceptance Criteria:**
- SSE stream sends events on case creation, assignment, status change, resolution.
- Event payload matches contract.
- Heartbeat every 30s.
- JWT validated on connect.
- Multiple concurrent clients supported.
**Technical Notes:** `SseEmitter` with `@GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)`. Application event publisher broadcasts case changes.
**Dependencies:** FRAUD-230

---

## EPIC-36 — Observability, Admin & CI
*Phase 10 · Lead: BE/DO · Depends on: EPIC-31–35*

### FRAUD-235 — Actuator + Prometheus + structured logging
**Type:** Story · **Epic:** EPIC-36 · **Complexity:** M · **Owner:** BE
**Description:** Expose `/actuator/health` (DB, Redis, Kafka), `/actuator/prometheus` (custom metrics), and structured JSON logging. Custom metrics: `http_server_requests_seconds`, `fraud_scoring_duration_seconds`, `fraud_decisions_total{decision}`, `cases_open_count`.
**Business Value:** Production-grade observability and alerting.
**Acceptance Criteria:**
- `/actuator/health` shows DB, Redis, Kafka status.
- `/actuator/prometheus` exposes custom metrics.
- Structured JSON logs with correlationId, userId, request path, status code.
**Technical Notes:** Micrometer for Prometheus. Logback with `LogstashEncoder`.
**Dependencies:** FRAUD-221, FRAUD-230

### FRAUD-236 — CI pipeline (Gradle build, test, lint, Flyway validate)
**Type:** Infrastructure · **Epic:** EPIC-36 · **Complexity:** M · **Owner:** DO
**Description:** GitHub Actions workflow on push/PR: `./gradlew build`, unit + integration tests (Testcontainers), Flyway validate against ephemeral Postgres, CodeQL analysis. Fail on violations.
**Business Value:** Visible green CI from commit 1.
**Acceptance Criteria:**
- Workflow runs on PR; failing build/test/lint blocks merge.
- Integration tests use Testcontainers Postgres + Redis.
- Flyway validate step catches migration issues.
- Status badges in README.
**Technical Notes:** `ubuntu-latest`, Java 21, Gradle cache. Separate job for Flyway validate.
**Dependencies:** FRAUD-210, FRAUD-211

---

# Importing into Linear

Same mapping as `../../docs/TICKETS.md` and `../../ml-service/docs/TICKETS.md`:

- **Phase 10** → `phase:10` label.
- **Epic (EPIC-31…36)** → project **milestone** and `Epic`-labelled **parent issue** inside the "Fraud Detection" project.
- **Ticket (FRAUD-208…236)** → **sub-issue** of its epic's parent issue, assigned to that epic's milestone.

**Field mapping** (matches the live BragDev workspace taxonomy):
- Title → `FRAUD-### — Title` (keep the id).
- Description / Business Value / Acceptance Criteria / Technical Notes → issue description (paste as-is; already Markdown).
- Complexity → **Estimate** points: `XS=1, S=2, M=3, L=5, XL=8`.
- Type → Story→`type:story`, Technical Task→`type:tech-task`, Infrastructure→`type:infra`.
- Domain → `domain:backend` on every ticket; plus area: `area:foundation` (E31), `area:security` (E32), `area:transactions` (E33), `area:rules` (E34), `area:case-mgmt` (E35), `area:observability` (E36).
- Team → `team:backend` (BE).
- `Dependencies` → add as **"blocked by"** relations.

> **CSV note:** import issues first (with labels + estimates + project), then add the "blocked by" relations from each ticket's `Dependencies` via the Linear UI or API.

---

*Backend backlog — 6 epics (EPIC-31 → EPIC-36), 29 tickets (FRAUD-208 → FRAUD-236), Phase 10. Extends the platform backlog (FRAUD-001 → 161) and the ML backlog (FRAUD-162 → 207).*