# Shary — Android Studio Agent Implementation Instructions
## Design: Proposal 5 "Soft Spatial"
## Stack: Kotlin · Jetpack Compose · Material 3

---

## 0. Your role and constraints

You are a Jetpack Compose frontend agent implementing the "Soft Spatial" design for the
Shary Android application. You write Kotlin with Jetpack Compose exclusively. You never
use XML layouts, View system components, or legacy AppCompat. You never deviate from
the design tokens defined in Section 2. You never invent UI patterns not described in
this document. When a screen or component is not yet specified, you ask before
implementing.

---

## 1. Project structure you must follow

```
ui/
├── theme/
│   ├── Color.kt          ← all color tokens
│   ├── Type.kt           ← all typography tokens
│   ├── Shape.kt          ← all shape tokens
│   ├── Spacing.kt        ← all spacing tokens
│   └── Theme.kt          ← SharyTheme composable
├── components/
│   ├── SharyTopBar.kt
│   ├── SharySearchBar.kt
│   ├── SharyCommandDock.kt
│   ├── FieldCard.kt
│   ├── UserCard.kt
│   ├── RequestCard.kt
│   ├── SelectionPill.kt
│   ├── TagChip.kt
│   ├── ReviewSectionCard.kt
│   ├── SendMethodCard.kt
│   ├── RecipientBlob.kt
│   ├── SharyButton.kt
│   └── EmptyState.kt
├── screens/
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   └── SignupScreen.kt
│   ├── fields/
│   │   ├── FieldsScreen.kt
│   │   └── FieldEditorSheet.kt
│   ├── users/
│   │   ├── UsersScreen.kt
│   │   └── UserEditorSheet.kt
│   ├── requests/
│   │   ├── RequestsScreen.kt
│   │   ├── AddRequestSheet.kt
│   │   └── FieldMatchingDialog.kt
│   └── summary/
│       ├── SummaryFieldScreen.kt
│       └── SummaryRequestScreen.kt
└── navigation/
    └── SharyNavGraph.kt
```

---

## 2. Design tokens — implement exactly as specified

### 2.1 Color.kt

```kotlin
package com.shary.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand palette ─────────────────────────────────────────────────────────────
val Violet50   = Color(0xFFEEEBF5)   // page background
val Violet100  = Color(0xFFE4E0EF)   // nav bar background, chip background
val Violet200  = Color(0xFFDDD8EF)   // borders, dividers
val Violet300  = Color(0xFFC4B8E8)   // hover borders, active chip border
val Violet400  = Color(0xFFAA9ADA)   // muted text on violet bg
val Violet500  = Color(0xFF9088A8)   // secondary text, placeholder
val Violet600  = Color(0xFF7A5AF8)   // PRIMARY ACCENT — buttons, selections, active states
val Violet700  = Color(0xFF6B4AE8)   // pressed state of primary
val Violet800  = Color(0xFF5A3FB8)   // tag text on violet bg
val Violet900  = Color(0xFF1E1630)   // primary text

val SurfaceWhite   = Color(0xFFFFFFFF)   // card surface
val SurfaceLight   = Color(0xFFF5F3FB)   // screen surface (phone bg)
val SurfaceMid     = Color(0xFFEAE6F5)   // input background, icon button bg

// ── Semantic tag colors ───────────────────────────────────────────────────────
val TagVioletBg    = Color(0xFFEDE9FF)
val TagVioletText  = Color(0xFF5A3FB8)
val TagMintBg      = Color(0xFFE2F5F0)
val TagMintText    = Color(0xFF1A6B52)
val TagPeachBg     = Color(0xFFFDEEE8)
val TagPeachText   = Color(0xFF9A3C1C)
val TagSkyBg       = Color(0xFFE5F0FB)
val TagSkyText     = Color(0xFF1A4F8A)

// ── Destructive ───────────────────────────────────────────────────────────────
val DestructiveBg  = Color(0xFFFDEEE8)
val DestructiveText= Color(0xFF9A3C1C)

// ── Selection pill ────────────────────────────────────────────────────────────
val SelectionBg    = Color(0xFFEDE9FF)
val SelectionText  = Color(0xFF5A3FB8)
```

### 2.2 Type.kt — fonts loaded via Google Fonts

