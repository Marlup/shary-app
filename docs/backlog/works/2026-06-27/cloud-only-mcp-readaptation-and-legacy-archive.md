# Cloud-only MCP readaptation and legacy local-flow archive

## Scope

Readapt the Shary MCP product flow to the cloud-only OAuth baseline, remove
local MCP from the Android product-facing Settings flow, archive superseded
local/desktop MCP docs and test assets, and align backend public docs with the
same support boundary.

## Change list

1. Replaced the Settings MCP controls with a cloud-grant management view that
   shows the public MCP endpoint, active remote grants, pending approvals,
   renewal pressure, and revoke actions.
2. Removed product-facing local MCP start/stop, pair, endpoint, and copied
   bearer UI from the Android Settings flow.
3. Added automatic revocation of any legacy active local connector on
   `ConnectorSettingsViewModel` initialization so unsupported bearer-based
   access is shut down when the user revisits Settings.
4. Archived the superseded local MCP architecture and ADB-only conformance
   docs under `docs/archive/mcp-local-legacy/` and moved the direct protocol
   harness to `mcp-testing/legacy-protocol/`.
5. Updated the active architecture/testing docs and backend public/operator
   docs so the supported path is the public OAuth MCP flow only.

## Plan source

Conversation-approved implementation plan from 2026-06-26. No repo-local plan
file existed before execution for this change set.

## Viability/complexity per change

1. Android Settings flow: medium risk because the edited files already had
   ongoing MCP changes and had to be updated without reverting unrelated work.
2. Legacy local-connector shutdown: low-to-medium risk because it reuses the
   existing connector revocation path instead of adding a new migration store.
3. Doc/archive reshaping: medium effort because active references needed to be
   updated after file moves.
4. Backend doc alignment: low risk because the backend runtime contract did not
   change in this pass.

## Design decisions

- Cloud OAuth is the only supported Shary MCP product path.
- Local loopback MCP remains only as deprecated development-only legacy
  material and internal execution plumbing where remote grant handling still
  depends on the shared dispatcher/connector core.
- The user-facing lifecycle is grant-based: authorize, approve writes,
  renew, revoke.
- Legacy local connectors are proactively revoked when Settings initializes so
  stale copied-bearer access does not remain silently active.

## Implementation plan

1. Narrow the Android Settings surface to cloud grant information and revoke
   controls only.
2. Reuse the existing `RemoteMcpCoordinator` state for pending approvals and
   renewal pressure instead of adding a second MCP lifecycle model.
3. Preserve the shared connector core and dispatcher path so remote execution
   continues to function without a broad connector refactor.
4. Move superseded local docs/assets into a dedicated archive subtree and keep
   active docs pointed only at the cloud OAuth path.
5. Update backend public/operator docs to explicitly describe the cloud-only
   supported flow.

## Validation plan

1. Compile the Android app with `./gradlew :app:compileDebugKotlin`.
2. Inspect active docs and archive paths to confirm moved local materials are
   no longer referenced as supported product flows.
3. Review backend public/operator docs for cloud-only wording consistency.

## Result summary

Implemented. The Android product-facing MCP setup is now cloud-first and no
longer instructs users to start a local server or copy bearer credentials.
Legacy local MCP design/testing assets were archived, and both repos now state
that the supported operator flow is the public OAuth MCP gateway.
