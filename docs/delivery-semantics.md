# Delivery Semantics — Decision Record

**Status:** Accepted  
**Date:** 2026-06-03  
**Decision:** At-least-once + idempotent consumers (NOT exactly-once)

## Context

Kafka supports three delivery guarantees:
- **At-most-once:** Fast but can lose events
- **At-least-once:** May duplicate but never loses events
- **Exactly-once (EOS):** Perfect but adds latency and complexity

## Decision

**At-least-once delivery with idempotent consumers** for all topics.

### Rationale

| Factor | At-least-once + Idempotent | Exactly-once (EOS) |
|--------|---------------------------|---------------------|
| Latency | Lower (no transaction coordinator) | Higher (2PC overhead) |
| Complexity | Simple (dedupe key in consumer) | Requires Kafka transactions + EOS producer |
| Correctness | Sufficient with idempotent processing | Perfect |
| Cost | Standard Kafka | Requires more broker resources |

### Per-topic guarantees

| Topic | Dedupe Key | Consumer Idempotency |
|-------|-----------|---------------------|
| `transactions.created` | `transactionId` | TransactionService publishes with `idempotencyKey`; FraudScoringService checks `transactionId` |
| `fraud.scored` | `transactionId` | Downstream consumers must dedupe |
| `fraud.review.required` | `transactionId` | CaseService uses `transactionId` as unique key |
| `fraud.confirmed` | `transactionId` | Label is append-only, dedupe by (transactionId, label) |
| `fraud.falsepositive` | `transactionId` | Label is append-only, dedupe by (transactionId, label) |

### Implementation

1. **Producer:** Transactional outbox pattern (FRAUD-038) ensures events are published at-least-once
2. **Consumer:** DefaultErrorHandler with FixedBackOff retries transient failures 3x
3. **DLQ:** Permanent failures (deserialization) route to `{topic}.dlq`
4. **Idempotency:** Each consumer checks for duplicate processing via unique keys in DB

## Consequences

- Consumers must be idempotent (already are via DB unique constraints)
- DLQ messages need manual inspection/replay
- No Kafka transactions or `transactional.id` required
