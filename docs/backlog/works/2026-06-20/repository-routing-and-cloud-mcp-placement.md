# Repository routing and cloud MCP placement

## Scope

Record the important Shary repository paths and evaluate where the
Firebase-backed remote MCP gateway should live.

## Change list

1. Added the Android, backend, and workspace paths to root `AGENT.md`.
2. Added matching root guidance to the backend repository.
3. Evaluated the current Firebase backend structure and 1,234-line functions
   entrypoint.
4. Selected the existing backend repository with a separate deployable MCP
   codebase/module.
5. Defined incremental refactor boundaries for the existing backend and shared
   Android connector dispatcher.

## Decision

The remote MCP gateway belongs in:

`C:\main_work\shary-project\shary-backend`

It should not be added directly to the existing monolithic
`firebase/functions/main.py`. The backend design recommends
`firebase/mcp-functions` as an independently deployable Firebase codebase,
while retaining shared schemas and operational ownership in the same repo.

Android local MCP and Firebase command execution must reuse the same
transport-neutral connector dispatcher, policy, idempotency, persistence, and
audit path.

## Validation plan

1. Update the Android policy manifest after changing `AGENT.md`.
2. Run the Android session-init integrity gate.
3. Run backend tests because backend code was not changed but repository
   guidance and architecture were added.
4. Run formatting and secret-pattern checks in both repositories.

## Result summary

Repository routing and the cloud MCP placement/refactor decision are now
durable in both owning repositories. No runtime implementation was changed.
