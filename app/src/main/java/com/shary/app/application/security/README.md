# Security Application Module

## Purpose

This module contains application-layer authentication and credential lifecycle
flows for Shary.

## Files and responsibilities

### `LoginUseCase.kt`

- Derives a local identity from username, password, and app ID.
- Requests a backend challenge.
- Signs the challenge locally with Ed25519.
- Sends the signature for backend verification.

### `RegisterUseCase.kt`

- Derives local identity material for a new user.
- Uploads public signing and key-exchange material through the auth backend.

### `ChangePasswordAndRewrapDataKeyUseCase.kt`

- Re-derives local keys from the old and new passwords.
- Decrypts credentials using the old KEK.
- Rewraps stored field/request data under the new KEK.
- Preserves stable request IDs and authenticated requester identity while
  rewriting request records.
- Writes the new credentials blob and rolls back local state if persistence
  fails.

## Inputs and outputs

- Inputs:
  - username/email/password values;
  - current authentication state;
  - credential storage and encrypted data snapshots.
- Outputs:
  - authentication success/failure;
  - updated encrypted local data and credentials.

## Dependencies

- `AuthBackend`
- `CryptographyManager`
- `AuthenticationService`
- `CredentialsStore`
- `FieldCodec`
- field/request DataStores

## Agent notes

- This module is about auth and local cryptographic state transitions, not MCP
  policy or cloud inbox review.
- `ChangePasswordAndRewrapDataKeyUseCase` is the main retrieval target for
  password-change side effects and rollback behavior.

## Update triggers

Update this README when:

- login or registration flow semantics change;
- password-change logic changes its rollback or storage behavior;
- new security use cases are added.
