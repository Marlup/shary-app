# FRONTEND_ADAPTATION_FROM_BACKEND_V1

Date: 2026-04-25  
Contract authority: Backend final freeze package `V1`  
Audience: Frontend Android implementation

## Purpose

Define the mandatory frontend adaptation baseline after backend freezes capabilities and requirements for version `V1`.

## Update Notices (Frontend Must Notice)

1. Backend is the final authority for endpoint semantics, payload schema, and error/ACK meaning.
2. `/request_decision` is canonical; `/payload_decision` is temporary compatibility only.
3. UI logic must be driven by backend structured codes/fields, not message text.
4. Correlation IDs are mandatory for traceability.
5. TTL and idempotency behavior must match backend-fixed rules exactly.

## Mandatory Frontend Adaptations

### A) Endpoint and routing adaptation
- Use `/request_decision` as canonical decision endpoint.
- Keep `/payload_decision` only as temporary compatibility route until backend deprecation ACK.
- Do not introduce endpoint alternatives outside backend contract list.

### B) ACK and error parsing adaptation
- Parse decision ACK as structured envelope:
  - `status`
  - `reason_code`
  - `decision_id`
  - `request_id`
  - `schema_version`
- Parse errors as structured envelope:
  - `error_code`
  - `message`
  - `request_id`
- Do not branch core UI behavior from free-form message text.

### C) Correlation and observability adaptation
- Send `X-Request-Id` for cloud requests.
- Consume correlation values from headers/body (`X-Request-Id`, `request_id`) and keep them in diagnostics.

### D) Schema/version adaptation
- Send `schema_version=1` for POST endpoints that backend marks versioned.
- Treat `unsupported_schema_version` as contract mismatch (non-retryable until client update).

### E) Reliability and policy adaptation
- Treat `429 rate_limited` as retryable using backend-provided `retry_after_seconds`.
- Enforce TTL bounds shown to users and sent to backend:
  - default `7d`
  - max `14d`

## Fixed Behavioral Assumptions Frontend Must Respect

- Decision is recipient-scoped and idempotent.
- `decision_conflict` is final for that payload identity and must not trigger automatic opposite-decision retries.
- Payload writes can fail on digest/signature verification; frontend must surface explicit feedback.

## Implementation Checklist

1. Update network adapter and parsing models for ACK/error envelopes.
2. Ensure request-id propagation in cloud request builder.
3. Ensure schema/version fields are attached where required.
4. Ensure rate-limit/cooldown handling is present in viewmodels/UX.
5. Add/refresh tests for:
   - ACK parsing
   - error-code mapping
   - request-id propagation
   - fallback path behavior
6. Validate TTL and decision semantics in summary and inbox flows.

## Compatibility Removal Criteria

- Backend confirms deprecation criteria are met.
- Frontend telemetry confirms fallback usage is zero in defined window.
- Backend publishes final removal window (target currently: after `2026-07-31` unless backend revises).
