# AI Session Init Enforcement

## Scope

Implement repository-level enforcement so each new AI session loads canonical policy files in a fixed order and fails fast when required files drift.

## Change list

1. Added explicit `Session-Init Checklist` and enforcement gate to `AGENT.md`.
2. Added `CODEX.md` and `CLAUDE.md` as thin adapters referencing canonical `AGENT.md`.
3. Added manifest-based integrity file: `tools/codex/session-init.manifest.json`.
4. Added startup gate script: `tools/codex/session-init.ps1`.
5. Added manifest refresh script: `tools/codex/update-manifest.ps1`.
6. Added short operator usage doc: `docs/ai/SESSION_INIT.md`.

## Viability/complexity per change

1. `AGENT.md` checklist: low complexity, high enforcement clarity.
2. Adapter files (`CODEX.md`, `CLAUDE.md`): low complexity, reduces duplicated policy risk.
3. Manifest + checksums: medium complexity, strong drift detection.
4. Startup gate script: medium complexity, direct operational control.
5. Manifest updater: low complexity, needed for intentional policy edits.
6. Usage doc: low complexity, improves repeatability in launcher/wrapper setups.

## Design decisions

1. Canonical policy source remains `AGENT.md`; tool-specific files only reference it.
2. Session-init load order is declared centrally in manifest and verified.
3. Startup fails on both missing files and checksum mismatch.
4. Prompt payload emission is supported in text and JSON to fit different launchers.

## Implementation plan

1. Add checklist and references.
2. Add adapters.
3. Add scripts and manifest.
4. Regenerate checksums.
5. Validate gate and prompt emission.

## Validation plan

1. Run `tools/codex/update-manifest.ps1`.
2. Run `tools/codex/session-init.ps1`.
3. Run `tools/codex/session-init.ps1 -EmitPrompt` smoke check.

## Result summary

Session-init checks pass with policy version `2026-05-31.1`, and prompt payload emission now provides deterministic first-session context loading.
