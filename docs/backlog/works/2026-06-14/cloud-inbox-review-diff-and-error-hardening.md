# Cloud inbox review diff and error hardening

## Scope

Retest the previous cloud-readiness batch, add focused regression coverage, and apply the next batch from the 2026-06-13 improvement plan:

- structured backend/cloud error mapping,
- request/field inbox review dashboard hardening,
- visual diff before field import.

## Change list

- Retested the existing cloud status and cloud error unit tests.
- Added stronger assertions for `CloudUiStatus` label/detail/safety text.
- Added `CloudFieldImportPlanner` for local-only decrypted payload import planning.
- Added `CloudImportDiff` to `CloudInboxItem` so the field inbox dialog can show clean imports versus detailed-review imports.
- Enabled local inbox preview for explicit review screens.
- Added schema display to field and request inbox review dialogs.
- Added field inbox diff text for added, changed, unchanged, duplicate/already-local, and ignored keys.
- Expanded `cloudErrorMessage` for backend-coded `rate_limited`, `unsupported_schema_version`, `decision_conflict`, stale token, and auth rejection states.
- Added unit tests for the import planner and cloud error mapping.
- Added Compose source tests for field/request inbox review dialogs and their accept/reject callbacks.

## Viability and complexity

- Previous batch retest: high feasibility, low complexity. Existing tests passed before new changes.
- Structured error mapping: high feasibility, low complexity. The app already had structured cloud exception types and a shared UI mapper.
- Review dashboard hardening: high feasibility, medium complexity. Existing dialogs already had sender, metadata, and accept/reject/cancel flow; this pass adds schema and preview verification.
- Visual diff before import: medium feasibility, medium complexity. It needs decrypted local payload planning but does not change the backend payload contract.

## Design decisions

- Keep diff planning local-only and deterministic. The app decrypts inbox payloads on device during explicit review and compares them with local fields.
- Do not overwrite local fields during import. Existing local keys are classified as already-local, with changed keys called out for review.
- Keep clean imports concise. Detailed review is required only when duplicates, changed existing fields, or ignored keys are detected.
- Keep backend error mapping in the existing shared `cloudErrorMessage` function so Fields, Requests, and Summary screens benefit from the same language.

## Validation plan

- `tools/codex/session-init.ps1`
- `./gradlew :app:testDebugUnitTest --tests "com.shary.app.ui.utils.CloudUiStateTest" --tests "com.shary.app.ui.screens.utils.CloudErrorMessageTest"`
- `./gradlew :app:testDebugUnitTest --tests "com.shary.app.ui.utils.CloudUiStateTest" --tests "com.shary.app.ui.screens.utils.CloudErrorMessageTest" --tests "com.shary.app.application.cloud.CloudFieldImportPlannerTest"`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:compileDebugAndroidTestKotlin`

## Result summary

- Session init passed.
- Previous-batch focused tests passed before the next-batch implementation.
- Targeted unit tests passed after the planner/parser fix.
- App Kotlin compile passed.
- Android test Kotlin compile passed.

## Residual risks

- Compose dialog tests were source-compiled but not executed on an emulator/device in this pass.
- Import diff currently covers field payload imports. Request inbox preview shows requested keys and schema, but request lifecycle/audit persistence remains a later backend/cloud state-model task.