```kotlin
package com.shary.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.shary.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage    = "com.google.android.gms",
    certificates       = R.array.com_google_android_gms_fonts_certs
)

// Display serif — used for screen titles, card titles, auth hero
val GupterFamily = FontFamily(
    Font(GoogleFont("Gupter"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("Gupter"), provider, weight = FontWeight.Medium),
    Font(GoogleFont("Gupter"), provider, weight = FontWeight.Bold),
)

// Body sans — used for body text, labels, metadata, buttons
val OutfitFamily = FontFamily(
    Font(GoogleFont("Outfit"), provider, weight = FontWeight.Light),
    Font(GoogleFont("Outfit"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("Outfit"), provider, weight = FontWeight.Medium),
    Font(GoogleFont("Outfit"), provider, weight = FontWeight.SemiBold),
)

// Mono — used for field values, keys, aliases, typed data
val JetBrainsMonoFamily = FontFamily(
    Font(GoogleFont("JetBrains Mono"), provider, weight = FontWeight.Normal),
    Font(GoogleFont("JetBrains Mono"), provider, weight = FontWeight.Medium),
)

val SharyTypography = Typography(
    // Screen titles, auth hero wordmark
    displayLarge = TextStyle(
        fontFamily = GupterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp
    ),
    // Section headers on auth screen
    displayMedium = TextStyle(
        fontFamily = GupterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 36.sp
    ),
    // Top bar titles, sheet titles
    headlineLarge = TextStyle(
        fontFamily = GupterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    // Card keys, request sender names
    headlineMedium = TextStyle(
        fontFamily = GupterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 22.sp
    ),
    // Card titles in list
    titleLarge = TextStyle(
        fontFamily = GupterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    // Section labels (uppercase small)
    titleSmall = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp
    ),
    // Body text, form helper text
    bodyLarge = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    // Secondary metadata, timestamps
    bodySmall = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp
    ),
    // Buttons, primary action labels
    labelLarge = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    // Chip labels, tag labels
    labelMedium = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    ),
    // Tiny caps labels
    labelSmall = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.8.sp
    )
)

// Extra semantic aliases for use throughout the codebase
// Use these directly in components — do not hardcode TextStyle inline
object SharyTextStyles {
    val fieldValue = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = androidx.compose.ui.graphics.Color.Unspecified // set per usage
    )
    val fieldValueMedium = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
    val sectionLabel = TextStyle(
        fontFamily = OutfitFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.0.sp
    )
}
```

### 2.3 Shape.kt

```kotlin
package com.shary.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SharyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // checkboxes, small badges
    small      = RoundedCornerShape(10.dp),  // icon buttons, small inputs
    medium     = RoundedCornerShape(14.dp),  // inputs, chips, filter bars
    large      = RoundedCornerShape(18.dp),  // cards (FieldCard, UserCard, etc.)
    extraLarge = RoundedCornerShape(24.dp),  // bottom sheets, dialogs
)

// Named aliases — use these in components, not raw dp values
object SharyRadius {
    val chip       = RoundedCornerShape(99.dp) // fully rounded pills
    val iconButton = RoundedCornerShape(10.dp)
    val input      = RoundedCornerShape(14.dp)
    val card       = RoundedCornerShape(18.dp)
    val sheet      = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val dialog     = RoundedCornerShape(24.dp)
    val avatar     = RoundedCornerShape(10.dp) // square-ish avatars
}
```

### 2.4 Spacing.kt

```kotlin
package com.shary.ui.theme

import androidx.compose.ui.unit.dp

object SharySpacing {
    val xxs  =  4.dp
    val xs   =  8.dp
    val sm   = 12.dp
    val md   = 16.dp
    val lg   = 20.dp
    val xl   = 24.dp
    val xxl  = 32.dp

    // Component-specific
    val cardPaddingH     = 16.dp
    val cardPaddingV     = 14.dp
    val screenPaddingH   = 18.dp
    val dockPaddingH     = 18.dp
    val dockPaddingBottom= 20.dp
    val chipPaddingH     = 12.dp
    val chipPaddingV     =  4.dp
    val sectionGap       = 14.dp
}
```

### 2.5 Theme.kt

