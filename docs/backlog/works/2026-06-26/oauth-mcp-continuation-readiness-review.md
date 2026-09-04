# OAuth MCP continuation readiness review

## Scope

Resume from the June 21 public OAuth / multi-vault MCP checkpoint, validate the
current Android and backend continuation state, and determine whether the next
step is more implementation work or deployment-readiness closure.

## Change list

1. Recovered the active continuation from the June 21 Android checkpoint and
   verified that the dirty worktrees in both repositories still align with that
   batch.
2. Ran the required Android policy gate and focused validation commands from
   `sharydemo`.
3. Ran the isolated MCP backend local validation and the read-only deployment
   preflight from `shary-backend/firebase/mcp-functions`.
4. Reduced the continuation outcome to one external production blocker instead
   of an open implementation defect.

## Plan source

Primary continuation source:
`[[docs/backlog/works/2026-06-21/public-oauth-multi-vault-mcp-implementation.md#Deployment prerequisites]]`

Related upstream design source:
`[[docs/backlog/works/2026-06-20/production-desktop-bridge-design.md#Implementation plan]] -> step 5`

## Viability/complexity per change

- Continuation recovery: high viability, low complexity. The relevant Android
  and backend checkpoints were already recorded in repo-local backlog docs.
- Android validation: high viability, low complexity. The current dirty worktree
  compiled and passed the focused checks without code changes.
- Backend readiness validation: high viability, moderate complexity because the
  deployment preflight depends on external Firebase/GCP configuration and local
  tool availability.
- Remaining blocker analysis: high viability, low complexity. The preflight
  failure narrowed to a production certificate mismatch rather than a code-path
  error.

## Design decisions

1. Treat the June 21 public OAuth / remote MCP batch as the active checkpoint,
   not the older June 20 desktop-bridge design in isolation.
2. Validate locally before proposing any new feature phase.
3. Use the read-only backend deployment preflight because the explicit product
   goal is to be deployment-ready without deploying automatically.
4. Preserve the result as a backlog checkpoint because the next action depends
   on external release operations rather than further Android coding.

## Implementation plan

1. Reconfirm the active checkpoint and plan source from backlog docs.
2. Run `tools/codex/session-init.ps1`, `./gradlew :app:compileDebugKotlin`,
   `./gradlew :app:testDebugUnitTest`, `./gradlew :app:compileDebugAndroidTestKotlin`,
   and `git diff --check` in `sharydemo`.
3. Run `venv\Scripts\python -m pytest -q` and `venv\Scripts\python -m compileall .`
   in `firebase/mcp-functions`.
4. Run the read-only `tools/deployment_preflight.ps1` in
   `firebase/mcp-functions`.
5. Convert the result into the next operational step.

## Validation plan

- Android policy gate must pass after the AGENT/session-init manifest update.
- Android Kotlin compile, unit tests, Android-test compile, and diff-format
  checks must pass.
- MCP backend local pytest suite must pass.
- Read-only deployment preflight must either pass fully or identify concrete
  external blockers without mutating cloud resources.

## Result summary

Validation passed on the current continuation state:

- `tools/codex/session-init.ps1`: passed.
- `./gradlew :app:compileDebugKotlin`: passed.
- `./gradlew :app:testDebugUnitTest`: passed.
- `./gradlew :app:compileDebugAndroidTestKotlin`: passed.
- `git diff --check`: passed.
- `venv\Scripts\python -m pytest -q` in `firebase/mcp-functions`: `23 passed`.
- `venv\Scripts\python -m compileall .` in `firebase/mcp-functions`: passed.
- `tools/deployment_preflight.ps1`: all checks passed except one.
- Frozen backend regression suite executed by the preflight: `19 passed`.

The only remaining blocker reported by the read-only deployment preflight is:

- `SHARY_ANDROID_CERT_SHA256` is still the debug certificate instead of the
  Play App Signing certificate.

No additional Android or backend code defect was identified in this pass. The
next step is release-operations work: obtain the Play App Signing SHA-256
fingerprint, update the MCP environment to use it, and then rerun the read-only
deployment preflight before any live Codex/Claude OAuth E2E or deployment.
