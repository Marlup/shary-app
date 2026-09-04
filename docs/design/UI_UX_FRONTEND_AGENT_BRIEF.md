# Shary UI/UX Frontend Design Brief

## Purpose

This document defines the desired UI and UX direction for the entire Shary Android application so a frontend-specialized agent can redesign the interface without changing the core product behavior.

The goal is not a superficial polish. The goal is a coherent, premium-feeling, privacy-first product UI with clearer hierarchy, more intuitive actions, and stronger consistency across all screens, dialogs, states, and components.

This brief is based on the current application structure and flows implemented in the codebase.

## Product Summary

Shary is a privacy-oriented Android app for:

- storing structured personal fields
- managing users/recipients
- creating requests for missing fields
- reviewing a summary before sending data
- sending data through email or cloud-related flows

At a UX level, the app is a structured data exchange tool, not a generic CRUD app. The design should communicate:

- trust
- clarity
- precision
- lightweight control
- secure handling of personal information

## Primary UX Problems To Solve

The redesign should explicitly address these issues in the current app:

- too many screens rely on generic Material controls with weak visual hierarchy
- key actions are compressed into small icon buttons with low discoverability
- list screens are functional but visually flat
- summaries look like simple tables instead of confirmation checkpoints
- authentication screens do not establish a strong product identity
- theme switching exists, but the overall visual language is not unified enough
- several important actions are only understandable after reading tooltip text
- selection mode behavior is implicit instead of visibly stateful
- empty states, success states, and destructive states are under-designed

## Non-Negotiable Functional Constraints

The redesign must preserve these existing product flows:

- login
- signup
- fields management
- users management
- requests management with two modes: `Received` and `Sent`
- field summary flow
- request summary flow
- modal-bottom-sheet editing flows for create/update
- field matching dialog for matching requested fields against stored fields
- theme selection support
- password change flow
- logout flow
- loading overlays, snackbars, and confirmation feedback

The redesign may change layout, hierarchy, iconography, sizing, color usage, copywriting, and interaction patterns, but should not remove supported capabilities.

## Design Direction

### Core Visual Positioning

The app should feel like a modern privacy workspace for personal data exchange.

Preferred tone:

- calm but not bland
- secure but not corporate
- efficient but not cold
- structured but not rigid

The UI should avoid:

- default purple Material look
- plain white screens with isolated buttons
- weak spacing rhythm
- tiny icons as the primary communication method
- “developer tool” visual roughness

### Proposed Aesthetic

Use a visual language built around:

- soft but intentional surfaces
- layered cards with subtle depth
- rounded geometry with disciplined spacing
- accent colors used sparingly for state and action
- typography with more personality than default Material text

Recommended baseline visual system:

- primary palette: slate, deep teal, warm gray, muted sand, or petrol blue
- accent palette: copper, coral, citron, seafoam, or electric cyan depending on state
- background treatment: slightly tinted backgrounds, soft gradients, or low-contrast texture
- surface strategy: one clear page background, one elevated card surface, one interactive active-state surface

## Brand and Personality

Shary should feel like:

- a trusted private locker
- a structured exchange desk
- a clean personal data workspace

Keywords:

- private
- portable
- intentional
- legible
- efficient
- exact

## Typography

Do not use a generic-looking default stack as the central design voice.

Typography goals:

- strong screen titles
- compact but readable metadata
- differentiated labels vs values
- clear emphasis for sensitive actions and review states

Recommended structure:

- display/title font with distinct character for authentication and top-level screens
- neutral, highly legible body font for lists, forms, and values
- mono or semi-mono treatment only where typed values, aliases, references, or keys benefit from precision

Suggested hierarchy:

- screen title: prominent, confident, short
- section title: medium weight, clear spacing above
- row title: medium emphasis
- row secondary text: softer contrast
- labels: compact uppercase or semibold small caps where appropriate
- support/error text: concise and visually secondary

## Layout Principles

- prefer strong vertical rhythm over dense packing
- maintain large touch targets
- avoid placing more than 3 equally weighted primary actions in one row when possible
- use clear separation between browsing mode and selection mode
- use persistent bottom action regions only for context-specific actions
- every screen should expose one obvious primary action

Spacing system:

- 4dp for micro-spacing only
- 8dp base rhythm
- 12dp or 16dp for component internals
- 20dp to 24dp for section gaps
- 28dp to 32dp for large screen transitions or hero spacing

Shape system:

- cards: medium-large radius
- sheets/dialogs: large radius
- pills/chips: fully rounded where they indicate status, filters, or tags
- destructive actions should not rely on shape alone; color and placement must reinforce intent

## Global Interaction Model

### Screen Shell

Most app screens currently use a common scaffold:

- top app bar with centered title
- optional top-right actions
- optional search/filter row under the app bar
- main content area
- bottom action bar
- snackbar feedback

Design target:

- evolve this into a more intentional shell with stable spacing and navigation behavior
- top bar and search region should feel visually connected
- bottom action bar should feel like a contextual command dock, not a row of detached icons

### Action Priority

Every screen should clearly distinguish:

- primary action
- secondary navigation action
- destructive action
- contextual row actions

Current app issue:

- many actions use equally styled compact icon buttons

Design target:

- one primary action should stand out
- destructive actions should be isolated and visually heavier
- navigation shortcuts should be lower emphasis than task completion actions
- summary/review actions should look like progression, not just another icon

### Selection Mode

Fields and users currently reveal extra row actions when selected.

Design target:

- make selection mode visible at the screen level
- show count of selected items
- animate transition into selection mode
- expose available actions in a sticky contextual toolbar or card dock
- clearly show how to deselect or clear selection

### Feedback

Use more structured feedback for:

- success
- warning
- destructive completion
- offline/cloud failure
- empty search
- no data available

Snackbar alone is not sufficient for all cases. Use inline messages, empty-state illustrations, or persistent banners where appropriate.

## Information Architecture

Current routes:

- `Logup`
- `Login`
- `Fields`
- `Users`
- `Requests`
- `SummaryField`
- `SummaryRequest`

There is also a `Home` screen in code, but the practical main post-login workspace is `Fields`.

Recommended product structure:

1. Authentication
2. Main workspace
3. Review and send

Recommended navigation model for redesign:

