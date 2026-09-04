# FRONTEND_ADAPTATION_FROM_BACKEND_V1_1_DELTA

Date: 2026-04-25  
Parent baseline: `FRONTEND_ADAPTATION_FROM_BACKEND_V1`  
Delta scope: Incremental frontend updates to align with backend `V1.1` freeze decisions

## Delta Intent

Capture only what changes from `V1` to `V1.1`, so frontend can patch without redoing the full adaptation baseline.

## Update Notices for Frontend Team

1. Treat this document as additive to `V1`, not a replacement.
2. Apply deltas in parser/network layer first, then propagate to UX and tests.
3. If backend `V1.1` release notes conflict with this file, backend release notes win.

## Required Frontend Delta (V1 -> V1.1)

### 1) Schema/version handling tightened
- Ensure POST requests to upload/decision endpoints include `schema_version=1` where backend requires it.
- Handle `unsupported_schema_version` as non-retryable contract mismatch.
- Show upgrade/actionable user feedback instead of blind retries.

### 2) Rate-limit behavior made explicit
- Handle `rate_limited` with delayed retry using `retry_after_seconds`.
- Block immediate repeated submissions while cooldown is active.

### 3) Correlation propagation remains mandatory
- Continue sending `X-Request-Id` for all supported cloud calls.
- Continue surfacing `request_id` in diagnostics/log exports.

### 4) Structured behavior mapping remains mandatory
- Continue mapping UX state from backend `error_code` and ACK fields only.
- Avoid fallback to free-text interpretation for control flow.

## No-Change Areas (Confirmed from V1)

- Decision ACK core schema remains aligned with V1:
  - `status`, `reason_code`, `decision_id`, `request_id`, `schema_version`
- Error envelope core remains aligned with V1:
  - `error_code`, `message`, `request_id`
- `/request_decision` remains canonical.
- `/payload_decision` remains temporary compatibility route until backend deprecation completion.

## Frontend Patch Checklist for V1.1

1. Update request builders for required schema/version fields.
2. Update response mappers for strict unsupported-version and rate-limit handling.
3. Update viewmodel retry/cooldown logic.
4. Add/refresh tests for:
   - unsupported schema handling
   - 429 + retry_after behavior
   - request-id propagation continuity
5. Confirm fallback telemetry remains visible until compatibility removal.
