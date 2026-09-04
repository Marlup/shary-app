# Command-driven real-agent MCP suite

## Scope

Turn the existing manual real-agent guide into a repository-local suite that
Codex can execute from a single natural-language command after MCP setup.
Preserve the existing direct PowerShell tests in a separate named folder.

## Change list

1. Moved the existing direct MCP scripts to `mcp-testing/protocol/`.
2. Added `mcp-testing/real/` with agent instructions, scenario definitions,
   evidence initialization, result recording, and validation.
3. Added root `mcp-testing/README.md` routing operators to protocol or
   real-agent testing.
4. Updated both MCP testing guides to use the new paths.
5. Added a command-driven Codex section to the real-agent guide.
6. Ignored generated real-agent evidence under
   `mcp-testing/real/results/`.

## Viability and complexity

- Protocol preservation: low risk; scripts remain together and only their
  repository-root calculation and documented paths change.
- Agent execution: moderate complexity because model behavior cannot be
  proven by a PowerShell script alone. The implementation uses a structured
  agent contract and validates recorded tool evidence.
- Mutations: controlled by the exact user phrase `including mutations`; the
  default mode is read-only.

## Design decisions

1. Named the preserved suite `protocol` because it directly tests transport,
   JSON-RPC, policy, CRUD, preview/commit, and idempotency.
2. Kept token handling outside evidence files and prohibited agents from
   printing or recording bearer values.
3. Made generated evidence local-only.
4. Required real Shary audit observations for passing scenarios.
5. Made the evidence validator fail on pending scenarios, missing expected
   tools, forbidden tools, missing Shary results, missing audit results, or
   explicitly failed scenarios.

## Implementation plan

1. Preserve and repair the protocol suite paths.
2. Add deterministic real-agent scenarios and execution instructions.
3. Add evidence lifecycle scripts.
4. Update operator documentation.
5. Parse all PowerShell files and exercise the evidence validator with
   synthetic pass and failure manifests.

## Validation plan

1. Parse every PowerShell file under `mcp-testing/`.
2. Parse `mcp-testing/real/scenarios.json`.
3. Initialize a run with protocol preflight disabled.
4. Verify incomplete evidence fails.
5. Record synthetic compliant evidence and verify report generation.
6. Run `git diff --check`.

## Result summary

The suite can now be initiated by commanding Codex to run
`mcp-testing/real`. It performs a protocol preflight, executes real MCP tool
scenarios, records structured evidence, validates tool-policy compliance, and
produces a cleanup-aware report.

Validation completed:

- The read-only initializer executed with protocol preflight disabled.
- The validator rejected both pending scenarios in an incomplete manifest.
- The recorder accepted synthetic evidence for the two read-only scenarios.
- The validator accepted the complete manifest and generated its Markdown
  report.
- `git diff --check` passed.
