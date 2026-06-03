-- Baseline DEPLOYED model so the model-registry readiness probe (FRAUD-124) reports UP on a fresh
-- install and the root /actuator/health (docker healthcheck) stays green. The ml-service supersedes
-- this once it registers and deploys a real model (which archives this baseline). Idempotent.
INSERT INTO model_versions (id, version, mlflow_run_id, status, deployed_at, created_at)
VALUES ('b0000000-0000-0000-0000-000000000001', 'baseline-rules-v1', NULL, 'DEPLOYED', NOW(), NOW())
ON CONFLICT (version) DO NOTHING;
