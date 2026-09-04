# Privacy Policy — DRAFT (working template, not legal advice)

> **Status:** Draft. Must be reviewed/approved by a qualified lawyer before
> publication. Per LSSI-CE 34/2002 and TRLGDCU, a **Spanish-language version is
> mandatory** for Spanish consumers; this English text is the working source.
> Replace every `[PLACEHOLDER]` before use.

**Controller:** `[Legal entity name, NIF, registered address, contact email]`
**Effective date:** `[date]`
**Supervisory authority:** Agencia Española de Protección de Datos (AEPD).

---

## 1. Summary

Shary is a local, offline-first encrypted vault for your personal fields, with
optional end-to-end encrypted sharing via email or a cloud relay, and optional
agent (MCP) access. Shary is designed so that **your vault content never leaves
your device in readable form**. This policy explains what we do and do not
process.

## 2. What we do NOT collect

- We do **not** read, store, or have access to the plaintext of your Fields or
  vault content. Data you share by email is prepared on your device and handed
  to your chosen sharing app. Data you share by cloud relay is encrypted on your
  device before upload; we only store **ciphertext**.
- We do **not** sell your data, and we do **not** use your data for advertising
  or profiling.

## 3. What we process, and why (lawful basis)

| Processing | Data | Lawful basis (GDPR Art. 6) |
|---|---|---|
| Account & identity registration | Email address (verified), public signing/encryption keys, key version, timestamps | 6(1)(b) performance of contract |
| Cloud relay (send/receive) | Encrypted ciphertext, sender/recipient hashes, timestamps, expiry | 6(1)(b) contract |
| Inbox decisions | Decision, reason code, decision ID, recipient hash | 6(1)(b) contract |
| Agent (MCP) access | Grant metadata, operation IDs, device ID, scope list | 6(1)(b) contract; 6(1)(a) consent for scopes |
| Abuse prevention | Rate-limit counters, IP-address-derived actor keys | 6(1)(f) legitimate interest (security) |
| Support | Whatever you send us | 6(1)(b) / 6(1)(a) |

## 4. Zero-knowledge disclosure (important)

Your identity keys are derived from your password and never leave your device.
**If you forget your password or lose your device, your data cannot be
recovered by you or by us.** Keep a secure copy of your recovery material. This
is an intended privacy property, not a defect.

## 5. Retention

- Cloud relay ciphertext and decisions are deleted automatically after their
  expiry (default 7 days, maximum 14 days).
- Identity records are kept until you delete them or request erasure.
- Rate-limit counters and logs are retained for short, security-related periods.

## 6. Data subject rights (GDPR Art. 15–22)

You may request **access, rectification, erasure, restriction, portability, and
objection**. Your vault data lives on your device, so most of these are
exercisable directly in the app (export, delete). For cloud identity records,
contact `[privacy email]`. You also have the right to lodge a complaint with the
AEPD.

## 7. International transfers

Processing infrastructure is hosted in the EU (`europe-southwest1`). We do not
transfer personal data outside the EEA except where an adequacy decision or
appropriate safeguards (Standard Contractual Clauses) apply, via our processors.

## 8. Security (GDPR Art. 32)

End-to-end encryption (X25519 + AES-256-GCM, Ed25519 signatures), on-device key
storage, hardware-backed key wrapping, verified-email authentication, and rate
limiting. No security measure is absolute; we maintain breach-notification
procedures under Art. 33/34.

## 9. Children

The service is not directed at children under 14 (Spanish age of consent). We do
not knowingly process their data.

## 10. Contact

- Data protection: `[privacy email]`
- Supervisory authority: Agencia Española de Protección de Datos (aepd.es)

## 11. Changes

We will notify you of material changes via the app or email before they take
effect.