- keep top-level destinations: Fields, Users, Requests
- strongly consider using a bottom navigation bar or segmented persistent nav for these 3 main areas
- keep summary screens as modal progression or full review screens outside top-level nav
- keep auth screens isolated from workspace shell

## Screen-by-Screen Design Specification

## 1. Login Screen

### Purpose

Authenticate returning users and re-enter after password change.

### Current Elements

- centered title `Shary`
- logout icon on the left
- theme picker icon on the right
- email field
- password field
- login button
- loading overlay
- snackbar message when password changed

### UX Intent

This should feel like the front door of a secure personal data tool.

### Design Requirements

- build a strong branded hero section
- communicate privacy and trust immediately
- make the form feel compact, stable, and calm
- treat the login CTA as the main focal point
- de-emphasize top-bar utility controls compared to the form

### Suggested Layout

- upper third: product identity, short subtitle, possibly a trust statement
- middle: card or panel containing form
- lower area: secondary actions such as theme and sign-up link

### Required Elements

- email input
- password input with show/hide affordance
- primary `Login` button
- clear route to signup
- visible feedback if login fails
- visible confirmation if password was changed previously

### UX Notes

- replace the top-left logout icon with a less confusing auth-safe control or remove it from login if it is not meaningful before session start
- add a secondary text link like `Create account`
- loading state should disable form and convert CTA label to progress state

## 2. Signup Screen

### Purpose

Create a new account and initialize cloud/user identity.

### Current Elements

- top app bar title `Logup`
- email field
- username field
- password field
- confirm password field
- create account button
- snackbar and toast-based event feedback

### UX Intent

This should feel welcoming and straightforward, not technical.

### Design Requirements

- rename visible label from `Logup` to `Create Account` or `Sign Up`
- make password requirements more visible if validation exists
- reinforce why account creation matters in product terms
- separate required data from advanced/implicit backend processes

### Suggested Layout

- identity header matching login visual family
- vertically stacked form card
- optional helper copy under username and password
- clear footer link back to login

### UX Notes

- user should not see backend cloud-session complexity
- toasts about cloud anonymous session should not dominate the experience
- progress and success states should feel human and product-focused

## 3. Fields Screen

### Purpose

Main data workspace. Store, browse, search, select, edit, copy, fetch, and prepare fields for sharing.

### Current Elements

- title `Fields`
- sort menu
- overflow menu with theme picker, change password, logout
- search bar with filter cycling
- list of field cards
- row selection reveals quick actions
- bottom action dock with delete, add, fetch, users, requests, summary
- add/edit/copy dialogs
- password-change dialog

### UX Intent

This is the heart of the app and should feel like a high-control inventory workspace.

### Design Requirements

- make this the best-designed screen in the app
- elevate the field list into a premium information browser
- show tags, aliases, types, and values with better structure
- make selected state visually unmistakable
- reduce ambiguity between navigation actions and task actions

### Content Hierarchy For Each Field Row

Each field row should ideally expose:

- key as the primary label
- current value as secondary line
- tag as metadata chip or badge
- alias as tertiary metadata or reveal-on-expand detail
- optional type indicator for typed values such as list, file, questionnaire, json, yaml

### Row Interaction Requirements

- tap behavior must be explicit: either select row, open details, or expand actions, but not feel ambiguous
- if selection remains tap-based, show immediate selection feedback with stronger visual treatment
- quick actions should be easier to parse than icon-only buttons
- row actions:
- edit
- copy content
- duplicate field
- show alias/details when present

### Bottom Action Dock Requirements

Current actions:

- delete selected fields
- add new field
- fetch fields from cloud
- go to users
- go to requests
- go to summary if users are selected

Recommended redesign:

- treat `Add field` as primary CTA
- group navigation shortcuts separately from field actions
- make `Summary` look like `Review and Send`
- isolate `Delete` as destructive with label support
- prefer text+icon buttons or segmented command cards over tiny floating circles

### Search and Sorting

- search should support direct query entry with explicit filter chip or dropdown
- current filter-cycling behavior is too hidden; use visible chips or dropdown selector
- sort should show current criterion and direction

### Empty State

If no fields:

- explain what a field is
- show one primary CTA: `Create your first field`
- show 1-2 examples such as passport number, emergency contact, insurance code

### Loading and Fetch States

- fetching from cloud should show inline state, not only overlay/snackbar
- if data was fetched, surface result count and highlight new items

## 4. Add / Update / Copy Field Sheets

### Purpose

Create and edit structured fields, including typed-value specs.

### Current Elements

- required section: key and value
- advanced options:
- alias
- tag picker
- typed value toggle
- type selector
- type-specific options
- typed list builder
- typed questionnaire answer builder
- typed file picker

### UX Intent

This editor should feel like a smart structured form, not a raw database editor.

### Design Requirements

- keep modal bottom sheet pattern
- separate simple mode from advanced mode visually
- use clear sectional grouping
- show lightweight inline explanations for typed values
- typed controls should feel like feature modules, not hidden extra fields

### Required Behavior

- key suggestions must remain available
- list type must preserve item-builder flow with `+`
- questionnaire type must preserve answer-builder flow with `+`
- file type must preserve Android document picker integration
- alias and tag must remain optional

### Improved UX Suggestions

- display a compact preview panel of the encoded field meaning, not raw contract JSON
- for typed values, show a small “How this behaves” helper block
- for JSON/YAML, use multiline editor with validation state and format button
- for file type, show selected filename, MIME, size, and option to replace/remove
- for list and questionnaire, allow drag reorder later if feasible

### Validation

- required fields should validate inline
- destructive recovery actions should be visually separated from save
- typed editors should validate at component level, not only on save

## 5. Tag Picker and Tag Dialogs

### Purpose

Assign and manage field categories.

### Design Requirements

- tags should be visually beautiful, not only functional
- present tags as color-coded chips with clear selected state
- adding or editing a tag should feel like editing a design token
- color selection should feel curated rather than arbitrary

### UX Notes

- tags are a strong opportunity to make the app feel organized and premium
- use chips, tonal fills, and elegant color previews

## 6. Users Screen

### Purpose

Manage recipients or participant users for sending and matching flows.

### Current Elements

