# Public OAuth multi-vault MCP implementation

## Scope

Implement the production agent-to-Shary path across the Android app and the
isolated Firebase MCP backend, while preserving the existing local connector
core as the policy and repository boundary.

## Android changes

1. Bound connector clients, sessions, policies, pending actions, and audits to
   owner UID, vault ID, agent profile ID, and grant ID.
2. Revoked legacy persisted clients that have no owner binding.
3. Split field metadata listing from exact field-value reads with
   `fields.metadata.read` and `fields.value.read`.
4. Added verified HTTPS app-link approval for
   `https://shary-21b61.web.app/mcp/approve`.
5. Added scope selection, owner alias, and biometric/device-credential
   confirmation.
6. Added Android Keystore-encrypted local grant credentials.
7. Added FCM wake-up, WorkManager operation claim/execution, crash recovery,
   pending-write notifications, explicit write approval/rejection, grant
   renewal, and cloud/local revocation.
8. Reused `SharyMcpDispatcher`, connector capabilities, confirmation policy,
   idempotency handling, repositories, and audit records.

## Backend changes

1. Added OAuth 2.1 authorization code + PKCE, dynamic client registration,
   protected-resource/authorization-server metadata, resource validation,
   15-minute access tokens, rotating refresh tokens, and one-time token-use
   claims.
2. Added separate agent profiles and multi-owner vault grants.
3. Added owner/device registration, grant binding, renewal, revocation,
   operation claim, confirmation, rejection, completion, and polling.
4. Added metadata-only field lists, exact value reads, mutation tools, vault
   invitation, and opaque multi-vault selection.
5. Added AES-256-GCM command/result envelope encryption with Cloud KMS key
   wrapping.
6. Added Firestore-backed distributed fixed-window request limits.
7. Added Firebase Hosting rewrites, verified app-link asset statements, and a
   QR authorization page.

## Security decisions

- The agent never receives owner UID, owner email, FCM token, or local
  connector credentials.
- Cloud Firestore stores encrypted command/result payloads only.
- The owner chooses per-vault scopes; value reads and writes are not
  preselected.
- Every write requires a separate in-app device-authenticated confirmation.
- Android release builds no longer use the debug signing key.
- ADB remains available only in development conformance guides and scripts.

## Documentation changes

- Added `docs/architecture/SHARY_REMOTE_MCP_OAUTH_DESIGN.md`.
- Replaced the real-agent E2E guide with the public OAuth flow.
- Adapted `mcp-testing/real` for remote OAuth and queued-operation polling.
- Labeled local quick-start, Phase 0, local-agent, and desktop-bridge documents
  with their current development/deferred status.
- Updated protocol tooling for nine local tools and metadata/value separation.

## Validation evidence

- `./gradlew :app:compileDebugKotlin`: passed.
- `./gradlew :app:testDebugUnitTest`: passed, 39 tests after the value-read
  coverage was added.
- `./gradlew :app:compileDebugAndroidTestKotlin`: passed.
- `git diff --check`: passed in both repositories.
- Secret-pattern scan found no copied bearer, prior test token, private key, or
  populated MCP signing secret.
- Isolated MCP backend suite: 23 passed after final OAuth, renewal,
  idempotency, rate-limit, runtime-identity, and protocol hardening.
- Frozen backend regression suite: 19 passed.
- Firebase Functions/Firestore emulator smoke: passed for health, protected
  resource discovery, and unauthenticated OAuth challenge.
- Python `compileall`, Firebase JSON parsing, and PowerShell/scenario syntax
  checks passed.
- No Firebase deployment was performed.

## Deployment prerequisites

1. Configure Cloud KMS and its runtime IAM grant.
2. Set the Firebase signing secret.
3. Configure the production MCP URLs and Play App Signing SHA-256 fingerprint.
4. Enable Firestore TTL policies.
5. Run both backend suites, Android compile/unit tests, emulator smoke, and a
   real Codex/Claude OAuth E2E.
6. Deploy only the MCP codebase and Hosting after explicit approval.

The read-only cloud preflight currently reports these external blockers:

- MCP signing secret missing;
- Cloud KMS API/key missing;
- dedicated MCP runtime service account/config missing;
- seven Firestore TTL policies missing;
- Play App Signing SHA-256 fingerprint unavailable;
- release Android variant has no local signing configuration.

The backend now includes dry-run-by-default provisioning and read-only
preflight scripts under `firebase/mcp-functions/tools/`.

## Test infrastructure provisioning result

With explicit user approval, the test-only environment was provisioned using
the Android debug certificate:

- Cloud KMS API enabled.
- Dedicated `shary-mcp-runtime` service account created.
- Firestore and FCM project roles granted.
- Regional 90-day-rotating KMS key created.
- Key-level encrypt/decrypt permission granted.
- Random MCP signing secret created without printing or storing its value.
- Secret accessor permission granted only to the MCP runtime account.
- All seven `delete_after` Firestore TTL policies enabled.
- Non-secret `.env.shary-21b61` created with the test App Links certificate.
- Full deployment preflight passed.
- MCP backend: 23 passed.
- Frozen backend: 19 passed.

No Firebase Functions, Hosting, or Android deployment was performed.
