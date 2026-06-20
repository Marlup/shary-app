# Production desktop bridge design

## Scope

Replace ADB forwarding as the desktop-agent transport with an installable,
production-oriented Shary Desktop Bridge while preserving Android as the data
owner and connector-policy authority.

## Change list

1. Added `docs/architecture/SHARY_DESKTOP_BRIDGE_DESIGN.md`.
2. Classified the existing loopback plus ADB path as development-only.
3. Designed a loopback desktop MCP endpoint and Android-initiated encrypted
   local-network device link.
4. Defined QR pairing, pinned TLS, persistent identities, agent profiles,
   credential mapping, lifecycle, failure semantics, and threat controls.
5. Defined repository modules, implementation phases, validation matrix, and
   production acceptance criteria.
6. Updated the current local-agent MCP design to route production desktop work
   to the bridge design.

## Viability and complexity

- Desktop bridge: high viability and moderate-to-high implementation
  complexity. It solves the production transport problem without duplicating
  Shary as a desktop product.
- Android-initiated local link: viable on private LANs; requires a visible
  foreground service, desktop firewall allowance, reconnect handling, and
  secure pairing.
- Cloud relay: viable but intentionally deferred because it changes the
  privacy and operational boundary.

## Design decisions

1. Keep MCP local to the desktop and expose only loopback.
2. Make Android initiate a pinned TLS WebSocket connection to the bridge.
3. Keep authorization and audit decisions in Shary.
4. Store only connector credentials in the desktop OS vault; never replicate
   Shary data.
5. Separate bridge-device pairing from capability-scoped agent profiles.
6. Preserve ADB only for development and conformance testing.

## Implementation plan

1. Prepare a transport-neutral MCP dispatcher and security lifecycle controls.
2. Add shared link protocol and desktop bridge modules.
3. Implement QR pairing and the Android foreground device-link service.
4. Add approved agent profile provisioning and credential refresh/revoke.
5. Harden, package, and validate with real desktop agents while ADB is absent.

## Validation plan

1. Review consistency with the current MCP adapter and connector core.
2. Verify both backlog indexes reference this design.
3. Run the repository policy gate and Markdown formatting checks.
4. Require implementation tests listed in the architecture validation matrix
   before any production claim.

## Result summary

The design establishes a production path for desktop agent tools without ADB:
an installable loopback MCP bridge on the PC connected to Shary through an
Android-initiated, mutually authenticated local-network link. Implementation
has not started.