```kotlin
package com.shary.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SharyLightColorScheme = lightColorScheme(
    primary            = Violet600,
    onPrimary          = Color.White,
    primaryContainer   = TagVioletBg,
    onPrimaryContainer = TagVioletText,
    secondary          = Violet500,
    onSecondary        = Color.White,
    background         = Violet50,
    onBackground       = Violet900,
    surface            = SurfaceLight,
    onSurface          = Violet900,
    surfaceVariant     = SurfaceMid,
    onSurfaceVariant   = Violet500,
    outline            = Violet200,
    outlineVariant     = Violet300,
    error              = DestructiveText,
    onError            = Color.White,
    errorContainer     = DestructiveBg,
    onErrorContainer   = DestructiveText,
)

@Composable
fun SharyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SharyLightColorScheme,
        typography  = SharyTypography,
        shapes      = SharyShapes,
        content     = content
    )
}
```

---

## 3. Component specifications

### 3.1 SharyTopBar

```kotlin
@Composable
fun SharyTopBar(
    title: String,
    subtitle: String? = null,         // e.g. "6 stored · 1 selected"
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
)
```

Rules:
- Title uses `MaterialTheme.typography.headlineLarge` with `GupterFamily`
- Subtitle uses `MaterialTheme.typography.labelSmall` in `Violet500`
- Background is `SurfaceLight`, no elevation, bottom border `Violet200` at 1.dp
- Height: 56.dp min, expands naturally for subtitle
- Actions use `SharyIconButton` (see 3.6)

### 3.2 FieldCard

```kotlin
@Composable
fun FieldCard(
    fieldKey: String,
    fieldValue: String,
    tag: FieldTag,           // enum: IDENTITY, HEALTH, FINANCE, CONTACT, OTHER
    alias: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Rules:
- Background: `SurfaceWhite` unselected, `Color(0xFFFAF8FF)` selected
- Border: `1.5.dp` solid `Violet200` unselected, `Violet600` selected
- Corner radius: `SharyRadius.card` (18.dp)
- Padding: `SharySpacing.cardPaddingH` / `SharySpacing.cardPaddingV`
- `fieldKey` uses `MaterialTheme.typography.titleLarge` (`GupterFamily Medium 15sp`)
- `fieldValue` uses `SharyTextStyles.fieldValue` in `Violet500`
- `alias` uses `MaterialTheme.typography.bodySmall` in `Color(0xFFB0A8C8)`
- Selection ring: 20.dp circle, `Violet200` border unselected, `Violet600` fill + white checkmark selected
- Animate border color and background with `animateColorAsState(tween(180))`
- `tag` renders as `TagChip` (see 3.3)

### 3.3 TagChip

```kotlin
enum class FieldTag { IDENTITY, HEALTH, FINANCE, CONTACT, OTHER }

