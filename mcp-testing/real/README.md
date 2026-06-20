# Shary real-agent E2E suite

This folder turns the manual real-agent guide into a repo-local execution
contract that Codex or another capable local agent can follow.

## One-time setup

1. Build and launch Shary:

   ```powershell
   .\mcp-testing\protocol\prepare-mcp-tests.ps1 -DeviceSerial "emulator-5554"
   ```

2. In **Settings > Assistant Connectors**, start MCP and pair the connector
   required for the intended run.
3. Configure the local AI client as documented in
   `docs/testing/connector-mcp-real-agent-e2e.md`.
4. Start or reconnect the AI client after setting its token environment
   variable.

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
- `start-real-agent-run.ps1`: protocol preflight and evidence initialization.
- `record-real-agent-result.ps1`: safe structured evidence updates.
- `test-real-agent-evidence.ps1`: completeness and rule validation.
- `results/`: local run artifacts; ignored by Git.

The direct non-agent suite remains in `mcp-testing/protocol/`.
