# ROPA — Record of Processing Activities — DRAFT

> **Status:** Internal working draft (GDPR Art. 30). Must be finalized and kept
> current once the legal entity and processors are confirmed. Placeholders `[]`
> must be replaced. This register covers the controller's processing.

**Controller:** `[Legal entity name, NIF, address]`
**Prepared:** 2026-08-30

## Processing activities

### 1. Account & identity registration
- **Purpose:** provision and secure the service; enable end-to-end encrypted
  sharing.
- **Lawful basis:** Art. 6(1)(b) contract.
- **Data subjects:** registered users.
- **Categories:** email (verified), public signing/encryption keys, key version,
  timestamps.
- **Recipients:** Firebase/Google (processor).
- **Retention:** until deletion/erasure.
- **Transfers:** EU region (`europe-southwest1`).
- **Security:** verified-email auth; key material never stored server-side.

### 2. Cloud relay — upload/fetch (fields & requests)
- **Purpose:** relay end-to-end encrypted shares between users.
- **Lawful basis:** Art. 6(1)(b) contract.
- **Data subjects:** senders and recipients.
- **Categories:** encrypted ciphertext, sender/recipient hashes, timestamps,
  expiry, verification/signature.
- **Recipients:** Firebase/Google (processor).
- **Retention:** TTL 7 days (default) / 14 days (max), then hard-deleted.
- **Transfers:** EU region.
- **Security:** E2E encryption; server stores ciphertext only.

### 3. Inbox decisions
- **Purpose:** record recipient accept/reject decisions idempotently.
- **Lawful basis:** Art. 6(1)(b) contract.
- **Data subjects:** recipients.
- **Categories:** decision, reason code, decision ID, recipient hash, timestamps.
- **Recipients:** Firebase/Google (processor).
- **Retention:** aligned to source request TTL, then hard-deleted.
- **Security:** recipient-scoped, idempotent.

### 4. Agent (MCP) access — OAuth, grants, operations
- **Purpose:** enable user-approved agent access to the vault.
- **Lawful basis:** Art. 6(1)(b) contract; Art. 6(1)(a) consent (scopes).
- **Data subjects:** users who connect agents.
- **Categories:** grant metadata, operation IDs, device ID, FCM token, scopes.
- **Recipients:** Firebase/Google (processor); KMS for envelope keys.
- **Retention:** while grants are active; operations until completion/expiry.
- **Transfers:** EU region.
- **Security:** OAuth 2.0 + PKCE; KMS envelope encryption; approval-gated writes.

### 5. Abuse prevention & operational logs
- **Purpose:** protect the service from abuse and maintain reliability.
- **Lawful basis:** Art. 6(1)(f) legitimate interest.
- **Data subjects:** users; attackers.
- **Categories:** rate-limit counters, IP-derived actor keys, request IDs,
  error codes, timestamps.
- **Recipients:** Firebase/Google (processor); monitoring tools.
- **Retention:** short, security-related period (rate-limit counters expire;
  logs retained minimally).
- **Security:** pseudonymized actor keys (hashed).

### 6. Support
- **Purpose:** respond to user requests.
- **Lawful basis:** Art. 6(1)(b) / 6(1)(a).
- **Data subjects:** users contacting support.
- **Categories:** whatever the user provides.
- **Retention:** limited to the support need.
- **Security:** access-restricted.

## Outstanding prerequisites (blockers before "complete")

- Confirm processor list and execute DPAs (GDPR Art. 28) — notably Google
  Cloud/Firebase.
- Confirm no transfers outside the EEA or document SCCs.
- Complete the DPIA for the biometric gate and MCP automation (Art. 35).
