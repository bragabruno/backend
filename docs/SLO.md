# Service Level Objectives & Error Budgets — Fraud Detection Backend

**Ticket:** FRAUD-125 · **Status:** definitions ready; dashboard wiring is FRAUD-122, burn-rate alerts are FRAUD-123.

> Targets below are **proposed defaults** to be ratified with product/risk owners. Each SLI is
> defined over metrics the backend already emits via `/actuator/prometheus`, so they are
> implementable today.

## Service Level Indicators (SLIs)

| # | SLI | Definition | Source metric |
|---|-----|------------|---------------|
| 1 | **Availability** | Fraction of API requests that do not error (non-`5xx`) | `http_server_requests_seconds_count` (Micrometer Spring MVC) |
| 2 | **Scoring latency** | Fraction of scoring evaluations faster than the latency budget | `fraud_scoring_duration_seconds` (Timer `fraud.scoring.duration`) |
| 3 | **Freshness** | A model is deployed and scoring is not stuck in degraded mode | `fraud_decisions_total` + the `modelRegistry` readiness indicator (FRAUD-124) |

### SLI PromQL

```promql
# 1. Availability (request success rate), 30d rolling
sum(rate(http_server_requests_seconds_count{status!~"5..", uri!~"/actuator.*"}[30d]))
  /
sum(rate(http_server_requests_seconds_count{uri!~"/actuator.*"}[30d]))

# 2. Scoring latency — fraction under 200ms (uses the Timer's histogram buckets)
sum(rate(fraud_scoring_duration_seconds_bucket{le="0.2"}[30d]))
  /
sum(rate(fraud_scoring_duration_seconds_count[30d]))

# 3. Freshness — fraction of decisions made with a model in the loop (not degraded)
#    (emit fraud_decisions_total with a `degraded` tag, or derive from the degraded-mode counter)
1 - (
  sum(rate(fraud_decisions_total{degraded="true"}[1h]))
  /
  sum(rate(fraud_decisions_total[1h]))
)
```

> Note: SLI #2 requires the scoring Timer to publish histogram buckets — enable
> `management.metrics.distribution.percentiles-histogram.fraud.scoring.duration=true` (and an SLO
> boundary at 200ms) so `..._bucket{le="0.2"}` exists. SLI #3 assumes a `degraded` tag on the
> decisions counter; if absent, add it where the counter is incremented.

## Service Level Objectives (SLOs)

| SLO | Target | Window | Error budget |
|-----|--------|--------|--------------|
| Availability | **99.9%** of requests succeed | 30 days | 0.1% ≈ **43m 12s**/30d |
| Scoring latency | **99%** of evaluations < **200 ms** | 30 days | 1% of evaluations |
| Freshness | **99.5%** of decisions use a live model (≤ 0.5% degraded) | 30 days | 0.5% of decisions |

## Error-budget policy

- **Budget remaining > 0** — ship freely; normal change velocity.
- **Budget < 25% remaining** — freeze risky changes to the scoring path; prioritize reliability work.
- **Budget exhausted** — change freeze on the affected service except reliability fixes and security patches, until the budget recovers over the trailing window.
- Budget burn is reviewed weekly; breaches trigger a blameless postmortem.

## Burn-rate alerts (FRAUD-123)

Multi-window, multi-burn-rate alerting (Google SRE) for the 99.9% availability SLO. A burn rate of
`1` consumes the entire 30-day budget exactly over 30 days; higher rates exhaust it faster.

```promql
# Error ratio over a window
# err(w) = sum(rate(http_server_requests_seconds_count{status=~"5..", uri!~"/actuator.*"}[w]))
#          / sum(rate(http_server_requests_seconds_count{uri!~"/actuator.*"}[w]))

# Fast burn (page): 14.4x over 1h AND 5m  -> ~2% of budget in 1h
- alert: AvailabilityFastBurn
  expr: err_1h > 14.4 * 0.001 and err_5m > 14.4 * 0.001
  for: 2m
  labels: { severity: page }

# Slow burn (ticket): 6x over 6h AND 30m  -> ~10% of budget in 6h
- alert: AvailabilitySlowBurn
  expr: err_6h > 6 * 0.001 and err_30m > 6 * 0.001
  for: 15m
  labels: { severity: ticket }
```

## Out of scope here (other tickets)

- Grafana SLO dashboard wiring — **FRAUD-122**.
- Alertmanager routing / on-call paging for the rules above — **FRAUD-123**.
