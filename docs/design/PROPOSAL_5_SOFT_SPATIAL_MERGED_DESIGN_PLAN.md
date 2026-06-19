# Shary Frontend Redesign Plan
## Proposal 5 "Soft Spatial" — definitive merged plan (current codebase aligned)

## 1. Goal
Deliver the Proposal 5 frontend for the existing Android Compose app while preserving current product behavior.

Scope boundaries:
- Frontend only: theme, composables, screens, navigation presentation, animations, copy, accessibility.
- Do not modify domain, repositories, cloud/crypto logic, or persistence contracts unless a UI block makes it strictly necessary.
- Keep existing flows and side effects intact: auth, fields, users, requests, matching, summary send, password change.

## 2. Design inputs used
- `Design instructions_Proposal 5 Soft Spatial.md`
- `shary_proposal_5_soft_spatial.html`

Implementation baseline reviewed:
- `app/src/main/java/com/shary/app/ui/theme/*`
- `app/src/main/java/com/shary/app/ui/components/*`
- `app/src/main/java/com/shary/app/ui/screens/login/LoginScreen.kt`
- `app/src/main/java/com/shary/app/ui/screens/logup/LogupScreen.kt`
- `app/src/main/java/com/shary/app/ui/screens/field/FieldScreen.kt`
- `app/src/main/java/com/shary/app/ui/screens/user/UserScreen.kt`
- `app/src/main/java/com/shary/app/ui/screens/request/RequestScreen.kt`
- `app/src/main/java/com/shary/app/ui/screens/request/utils/AddRequestDialog.kt`
- `app/src/main/java/com/shary/app/ui/screens/utils/FieldMatchingDialog.kt`
- `app/src/main/java/com/shary/app/ui/screens/summary/SummaryFieldScreen.kt`
- `app/src/main/java/com/shary/app/ui/screens/summary/SummaryRequestScreen.kt`
- `app/src/main/java/com/shary/app/ui/screens/home/utils/AppNavigator.kt`
- `app/src/main/java/com/shary/app/ui/screens/home/utils/Screen.kt`

## 3. Current merge status (as of this branch)

## 3.1 Completed and aligned
- Proposal tokenized theme foundation is implemented:
- `Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Theme.kt`, `font_certs.xml`.
- New shared component layer exists and is in use:
- top bar, search bar, command dock, card set, review components, empty state, shimmer.
- Major screens migrated to Proposal shell:
- login/signup, fields, users, requests, summary field/request.
- Summary screens now use review-first composition and `Send securely` CTA.
- Visible auth copy is updated to `Sign in` and `Create Account`.

## 3.2 Partially aligned (functional, needs final design pass)
- Modal sheets still mostly legacy visual language:
- `FieldUiManagement.kt`, `UserEditorDialog.kt`, `AddRequestDialog.kt`.
- Field matching dialog remains legacy structure:
- `FieldMatchingDialog.kt` uses default Material controls/elevated cards and not full Proposal 5 two-column visual contract.
- Requests screen mode switcher behavior is correct, but transition/motion spec is not fully implemented.
- Request status/tag semantics are placeholder-like (`FieldTag.OTHER`) and not design-token mapped for pending/sent states.

## 3.3 Not yet implemented from proposal
- Navigation transition spec in NavHost (`fade + slide`) is not configured.
- Dedicated bottom `NavigationBar` for `Fields/Users/Requests` is not implemented.
- Full animation pass (`AnimatedContent` for mode switcher, prescribed timings everywhere) is incomplete.
- Full accessibility audit pass is incomplete (touch target and contentDescription consistency on all legacy utilities/dialog internals).

## 4. Merge decisions (definitive)
1. Keep existing ViewModel contracts and event collectors unchanged.
2. Keep route/file compatibility where needed (`logup` route may remain internal), but ensure UI copy is proposal-compliant.
3. Rebuild visual structure first; preserve business logic and event timing.
4. Retain current custom components and expand them instead of creating parallel duplicates.
5. Add navigation visuals (bottom bar + transitions) only in UI layer; no domain/service impact.
6. Treat existing untracked backend/domain files as out-of-scope for this redesign pass.

## 5. Remaining implementation plan (ordered)

## Phase A — Navigation and naming normalization
Objective: align route semantics and proposal navigation behavior without breaking flows.

Files:
- `app/src/main/java/com/shary/app/ui/screens/home/utils/Screen.kt`
- `app/src/main/java/com/shary/app/ui/screens/home/utils/AppNavigator.kt`
- optionally add `app/src/main/java/com/shary/app/ui/navigation/SharyNavGraph.kt` if split is preferred

Tasks:
- Keep `Screen.Logup` compatibility alias but introduce canonical `Screen.Signup`.
- Route login -> signup navigation through canonical route while preserving backward compatibility.
- Add proposal enter/exit transitions in NavHost.
- Add Material3 `NavigationBar` + `NavigationBarItem` visible only on `fields/users/requests`.
- Resolve command dock + bottom nav coexistence rule:
- bottom nav handles destination switching only.
- command dock handles contextual actions only.

Done criteria:
- Navigation behavior unchanged functionally.
- Bottom nav visible only on the 3 app sections.
- No duplicate destination controls that conflict with command dock actions.

## Phase B — Sheet/dialog visual system migration
Objective: restyle all modal input surfaces to Proposal 5 while preserving existing forms and validation behavior.

