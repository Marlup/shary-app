# P0 Option A Completion Pass - Tests and Durable Connector Persistence (2026-05-17)

## Scope
Execute both pending follow-ups:
1. Add focused tests for connector policy and preview/commit behavior.
2. Replace in-memory connector storage with durable local persistence.

## Changes

### Durable persistence migration
- Replaced in-memory `ConnectorRepositoryImpl` with DataStore-backed implementation (`Preferences` JSON payload storage).
- Persisted connector domains:
  - clients
  - sessions
  - policies
  - audit events
  - pending actions
- Added JSON serialization/deserialization for typed action lists and structured summaries.
- Updated DI provider to construct repository with `@ApplicationContext`.

Files:
- `app/src/main/java/com/shary/app/infrastructure/persistance/repositories/ConnectorRepositoryImpl.kt`
- `app/src/main/java/com/shary/app/infrastructure/di/DependenciesContainer.kt`

### Focused tests
- Added policy evaluator unit tests:
  - missing scope denial
  - strict write confirmation requirement
- Added preview/commit unit test with fakes:
  - preview creates pending action
  - commit applies field/user mutations
  - commit appends audit event

Files:
- `app/src/test/java/com/shary/app/application/connector/policy/ConnectorPolicyEvaluatorTest.kt`
- `app/src/test/java/com/shary/app/application/connector/PreviewCommitConnectorActionsUseCaseTest.kt`

## Validation
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.shary.app.application.connector.policy.ConnectorPolicyEvaluatorTest" --tests "com.shary.app.application.connector.PreviewCommitConnectorActionsUseCaseTest"`
- Result: BUILD SUCCESSFUL.

## Residual risks / pending
- Router/auth integration tests are still pending (current tests are use-case + policy-focused).
- Current persistence format is JSON-in-Preferences; if connector volume grows significantly, Room migration should be considered.
