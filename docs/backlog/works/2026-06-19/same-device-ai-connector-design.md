# Same-device AI connector design

## Scope

Design how local and cloud-brokered AI assistants can connect to Shary when
Shary and the AI client application are installed on the same Android device.

## Change list

1. Initially added a broad same-device architecture, later superseded by
   `docs/architecture/SHARY_LOCAL_AGENT_MCP_DESIGN.md` when scope was narrowed
   to MCP-capable agent tools.
2. Defined Local Direct, Android IPC, Shary-owned assistant, and optional Cloud
   Bridge paths.
3. Preserved the existing connector core as the only authorization and policy
   boundary.
4. Added security, lifecycle, capability, audit, Android platform, rollout,
   and acceptance requirements.

## Viability and complexity

- Local MCP: viable for clients that accept a localhost MCP endpoint; moderate
  hardening work is required for full Streamable HTTP compatibility.
- Android IPC: viable for cooperative partner applications; clients must adopt
  the Shary SDK.
- Consumer mobile assistants: not universally reachable through local IPC
  because some connectors execute in provider infrastructure.
- Remote relay: viable but high complexity and changes the privacy boundary.
- Shary-owned assistant: viable and predictable, but requires model API
  adapters and provider credentials.

## Design decisions

1. Use a dual-mode architecture rather than assuming same-device installation
   implies local connector execution.
2. Implement local MCP and Android IPC before any public relay.
3. Split field metadata access from field-value access.
4. Use one-time pairing and device/client identity instead of manually copied
   long-lived bearer tokens.
5. Keep Cloud Bridge disabled by default with explicit data-egress warnings.

## Implementation plan

1. Complete and harden local Streamable HTTP MCP.
2. Add foreground lifecycle, vault-lock enforcement, and local pairing.
3. Add a Messenger-based Android partner service and Kotlin SDK.
4. Add a Shary-owned provider-neutral assistant if a guaranteed mobile UX is
   required.
5. Add the public remote MCP relay only for cloud-originated connectors.

## Validation plan

1. Protocol conformance tests for Streamable HTTP.
2. Same-phone Android MCP test-client E2E without ADB.
3. IPC caller identity, capability, lock, revoke, and pagination tests.
4. Data-egress and confirmation UX tests.
5. Remote OAuth/tunnel threat model and provider-specific E2E tests.

## Result summary

The design establishes a realistic path for broad AI compatibility while
making the hard boundary explicit: unmodified consumer apps cannot be forced
to use a local Shary endpoint. Cooperative local clients use MCP or Android
IPC; cloud-brokered assistants require an optional HTTPS relay; a Shary-owned
assistant provides the most reliable same-device alternative.