- title `Users`
- search row with attribute filter
- list of selectable users
- bottom action dock with delete, add, go to fields, go to requests, summary
- add and copy user sheets

### UX Intent

This screen should feel like a lightweight contact book with task context.

### Design Requirements

- make users feel like recipients, collaborators, or trusted endpoints
- emphasize selected recipients when preparing summary/send flows
- distinguish owner/current user from saved recipients if applicable

### Row Design

Each user row should show:

- username as primary
- email as secondary
- optional status chip such as owner, selected, available

### Action Design

Current row quick actions:

- edit
- copy
- add copy

Recommended:

- present actions via swipe reveal, trailing menu, or expanded card actions
- avoid making copy actions visually equal to edit unless usage data supports it

### Empty State

- explain why users matter: fields and requests are sent to users
- CTA: `Add recipient`

## 7. User Editor Sheets

### Purpose

Create or edit a user.

### Required Fields

- username
- email

### Design Requirements

- keep the sheet simple and fast
- stronger input guidance for valid email
- support duplicate/copy flow without confusion

## 8. Requests Screen

### Purpose

Manage outgoing requests for information and incoming requests from others.

### Current Elements

- title `Requests`
- segmented control: `Received` / `Sent`
- top list of requests
- for `Sent`, lower list of drafted fields
- bottom action dock:
- delete drafted fields
- add request or match request depending on mode
- fetch requests from cloud
- navigate to users
- navigate to fields
- summary
- `Received` mode can launch field matching dialog

### UX Intent

This screen should feel like a workflow board, not just another list.

### Design Requirements

- visually differentiate `Received` and `Sent` modes
- make mode switch feel significant
- clarify the difference between a request object and drafted requested fields
- improve the hierarchy between upper and lower panels in `Sent`

### Received Mode UX

User goal:

- review incoming requests
- select one request
- match requested keys to stored local fields

Design target:

- display each request as a request card
- show number of requested keys
- show sender/recipient metadata if available in the future
- selected request should become obviously active

### Sent Mode UX

User goal:

- define keys they want from others
- review drafted request content
- move to summary/send flow

Design target:

- top panel: outgoing request history
- bottom panel: current drafted request
- primary action: `Add requested field`
- summary CTA should read like `Review Request`

### Bottom Dock Requirements

Do not make all actions equal. Use:

- primary action for mode-specific creation/matching
- secondary nav shortcuts
- destructive delete separated
- review CTA emphasized as progression

### Empty States

Received mode:

- `No received requests yet`
- explain where requests come from

Sent mode:

- `No drafted request yet`
- CTA to add the first requested key

## 9. Add Request Sheet

### Purpose

Create a requested key for the current draft request.

### Current Elements

- key with suggestions
- optional alias in advanced section

### Design Requirements

- preserve key suggestion behavior
- frame this as asking for information, not creating a normal field
- example helper copy would improve clarity

## 10. Field Matching Dialog

### Purpose

Match incoming requested fields to the local stored fields before fulfilling a request.

### Current Elements

- dialog layout
- left column: stored fields
- right column: requested fields
- click-to-match behavior
- matched progress chip and progress bar
- undo/redo
- add local field
- accept/cancel

### UX Intent

This is one of the app’s most specialized flows and should feel like a high-confidence mapping workspace.

### Design Requirements

- this should look distinct from ordinary dialogs
- preserve two-column matching model
- make relationship-building between sides visually obvious
- matched items should show clear linked state
- progress should be prominent

### Recommended Enhancements

- add directional visual connectors or numbering badges
- make selected left/right items glow or elevate
- show a status legend: unmatched, selected, matched
- keep undo/redo highly visible
- make `Accept` appear only when complete or show why it is disabled

### Required Actions

- cancel
- accept
- add local field
- undo
- redo
- select/match entries

## 11. Summary Screens

Two summary screens exist:

- field summary
- request summary

### Purpose

Review what will be sent and to whom before sending through email or cloud.

### Current Elements

- top app bar
- simple two-column table: emails and fields
- floating navigation buttons back to related screens
- share button opening send method dialog

### UX Intent

These should feel like review-and-confirm checkpoints.

### Design Requirements

- make summaries visually important
- replace simplistic tables with review cards or grouped sections
- show recipients and payload in a more human way
- make `Send` feel like a final action, not just another button

### Suggested Structure

- top: summary title and context sentence
- section 1: recipients
- section 2: fields/requested keys
- section 3: send method action
- sticky footer or bottom CTA for send/review

### Required Actions

Field summary:

- go to fields
- go to users
- open send method chooser

Request summary:

- go to fields
- go to users
- go to requests
- open send method chooser

### UX Notes

- current floating vertical action stack should be reconsidered
- back-navigation shortcuts may be better as top chips or secondary footer actions
- send action should be the visual primary CTA

## 12. Send Method Dialog

### Purpose

Let the user choose how to send data.

### Current Elements

- selectable list of send options
- cancel
- send

### Design Requirements

- treat options as method cards, not generic buttons
- include icon and short explanation for each method
- selected state should be obvious
- `Send` must remain disabled until a method is chosen

### Suggested Options Presentation

For each send method:

- icon
- title
- one-line description
- optional note on what happens next

## 13. Home Screen

### Status

There is a simple home screen in code, but it is not the main navigational backbone today.

### Recommendation

Either:

- remove it from the future UX narrative, or
- redesign it as a real dashboard

If kept, it should become a dashboard with:

- current workspace snapshot
- quick stats
- shortcuts to Fields, Users, Requests
- recent activity or pending request count

## Buttons and Interaction Inventory

This section should guide the frontend agent on how to treat buttons across the app.

### Primary Buttons

Use for:

- Login
- Create Account
- Add Field
- Add User
- Add Request
- Review and Send
- Send
- Save changes

Visual behavior:

- highest contrast
- full label, not icon-only
- clear pressed and disabled states

### Secondary Buttons

Use for:

- Cancel
- Back to related screen
- Replace file
- Open theme picker

### Destructive Buttons

Use for:

- Delete selected items
- Logout
- Clear drafted request

Visual behavior:

- color and icon both indicate risk
- physically separated from primary progression actions

### Tertiary / Icon Buttons

Use for:

- row-level edit
- copy
- duplicate
- overflow menu
- undo/redo

