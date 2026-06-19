# P0 Option A Implementation - Phase 0 Skeleton (2026-05-17)

## Scope
Implement the first technical slice of P0 Option A before transport/use-case logic:
- connector domain contracts
- capability/policy/audit enums
- response envelope/error catalog
- repository abstraction + DI wiring

## Change list
1. Added connector capability/status/policy/audit enums.
2. Added connector core value objects for clients/sessions/policies/audit/pending-actions.
3. Added connector success/error envelope contracts and initial error code catalog.
4. Added `ConnectorRepository` interface for P0 persistence contract.
5. Added `ConnectorRepositoryImpl` as a thread-safe in-memory Phase 0 scaffold.
6. Wired repository provider in `DependenciesContainer` for DI access.

## Viability/complexity per change
- Domain contracts/enums: low risk, high leverage.
- Envelope/error catalog: low risk, medium leverage (stabilizes API/use-case boundaries).
- Repository + DI: medium risk, high leverage.

## Design decisions
- Kept persistence as in-memory scaffold in Phase 0 to unblock application-layer implementation without prematurely freezing on-disk schema details.
- Preserved transport-agnostic core so loopback API and future MCP adapter can reuse identical policy/audit flows.

## Validation
- Ran compile check: `./gradlew :app:compileDebugKotlin`.
- Result: BUILD SUCCESSFUL.

## Residual risks / pending
- Repository is non-persistent for now; app restarts lose connector state.
- Next phase must add durable storage/migrations and application use-cases (pair/refresh/preview/commit).
