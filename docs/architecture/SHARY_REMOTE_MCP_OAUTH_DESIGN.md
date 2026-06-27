# Shary remote OAuth MCP architecture

## Decision

Use a public Firebase-hosted Streamable HTTP MCP endpoint with OAuth 2.1 and
keep Android as the vault execution, confirmation, renewal, and revocation
boundary.

```text
Codex or Claude
  -> OAuth access token for one agent profile
  -> Firebase MCP gateway
  -> encrypted operation for one opaque vault ID
  -> owner-bound Android device
  -> existing connector dispatcher and policy
```

This is the supported Shary MCP product flow.
Local loopback MCP, copied bearer setup, and desktop-bridge experiments are
archived under `docs/archive/mcp-local-legacy/` and are no longer product
entry points.

## Identity model

- OAuth client: one Codex/Claude installation or MCP client registration.
- Agent profile: created by one completed OAuth authorization.
- Vault grant: one owner-authorized vault attached to an agent profile.
- Device binding: owner UID + installation vault ID + installation device ID.
- Local connector: least-privilege Android policy/session bound to the exact
  owner UID, vault ID, agent profile ID, and grant ID.

An agent profile can hold many grants from different owners. Each owner sees
and controls only their own grant.

## Authorization flow

1. Agent connects to `https://shary-21b61.web.app/mcp`.
2. Gateway returns `401` with canonical protected-resource metadata.
3. Agent dynamically registers and starts authorization code + PKCE.
4. Browser shows a five-minute QR/app link.
5. Shary verifies the signed-in owner, registered device, and vault.
6. Owner reviews the client name, scopes, and chosen alias, then authenticates
   with the device credential.
7. Shary creates a matching local connector and binds the cloud grant.
8. Only after binding does the OAuth browser receive its one-time code.

No bearer token is copied by the user.

## Operation flow

- The gateway authorizes both OAuth token scope and vault grant scope.
- Commands and results use AES-256-GCM envelope encryption with Cloud KMS key
  wrapping in Firestore.
- FCM wakes the bound Android installation.
- WorkManager claims operations over owner-authenticated HTTPS.
- Read operations execute through `SharyMcpDispatcher`.
- Write operations remain pending until a biometric/device-credential
  confirmation.
- The agent polls `shary.operations.get` for terminal status.
- Operation and result documents use short expiration windows and TTL cleanup.

## Product lifecycle model

The product-facing lifecycle is grant-based, not local-server based.

- Agents connect through cloud OAuth.
- Owners approve scopes and aliases in Shary.
- Settings lists active vault grants and revoke controls.
- Renewal is owner-authenticated and prompted during the final seven days.
- Writes always require an in-app confirmation.
- Revocation disables the cloud grant and the bound Android connector together.

## Disclosure rules

- `shary.vaults.list` exposes alias, opaque vault ID, scopes, expiry, and
  online state only.
- `shary.fields.list` exposes metadata only.
- `shary.fields.get` requires `fields.value.read` and an exact key.
- Owner UID, email, FCM token, local connector bearer, and refresh secret are
  never returned to the agent.

## Security controls

- PKCE S256, dynamic client registration, audience/resource validation.
- One-time authorization code and refresh-token consumption records.
- Verified Firebase owner email.
- Registered owner/device/vault check before grant creation.
- Android Keystore encryption for local remote-connector credentials.
- Firestore distributed fixed-window rate limits.
- Browser-origin protocol requests rejected.
- Existing connector policy, audit, idempotency, and repository paths reused.

## Deployment boundary

Backend code lives in the isolated `firebase/mcp-functions` codebase in:

`C:\main_work\shary-project\shary-backend`

Android execution lives in:

`app/src/main/java/com/shary/app/infrastructure/connector/remote`

Deployment requires Cloud KMS, Firebase Secret Manager, Firestore TTL, Hosting
rewrites, and the production Play App Signing SHA-256 fingerprint.