Files:
- `app/src/main/java/com/shary/app/ui/screens/field/components/FieldUiManagement.kt`
- `app/src/main/java/com/shary/app/ui/screens/user/components/UserEditorDialog.kt`
- `app/src/main/java/com/shary/app/ui/screens/request/utils/AddRequestDialog.kt`
- `app/src/main/java/com/shary/app/ui/screens/field/components/AddNewTagDialog.kt`
- `app/src/main/java/com/shary/app/ui/screens/field/components/UpdateTagDialog.kt`

Tasks:
- Apply global sheet contract:
- `shape = SharyRadius.sheet`
- `containerColor = SurfaceLight`
- drag handle pill style.
- Replace legacy `OutlinedButton/FilledTonalButton` footers with:
- `SharySoftButton("Cancel")`
- `SharyPrimaryButton("Save"/"Add")`.
- Keep existing required/advanced section behavior and validation messaging.
- Ensure text labels and helper text use `SharyTypography` roles and proposal colors.

Done criteria:
- All bottom sheets match the same visual grammar.
- Existing callbacks/validation paths remain unchanged.

## Phase C — Field matching dialog redesign
Objective: recompose matching UI to proposal structure with current matching logic preserved.

File:
- `app/src/main/java/com/shary/app/ui/screens/utils/FieldMatchingDialog.kt`

Tasks:
- Keep current matching, undo/redo, and add-field behavior.
- Recompose as full Proposal dialog:
- two equal columns (`Stored` / `Requested`) with central divider.
- top progress indicator with proposal colors.
- numbered match badges on both sides.
- footer buttons as soft + primary.
- replace elevated defaults with flat card + border style.

Done criteria:
- Accept stays disabled until complete match.
- Undo/redo/add still work exactly as before.
- Visual style matches Proposal 5 token system.

## Phase D — Requests screen parity pass
Objective: complete visual and interaction parity for requests workflow.

Files:
- `app/src/main/java/com/shary/app/ui/screens/request/RequestScreen.kt`
- `app/src/main/java/com/shary/app/ui/components/RequestCard.kt`

Tasks:
- Add `AnimatedContent(targetState = listMode)` for mode transitions.
- Refine status chip semantics for received/sent/pending states using tokenized colors.
- Ensure active card style exactly follows selected border/background rule.
- Ensure empty states and primary actions match mode-specific proposal wording.

Done criteria:
- Received and sent modes are visually distinct and animated per spec.
- Match/add paths unchanged.

## Phase E — Motion and accessibility closure
Objective: complete formal animation + a11y requirements from proposal.

Files:
- `app/src/main/java/com/shary/app/ui/components/*`
- `app/src/main/java/com/shary/app/ui/screens/*`

Tasks:
- Standardize timing:
- `tween(180)` interaction color changes.
- `tween(220-340)` for screen or larger transitions per spec intent.
- Audit all icon-only controls for non-null `contentDescription`.
- Ensure touch targets >= 48dp via `minimumInteractiveComponentSize()` where visually smaller controls exist.
- Verify all `OutlinedTextField` use labels (not placeholder-only semantics).
- Verify text contrast on `Violet600` backgrounds is white.

Done criteria:
- No accessibility regressions in migrated screens.
- Animations are consistent and non-decorative (state-change only).

## Phase F — Cleanup and final consistency
Objective: remove dead UI pieces and avoid conflicting patterns.

Files:
- `app/src/main/java/com/shary/app/ui/screens/summary/utils/SummaryTopAppBar.kt`
- any remaining legacy utility components no longer referenced by migrated screens

Tasks:
- Remove or deprecate unused summary top app bar and stale helpers.
- Confirm no user-facing `Logup`/`Summary` legacy labels remain.
- Keep internal compatibility symbols only where necessary.

Done criteria:
- Clean compile with no new warnings introduced by redesign.
- UI surface is coherent and proposal-consistent.

## 6. Verification protocol
1. Build compile gate:
- `./gradlew :app:compileDebugKotlin`

2. Visual smoke checks:
- Login + signup hero, text fields, CTA labels.
- Fields list: search/filter/selection/delete/review.
- Users list: add/edit/copy/select/review.
- Requests: mode switch, received selection, sent draft add.
- Matching dialog: pair, undo/redo, accept.
- Summary field/request: recipient, rows, send method, send action.

3. Regression checks:
- Password-change flow still redirects to login with snackbar context.
- Cloud fetch/upload triggers unchanged.
- Email send intent still emitted.

## 7. Acceptance checklist
- Proposal token system is the active visual system for all migrated surfaces.
- No user-visible `Logup` label remains.
- All modal sheets/dialogs used in primary flows follow the same Proposal 5 structure.
- Matching dialog is fully proposal-styled without behavior loss.
- Requests mode interactions and visual states are complete.
- Navigation transitions and section bottom navigation are implemented and stable.
- Accessibility requirements are satisfied for migrated surfaces.
- Backend/domain/persistence behavior is unchanged.

## 8. Non-goals
- Domain model redesign.
- Repository/service contract changes.
- Crypto, cloud protocol, or persistence schema changes.
- New product capabilities outside current ViewModel use cases.

## 9. Execution recommendation
Implement remaining phases in order A -> F. This minimizes regression risk by stabilizing navigation and shared modal patterns before finishing motion/a11y and cleanup.
