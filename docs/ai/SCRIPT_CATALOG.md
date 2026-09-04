# Shary Script Catalog

## Purpose

Give agents a text-first contract for the repo's script entrypoints so common
tasks do not require opening each script for first-pass understanding.

## Root scripts

### `main.py`

- Purpose:
  - Firebase HTTP-function entrypoint that handles Shary identity registration,
    rotation, deletion, public-key reads, payload upload/fetch, request
    upload/fetch, login refresh-token exchange, and cleanup of expired docs.
- Main components:
  - HTTP error helpers, bearer-token validation, Ed25519 signature checks,
    Firestore access, identity-v2 endpoints, legacy compatibility endpoints,
    payload/request endpoints, and scheduler cleanup.
- Inputs:
  - HTTP requests, JSON bodies, bearer tokens, and Firestore collections.
- Outputs:
  - HTTP JSON responses, Firestore mutations, and scheduler cleanup status.
- Side effects:
  - Creates or updates identity documents, request/payload documents, and
    nonce records; deletes expired documents during scheduler cleanup.
- Current positioning:
  - This file is a preserved repo-local backend/function surface. The active
    supported MCP product path is still documented separately in
    `docs/architecture/SHARY_REMOTE_MCP_OAUTH_DESIGN.md`.
- Companion docs:
  - `docs/ai/SHARY_AGENT_REPO_SPEC.md`
  - `docs/design/SHARY_APP_TECHNICAL_OVERVIEW.md`

## `mcp-testing/real`

Use the local README for detailed per-script instructions:

- [mcp-testing/real/README.md](../../mcp-testing/real/README.md)

Scripts in that folder:

- `start-real-agent-run.ps1`
  - Initializes a run file, selects mode/transport, and performs preflight.
- `record-real-agent-result.ps1`
  - Writes scenario evidence into the run file.
- `test-real-agent-evidence.ps1`
  - Validates run completeness and renders the final Markdown report.

## `mcp-testing/legacy-protocol`

Use the local README for detailed per-script instructions:

- [mcp-testing/legacy-protocol/README.md](../../mcp-testing/legacy-protocol/README.md)

Scripts in that folder:

- `prepare-mcp-tests.ps1`
- `run-mcp-tests.ps1`
- `cleanup-mcp-tests.ps1`
- `mcp-common.ps1`
- `mcp-client.ps1`
- `mcp-prerequisites.ps1`
- `mcp-init.ps1`
- `mcp-ping-noAuth.ps1`
- `mcp-discover-tools.ps1`
- `mcp-origin-protection.ps1`
- `mcp-list-fields.ps1`
- `mcp-list-users.ps1`
- `mcp-readonly-denial.ps1`
- `mcp-policy-errors.ps1`
- `mcp-readwrite-crud.ps1`
- `mcp-preview-commit.ps1`

## `tools/codex`

Use the local README for detailed per-script instructions:

- [tools/codex/README.md](../../tools/codex/README.md)

Scripts in that folder:

- `session-init.ps1`
  - Verifies required policy files and can emit prompt context payloads.
- `update-manifest.ps1`
  - Recomputes policy-file hashes in the session-init manifest.

## Update rule

Whenever a script is added, removed, renamed, or its contract changes, update:

1. this catalog;
2. the nearest script-folder README;
3. any policy file that references the script by path or workflow.
