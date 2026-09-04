# AGENTS.md

## Purpose
Operational guide for agent work in this repository.  
Default mode: implement pragmatically, validate changes, and keep durable docs aligned with code.

## Repository Map

- Shary Android application (this repository):
  `C:\Users\marsu\AndroidStudioProjects\sharydemo`
- Shary project workspace:
  `C:\main_work\shary-project`
- Shary Firebase backend:
  `C:\main_work\shary-project\shary-backend`
- When work changes Android/backend contracts, inspect and update both owning
  repositories rather than copying backend implementation into this repo.

## Communication Language (Required)

1. All agent-authored communication must be in English, regardless of the language used by the user.
2. This includes responses, progress updates, plans, explanations, repository documentation, code comments, commit messages, and generated instructions.
3. Preserve non-English text only when quoting user-provided content verbatim or storing a raw-order backup; keep all surrounding agent-authored metadata in English.

## Agent Retrieval Surfaces

After session-init passes, use these repo-local retrieval surfaces before
loading broad code or historical notes:

1. `docs/ai/README.md`
2. `docs/ai/CODEBASE_NAVIGATION.md` for task-to-code and validation shortcuts
3. the nearest scoped `AGENTS.md`, then the nearest module `README.md`
4. `docs/ai/SHARY_AGENT_REPO_SPEC.md`
5. `docs/ai/SCRIPT_CATALOG.md` when a script or harness is involved
6. the relevant active product/architecture doc under `docs/design/` or
   `docs/architecture/`

## Session-Init Checklist (Required)

Load and enforce policy files in this exact order at every session start:

1. `AGENTS.md` (canonical source of truth)
2. `CODEX.md` (Codex adapter; references `AGENTS.md`)
3. `CLAUDE.md` (Claude adapter; references `AGENTS.md`)
4. `.claude/settings.local.json` (tool-local settings)

Enforcement gate:

1. Run `tools/codex/session-init.ps1` before starting the agent session.
2. Fail startup if any required file is missing.
3. Fail startup if any required file checksum does not match `tools/codex/session-init.manifest.json`.
4. After intentional policy edits, run `tools/codex/update-manifest.ps1` and commit the manifest change together with the edited policy files.

## Documentation Sync Rule (Required)

When changing an agent-facing script, module boundary, or repo-local workflow,
update the matching documentation in the same patch.

At minimum:

- script changes:
  - update the nearest script-folder `README.md`;
  - update `docs/ai/SCRIPT_CATALOG.md`.
- application-layer changes:
  - update the nearest module `README.md` under
    `app/src/main/java/com/shary/app/application/`.
- policy or retrieval-flow changes:
  - update this file and the relevant `docs/ai/*.md` file.

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
   - 2-8 sentence description
6. When a completed task implements or advances a defined plan step, add an
   explicit plan-source pointer in the polished work log and in the relevant
   index entry when concise:
   - use an Obsidian-style wikilink to the plan file:
     `[[docs/backlog/works/<YYYY-MM-DD>/<plan-file-name>.md]]`
   - if the work maps to a specific section or numbered step, append the exact
     path in plain text, for example:
     `Plan source: [[docs/backlog/works/2026-06-20/production-desktop-bridge-design.md#Implementation plan]] -> step 3`
   - prefer the most specific stable pointer available: heading first, then
     step number if the file uses numbered lists rather than step subheadings
7. Persist planning artifacts in `docs/backlog/works`:
   - analysis
   - design options/drafts
   - evaluation and decision notes

## Checkpoint review (Required)

To recover the latest checkpoint and review the last completed task:

1. Read the first (newest) entry in both:
   - `docs/backlog/orders_draft/works-index.md`
   - `docs/backlog/works/works-index.md`
2. Then read the related raw order and polished work note linked by those entries.

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
3. Plan source (when applicable)
4. Viability/complexity per change
5. Design decisions
6. Implementation plan
7. Validation plan
8. Result summary
