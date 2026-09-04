# Shary Agent Repo Spec

## Purpose

Define the repo-specific instructions that an agent needs in `sharydemo`
without copying all general governance from Enhanced-BRAIN.

## Recommended model

- Enhanced-BRAIN remains the authority for:
  - general task-record governance;
  - reusable skills such as `record-task` and `librarian`;
  - case review and broader workspace operating rules.
- `sharydemo` remains the authority for:
  - repository-specific policy;
  - Android app and MCP product boundaries;
  - backlog work logs and raw-order backups;
  - module-awareness docs;
  - script contracts and local test harness rules.

## Task recording model

Do not clone the full Enhanced-BRAIN `request.md` plus `work.md` directory
scheme into `sharydemo`.

Use the existing `sharydemo` dual-surface model instead:

- polished work record:
  - `docs/backlog/works/<YYYY-MM-DD>/<work-file>.md`
- raw user order backup:
  - `docs/backlog/orders_draft/<YYYY-MM-DD>/<raw-order-file>.md`
- discovery indexes:
  - `docs/backlog/works/works-index.md`
  - `docs/backlog/orders_draft/works-index.md`

This is the repo-local equivalent of a task record. It is already established,
durable, and lighter than the Enhanced-BRAIN record tree.

## Retrieval surfaces for agents

### Repo policy

- Root `AGENTS.md` is canonical.
- `CODEX.md` and `CLAUDE.md` are adapters only.
- `docs/ai/SESSION_INIT.md` explains the startup gate and prompt emission.

### Product and architecture

- `docs/ai/CODEBASE_NAVIGATION.md`
  - First-stop task-to-code, ownership, search, and validation map.
- `docs/design/SHARY_APP_TECHNICAL_OVERVIEW.md`
  - Current app architecture, runtime flows, capabilities, and product
    boundaries.
- `docs/architecture/SHARY_REMOTE_MCP_OAUTH_DESIGN.md`
  - Supported remote OAuth MCP architecture.

### Durable work history

- `docs/backlog/works/works-index.md`
  - Discovery surface for completed and ongoing work notes.
- `docs/backlog/orders_draft/works-index.md`
  - Discovery surface for raw user-order backups.

### Module awareness

- `app/AGENTS.md`
  - Android module source-set routing, architecture boundaries, and validation.
- `app/src/main/java/com/shary/app/ui/AGENTS.md`
  - Compose ownership, route-versus-renderer rules, feature paths, and UI tests.
- `app/src/main/java/com/shary/app/application/README.md`
  - Application-layer map.
- One `README.md` per application submodule folder:
  - `cloud/`
  - `connector/`
  - `security/`
  - `usecases/`

### Script awareness

- `docs/ai/SCRIPT_CATALOG.md`
  - Cross-folder catalog for root scripts and harness entrypoints.
- Local script-folder READMEs:
  - `mcp-testing/real/README.md`
  - `mcp-testing/legacy-protocol/README.md`
  - `tools/codex/README.md`

## Librarian-friendly retrieval rules

When using bounded Markdown retrieval, prefer this order:

1. nearest scoped `AGENTS.md`, then the nearest module `README.md`;
2. `docs/ai/CODEBASE_NAVIGATION.md` for code and test ownership;
3. another `docs/ai` file with the matching concern;
4. active architecture or technical-overview doc;
5. matching backlog work note only when implementation history matters.

Prefer file-local exact sections such as:

- `## Task recording model`
- `## Retrieval surfaces for agents`
- `## <script-name>`
- `## Files and responsibilities`
- `## Update triggers`

## What not to do

- Do not duplicate Enhanced-BRAIN case policy or skill internals here.
- Do not treat archived local MCP material as the active product path.
- Do not rely on historical docs that still mention `AGENT.md` when the live
  repo file is `AGENTS.md`.

## Mandatory update triggers

Update the relevant repo-local docs in the same patch when any of these change:

- root policy filename or session-init load order;
- backlog recording procedure;
- supported MCP architecture or product boundary;
- application-module responsibilities or file ownership;
- task-to-code routing or scoped `AGENTS.md` ownership;
- script parameters, outputs, side effects, or expected operator flow.

At minimum, update:

- root `AGENTS.md` for policy changes;
- the nearest module `README.md` for application-layer changes;
- the local script-folder `README.md` and `docs/ai/SCRIPT_CATALOG.md` for
  script changes.
