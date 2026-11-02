# Vault Backup Application Module

## Purpose

Own the portable, encrypted vault export/import flow. The user chooses a
passphrase; Shary writes one encrypted file and can merge it back into the local
vault later.

## Files and responsibilities

### `VaultBackupManager.kt`

- Builds a `VaultBackupPayload` from local repositories.
- Encrypts and decrypts through `VaultBackupCipher`.
- Validates format and version before import.
- Merges tags, fields, contacts, requests, and safe settings.
- Import is additive and non-destructive:
  - fields are matched by key;
  - contacts are matched by email;
  - requests are matched by stable request ID;
  - tags are matched by name.
- Existing items are kept; duplicates are skipped.

## What is exported

- Fields, including typed value, alias, tag, and MCP-shareable flag.
- Contacts.
- Received and sent requests, including delivery metadata.
- Custom field tags and contact group tags.
- Safe preferences: cloud expiry, biometric gate, compact mode, Wi-Fi-only
  sync, copy long-press, clipboard auto-clear, auto-open inbox.

## What is not exported

- Passwords and the credentials blob.
- MCP access/refresh tokens, grants, devices, and connector state.
- Request workflow drafts.
- Cloud ciphertext already on the server.
- Remembered login email and login preferences.

## Encryption format

- PBKDF2-HMAC-SHA256, 600,000 iterations, 32-byte key, 16-byte salt.
- AES-256-GCM, 12-byte IV, 128-bit tag.
- Versioned binary container with authenticated header.
- The passphrase is never stored; losing it means losing the backup.

## Validation

```powershell
.\gradlew.bat :app:testDebugUnitTest `
  --tests "com.shary.app.infrastructure.security.backup.VaultBackupCipherTest" `
  --tests "com.shary.app.core.domain.types.valueobjects.VaultBackupPayloadTest" `
  --tests "com.shary.app.application.backup.VaultBackupManagerTest"
```

Manual device check:

1. Settings → Export vault → choose a passphrase of at least 12 characters →
   save a `.sharyvault` file.
2. Change or clear a field, then Settings → Import vault → choose the file →
   enter the passphrase.
3. Confirm the preview counts, import, and verify existing items were kept.
4. Repeat with a wrong passphrase and confirm a clear error.
5. Cancel the file picker and confirm no state is lost.

## Update triggers

Update this file when:

- the payload fields or version change;
- import becomes destructive or supports replace mode;
- encryption parameters change;
- credentials, MCP state, or drafts become part of the backup.
