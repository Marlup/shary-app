# Application Layer

## Purpose

This folder contains the app-level orchestration layer between UI/viewmodels
and lower-level domain or infrastructure services.

## Submodules

- `cloud/`
  - Cloud inbox/import planning, terminal delivery reduction, and persisted
    outgoing-Request/received-response receipt mapping.
- `connector/`
  - Connector pairing, policy, preview/commit, session refresh, and connector
    lifecycle management.
- `email/`
  - Idempotent Android share-sheet handoff state; delivery is deliberately not
    inferred from chooser launch.
- `request/`
  - Pure received-response and outgoing-request workflow state transitions.
- `security/`
  - Authentication and credential-rewrap use cases.
- `session/`
  - Shared post-login field/user loading and background session timeout.
- `usecases/`
  - Reserved for future cross-domain use cases; currently empty.

## How to use these docs

Read the nearest submodule `README.md` before opening code when the task is
module-specific. These files are intended to give agents bounded retrieval
surfaces for module purpose, file ownership, dependencies, and update triggers.

## Update triggers

Update this file when:

- a new submodule is added or removed;
- responsibility shifts between submodules;
- `usecases/` stops being an empty placeholder.
