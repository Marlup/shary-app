# Session Application Module

## Purpose

Own authenticated-session data preloading and process-background session
closure.

## Responsibilities

- `SessionDataCoordinator` decrypts fields progressively, loads users
  concurrently, publishes shared screen state, and clears it on logout.
- `SessionTimeoutCoordinator` observes the application process lifecycle,
  closes sessions after five background minutes, and emits the navigation-reset
  event.
- `ClipboardCleanupCoordinator` owns clipboard expiry outside Compose, marks
  sensitive Field clips for Android, and clears the current Shary-owned clip on
  process background or authentication teardown without clearing a newer
  external clip.
- Successful password changes clear `SessionDataCoordinator` before the shared
  runtime authentication teardown and navigation reset.
- `INITIAL_FIELD_BATCH_SIZE` is the login-to-Fields readiness threshold.

## Security Rules

- Failed field decryptions are counted and omitted; ciphertext is never
  promoted to display state.
- Backgrounding immediately clears screen-local sensitive reveal state.
- Backgrounding and every runtime authentication teardown immediately clear the
  current Shary-owned clipboard item; navigation cannot cancel its configured
  expiry timer.
- Timeout, manual logout, password-change relogin, and failed sign-in use the
  same runtime authentication/cache/crypto/cloud cleanup path.
- Login and sign-up clear progressively decrypted session data before the
  account-replacing transition and clear it again when the attempt fails.
- Authentication transitions are serialized in the service and duplicate UI
  submissions are ignored while one transition is active.
- Runtime teardown does not delete the durable local vault. A different account
  still requires an explicit vault-owner policy before cross-account persistence
  isolation can be considered complete.
- Runtime cryptographic teardown zeroes cached private signing and key-exchange
  seeds before releasing the identity object.

## Update Triggers

Update this file when readiness thresholds, session timeout semantics, startup
data ownership, or logout cleanup changes.
