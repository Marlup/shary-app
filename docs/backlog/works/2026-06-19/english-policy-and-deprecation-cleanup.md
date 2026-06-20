# English-only policy and deprecation cleanup

## Scope

Make English mandatory for all agent-authored communication and remove Gradle/Kotlin deprecations that can be addressed without a broad build-system or UI migration.

## Change list

1. Added the English-only communication rule to canonical `AGENT.md`.
2. Made Kotlin annotation targets explicit for injected constructor parameters.
3. Migrated lifecycle-owner imports to `androidx.lifecycle.compose`.
4. Migrated deprecated Material 3 dropdown-anchor and tooltip APIs.
5. Removed deprecated AGP compatibility properties where the project already has explicit equivalent configuration.
6. Added a type guard and scoped suppression for the unavoidable generic cast in `ThemeViewModelFactory`.

## Viability and complexity

- English-only policy: low complexity and low risk.
- Explicit Kotlin annotation targets: low complexity and behavior-preserving.
- Compose API replacements: low complexity and behavior-preserving.
- Removed AGP properties: low risk because `targetSdk` is explicit, no `resValue` or manifest `uses-sdk` declaration is present, and compilation validates generated-resource behavior.

## Design decisions

- Keep `android.builtInKotlin=false` and `android.newDsl=false`; enabling them is a build migration and the protobuf Gradle plugin still uses legacy variant APIs.
- Keep `android.r8.optimizedResourceShrinking=false`; changing it affects release shrinking and requires release-specific validation.
- Keep `android.dependency.useConstraints=true`; disabling dependency constraints to silence a performance recommendation can alter dependency resolution and is outside this low-risk pass.
- Keep deprecated direct status-bar color assignments for now; replacing them requires an edge-to-edge visual behavior review.
- Preserve user language verbatim only inside raw-order backups or explicit quotations.

## Implementation plan

1. Update canonical policy and its checksum manifest.
2. Apply behavior-preserving source deprecation replacements.
3. Remove only low-risk deprecated Gradle properties.
4. Compile debug Kotlin and Android tests.
5. Re-run focused MCP tests to protect the current connector checkpoint.

## Validation plan

- `powershell.exe -ExecutionPolicy Bypass -File tools/codex/update-manifest.ps1`
- `powershell.exe -ExecutionPolicy Bypass -File tools/codex/session-init.ps1`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:compileDebugAndroidTestKotlin`
- `./gradlew :app:testDebugUnitTest --tests "com.shary.app.infrastructure.connector.mcp.SharyMcpServerTest"`

## Result summary

Validation succeeded on 2026-06-19:

- Policy manifest refreshed and session-init passed with policy version `2026-06-19.1`.
- Fresh debug Kotlin compilation completed with `BUILD SUCCESSFUL`.
- Android test Kotlin compilation completed with `BUILD SUCCESSFUL`.
- Focused `SharyMcpServerTest` completed successfully.
- Cleared the low-risk AGP property, Kotlin annotation-target, lifecycle-owner, dropdown-anchor, tooltip-positioning, and ViewModel factory warnings.
- Deferred warnings are limited to AGP built-in Kotlin/new DSL migration, protobuf legacy variant APIs, release optimized resource shrinking, two status-bar color assignments requiring visual review, and Hilt/kapt processor-option noise in Android tests.
