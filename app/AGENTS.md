# Android app module guidance

## Scope and authority

This file applies to the complete `app/` Android module. Root `AGENTS.md`
remains authoritative; this file adds module-specific retrieval, architecture,
and validation guidance.

## Read before broad source scans

1. `docs/ai/CODEBASE_NAVIGATION.md`
2. the nearest more-specific `AGENTS.md`, when present
3. the nearest module `README.md`, especially under `application/`
4. the relevant active document under `docs/design/`, `docs/architecture/`, or
   `docs/testing/`

Use `docs/backlog/works/works-index.md` only when implementation history or an
active plan is relevant. Source `TODO` searches do not replace that index.

## Source-set map

| Path | Responsibility | Default validation |
|---|---|---|
| `src/main/java/com/shary/app/ui/` | Compose screens, shared UI, navigation, theme, previews | JVM policy tests plus Compose device tests when behavior is visual or semantic |
| `src/main/java/com/shary/app/viewmodels/` | Screen state, events, and repository/application coordination | JVM unit tests around reducers and state transitions |
| `src/main/java/com/shary/app/application/` | Cross-domain use cases and session/connector orchestration | JVM unit tests; update the nearest module README when responsibilities move |
| `src/main/java/com/shary/app/core/` | Domain models, value contracts, repository interfaces | Pure JVM tests; keep Android and infrastructure dependencies out |
| `src/main/java/com/shary/app/infrastructure/` | Persistence, cloud, connector transport, MCP, Android services | Unit/integration tests and contract docs; inspect the backend repo for shared contracts |
| `src/main/java/com/shary/app/utils/` | Small reusable controllers and helpers | Focused JVM tests |
| `src/test/java/com/shary/app/` | Host-side unit tests | `:app:testDebugUnitTest` |
| `src/androidTest/java/com/shary/app/` | Compose, device, storage, and UI Automator tests | Android-test compile, then targeted instrumentation |

## Change-routing rules

- Start at the user-visible screen or failing test, then trace state ownership
  through ViewModel, application/domain contract, and infrastructure. Do not
  open every layer by default.
- Keep Compose and Android UI types out of `core/` and `application/`.
- Keep repository interfaces in `core/`; implementations belong in
  `infrastructure/`.
- Keep Hilt/ViewModel resolution at route or container boundaries. Reusable UI
  renderers receive explicit state and callbacks so they remain previewable and
  testable in a plain activity.
- Preserve offline-first local flows. Cloud readiness can gate cloud actions,
  but it must not gate local Fields or Contacts work.
- If an Android/backend contract changes, inspect
  `C:\main_work\shary-project\shary-backend` and update the owning repository.
- Do not put secrets, tokens, private endpoints, or credentials in source,
  resources, tests, or documentation.

## Fast validation

Use the narrowest meaningful command first:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "fully.qualified.TestClass"
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:lintDebug
```

Use the full device procedure from `docs/testing/android-native-ui-testing.md`
when behavior depends on Compose semantics, Android APIs, lifecycle, storage,
IME, system UI, or accessibility.

## Documentation sync

- Application responsibility changes update the nearest
  `application/**/README.md`.
- Navigation, primary action, or security-guard changes update
  `docs/design/SHARY_ELEMENT_ACTION_MAP.md` when its relationships change.
- Architecture or product-boundary changes update the relevant active design or
  architecture document.
- Task-to-code ownership changes update
  `docs/ai/CODEBASE_NAVIGATION.md` and this file when applicable.