Rules:

- never rely on icon-only controls for critical meaning without strong context
- when many icon actions are grouped, support labels, chips, or tooltip-on-press behavior

## Shared Components Design Spec

## App Bars

- should feel brand-consistent across screens
- support large-title feeling without wasting height
- screen title must clearly dominate utility icons

## Search Row

- should look like a filterable command bar
- search input and filter selector should feel like one control family
- active filter should be visible without needing interaction

## Cards

Used for:

- fields
- users
- requests
- drafted request items
- summary sections

Card rules:

- clear surface separation from page background
- one highlighted leading edge or chip for status
- enough padding for scanning
- selected state must be obvious and elegant

## Bottom Action Bar / Command Dock

- should read as a command center
- use grouped actions, not evenly weighted tiny circles
- preserve thumb accessibility
- highlight the primary next-step action

## Dialogs and Bottom Sheets

- use large rounded corners
- strong title and sectioning
- clear primary/secondary CTA placement
- avoid excessive form density
- preserve scrollability on smaller screens

## Chips and Pills

Use for:

- tags
- selected count
- filter mode
- request state
- match progress
- type indicators

## States and Motion

### Required States

Every redesigned screen should define:

- default
- loading
- empty
- error
- success feedback
- selected mode where relevant
- disabled CTA state

### Motion Guidance

Use motion sparingly but intentionally:

- screen load: slight rise/fade
- list item selection: color and elevation transition
- contextual action reveal: slide/fade
- bottom-sheet opening: smooth upward transition
- mode switch on requests: content transition, not abrupt swap

Avoid:

- decorative motion without meaning
- excessive bounce
- animation that delays task completion

## Accessibility and Usability Requirements

- support one-handed use for primary flows
- maintain large touch targets
- preserve readable contrast in all theme variants
- do not use color as the only signifier of selected/destructive states
- ensure typed-value builders are obvious for keyboard users
- keep important labels textual, not icon-only

## Copywriting Guidance

The UI text should be:

- short
- concrete
- calm
- not overly technical

Prefer:

- `Review and Send`
- `Add recipient`
- `Requested field`
- `Selected: 3`
- `No fields yet`

Avoid:

- vague labels like `Summary`
- awkward labels like `Logup`
- backend-oriented copy in user-facing flows

## Theme System Guidance

The app already supports multiple themes. The redesign should:

- create one strong default theme first
- ensure all core components work beautifully in that theme
- then adapt the system for alternate palettes

Do not design ten unrelated visual styles. Build one robust design system that can absorb multiple colorways.

Theme expectations:

- semantic role colors must remain stable
- destructive, success, info, and selected states must be clear in every palette
- extended colors can remain, but should be organized around component usage rather than arbitrary color buckets

## Functional UX Opportunities To Introduce

These are recommended improvements that respect the existing product:

- visible selected-count bar on Fields and Users screens
- labels on major bottom actions
- better distinction between browse mode and selected mode
- richer preview for typed fields
- recipient count and field count chips on summary screens
- clearer request cards with sender/date metadata when available
- stronger empty-state education
- inline helper text for advanced typed-value editors

## Deliverables Expected From A Frontend Agent

The frontend design agent should produce:

1. A full visual redesign proposal for all app screens.
2. A component system for app bars, cards, sheets, chips, forms, command docks, and summary sections.
3. Screen mockups or Compose implementations for:
- Login
- Sign Up
- Fields
- Field Editor Sheet
- Users
- User Editor Sheet
- Requests
- Add Request Sheet
- Field Matching Dialog
- Summary Field
- Summary Request
- Send Method Dialog
4. Clear state handling for loading, empty, error, and selection modes.
5. A navigation model that improves discoverability between Fields, Users, and Requests.

## Final Implementation Priorities

If redesign work must be phased, prioritize in this order:

1. Login and Signup identity
2. Fields screen and field editor
3. Users screen and user editor
4. Requests screen and matching flow
5. Summary screens and send dialog
6. Theme-system cleanup and harmonization

## Summary For The Frontend Agent

Build Shary as a privacy-first personal data workspace.

The redesign must:

- feel premium and intentional
- improve discoverability of actions
- clarify selection and progression flows
- make Fields, Users, and Requests feel like parts of one coherent system
- preserve typed field editing and document picking
- turn summary into a meaningful review step

Do not produce a generic Material reskin. Produce a clear product interface with strong hierarchy, better commands, and a consistent visual system.

---

## Implementation-Oriented Frontend Specification

This section is intended to help a frontend-specialized agent translate the UX brief into a concrete Jetpack Compose implementation plan.

## Compose Architecture Guidance

### General Rules

- keep screen composables focused on orchestration and state wiring
- extract visual sections into reusable presentational composables
- isolate design-system primitives from screen-specific implementations
- centralize colors, spacing, elevation, radii, and motion values in theme tokens
- prefer stable reusable patterns over one-off custom compositions

### Recommended UI Package Structure

Suggested structure for the redesign:

- `ui/designsystem/foundation`
- `ui/designsystem/components`
- `ui/designsystem/patterns`
- `ui/screens/auth`
- `ui/screens/fields`
- `ui/screens/users`
- `ui/screens/requests`
- `ui/screens/summary`

Suggested responsibility split:

- `foundation`: color tokens, spacing tokens, typography tokens, elevation, shape, motion constants
- `components`: buttons, chips, cards, top bars, search bars, text fields, segmented controls, banners
- `patterns`: command dock, empty state, list row layout, review section, modal sheet scaffolds
- `screens`: route-specific composition only

## Design Tokens

The frontend agent should formalize tokens rather than styling inline.

### Color Tokens

Suggested semantic tokens:

- `bg.app`
- `bg.surface`
- `bg.elevated`
- `bg.selected`
- `bg.warning`
- `bg.destructive`
- `fg.primary`
- `fg.secondary`
- `fg.muted`
- `fg.onAccent`
- `stroke.default`
- `stroke.strong`
- `accent.primary`
- `accent.secondary`
- `accent.success`
- `accent.warning`
- `accent.destructive`

Use semantic mapping instead of directly styling from random theme colors.

### Spacing Tokens

Suggested token set:

- `space.4`
- `space.8`
- `space.12`
- `space.16`
- `space.20`
- `space.24`
- `space.32`

