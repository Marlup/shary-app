# Shary real-agent E2E suite

This folder turns the real-agent guide into a repo-local execution contract
that Codex or another capable agent can follow against the public OAuth MCP
server.

## One-time setup

1. Install a Shary Android build whose app link matches the deployed
   `assetlinks.json`.
2. Sign in with a verified Shary account and keep the device online.
3. Configure the AI client and complete OAuth as documented in
   `docs/testing/connector-mcp-real-agent-e2e.md`.
4. Approve the requested scopes in Shary. Do not copy or manage bearer tokens.

## Command Codex

Read-only run:

```text
Run the Shary real-agent E2E suite in mcp-testing/real. Follow AGENTS.md,
execute all applicable read-only scenarios, record evidence, validate it, and
report the result.
```

Read-write run:

```text
Run the Shary real-agent E2E suite in mcp-testing/real, including mutations.
Follow AGENTS.md, use unique agent-e2e records, record evidence, validate it,
and report both the result and cleanup list.
```

The phrase `including mutations` is the explicit authorization boundary for
creating test records. Without it, the agent must initialize the run in
`ReadOnly` mode and must not call mutation tools.

## Files

- `AGENTS.md`: executable instructions for the agent.
- `scenarios.json`: machine-readable scenario and tool expectations.
- `start-real-agent-run.ps1`: OAuth discovery preflight and evidence initialization.
- `record-real-agent-result.ps1`: safe structured evidence updates.
- `test-real-agent-evidence.ps1`: completeness and rule validation.
- `results/`: local run artifacts; ignored by Git.

Deprecated local ADB conformance assets are archived in
`docs/archive/mcp-local-legacy/` and `mcp-testing/legacy-protocol/`.
