# Shary Docs Index

This folder is organized by intent so current source-of-truth docs are easy to find and historical notes do not clutter the root.

## Folder Map

- `backend-freeze/`
  - Canonical backend contract freeze, backend handovers, backend review, Firebase deploy checklist.
- `frontend-freeze/`
  - Frontend adaptation/handovers aligned to backend freeze decisions.
- `architecture/`
  - Security/auth architecture and device-storage design notes.
  - Local agent MCP design:
    `docs/architecture/SHARY_LOCAL_AGENT_MCP_DESIGN.md`.
- `design/`
  - UX/UI design briefs and proposal implementation plans.
- `implementation/`
  - Consolidated implementation summaries (canonical merged reports).
- `testing/`
  - Manual/operative test plans.
  - Connector and MCP end-to-end quick start:
    `docs/testing/connector-mcp-e2e-quickstart.md`.
  - Real AI-agent MCP end-to-end guide:
    `docs/testing/connector-mcp-real-agent-e2e.md`.
- `widget/`
  - Shary widget V1 design and implementation briefs.
- `roadmap/`
  - Deferred and next-phase ideas.
- `archive/`
  - Historical detailed reports preserved for traceability.

## Canonical Entry Points

- Backend-first contract context:
  - `docs/backend-freeze/BACKEND_FINAL_DECISION_FREEZE_BRIEF_2026-04-25.md`
  - `docs/backend-freeze/HANDOVER_BACKEND_P0_P1_IMPLEMENTATION_2026-04-25.md`
- Frontend adaptation baseline:
  - `docs/frontend-freeze/FRONTEND_ADAPTATION_FROM_BACKEND_V1.md`
- Unified implementation summary:
  - `docs/implementation/IMPLEMENTATION_CONSOLIDATED_2026-04.md`

## What Was Tidied

- Root-level doc clutter was grouped into purpose-based folders.
- Similar implementation notes were unified into one canonical file.
- Older granular summaries were moved to `docs/archive/implementation-reports/`.
- Unnamed loose report was normalized to:
  - `docs/archive/alignment-reports/BACKEND_ALIGNMENT_REPORT_2026-04-25.md`

## Rules To Keep It Tidy

1. Put new implementation summaries in `implementation/` and keep one rolling consolidated file per month/release.
2. Move superseded detailed notes to `archive/` instead of leaving duplicates in active folders.
3. Keep freeze/contract docs immutable after publishing; add deltas as new files rather than editing history.
4. Use date suffixes (`YYYY-MM-DD`) for freeze/handover/report docs.
5. Keep folder depth shallow (max 2 levels under `docs/`) unless there is a strong reason.

## Additional Cleanup Suggestions

1. Create `docs/templates/` with templates for handover, review report, implementation summary, and test plan.
2. Add a lightweight docs lint/check script (naming + broken relative links).
3. Add `docs/CHANGELOG.md` that logs only document moves/renames and canonical-source changes.
4. Move emoji-heavy historical README-style content out of technical docs unless it is user-facing product documentation.