### Radius Tokens

Suggested token set:

- `radius.sm = 8dp`
- `radius.md = 14dp`
- `radius.lg = 20dp`
- `radius.xl = 28dp`
- `radius.pill = 999dp`

### Elevation Tokens

Suggested token set:

- `elevation.flat`
- `elevation.raised`
- `elevation.overlay`

### Motion Tokens

Suggested token set:

- `motion.fast = 120ms`
- `motion.medium = 220ms`
- `motion.slow = 320ms`

Use motion for:

- content reveal
- mode transitions
- card selection
- bottom-sheet transitions
- command dock state changes

## Typography System

The frontend agent should define explicit text styles beyond default Material typography names.

Suggested semantic type roles:

- `type.hero`
- `type.screenTitle`
- `type.sectionTitle`
- `type.cardTitle`
- `type.body`
- `type.bodyStrong`
- `type.meta`
- `type.label`
- `type.caption`

Recommended usage:

- `hero`: auth entry screens
- `screenTitle`: top-level workspace screens
- `sectionTitle`: section headers in forms and summaries
- `cardTitle`: field/user/request row title
- `meta`: tags, type indicators, selected count, helper info

## Core Reusable Components To Build

The redesign should not be implemented by directly styling each screen independently. Build these reusable pieces first.

## 1. `SharyTopBar`

Purpose:

- top bar for main screens

Props:

- title
- subtitle optional
- navigationIcon optional
- actions slot
- search slot optional
- contextualStatus optional

Behavior:

- stable height and spacing
- integrated search/filter section
- title should remain visually dominant

## 2. `SharyCommandDock`

Purpose:

- bottom command area for Fields, Users, Requests

Props:

- destructive action
- primary action
- secondary actions list
- review action
- selectedCount optional
- mode indicator optional

Behavior:

- visually grouped actions
- supports selection mode
- should adapt to disabled states gracefully

## 3. `SharySearchBar`

Purpose:

- search + filter combination

Props:

- query
- placeholder
- selectedFilter
- filters
- onQueryChange
- onFilterChange

Behavior:

- visible current filter
- search field and filter control appear as one system

## 4. `SelectableEntityCard`

Purpose:

- shared base for field, user, and request cards

Props:

- title
- subtitle
- metadata slot
- leadingAccent
- selected
- actionsVisible
- onClick
- trailingActions slot

Behavior:

- supports selected and unselected states
- supports trailing action reveal or expanded action row
- smooth selection animation

## 5. `EntityActionStrip`

Purpose:

- reusable row-level quick actions

Actions may include:

- edit
- copy
- duplicate
- details
- remove

Behavior:

- use icon plus optional label depending on available width
- do not use cryptic unlabeled icons in high-density groups unless the row container already gives strong context

## 6. `SharyBottomSheetScaffold`

Purpose:

- standardize create/update sheets

Props:

- title
- intro text optional
- primary action label
- secondary action label
- content slot
- footer actions slot optional

Behavior:

- consistent padding
- section spacing
- sticky or clearly visible footer actions

## 7. `StructuredInputSection`

Purpose:

- reusable form section block

Use for:

- required inputs
- advanced options
- typed field sections
- request metadata

## 8. `BuilderListEditor`

Purpose:

- reusable add-with-`+` builder for:
- typed lists
- questionnaire answers
- future chips/tags/options

Props:

- inputLabel
- helperText
- items
- onAdd
- onRemove
- emptyText optional

Behavior:

- add item from pending input
- clear input on add
- show removable item list

## 9. `ReviewSectionCard`

Purpose:

- summary screens

Use for:

- recipients
- fields to send
- request keys to send
- send method summary

## 10. `SharyEmptyState`

Purpose:

- unified empty state pattern

Props:

- title
- body
- illustration or icon
- primary action optional
- secondary action optional

## 11. `StateBanner`

Purpose:

- inline success/error/warning/info banner

Use for:

- cloud fetch result
- offline status
- validation guidance
- password change notice

## 12. `ThemePickerSheet` or `ThemePickerMenu`

Purpose:

- modernized theme selection

Behavior:

- preview theme options visually
- names alone are not enough; show color swatches

## Screen Composition Spec

This section describes how each screen should be assembled in Compose terms.

## Login Screen Composition

Suggested composition:

- `AuthBackground`
- `AuthHero`
- `AuthFormCard`
- `EmailInput`
- `PasswordInput`
- `PrimaryButton(Login)`
- `InlineLink(Create account)`
- `InfoBanner` when password changed

Recommended file split:

- `LoginRoute`
- `LoginScreen`
- `AuthHero`
- `AuthFormCard`

## Signup Screen Composition

Suggested composition:

- `AuthBackground`
- `AuthHero`
- `AuthFormCard`
- `EmailInput`
- `UsernameInput`
- `PasswordInput`
- `ConfirmPasswordInput`
- `PasswordGuidanceBlock`
- `PrimaryButton(Create account)`
- `InlineLink(Already have an account?)`

## Fields Screen Composition

Suggested composition:

- `FieldsRoute`
- `FieldsScreen`
- `SharyTopBar`
- `SharySearchBar`
- `SelectionStatusBar` when at least one field is selected
- `LazyColumn` of `FieldCard`
- `SharyCommandDock`
- `AddFieldSheet`
- `EditFieldSheet`
- `ChangePasswordDialog`

Suggested field card internals:

- `FieldTypeBadge`
- `FieldTagChip`
- `FieldAliasMeta`
- `FieldValuePreview`

## Users Screen Composition

Suggested composition:

- `UsersRoute`
- `UsersScreen`
- `SharyTopBar`
- `SharySearchBar`
- `SelectionStatusBar`
- `LazyColumn` of `UserCard`
- `SharyCommandDock`
- `UserEditorSheet`

## Requests Screen Composition

Suggested composition:

- `RequestsRoute`
- `RequestsScreen`
- `SharyTopBar`
- `ModeSwitcher` for `Received` / `Sent`
- `RequestBoard`
- `RequestHistorySection`
- `DraftRequestSection` when in sent mode
- `SharyCommandDock`
- `AddRequestSheet`
- `FieldMatchingDialog`

Recommended request board structure:

