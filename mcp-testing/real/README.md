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

## Script contracts

### `start-real-agent-run.ps1`

- Purpose:
  - initialize one evidence run and select the applicable scenarios.
- Inputs:
  - `-Mode`:
    - `ReadOnly`
    - `ReadWrite`
  - `-Transport`:
    - `RemoteOAuth`
    - `LocalAdb`
  - optional `-DeviceSerial`, `-McpUrl`, `-SkipProtocolPreflight`.
- Outputs:
  - one JSON run file under `results/`;
  - console summary with run ID and evidence path.
- Side effects:
  - for `LocalAdb`, can execute the legacy protocol preflight runner;
  - for `RemoteOAuth`, probes the MCP endpoint for the expected OAuth
    challenge behavior.

### `record-real-agent-result.ps1`

- Purpose:
  - write the result for one scenario into an existing run file.
- Inputs:
  - required `-RunFile`, `-ScenarioId`, `-Status`;
  - optional `-ToolCalls`, `-SharyResult`, `-AuditResult`, `-Notes`,
    `-GeneratedRecords`.
- Outputs:
  - updated run JSON with the scenario result persisted.
- Side effects:
  - refuses paths outside `results/` to avoid accidental arbitrary-file edits.

### `test-real-agent-evidence.ps1`

- Purpose:
  - validate scenario completion, tool usage, result narratives, and skipped
    prerequisite notes, then render the final Markdown report.
- Inputs:
  - required `-RunFile`.
- Outputs:
  - updated run JSON with `completedAtUtc`;
  - Markdown report beside the JSON file;
  - exit code `0` only when evidence is valid and no scenario failed.
- Side effects:
  - marks validation errors as a failing process exit.

## Update triggers

Update this README and `docs/ai/SCRIPT_CATALOG.md` when:

- a script parameter changes;
- run-file schema changes;
- scenario evidence rules change;
- the active transport model changes.
