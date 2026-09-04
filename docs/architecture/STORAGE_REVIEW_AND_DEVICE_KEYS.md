# Storage Review And Device-Centric Key Approach

Date: 2026-04-10
Scope: Android app private storage under `com.shary.app/files/` with focus on `files/data/authentication/`.

## Goals
- Explain how the current storage and key flows work.
- Describe the risks found in the storage modules.
- Propose changes that improve robustness without changing the on-device, ephemeral key strategy.
- Make explicit why the changes do **not** break the “generate keys on the fly, avoid storing them” approach.

## Where Data Lives
All files referenced below are inside the app sandbox returned by `context.filesDir`.

### Authentication Directory
Location: `files/data/authentication/`

Files:
- `signature.json`
- `.credentials`
- `credentials.timestamp`

### Other Files In `files/`
Imports and exports are currently stored at the root of `files/`:
- Imported JSON files (e.g., `*.json`)
- Imported ZIP files (e.g., `*.zip`)

The constant `PATH_DOWNLOAD = "data/download"` is defined but not used by file services at the moment.

## Modules And Responsibilities

### `SecurityUtils`
File: `app/src/main/java/com/shary/app/infrastructure/security/helper/SecurityUtils.kt`

Responsibilities:
- Build the authentication file paths.
- Ensure directories exist.
- Provide lightweight helpers (hashing, timestamps, base64).

Key points:
- `signatureFile(context)` resolves to `files/data/authentication/signature.json`.
- `credentialsFile(context)` resolves to `files/data/authentication/.credentials`.
- `loadOrCreateTimestamp` writes `credentials.timestamp` as a simple marker.

### `FileCredentialsStore`
File: `app/src/main/java/com/shary/app/infrastructure/persistance/credentials/FileCredentialsStore.kt`

Responsibilities:
- Read/write/delete `.credentials`.

Key points:
- `writeCredentials` overwrites the file in place.
- `deleteCredentials` deletes only `.credentials`.

### `CryptographyManagerImpl`
File: `app/src/main/java/com/shary/app/infrastructure/security/manager/CryptographyManagerImpl.kt`

Responsibilities:
- Derive identity keys on the fly.
- Encrypt/decrypt credentials for storage.
- Write `signature.json` with **public** data only.

Key points:
- Keys are derived from `(email, safePassword, appId)`.
- Only **public** keys are persisted in `signature.json`.
- The private seeds are never written to disk.

### `AuthenticationServiceImpl`
File: `app/src/main/java/com/shary/app/infrastructure/security/auth/AuthenticationServiceImpl.kt`

Responsibilities:
- Sign‑up / sign‑in flow.
- Persist encrypted credentials to disk.
- Keep current auth state in memory.

Key points:
- Uses `crypto.encryptCredentialsByDerivation(...)`.
- The encrypted blob is written to `.credentials`.
- On sign‑out it deletes `.credentials` only.

### File Import Services
Files:
- `app/src/main/java/com/shary/app/infrastructure/services/file/JsonFileServiceImpl.kt`
- `app/src/main/java/com/shary/app/infrastructure/services/file/ZipFileServiceImpl.kt`

Responsibilities:
- Copy/import JSON/ZIP data into private storage.
- Parse metadata and field payloads.

Key points:
- Some methods write directly to `files/` using `uri.lastPathSegment`.
- There is no uniqueness or overwrite protection in those paths.

## Observed Risks (Storage Robustness)

1. Non‑atomic writes can corrupt files if the app crashes mid‑write.
   - Affected: `.credentials`, `signature.json`, `credentials.timestamp`.
   - Risk: user lockout and invalid local auth state.

2. Sign‑out leaves some auth artifacts on disk.
   - `.credentials` is deleted, but `signature.json` and `credentials.timestamp` remain.
   - Risk: residual PII (email) and public keys persist on device.

3. File import uses unsafe names in some paths.
   - `uri.lastPathSegment!!` can be null and can overwrite existing files.
   - Risk: accidental data loss and crashes.

## Proposed Changes

### Change 1: Atomic Writes
Replace direct `writeText` / `writeBytes` with an atomic write pattern for:
- `.credentials`
- `signature.json`
- `credentials.timestamp`

Why:
- Protects against partial writes and corrupted files.

### Change 2: Optional Full Local Cleanup
On sign‑out, optionally delete:
- `signature.json`
- `credentials.timestamp`

Why:
- Ensures user data is fully removed if the user logs out.

### Change 3: Safer File Imports
Replace uses of `uri.lastPathSegment!!` with:
- `uniqueName(...)`
- A safe fallback for missing names

Why:
- Prevents overwrites and null crashes.

## Why These Changes Do Not Break The Device‑Centric Key Model

The device‑centric model here is:
- **Keys are derived on the fly** from (email, safePassword, appId).
- **Private keys are never stored** on disk.
- Only **public keys** and **encrypted blobs** are written to storage.

The proposed changes do **not** alter any of that. They only improve the safety of how data is written and removed:

- Atomic writes do not change the content or the cryptography.
- Cleanup only removes files that are already optional to the crypto flow.
- No new storage is introduced, no private key material is persisted.

In short, the approach stays:
- On‑device derivation is preserved.
- No persistent private keys are introduced.
- Storage remains strictly minimal and device‑local.

## Next Steps (If You Approve)
- Implement atomic writes via `AtomicFile` or a temporary file + rename strategy.
- Add deletion of `signature.json` and `credentials.timestamp` to sign‑out logic.
- Harden file import naming.