- top summary chips
- active mode explanation line
- main content list
- optional secondary content panel

## Summary Screen Composition

Suggested composition:

- `SummaryRoute`
- `SummaryScreen`
- `SummaryHero`
- `ReviewSectionCard(Recipients)`
- `ReviewSectionCard(Payload)`
- `ReviewSectionCard(Method)`
- sticky bottom CTA group:
- primary `Choose send method` or `Send`
- secondary navigation shortcuts

## Dialog and Sheet Composition Rules

### Bottom Sheets

All create/update sheets should use the same structural pattern:

- title
- optional short intro
- required section
- optional advanced section
- footer actions

### Dialogs

Use standard dialog layout:

- clear title
- concise explanation
- content body
- destructive or confirm actions aligned consistently

### Matching Dialog

This one should be treated as a special workflow dialog with its own internal layout system rather than a standard alert.

## State Management UI Rules

The frontend agent should define visual behavior for every common UI state.

## Loading

Rules:

- use local loading indicators where possible
- reserve full-screen overlay only for blocking operations
- buttons should show inline progress where practical

## Empty

Rules:

- every empty state must explain what the screen is for
- every empty state should offer one next action

## Error

Rules:

- use inline banners when the error affects screen context
- use snackbar only for lightweight transient failures
- destructive or network failures should persist long enough to be actionable

## Selection

Rules:

- selected state must be visible in:
- row styling
- top or bottom command region
- selected count text

## Disabled

Rules:

- disabled actions should remain legible
- explain why high-importance actions are disabled when possible

Example:

- `Review and Send` disabled because no recipients are selected

## UX Copy Mapping

The frontend agent should improve visible labels during redesign.

Replace weak labels with stronger wording:

- `Summary` -> `Review and Send`
- `Logup` -> `Create Account`
- `Go to Request Screen` -> `Requests`
- `Send to Users` -> `Recipients`
- `Fetch Fields from Cloud` -> `Sync from Cloud`
- `Add Field` in request draft context -> `Add requested key`

## Suggested Visual States For Top-Level Destinations

To help the frontend agent create stronger distinction:

- `Fields`: structured archive, confident, information-dense
- `Users`: relational/contact-like, lighter and more human
- `Requests`: workflow/task-oriented, slightly more procedural
- `Summary`: calm, review-focused, high-confidence confirmation
- `Auth`: branded, minimal, trust-centered

## Responsive and Device Behavior

Even though this is Android-first, the UI should adapt across:

- compact phones
- large phones
- tablets

Recommendations:

- compact: stacked layouts, bottom dock emphasis
- medium/large: more two-column opportunities on summary and requests
- tablets: side-by-side matching flow and richer summary layout

## Frontend Implementation Priorities

Recommended implementation sequence for the design agent:

1. Establish tokens and shared components.
2. Rebuild auth screens with final visual language.
3. Rebuild Fields screen and Field sheet.
4. Rebuild Users screen and User sheet.
5. Rebuild Requests screen and Add Request sheet.
6. Rebuild Matching dialog.
7. Rebuild Summary screens and Send dialog.
8. Harmonize theme switching and remaining utility UI.

## Acceptance Criteria For The Redesign

The redesign should be considered successful if:

- the app feels like one coherent product, not multiple unrelated screens
- top-level flows are understandable without relying on tooltips
- primary actions are obvious on every screen
- selected-state behavior is visually clear
- field typing workflows are easier to understand
- review/send feels like a meaningful final checkpoint
- the UI remains compatible with current product capabilities

## Optional Advanced Deliverables

If the frontend agent can go further, useful additions would be:

- component preview screen for the design system
- Compose previews for all major states
- semantic token mapping for each theme
- animation spec for key transitions
- icon audit and replacement plan

---

## Next-Step Suggestions

These are recommended follow-up documents that would make the frontend redesign handoff even stronger.

### 1. Exact Component Contracts

Create a stricter technical handoff document that defines:

- exact component names
- purpose of each component
- required props
- optional props
- event callbacks
- supported visual states
- preview cases

Recommended components to define this way:

- `SharyTopBar`
- `SharyCommandDock`
- `SharySearchBar`
- `SelectableEntityCard`
- `EntityActionStrip`
- `SharyBottomSheetScaffold`
- `StructuredInputSection`
- `BuilderListEditor`
- `ReviewSectionCard`
- `SharyEmptyState`
- `StateBanner`
- `ThemePickerSheet`

This would reduce ambiguity for a frontend implementation agent and make the redesign easier to build consistently.

### 2. Repo-Tied Migration Plan

Create a phased migration plan mapped to the current codebase so redesign work can be applied incrementally without breaking the app.

That plan should include:

- which existing files should be replaced
- which files should be refactored
- which shared components should be introduced first
- which screens should migrate first
- how old and new UI can coexist during transition

Recommended migration order:

1. design tokens and shared design-system components
2. auth screens
3. fields screen and field editor sheet
4. users screen and user editor sheet
5. requests screen and add request sheet
6. matching dialog
7. summary screens and send dialog
8. theme picker and remaining utility surfaces

### 3. Design-System Preview Catalog

Add a dedicated internal preview/demo screen that showcases:

- buttons
- cards
- chips
- form fields
- top bars
- command docks
- empty states
- banners
- sheet layouts
- light/dark theme variants

This would give the frontend agent a stable place to validate consistency before touching all screens.

### 4. State Matrix Per Screen

Create a state matrix document for each major screen covering:

- default
- loading
- empty
- error
- selection mode
- success feedback
- disabled CTA states

This would help prevent polished primary states paired with weak edge-case states.

### 5. Copywriting Pass

Create a short UX-copy appendix with approved wording for:

- button labels
- empty states
- form helper text
- destructive confirmations
- sync/fetch messaging
- summary/review messaging

This would keep the redesigned UI linguistically consistent.

### 6. Motion and Transition Spec

Create a compact motion guide defining:

- list-item selection animation
- screen entrance transitions
- bottom-sheet motion
- command dock transitions
- segmented-mode switching behavior
- feedback banner entrance/exit

This would help the redesign feel intentional instead of static.

---

## Component Contracts Appendix

This appendix defines the recommended component names, responsibilities, and prop contracts for a frontend implementation agent working in Jetpack Compose.

