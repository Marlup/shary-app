# Secure Field Types, Session, and Loading

Date: 2026-07-05

## Scope

Implement task and sensitive field types, sensitive-data authorization and MCP policy, a five-minute background session timeout, progressive post-login field/user loading, and a development-only application element/action graph.

## Plan Source

Approved conversational implementation plan from the 2026-07-05 user exchange. Enhanced-BRAIN record: `agent-workspace/vaults/agent-vault/02-agent-workflows/task-records/2026-07-05/shary-secure-field-types-session-loading/`.

## Viability and Complexity

- Typed-field and task editing: medium; extends the current value envelope and compact field editor.
- Sensitive local/MCP policy: high; must cover cards, semantics, quick actions, review/send, direct connector routes, and preview/commit.
- Session timeout: medium; centralizes currently fragmented logout navigation and authentication cleanup.
- Startup loading: high; moves progressive decryption before navigation and shares state across view models.
- Element/action map: low; canonical Markdown with stable graph relations and code references.

## Design Decisions

- Store task items as canonical JSON in the typed value envelope.
- Store `isMcpShareable` as a new optional proto boolean and apply it only to sensitive fields.
- Never render ciphertext when decryption fails.
- Clear sensitive reveal grants immediately on background; close the whole session after five minutes.
- Navigate after five successfully decrypted fields, or after all records are attempted when fewer than five can load.
- Preserve existing behavior for non-sensitive fields.

## Implementation Plan

1. Extend value contracts, field persistence, strict decoding, and rewrap preservation.
2. Add centralized sensitive authorization and masking to field/detail/review flows.
3. Enforce sensitive policy across MCP reads, writes, preview, and commit.
4. Add session timeout and shared post-login loading state.
5. Add the application element/action graph and synchronize module docs.
6. Add focused unit/UI tests and run repository validation gates.

## Validation Plan

- Run focused value-contract, persistence, connector/MCP, startup-loader, and timeout tests.
- Compile main and Android test Kotlin.
- Run focused instrumentation tests on an available emulator/device.
- Re-run `tools/codex/session-init.ps1`.

## Result Summary

Implemented:

- `tasks` values with per-item `todo`, `doing`, `done`, `review`, and `cancel`
  status editing;
- `sensitive` values with exact `*****` card/row masking, authenticated detail
  and review/send flows, and a local MCP-sharing toggle;
- append-only persistence for `isMcpShareable`, including password rewrap;
- strict field decoding that counts and omits failed records instead of
  displaying ciphertext;
- direct and preview/commit MCP policy enforcement, including denied sensitive
  creation/type transitions;
- five-minute process-background session closure and centralized manual logout;
- session-scoped field/user loading with a five-field navigation threshold and
  continued repository observation;
- `docs/design/SHARY_ELEMENT_ACTION_MAP.md` as the canonical development map.

Validation:

- `./gradlew :app:testDebugUnitTest :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin`
  passed.
- `tools/codex/session-init.ps1` passed with policy version `2026-07-04.1`.
- No emulator/device was attached, so instrumentation execution remains a
  manual follow-up; Android-test Kotlin compilation passed.
