# Checkpoint review policy synchronization

## Scope

Review the user-added `AGENTS.md` checkpoint-recovery rule, preserve its intent,
make its retrieval order unambiguous, synchronize the agent-facing retrieval
guide, and restore the required checksum gate before continuing application
work.

## Change list

1. Clarified that the first entry means the newest entry in each backlog index.
2. Replaced unresolved bracket-style paths with explicit code-form paths.
3. Required opening both the raw order and polished work note linked by those
   entries.
4. Mirrored the rule in `docs/ai/README.md`, including the authority split:
   polished notes carry validated state; raw orders carry the exact scope.
5. Refreshed `tools/codex/session-init.manifest.json`.

## Viability and complexity per change

- Policy wording: low complexity. The user intent was already sound; only
  ambiguity and broken Markdown-like path notation needed correction.
- Documentation synchronization: low complexity and required by the repository
  policy for retrieval-flow changes.
- Manifest refresh: low complexity and mandatory after intentional policy edits.

## Design decisions

- Keep the index-first rule short enough to execute every continuation turn.
- Do not require broad historical-note loading after the linked notes resolve
  the active checkpoint.
- Treat the work note and raw order as complementary rather than interchangeable.

## Implementation plan

1. Read the changed policy in canonical load order. Completed.
2. Tighten the checkpoint wording without changing its intent. Completed.
3. Mirror it in the AI retrieval guide. Completed.
4. Refresh the manifest and rerun session-init. Completed.

## Validation plan

- Run `tools/codex/update-manifest.ps1`.
- Run `tools/codex/session-init.ps1`.
- Exercise the rule by reading both newest index entries and their linked notes
  before starting the Contact migration.

## Result summary

The new checkpoint-review policy is active and was used immediately for the
Phase 2 continuation. Manifest refresh completed and session-init passes with
policy version `2026-07-18.1`.
