# Agent connection checkpoint consolidation

## Scope

Consolidate the uncommitted Shary agent-connection implementation,
documentation, and executable test assets into a validated Git checkpoint.

## Change list

1. Reviewed the committed MCP adapter and the later transport-hardening work.
2. Kept unrelated root artifacts outside checkpoint scope.
3. Ran the mandatory repository policy integrity gate.
4. Revalidated debug and Android-test Kotlin compilation.
5. Reran focused MCP, connector-management, preview/commit, refresh-session,
   and policy tests.
6. Reconciled the transport-hardening note with current validation evidence.
7. Preserved the live Android protocol and real-agent run as an explicit
   operator acceptance step.

## Viability and complexity

- Checkpoint consolidation: low implementation risk because the code and docs
  already formed a coherent feature slice.
- Regression validation: moderate execution cost due to Android compilation,
  but covered by focused deterministic tests.
- Live E2E: currently blocked by environment availability, not by code; `adb
  devices` reported no connected target.

## Design decisions

1. Commit the MCP implementation, lifecycle UI, transport hardening, tests,
   operator guides, backlog records, and policy-aligned cleanup together.
2. Exclude unrelated `TODO`, `main.py`, `notes.txt`, and `__pycache__`
   artifacts without modifying them.
3. Do not treat synthetic evidence as a live Codex-to-Shary E2E result.
4. Require a connected emulator/device for the remaining protocol and
   real-agent acceptance run.

## Validation plan

1. Run `tools/codex/session-init.ps1`.
2. Run `git diff --check`.
3. Compile debug and Android-test Kotlin.
4. Run focused connector and MCP unit tests with `--rerun-tasks`.
5. Parse all PowerShell test scripts and scenario JSON.
6. Validate the stored synthetic evidence manifest only as harness evidence.

## Result summary

The policy gate passed. Debug and Android-test Kotlin compilation succeeded,
and the focused regression suite completed with `BUILD SUCCESSFUL`. No Android
target was connected, so the live protocol and real-agent acceptance suites
were not rerun during checkpoint creation.

The live read-only protocol acceptance suite was subsequently rerun against
`emulator-5554` with the committed debug build. ADB forwarding and MCP
transport, initialization, ping, eight-tool discovery, origin protection,
authenticated field and user reads, and read-only mutation denial all passed.
The run observed seven fields and three users. Read-write and mutating
scenarios were not requested and remained skipped. No bearer token was stored
in repository files or test evidence.