The signatures below are intentionally implementation-oriented but still flexible enough to adapt during buildout.

## Conventions

- use stateless composables by default
- hoist state where practical
- expose slots when layout flexibility is important
- prefer semantic parameter names over visual-only names
- model state explicitly rather than inferring it from nullable values where possible

## 1. `SharyTopBar`

### Purpose

Primary app bar for top-level and secondary screens.

### Recommended Signature

```kotlin
@Composable
fun SharyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: (@Composable () -> Unit)? = null,
    style: SharyTopBarStyle = SharyTopBarStyle.Primary
)
```

### Props

- `title`: primary screen title
- `subtitle`: optional supporting context
- `navigationIcon`: optional back/home/close control
- `actions`: trailing actions slot
- `bottomContent`: optional region for search, tabs, status chips, or segmented controls
- `style`: visual variant

### Variants

```kotlin
enum class SharyTopBarStyle {
    Primary,
    Secondary,
    Summary,
    Auth
}
```

### Required States

- default
- scrolled/elevated
- compact

## 2. `SharyCommandDock`

### Purpose

Persistent bottom command area for top-level workspace screens.

### Recommended Signature

```kotlin
@Composable
fun SharyCommandDock(
    modifier: Modifier = Modifier,
    selectedCount: Int = 0,
    primaryAction: CommandDockAction,
    reviewAction: CommandDockAction? = null,
    destructiveAction: CommandDockAction? = null,
    secondaryActions: List<CommandDockAction> = emptyList(),
    modeLabel: String? = null,
    enabled: Boolean = true
)
```

### Supporting Model

```kotlin
data class CommandDockAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val emphasis: CommandDockActionEmphasis = CommandDockActionEmphasis.Secondary,
    val onClick: () -> Unit
)

enum class CommandDockActionEmphasis {
    Primary,
    Secondary,
    Destructive,
    Review
}
```

### Notes

- `primaryAction` should be visually dominant
- `reviewAction` should read as progression
- `destructiveAction` should be isolated
- `selectedCount` should appear when relevant

## 3. `SharySearchBar`

### Purpose

Search + filter control for list-based screens.

### Recommended Signature

```kotlin
@Composable
fun <T> SharySearchBar(
    query: String,
    selectedFilter: T,
    filters: List<T>,
    onQueryChange: (String) -> Unit,
    onFilterChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    filterLabel: (T) -> String
)
```

### Notes

- filter should be visible, not hidden behind cycling-only behavior
- allow future support for chips, dropdown, or segmented presentation

## 4. `SelectionStatusBar`

### Purpose

Top-of-list or above-content bar indicating selection mode.

### Recommended Signature

```kotlin
@Composable
fun SelectionStatusBar(
    selectedCount: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClearSelection: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
)
```

### Notes

- use on Fields and Users screens
- optional quick actions can appear here if the bottom dock is simplified

## 5. `SelectableEntityCard`

### Purpose

Shared base card for fields, users, and requests.

### Recommended Signature

```kotlin
@Composable
fun SelectableEntityCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingAccentColor: Color? = null,
    supportingContent: (@Composable ColumnScope.() -> Unit)? = null,
    metaContent: (@Composable RowScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
)
```

### Notes

- `supportingContent` can render alias, tags, type indicators, counts
- `metaContent` is useful for chips or badges
- `trailingContent` can host action menu or selection indicator

## 6. `EntityActionStrip`

### Purpose

Contextual row actions for cards.

### Recommended Signature

```kotlin
@Composable
fun EntityActionStrip(
    actions: List<EntityAction>,
    modifier: Modifier = Modifier,
    layout: EntityActionStripLayout = EntityActionStripLayout.Compact
)
```

### Supporting Model

```kotlin
data class EntityAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val tone: EntityActionTone = EntityActionTone.Neutral,
    val onClick: () -> Unit
)

enum class EntityActionTone {
    Neutral,
    Accent,
    Destructive
}

enum class EntityActionStripLayout {
    Compact,
    Expanded
}
```

### Notes

- for narrow layouts, compact mode may show icon + tooltip
- for larger layouts, expanded mode should show labels

## 7. `FieldCard`

### Purpose

Specialized field representation built on top of `SelectableEntityCard`.

### Recommended Signature

```kotlin
@Composable
fun FieldCard(
    title: String,
    valuePreview: String,
    modifier: Modifier = Modifier,
    alias: String? = null,
    tagLabel: String? = null,
    tagColor: Color? = null,
    typeLabel: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
    actions: List<EntityAction> = emptyList()
)
```

### Notes

- `typeLabel` should support typed values such as `List`, `File`, `Questionnaire`
- `tagLabel` should render as a chip or badge

## 8. `UserCard`

### Purpose

Specialized user representation.

### Recommended Signature

```kotlin
@Composable
fun UserCard(
    username: String,
    email: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    roleLabel: String? = null,
    onClick: () -> Unit,
    actions: List<EntityAction> = emptyList()
)
```

### Notes

- `roleLabel` can support owner/current user in the future

## 9. `RequestCard`

### Purpose

Specialized request representation for received/sent lists.

### Recommended Signature

```kotlin
@Composable
fun RequestCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    mode: RequestCardMode = RequestCardMode.Received,
    countBadge: String? = null,
    statusLabel: String? = null,
    onClick: (() -> Unit)? = null
)
```

### Supporting Model

```kotlin
enum class RequestCardMode {
    Received,
    Sent,
    Draft
}
```

## 10. `SharyBottomSheetScaffold`

### Purpose

Unified layout shell for modal bottom sheets.

### Recommended Signature

```kotlin
@Composable
fun SharyBottomSheetScaffold(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    primaryActionLabel: String,
    secondaryActionLabel: String = "Cancel",
    primaryActionEnabled: Boolean = true,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit = onDismiss,
    content: @Composable ColumnScope.() -> Unit
)
```

### Notes

- use for field, user, and request editors where possible
- keep consistent spacing and footer behavior

## 11. `StructuredInputSection`

### Purpose

Reusable labeled section for form groups.

### Recommended Signature

```kotlin
@Composable
fun StructuredInputSection(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
)
```

### Notes

- use for `Required`, `Advanced`, `Typed value`, `Recipients`, `Review`

