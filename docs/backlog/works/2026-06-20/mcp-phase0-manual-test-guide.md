# Codex-operated MCP Phase 0 live test guide

## Scope

Add a concise Codex execution contract for validating the new MCP dispatcher,
signed-out lifecycle denial, and bearer-scoped rate limit on a real Android
target.

## Change list

1. Checked the existing MCP quick-start and real-agent guides.
2. Added `docs/testing/connector-mcp-phase0-manual-check.md`.
3. Reworked the initial user-operated command list into a Codex-operated
   workflow after the user clarified the intended meaning of "manually".
4. Defined agent preflight, assertions, pause points, evidence reporting, and
   secure cleanup.
5. Added the required raw-order backup and backlog index entries.

## Viability and complexity

The check is low complexity and uses the existing debug MCP endpoint and ADB
forwarding path. Codex can automate all transport and assertion work. User
interaction remains necessary only for in-app authentication, connector
pairing, and sign-out/sign-in transitions.

## Design decisions

1. Make the guide an execution contract that the user commands Codex to run.
2. Use a read-only connector and `shary.fields.list`.
3. Require Codex to pause for unavoidable Android security actions.
4. Keep the bearer token process-scoped and exclude values from evidence.

## Validation plan

1. Check Markdown formatting.
2. Verify the contract matches the current endpoint, protocol version, eight
   tools, error codes, and 120-request fixed-window limit.

## Result summary

The repository now contains a focused Codex-operated real-environment check
for all new Phase 0 behavior.
