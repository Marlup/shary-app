# AGENT.md

## Purpose
Operational guide for agent work in this repository.  
Default mode: implement pragmatically, validate changes, and keep durable docs aligned with code.

## Session-Init Checklist (Required)

Load and enforce policy files in this exact order at every session start:

1. `AGENT.md` (canonical source of truth)
2. `CODEX.md` (Codex adapter; references `AGENT.md`)
3. `CLAUDE.md` (Claude adapter; references `AGENT.md`)
4. `.claude/settings.local.json` (tool-local settings)

Enforcement gate:

1. Run `tools/codex/session-init.ps1` before starting the agent session.
2. Fail startup if any required file is missing.
3. Fail startup if any required file checksum does not match `tools/codex/session-init.manifest.json`.
4. After intentional policy edits, run `tools/codex/update-manifest.ps1` and commit the manifest change together with the edited policy files.

## Backlog Recording (Required)

For each non-trivial task:

1. Keep all durable task recordings under `docs/backlog/`; do not create parallel backlog procedures elsewhere.
2. Create the polished dated work log under:
   - `docs/backlog/works/<YYYY-MM-DD>/`
3. Store the raw user order backup under:
   - `docs/backlog/orders_draft/<YYYY-MM-DD>/`
4. Add/update both indexes in the same pass:
   - `docs/backlog/works/works-index.md`
   - `docs/backlog/orders_draft/works-index.md`
5. Add one short index entry per change:
   - brief title
   - 1-2 sentence description
6. Persist planning artifacts in `docs/backlog/works`:
   - analysis
   - design options/drafts
   - evaluation and decision notes

## Delivery Procedure

1. Assess viability and complexity before implementation.
2. Propose implementation order with low-regression steps first.
3. Implement in small scoped patches.
4. Validate with targeted tests and manual flow checks.
5. Report:
   - what changed
   - what was verified
   - residual risks / pending items

## Unit-test to ensure functionality (Required)

For behavior-changing updates (especially callbacks, state transitions, selection logic, copy/edit/delete flows):

1. Add or update unit tests that exercise the changed logic directly when possible.
2. If UI-layer callbacks cannot be meaningfully unit-tested in isolation, add nearest-logic unit tests and document the UI manual verification steps explicitly.
3. Do not close implementation without either:
   - passing updated unit tests, or
   - a clear note explaining why a specific path is not unit-testable and how it was verified manually.

## UX/Product Preferences to Preserve

- Keep offline-first behavior and avoid hard-gating local flows on cloud availability.
- Keep disabled cloud actions visible with specific reason messaging.
- Prefer progressive rendering and low-latency first paint.
- Preserve established single-selection top actions when exactly one item is selected.
- Favor reversible destructive actions (undo where practical).
- Keep list/toolbars concise; remove redundant labels and dead controls.

## Engineering Standards

- Match existing architecture and patterns in the repo.
- Avoid broad refactors during focused feature requests.
- Add tests proportional to risk.
- Keep docs in sync when behavior/contracts change.
- Do not revert unrelated local changes.

## Suggested Work Note Template

Use this structure in each `docs/backlog/works/<date>/...md`:

1. Scope
2. Change list
3. Viability/complexity per change
4. Design decisions
5. Implementation plan
6. Validation plan
7. Result summary
