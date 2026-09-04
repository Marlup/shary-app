# Scope

Implement the recovered `inbox-fromuser` Android UI/accessibility correction plan and the follow-up requirement to add an explicit MCP capability toggle that defaults to `false`.

## Change list

1. Added `Screen.Connectors` and `SectionTab.CONNECT`, wired into the shared bottom navigation and app navigator.
2. Added a dedicated `ConnectorsScreen` that owns MCP capability enablement, endpoint visibility, pending approval/renewal counts, grant revocation, and recent activity.
3. Removed persistent assistant-connector management from `SettingsScreen` and replaced it with a simple handoff to the new `Connect` tab.
4. Added the same overflow entrypoint on `Fields`, `Users`, `Requests`, and `Connect` for `Settings`, password change, and logout.
5. Introduced `SharySectionActionRow` and moved non-contextual actions out of the top-right icon cluster.
6. Added persisted `mcpEnabled` app settings, defaulting to `false`, and gated remote MCP deep-link approval, background sync, notifications, and dialog surfacing behind it.
7. Increased shared target sizes and contrast in `SharyIconButton`, `SelectionPill`, bottom navigation, search, cloud status, and card metadata/selection affordances.
8. Added focused Android UI tests for the 4-tab nav, `SharyIconButton`, and `ConnectorsContent`.

## Plan source

`[[docs/backlog/works/2026-07-04/inbox-fromuser-accessibility-ui-correction-plan.md]]`

## Viability/complexity per change

1. Connect tab and connector screen: medium. Navigation and state assembly were localized.
2. MCP capability toggle: medium. Required both persisted settings and remote coordinator/worker gating.
3. Shared overflow/action-row refactor: medium. Needed consistent behavior across three existing tabs plus the new tab.
4. Shared accessibility primitive fixes: medium. Most of the impact comes from changing a small number of reused components.

## Design decisions

1. The MCP master switch uses `SettingsRepository` state and `RemoteMcpCoordinator.applyCapabilityChange(...)` so the UI and background execution share one truth source.
2. Disabling MCP does not revoke existing grants automatically; it stops approvals, sync, and modal prompts until re-enabled.
3. Password change remained available from every primary tab by reusing `ChangePasswordDialog` and the existing `FieldViewModel` password flow rather than duplicating security logic.
4. The dedicated `ConnectorsContent` composable was extracted so the new MCP UI can be exercised in Android tests without Hilt navigation setup.

## Implementation plan

1. Persist and gate MCP capability.
2. Add `Connect` navigation and the dedicated management screen.
3. Move shared/global actions into section rows and shared overflow menus.
4. Harden shared Compose primitives for target size and contrast.
5. Compile, compile Android tests, and run focused unit coverage.

## Validation plan

1. Run `./gradlew :app:compileDebugKotlin`.
2. Run `./gradlew :app:compileDebugAndroidTestKotlin`.
3. Run a focused unit-test slice for stable helper coverage.
4. Run the new focused instrumentation tests on an attached device or emulator.
5. Run the requested manual Accessibility Scanner and TalkBack pass on device.

## Result summary

The Android app now has a dedicated `Connect` tab for assistant connectors and MCP, a default-off MCP capability toggle, unified overflow access on every primary tab, less crowded top bars, and stronger shared accessibility primitives. Source compilation, Android-test source compilation, focused unit tests, APK installation on `emulator-5554`, and the focused connected instrumentation slice all passed. The remaining validation gap is the manual Accessibility Scanner and TalkBack pass, which still requires interactive on-device review rather than ADB-only automation.
