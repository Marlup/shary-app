# Debug certificate remote OAuth test path

## Scope

Confirm the recommended interim workaround for the remote OAuth / MCP flow
while the Android app is not yet released through Play App Signing.

## Change list

1. Reviewed the backend MCP tooling and confirmed the repository already
   supports an explicit debug-certificate test mode.
2. Avoided rerunning the mutating cloud provisioning script because the
   temporary environment was already provisioned and configured with the debug
   certificate.
3. Ran the read-only deployment preflight with the explicit
   `-AllowDebugCertificateForTesting` override.
4. Confirmed the temporary remote OAuth path passes all local/backend readiness
   checks without deployment.

## Plan source

Primary source:
`[[docs/backlog/works/2026-06-21/public-oauth-multi-vault-mcp-implementation.md#Deployment prerequisites]]`

Continuation source:
`[[docs/backlog/works/2026-06-26/oauth-mcp-continuation-readiness-review.md]]`

## Viability/complexity per change

- Tooling review: high viability, low complexity. The backend scripts already
  encoded the temporary testing path.
- Non-mutating verification: high viability, low complexity. The preflight
  supports the debug-certificate allowance directly.
- Operational outcome: high viability, low complexity. This unblocks remote
  OAuth testing without pretending production readiness.

## Design decisions

1. Keep the existing cloud test environment instead of reprovisioning it.
2. Use the remote OAuth architecture, not ADB, as the preferred temporary test
   path because it exercises the real public MCP design.
3. Keep the Play App Signing requirement as a production-only gate.
4. Treat the debug certificate as test-only and require explicit opt-in on the
   preflight command.

## Implementation plan

1. Confirm the backend scripts document and enforce a debug-certificate test
   mode.
2. Verify the current environment already points to the known debug SHA-256.
3. Run the read-only deployment preflight with
   `-AllowDebugCertificateForTesting`.
4. Preserve the exact interim path as a backlog checkpoint.

## Validation plan

- The preflight must pass with no cloud mutations.
- MCP backend tests and frozen backend regression tests invoked by the
  preflight must also pass.
- The output must explicitly identify the certificate as test-only.

## Result summary

Validated command:

`powershell -ExecutionPolicy Bypass -File tools\deployment_preflight.ps1 -AllowDebugCertificateForTesting`

Observed result:

- deployment preflight passed;
- MCP backend tests passed (`23 passed`);
- frozen backend regression tests passed (`19 passed`);
- no cloud resources were changed and no deployment was performed;
- App Links certificate was accepted only as an explicitly allowed test-only
  debug certificate.

Interim operational path until Play release:

1. Use the existing remote MCP endpoint and OAuth flow.
2. Install a debug APK on the device.
3. Treat the environment as test-only.
4. Replace the debug fingerprint with the Play App Signing SHA-256 before any
   production readiness claim or deployment.
