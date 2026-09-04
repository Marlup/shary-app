# P0 Option A Implementation - Phase 1 and Phase 2 (2026-05-17)

## Scope
Execute requested sequence:
1. Phase 1: pair/refresh use-cases and policy evaluator skeleton.
2. Phase 2: loopback transport read-only endpoints for fields/users.

## Changes

### Phase 1
- Added pair connector use-case:
  - `PairConnectorUseCase`
  - creates connector client + strict default policy + short-lived session
  - returns plain access token and refresh secret once
- Added session refresh use-case:
  - `RefreshConnectorSessionUseCase`
  - rotates access token on valid refresh secret
- Added policy evaluator skeleton:
  - `ConnectorPolicyEvaluator`
  - denies missing scope
  - enforces confirmation based on policy mode and capability
- Added shared SHA-256 helper for token hashing.
- Extended connector repository contract with:
  - `getLatestSessionForClient`
  - `getSessionByAccessTokenHash`

### Phase 2
- Added transport-agnostic loopback request/response models.
- Added auth service for bearer validation:
  - access token hash lookup
  - expiry/revocation checks
  - locked/revoked client checks
- Added read-only loopback router endpoints:
  - `GET /connector/v1/fields`
  - `GET /connector/v1/users`
- Added loopback server facade delegating to router.

## Notes
- Current loopback transport is implemented as in-process routing (`handle(request)`), not yet bound to a socket listener. This keeps app-layer contracts stable while allowing a later binding to real localhost transport.
- Policy checks for read endpoints are enforced through the same policy evaluator skeleton.

## Validation
- Command: `./gradlew :app:compileDebugKotlin`
- Result: BUILD SUCCESSFUL.

## Pending
- Implement durable connector storage (replace in-memory scaffold).
- Add write endpoints and preview/commit orchestration.
- Add tests for pair/refresh/policy and read endpoint authorization matrix.