## 12. `BuilderListEditor`

### Purpose

Reusable item-builder editor.

### Recommended Signature

```kotlin
@Composable
fun BuilderListEditor(
    items: List<String>,
    onItemsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    inputLabel: String,
    helperText: String,
    addButtonLabel: String = "+",
    emptyStateText: String? = null
)
```

### Notes

- use for typed list values
- use for questionnaire answer options
- may later support reordering

## 13. `FilePickerField`

### Purpose

Structured file input UI for typed file values.

### Recommended Signature

```kotlin
@Composable
fun FilePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fileName: String? = null,
    mimeType: String? = null,
    sizeLabel: String? = null,
    onBrowseClick: () -> Unit,
    onClearClick: (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null
)
```

### Notes

- preserve browse integration with Android document picker
- show metadata elegantly
- future-proof for replace/remove interactions

## 14. `TypedValueSection`

### Purpose

Encapsulate typed-value controls for the field editor.

### Recommended Signature

```kotlin
@Composable
fun TypedValueSection(
    draft: TypedValueUiState,
    onDraftChange: (TypedValueUiState) -> Unit,
    modifier: Modifier = Modifier,
    valueInput: @Composable () -> Unit
)
```

### Supporting Model

```kotlin
data class TypedValueUiState(
    val enabled: Boolean,
    val type: TypedValueUiType,
    val separator: String = ",",
    val isMultiSelection: Boolean = false,
    val answerCount: String = "",
    val answers: List<String> = emptyList(),
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long? = null
)

enum class TypedValueUiType {
    List,
    Json,
    Questionnaire,
    Yaml,
    File
}
```

### Notes

- this mirrors the current feature set without binding to the exact current draft class

## 15. `TagChipPicker`

### Purpose

Visual tag selection and management.

### Recommended Signature

```kotlin
@Composable
fun TagChipPicker(
    selectedTag: String?,
    availableTags: List<TagChipOption>,
    modifier: Modifier = Modifier,
    onTagSelected: (TagChipOption) -> Unit,
    onCreateTag: (() -> Unit)? = null,
    onEditTag: ((TagChipOption) -> Unit)? = null
)
```

### Supporting Model

```kotlin
data class TagChipOption(
    val id: String,
    val label: String,
    val color: Color
)
```

## 16. `ReviewSectionCard`

### Purpose

Review content block for summary screens.

### Recommended Signature

```kotlin
@Composable
fun ReviewSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
)
```

### Notes

- use for recipients, payload, send method, warnings, notes

## 17. `RecipientListReview`

### Purpose

Display selected users in summary context.

### Recommended Signature

```kotlin
@Composable
fun RecipientListReview(
    recipients: List<RecipientReviewItem>,
    modifier: Modifier = Modifier
)
```

### Supporting Model

```kotlin
data class RecipientReviewItem(
    val title: String,
    val subtitle: String
)
```

## 18. `PayloadListReview`

### Purpose

Display fields or requested keys in summary context.

### Recommended Signature

```kotlin
@Composable
fun PayloadListReview(
    items: List<PayloadReviewItem>,
    modifier: Modifier = Modifier
)
```

### Supporting Model

```kotlin
data class PayloadReviewItem(
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null
)
```

## 19. `SendMethodSelectorDialog`

### Purpose

Select send mechanism from a small list of supported options.

### Recommended Signature

```kotlin
@Composable
fun SendMethodSelectorDialog(
    methods: List<SendMethodOption>,
    selectedMethodId: String?,
    onMethodSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
)
```

### Supporting Model

```kotlin
data class SendMethodOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)
```

## 20. `SharyEmptyState`

### Purpose

Unified empty-state block.

### Recommended Signature

```kotlin
@Composable
fun SharyEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
)
```

## 21. `StateBanner`

### Purpose

Inline message banner.

### Recommended Signature

```kotlin
@Composable
fun StateBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: StateBannerTone = StateBannerTone.Info,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
)
```

### Supporting Model

```kotlin
enum class StateBannerTone {
    Info,
    Success,
    Warning,
    Error
}
```

## 22. `SegmentedModeSwitcher`

### Purpose

Used on requests screen and anywhere else a two-mode task switch exists.

### Recommended Signature

```kotlin
@Composable
fun <T> SegmentedModeSwitcher(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: (T) -> String
)
```

## 23. `MatchingWorkspaceDialog`

### Purpose

Specialized matching workflow container.

### Recommended Signature

```kotlin
@Composable
fun MatchingWorkspaceDialog(
    storedItems: List<MatchingFieldItem>,
    requestItems: List<MatchingFieldItem>,
    matchedCount: Int,
    totalCount: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onAddStoredItem: () -> Unit,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    content: @Composable () -> Unit
)
```

### Supporting Model

```kotlin
data class MatchingFieldItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val state: MatchingFieldItemState = MatchingFieldItemState.Idle
)

enum class MatchingFieldItemState {
    Idle,
    Selected,
    Matched
}
```

## 24. `ThemeOptionCard`

### Purpose

Visual theme option inside theme picker UI.

### Recommended Signature

```kotlin
@Composable
fun ThemeOptionCard(
    name: String,
    swatches: List<Color>,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
)
```

## Preview Requirements Per Component

Every major component should include previews for:

- default
- selected
- disabled
- long text
- compact width
- dark theme

At minimum, provide Compose previews for:

- `SharyTopBar`
- `SharyCommandDock`
- `SelectableEntityCard`
- `FieldCard`
- `UserCard`
- `RequestCard`
- `SharyBottomSheetScaffold`
- `BuilderListEditor`
- `ReviewSectionCard`
- `SharyEmptyState`
- `StateBanner`
- `SendMethodSelectorDialog`

## Integration Guidance

The frontend agent should integrate these components incrementally:

1. Build foundation tokens.
2. Build the reusable components in isolation with previews.
3. Replace screen-specific raw Material layouts with these components.
4. Only after component stabilization, migrate dialog/sheet flows and the matching workspace.

## Contract Stability Rule

Once a component is used across at least 2 screens:

- avoid casual prop renaming
- avoid screen-specific forks of the same component
- prefer extending the shared contract or adding a slot

This will keep the redesign coherent and maintainable.
