# Scope

Recover and persist the Android UI/accessibility correction plan that was provided in chat for the `inbox-fromuser` Accessibility Scanner review, then use it as the durable plan source for implementation work on `2026-07-04`.

## Plan Summary

1. Add a first-class `Connect` bottom-tab destination for MCP/connectivity management.
2. Move assistant connector management out of `Settings` and into a dedicated screen.
3. Add the same overflow entrypoint on every main tab for `Settings`, password change, and logout.
4. Relocate non-contextual tab actions into a shared section action row below the top bar.
5. Raise interactive targets to at least `48.dp` for shared icon/chip/selection controls.
6. Improve contrast on shared secondary text and actionable labels on light surfaces.
7. Keep remote MCP approval and renewal dialogs modal for urgent flows.
8. Add a user-controlled MCP capability toggle with default `false`.

## Acceptance Direction

1. `Connect` is reachable from every primary tab through bottom navigation.
2. MCP capability is disabled by default and must be explicitly enabled before approvals, background sync, or grant execution proceed.
3. The repeated shared Accessibility Scanner failures should be reduced by fixing shared primitives rather than per-screen one-offs.
4. Validation should include Kotlin compile, Android-test source compile, and manual scanner/TalkBack follow-up on device.
