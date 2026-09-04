# Shary MCP testing

The test assets are split by responsibility:

- `legacy-protocol/` preserves the deprecated direct PowerShell transport,
  JSON-RPC, policy, CRUD, preview/commit, and idempotency checks for
  development-only local conformance.
- `real/` drives the supported public OAuth MCP server through a real AI agent
  such as Codex and validates the evidence produced by that run.

For the operator-command workflow, open this repository in a Codex session
with the `shary` MCP server connected and say:

```text
Run the Shary real-agent E2E suite in mcp-testing/real. Follow its AGENTS.md,
execute every applicable read-only scenario, save the evidence, and validate
the final report.
```

To authorize harmless test writes as well:

```text
Run the Shary real-agent E2E suite in mcp-testing/real, including mutations.
Follow its AGENTS.md, save the evidence, validate the report, and tell me which
generated Shary records must be removed afterward.
```