@Composable
fun TagChip(tag: FieldTag)
```

Color mapping (bg / text):
- `IDENTITY` → `TagVioletBg` / `TagVioletText`
- `HEALTH`   → `TagMintBg`   / `TagMintText`
- `FINANCE`  → `TagSkyBg`    / `TagSkyText`
- `CONTACT`  → `TagPeachBg`  / `TagPeachText`
- `OTHER`    → `SurfaceMid`  / `Violet800`

Rules:
- Shape: `SharyRadius.chip` (fully rounded)
- Padding: `3.dp` vertical, `9.dp` horizontal
- Text: `MaterialTheme.typography.labelMedium` (10sp semibold 0.5sp tracking)
- Text is UPPERCASE

### 3.4 SharyCommandDock

```kotlin
@Composable
fun SharyCommandDock(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    primaryAction: DockAction,          // "Review & Send" / "Match fields" etc.
    secondaryActions: List<DockAction>, // "Users", "Fields", "Requests"
    destructiveAction: DockAction?,     // "Delete"
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Rules:
- Background: `SurfaceLight`, top border `Violet200`
- Padding: `SharySpacing.dockPaddingH` H, `10.dp` top, `SharySpacing.dockPaddingBottom` bottom
- Selection pill: shown only when `selectedCount > 0`
    - Background `SelectionBg`, corner `SharyRadius.chip`
    - Left: "$selectedCount field(s) selected" in `SelectionText` `OutfitFamily SemiBold 12sp`
    - Right: "Clear ✕" in `Violet500`, tappable
- Action row: `Row(horizontalArrangement = Arrangement.spacedBy(7.dp))`
- Primary button: `SharyPrimaryButton` fills remaining space via `weight(1f)`
- Secondary buttons: `SharySoftButton`
- Destructive button: `SharyDestructiveButton`
- Animate `SelectionPill` visibility with `AnimatedVisibility(visible = selectedCount > 0)`

### 3.5 SharySearchBar

```kotlin
@Composable
fun SharySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    activeFilter: String,
    filters: List<String>,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

Rules:
- Outer container: `SurfaceLight`, padding `8.dp` top `18.dp` bottom `18.dp` H
- Search row: `SurfaceMid` background, `SharyRadius.input` (14.dp), padding `10.dp` V `14.dp` H
- Search icon: `Icons.Default.Search` in `Color(0xFFB0A8C8)` 16.dp
- Input text: `MaterialTheme.typography.bodyMedium`, `Violet900`
- Chips row below: `FlowRow` (Accompanist) with `6.dp` gap, `10.dp` top padding
- Filter chips: `SharyRadius.chip`, `SurfaceLight` unselected / `Violet600` selected
- Chip text: `MaterialTheme.typography.labelMedium`

### 3.6 SharyIconButton

```kotlin
@Composable
fun SharyIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Rules:
- Size: 32.dp × 32.dp
- Background: `SurfaceMid` (`EAE6F5`)
- Shape: `SharyRadius.iconButton` (10.dp)
- Icon tint: `Violet800` at 16.dp

### 3.7 Button variants

**SharyPrimaryButton** — violet filled, white text
- Background `Violet600`, pressed `Violet700`
- Corner `14.dp`
- Text `labelLarge OutfitFamily SemiBold 14sp`
- Height 48.dp min

**SharySoftButton** — lavender tinted, muted text
- Background `SurfaceMid` (`EAE6F5`)
- Text color `Violet800`
- Corner `12.dp`
- Height 46.dp min

**SharyDestructiveButton** — peach tinted, red text
- Background `DestructiveBg`
- Text/icon color `DestructiveText`
- Corner `12.dp`
- Height 46.dp min

**SharyTextButton** — no background, violet text link
- Color `Violet600`, `OutfitFamily SemiBold 12sp`
- Used for "Create account", "Clear ✕"

### 3.8 RecipientBlob

```kotlin
@Composable
fun RecipientBlob(name: String, email: String)
```

Rules:
- Outer: `TagVioletBg` background, `14.dp` corner, padding `9.dp` V `14.dp` H
- Avatar: 30.dp × 30.dp, `Violet600` background, `10.dp` corner, initials in white `OutfitFamily Bold 12sp`
- Name: `MaterialTheme.typography.bodyMedium` `SemiBold` `Violet900`
- Email: `MaterialTheme.typography.bodySmall` `Violet500`

### 3.9 ReviewSectionCard

```kotlin
@Composable
fun ReviewSectionCard(
    sectionLabel: String,
    content: @Composable ColumnScope.() -> Unit
)
```

Rules:
- Label: `SharyTextStyles.sectionLabel` UPPERCASE `Violet500`, `8.dp` bottom margin
- Card: `SurfaceWhite`, `SharyRadius.card`, 1.dp border `Violet200`
- Dividers between rows: `1.dp` `Violet50`
- Row key: `bodyMedium` `Violet500`
- Row value: `SharyTextStyles.fieldValueMedium` `Violet900`

### 3.10 SendMethodCard

```kotlin
@Composable
fun SendMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
)
```

Rules:
- Border: `1.5.dp` `Violet200` unselected, `Violet600` selected
- Background: `SurfaceWhite` unselected, `Color(0xFFFAF8FF)` selected
- Corner: `SharyRadius.card`
- Icon: 24.dp, `Violet600`
- Title: `titleLarge GupterFamily` `Violet900`
- Description: `bodySmall` `Violet500`
- Animate with `animateColorAsState`

---

## 4. Screen specifications

### 4.1 LoginScreen

Layout structure (top to bottom, no scrolling):
1. Auth hero block — `Violet600` background, top 36.dp padding
    - Centered 64×64.dp box, `Violet600` background + white 20% border, 20.dp corner, letter "S" `GupterFamily Bold 28sp white`
    - Wordmark "Shary" `displayMedium GupterFamily 34sp white`
    - Tagline: 2-line `bodyMedium white 65% opacity`
2. Form block — `SurfaceLight` background, `24.dp` padding
    - Email `OutlinedTextField` with `SharyRadius.input`, focus border `Violet600`
    - Password `OutlinedTextField` with show/hide toggle icon
    - `SharyPrimaryButton("Sign in")` full width
    - `SharyTextButton("Create account")` centered below
3. Status bar should be `Violet600` — set via `accompanist-systemuicontroller`

### 4.2 SignupScreen

Same hero block as Login, then form with: email, username, password, confirm password.
Rename screen title to "Create Account" — never display "Logup" anywhere.

### 4.3 FieldsScreen

Scaffold structure:
- `SharyTopBar(title = "Fields", subtitle = "$count stored · $selectedCount selected")`
- `SharySearchBar` with filters: All, Identity, Health, Finance, Contact
- `LazyColumn` of `FieldCard` items with `8.dp` vertical gap, `18.dp` H padding
- `SharyCommandDock` at bottom

State rules:
- Selection is toggled by tapping a card
- Selection mode does NOT require long-press — first tap selects
- `selectedCount` drives dock pill visibility and subtitle
- Empty state: `EmptyState(title = "No fields yet", body = "Store your first piece of personal data", primaryAction = "Create your first field")`
- Loading state: show 3 `FieldCard` skeletons with shimmer animation (use `Modifier.shimmer()` pattern)

### 4.4 UsersScreen

Same shell as FieldsScreen.
`UserCard` shows: avatar square (initials, `Violet600` bg), name (`titleLarge GupterFamily`), email (`bodySmall Violet500`), selection ring.
Empty state: "No recipients yet" / "Add your first recipient"

### 4.5 RequestsScreen

- `SharyTopBar(title = "Requests")`
- Mode switcher below top bar: two `SharyModeButton` side by side in `SurfaceMid` container with `12.dp` corner
    - Active: `Violet600` fill, white text
    - Inactive: transparent, `Violet500` text
    - Show badge count on "Received" when > 0
- `LazyColumn` of `RequestCard` below
- `SharyCommandDock` adapts actions by mode:
    - Received: primary = "Match fields →"
    - Sent: primary = "Add requested key"

`RequestCard` rules:
- Full-width card, `SharyRadius.card`
- Shows: sender name (`titleLarge GupterFamily`), timestamp (`bodySmall Violet500`), `TagChip` with field count
- Expandable row of requested key chips below
- "Match to my fields →" `SharyTextButton` shown when card is active/selected
- Active card: border `Violet600`, `Color(0xFFFAF8FF)` bg

### 4.6 SummaryFieldScreen / SummaryRequestScreen

Hero strip — `Violet600` background:
- Back icon button (`SurfaceWhite 15% opacity`) on left
- Title "Review & Send" `headlineLarge GupterFamily white`
- Subtitle: "$fieldCount fields · $recipientCount recipient(s) · encrypted" `bodySmall white 65%`

Scrollable body — `Violet50` background, `18.dp` H padding, `14.dp` section gaps:
1. `ReviewSectionCard("Recipient")` → `RecipientBlob`
2. `ReviewSectionCard("Fields")` → rows of key/value
3. `ReviewSectionCard("Send method")` → list of `SendMethodCard`
4. `SharyPrimaryButton("Send securely")` full width, `14.dp` bottom margin
5. Caption: "End-to-end encrypted · data never stored" `bodySmall Violet500` centered

---

## 5. Sheet and dialog specifications

### 5.1 All ModalBottomSheets

```kotlin
ModalBottomSheet(
    shape = SharyRadius.sheet,
    containerColor = SurfaceLight,
    dragHandle = { /* 40×4.dp pill, Violet200 */ }
)
```

Internal layout:
- Title: `headlineLarge GupterFamily` `Violet900`, `20.dp` top `18.dp` H padding
- Optional intro text: `bodyMedium Violet500`
- Required section first, then `HorizontalDivider` + Advanced section (collapsed by default)
- Footer: `Row` with `SharySoftButton("Cancel")` + `SharyPrimaryButton("Save")`, `16.dp` top gap, `18.dp` H padding, `20.dp` bottom padding

### 5.2 FieldEditorSheet

Sections:
- Required: Key (`OutlinedTextField` with suggestions dropdown), Value
- Advanced (expand toggle): Alias, Tag picker, Typed value toggle

Key suggestion dropdown: `ExposedDropdownMenuBox` styled with `SurfaceLight` container and `Violet300` border.

### 5.3 FieldMatchingDialog

Full-screen `Dialog` with `SharyRadius.dialog`, NOT a bottom sheet.
Layout: two equal columns separated by 1.dp `Violet200` divider.
Left: stored fields list. Right: requested fields list.
Matched pairs: show numbered badge (`Violet600` circle) on both sides.
Progress bar: `LinearProgressIndicator` with `Violet600` track, `Violet200` background, `8.dp` below header.
Footer: `SharyPrimaryButton("Accept")` disabled until all matched, `SharySoftButton("Cancel")`, undo/redo `SharyIconButton` pair.

---

## 6. Animation and motion rules

Use `animateColorAsState` everywhere a color changes on interaction. Duration: `tween(180)` for fast (selection), `tween(280)` for medium (sheet open), `tween(340)` for slow (screen transitions).

Specific requirements:
- `FieldCard` border and background: `animateColorAsState(tween(180))`
- `SelectionPill` appear/disappear: `AnimatedVisibility` with `fadeIn + slideInVertically` / `fadeOut + slideOutVertically`
- `SharyCommandDock` action row: `AnimatedContent` when transitioning between normal and selection-mode actions
- Screen transitions in `SharyNavGraph`: `fadeIn(tween(220)) + slideInVertically { it / 20 }` enter, matching exit
- Bottom sheet: use default Material 3 sheet animation (do not override)
- Mode switcher on RequestsScreen: `AnimatedContent(targetState = activeMode)`

Do NOT add any decorative looping animations. Motion is reserved for state changes only.

---

## 7. Navigation graph

```kotlin
@Composable
fun SharyNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "login",
        enterTransition = { fadeIn(tween(220)) + slideInVertically { it / 20 } },
        exitTransition  = { fadeOut(tween(180)) }
    ) {
        composable("login")    { LoginScreen(navController) }
        composable("signup")   { SignupScreen(navController) }
        composable("fields")   { FieldsScreen(navController) }
        composable("users")    { UsersScreen(navController) }
        composable("requests") { RequestsScreen(navController) }
        composable("summary_field")   { SummaryFieldScreen(navController) }
        composable("summary_request") { SummaryRequestScreen(navController) }
    }
}
```

Bottom navigation bar (Fields / Users / Requests):
- Only shown when on one of those three destinations
- Background `SurfaceLight`, top border `Violet200`
- Selected icon + label: `Violet600`
- Unselected: `Violet400`
- No indicator pill — use color only
- Use `NavigationBar` + `NavigationBarItem` from Material 3

---

## 8. Accessibility rules

- Every icon-only button must have a `contentDescription`
- Touch targets minimum 48×48.dp even if visual is smaller — use `Modifier.minimumInteractiveComponentSize()`
- Color is never the only differentiator: selected cards also have border width change
- All text on `Violet600` background must be white (`Color.White`) — never gray
- Screen reader order must match visual top-to-bottom order — avoid `Modifier.zIndex` reordering
- `OutlinedTextField` must have `label` param set, not just `placeholder`

---

## 9. Things the agent must NEVER do

- Never use XML layouts or `setContentView`
- Never hardcode color hex values inline in composables — always reference `SharyTheme` tokens or named color constants from `Color.kt`
- Never use `androidx.compose.material` (Material 2) — only `androidx.compose.material3`
- Never place primary and destructive actions next to each other without visual separation
- Never use default purple Material 3 tones — always override with `SharyLightColorScheme`
- Never display the word "Logup" — use "Create Account" or "Sign Up"
- Never display "Summary" as a button label — use "Review & Send"
- Never create a new component that duplicates an existing one from Section 3
- Never apply `elevation` to cards — use border + background color contrast instead
- Never use `Card` composable's default elevation shadow — set `elevation = CardDefaults.cardElevation(0.dp)` and control depth via border

---

## 10. Implementation order

Follow this exact sequence. Do not skip ahead.

1. `Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Theme.kt`
2. `SharyIconButton`, `SharyPrimaryButton`, `SharySoftButton`, `SharyDestructiveButton`
3. `TagChip`, `SelectionPill`, `SharySearchBar`
4. `FieldCard`, `UserCard`
5. `SharyTopBar`, `SharyCommandDock`
6. `LoginScreen`, `SignupScreen`
7. `FieldsScreen` + `FieldEditorSheet`
8. `UsersScreen` + `UserEditorSheet`
9. `RequestsScreen` + `AddRequestSheet`
10. `FieldMatchingDialog`
11. `SummaryFieldScreen`, `SummaryRequestScreen`
12. `SendMethodCard`, `RecipientBlob`, `ReviewSectionCard`
13. `SharyNavGraph` + bottom `NavigationBar`
14. Animation pass — add all `animateColorAsState` and `AnimatedVisibility` calls
15. Accessibility audit — check all `contentDescription`, touch targets, contrast

---

*End of agent instructions.*