# Encrypted Vault Export/Import

Date: 2026-11-02
Status: Implemented; focused tests and full JVM suite pass; manual SAF check pending
Owner: Founder (single-owner project)
Scope: First zero-cost candidate from the Phase 1 continuation list.

## 1. What changed

- Added a versioned, encrypted `.sharyvault` export/import flow in Settings.
- Added `VaultBackupPayload` DTOs for fields, contacts, requests, tags, and safe
  settings.
- Added `VaultBackupCipher` with PBKDF2-HMAC-SHA256 at 600,000 iterations and
  AES-256-GCM.
- Added `VaultBackupManager` for additive merge into local repositories.
- Added `VaultBackupFileService` for Android SAF read/write with a 64 MB cap.
- Added Settings UI dialogs for passphrase, file picker, preview, and result.
- Added `VaultBackupCipherTest`, `VaultBackupPayloadTest`, and
  `VaultBackupManagerTest`.
- Updated the application module docs, codebase navigation, technical overview,
  and element/action map.

## 2. Design decisions

- **Encryption:** PBKDF2-HMAC-SHA256 600k, 16-byte salt, 32-byte key,
  AES-256-GCM with a 12-byte IV and a 128-bit tag.
- **Container:** `SHARYVB1` magic, version, iteration count, salt, IV, and
  ciphertext length; the header is authenticated as AAD.
- **Passphrase:** at least 12 characters; confirmation required on export; never
  stored. Losing it means losing the backup.
- **Import:** additive and non-destructive. Existing field keys, contact emails,
  request IDs, and tag names are kept; duplicates are skipped.
- **Excluded:** credentials, MCP tokens/grants/devices, connector state,
  workflow drafts, remembered email, and cloud ciphertext.
- **File access:** Android Storage Access Framework; no server involved.

## 3. Files

- `core/domain/types/valueobjects/VaultBackupPayload.kt`
- `infrastructure/security/backup/VaultBackupCipher.kt`
- `infrastructure/services/backup/VaultBackupFileService.kt`
- `application/backup/VaultBackupManager.kt`
- `application/backup/README.md`
- `viewmodels/configuration/SettingsViewModel.kt`
- `ui/screens/settings/SettingsScreen.kt`
- Tests under `app/src/test/java/com/shary/app/...`

## 4. Validation

- `:app:compileDebugKotlin` passed.
- Focused cipher/payload/manager tests passed.
- Full `:app:testDebugUnitTest` passed.
- `:app:lintDebug` passed.
- Manual device check is still required:
  1. Export a vault with a passphrase.
  2. Change a field, then import and confirm the preview.
  3. Verify existing items are kept and duplicates are skipped.
  4. Verify a wrong passphrase shows a clear error.
  5. Cancel the file picker and confirm no state is lost.

## 5. Residual risks

- Import is additive only; replace mode is not implemented.
- Import applies settings and repositories sequentially, not in one transaction.
- The passphrase is held as a Compose `String` while the dialog is open; the
  ViewModel clears it after the operation, but immutable strings cannot be
  zeroized.
- Field value history, theme, MCP grants, and credentials are not exported.
- A 64 MB cap protects memory; very large vaults will fail with a clear error.

## 6. Plan source

Candidate 1 in the Phase 1 continuation list:
`docs/backlog/works/2026-11-02/phase-1-continuation.md`.
