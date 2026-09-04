# Shary Agent Docs

## Purpose

This folder is the repo-specific retrieval surface for AI agents working in
`sharydemo`. It complements, but does not replace, the broader governance kept
in Enhanced-BRAIN.

## Retrieval order

1. Read root `AGENTS.md` first.
2. Read `docs/README.md` for the active document map.
3. Read `docs/ai/CODEBASE_NAVIGATION.md` for task-to-code, owner, and test
   shortcuts.
4. Read the nearest scoped `AGENTS.md`, then the nearest module `README.md`.
5. Read `docs/ai/SHARY_AGENT_REPO_SPEC.md` for the repo-specific agent model.
6. Read `docs/ai/SCRIPT_CATALOG.md` when the task touches a script or test
   harness.

## Continuation checkpoint

When continuing prior work, do not reconstruct the active step from source
history alone:

1. Read the first (newest) entry in both backlog indexes.
2. Open the raw order and polished work note linked by those entries.
3. Use the polished work note for validated state and the raw order for the
   exact scope boundary.

## Files in this folder

- `SESSION_INIT.md`
  - Session-init gate, prompt emission, and manifest refresh commands.
- `SHARY_AGENT_REPO_SPEC.md`
  - Repo-specific agent contract, relationship to Enhanced-BRAIN, retrieval
    targets, and documentation-update rules.
- `CODEBASE_NAVIGATION.md`
  - Curated task-to-code map, architecture ownership, exact search shortcuts,
    test routing, and maintenance strategy.
- `SCRIPT_CATALOG.md`
  - Script-by-script purpose, inputs, outputs, side effects, and companion
    docs.

## Relationship to Enhanced-BRAIN

- Keep general agent governance, cross-repo task recording, case review,
  and reusable skills in Enhanced-BRAIN.
- Keep `sharydemo`-specific boundaries, module summaries, script contracts,
  and backlog routing in this repository.
- When work spans both repositories, maintain both durable record systems.

## Librarian targeting guidance

These docs use stable headings and compact sections so an agent can retrieve
only the exact section it needs instead of reading large mixed files.

## Update rule

When a repo-specific agent workflow, module boundary, or script contract
changes, update the relevant file in this folder in the same patch as the
code or policy change.
