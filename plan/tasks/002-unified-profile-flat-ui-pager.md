# Task 002 — Unified profile screen, flat cards, no wordmark, swipeable tabs

Read `AGENTS.md` first. Implement exactly the four workstreams below. Do not commit, push, or create branches. Do not touch `plan/todo.md`, `.env`, or anything outside `app/src/main/java` (plus no resource changes are required).

Build with:

```
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

Both must pass before you report DONE.

---

## 1. One profile/settings screen for every tab

### 1a. `ui/common/BioTraceTopBar.kt`
- Delete `DataSourceDialog` and the dialog state entirely.
- Delete the `LastSyncStore`, `LocalContext`, `Instant`, `ZoneId`, `DateTimeFormatter`, `Locale`, `AlertDialog`, `TextButton`, `CheckCircle`, `RoundedCornerShape`, `height` imports if they become unused.
- New signature (keep the `modifier` parameter):

```kotlin
@Composable
fun BioTraceTopBar(
    onProfileClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

- Layout: a `Row` with `Arrangement.SpaceBetween`, `statusBarsPadding`, same background/padding as today.
  - Left: if `onBackClick != null`, show an `IconButton` with `Icons.AutoMirrored.Filled.ArrowBack`, content description `"Back"`.
  - Right: if `onProfileClick != null`, show the existing circular avatar `Box` (38.dp, border, no shadow — see workstream 2) whose `clickable` calls `onProfileClick`. Content description: `"Profile and settings"`.
  - When both are null the row is just an empty spacer bar; that is fine.

### 1b. `ui/settings/SettingsScreen.kt`
- Add `onNavigateBack: () -> Unit` parameter (before `viewModel`).
- Top bar becomes:

```kotlin
BioTraceTopBar(onBackClick = onNavigateBack)
```

  (Use the existing fully-qualified `io.biotrace.app.ui.common.BioTraceTopBar` style or add an import.)

- Insert a new first card, **Health Connect**, that merges the old data-source dialog content with the old permissions card:
  - Title row: `Icons.Default.CheckCircle` tinted `ClinicalEmerald` + `Text(stringResource(R.string.hc_title))` (`"Health Connect"`), bold.
  - `Text` with the last-sync label, computed with `LastSyncStore`:
    - `"Last sync: MMM d, HH:mm"` via `DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())`, or `"Last sync: Not synced yet"` when null.
  - Explanation: `"BioTrace reads health data directly from Health Connect. Data appears here from any app you allow to write to Health Connect."`
  - `OutlinedButton(onClick = onNavigateToPermissions)` labelled `stringResource(R.string.hc_grant_permissions)`.
- Delete the now-redundant standalone "Health Connect Permissions" card.
- The Gemini API key card, Data Management card, and About card stay, but remove their `elevation = CardDefaults.cardElevation(...)` arguments (workstream 2).
- Read the last sync with `LocalContext.current` and `remember { LastSyncStore.lastSyncMillis(context) }`.

### 1c. `ui/navigation/BioTraceNavHost.kt`
- Pass `onProfileClick = { navController.navigate(Screen.Settings.route) }` to the top-level screens:
  - `VitalsScreen` (add `onNavigateToSettings` parameter to that composable; keep its existing `onNavigateToPermissions` for permission CTAs inside the screen).
  - `LabsScreen(onNavigateToSettings = ...)`.
  - `FoodScanScreen(onNavigateToSettings = ...)` already accepts it.
  - `BiometricInsightsScreen` (add `onNavigateToSettings` parameter).
- `SettingsScreen(onNavigateBack = { navController.popBackStack() }, onNavigateToPermissions = ...)`.
- Keep `ChatScreen`/`DashboardScreen` compiling (they are not bottom tabs). Update their top-bar calls to `onProfileClick = onNavigateToSettings`; `DashboardScreen` already receives the callback, `ChatScreen` too.

### 1d. Screen call sites
- `VitalsScreen`: remove the now-dead dialog path; top bar gets `onProfileClick = onNavigateToSettings`.
- `BiometricInsightsScreen`: add `onNavigateToSettings: () -> Unit = {}` and pass it.
- `LabsScreen`, `FoodScanScreen`, `ChatScreen`, `DashboardScreen`: replace `onAvatarClick` / `onSettingsClick` with `onProfileClick`.
- Remove the dead `onSettingsClick` parameter everywhere.

Result: the avatar always opens the same Settings screen, which shows source, last sync, permissions, and the API key. Back arrow returns to the tab you came from.

---

## 2. Remove all card shadows / elevation

Remove every explicit shadow or elevation so surfaces are flat and rely on the existing hairline borders:

- `ui/theme/BioTraceTokens.kt`: delete `HeroCardElevation`, `BentoCardElevation`, `FloatingBarElevation`, `PillElevation`, `ShadowSpotColor`, `ShadowAmbientColor`.
- Delete every `shadowElevation = ...` argument in:
  - `ui/vitals/VitalsScreen.kt` (3), `ui/vitals/VitalDetailScreen.kt` (5), `ui/insights/BiometricInsightsScreen.kt` (4).
  - `ui/common/BioTraceFloatingBottomBar.kt` (1).
- Delete `.shadow(...)` modifiers:
  - `ui/common/BioTraceTopBar.kt` (avatar).
  - `ui/dashboard/DashboardScreen.kt:428` (orange CTA).
  - `ui/nutrition/FoodScanScreen.kt:629` (orange CTA).
- Delete `elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)` in `ui/settings/SettingsScreen.kt` (4 cards) and `ui/permissions/HealthConnectPermissionScreen.kt` (1 card).
- Delete `tonalElevation = 4.dp` and `shadowElevation = 4.dp` on the chat input `Surface` in `ui/chat/ChatScreen.kt`.
- Leave `AlertDialog`s alone (platform elevation is internal).
- Remove imports that become unused.

After this, `rg "shadowElevation|\\.shadow\\(|cardElevation|tonalElevation" app/src/main/java` must return no matches.

---

## 3. Remove the BioTrace wordmark

- In `BioTraceTopBar`, delete the `"BioTrace"` text + orange dot row; the left side is now only the optional back arrow.
- Delete the `subtitle` parameter from `BioTraceTopBar` and remove the `subtitle = "..."` argument from every call site (`VitalsScreen`, `LabsScreen`, `FoodScanScreen`, `BiometricInsightsScreen`, `ChatScreen`, `DashboardScreen`, `SettingsScreen`).
- Change the dashboard fallback greeting `"Welcome to BioTrace"` to `"Welcome!"` in `ui/dashboard/DashboardScreen.kt`.
- Do NOT remove "BioTrace v1.0.0" from the About card, and do not change onboarding branding.

---

## 4. Swipeable tabs with a pager

Convert the four bottom tabs into a single `HorizontalPager` shell. Keep NavHost for Settings, VitalDetail, Onboarding, Permissions, and Copilot.

### 4a. `ui/common/BioTraceFloatingBottomBar.kt`
Change the signature to:

```kotlin
fun BioTraceFloatingBottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
)
```

- Iterate `bioTraceBottomTabs.withIndex()`; `isSelected = index == selectedIndex`.
- On click, call `onSelect(index)` when not already selected.
- Keep the existing colors/animation/geometry.

### 4b. `ui/navigation/BioTraceNavHost.kt`
Restructure as follows:

```kotlin
val pagerState = rememberPagerState(pageCount = { bioTraceBottomTabs.size })
val scope = rememberCoroutineScope()
val currentRoute = ...
val onMainShell = currentRoute == Screen.Main.route
```

- Scaffold `bottomBar`: show `BioTraceFloatingBottomBar(selectedIndex = pagerState.currentPage, onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } })` only when `onMainShell`.
- `NavHost(startDestination = Screen.Main.route, ...)`.
- Replace the four separate `composable(Screen.Vitals/Labs/Nutrition/BiometricInsights.route)` blocks with one:

```kotlin
composable(Screen.Main.route) {
    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 3,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> VitalsScreen(
                onNavigateToDetail = { navController.navigate(Screen.VitalDetail.createRoute(it.name)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) }
            )
            1 -> LabsScreen(onNavigateToSettings = { navController.navigate(Screen.Settings.route) })
            2 -> FoodScanScreen(onNavigateToSettings = { navController.navigate(Screen.Settings.route) })
            3 -> BiometricInsightsScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToLabs = { scope.launch { pagerState.animateScrollToPage(1) } }
            )
        }
    }
}
```

- `OnboardingScreen` `onGetStarted` navigates to `Screen.Main.route`.
- Add at the top level of `BioTraceNavHost` (outside the Scaffold/NavHost content is fine, as long as it is in the composable body):

```kotlin
BackHandler(enabled = onMainShell && pagerState.currentPage != 0) {
    scope.launch { pagerState.animateScrollToPage(0) }
}
```

- Keep the Settings, VitalDetail, Permissions, Onboarding, and Copilot composables.
- Do not wrap or change the internal `Scaffold`/content padding of the four screens; each page keeps its own scroll state because `beyondViewportPageCount = 3` keeps all pages composed.

Imports to add: `androidx.activity.compose.BackHandler`, `androidx.compose.foundation.layout.fillMaxSize`, `androidx.compose.foundation.pager.HorizontalPager`, `androidx.compose.foundation.pager.rememberPagerState`, `androidx.compose.runtime.rememberCoroutineScope`, `kotlinx.coroutines.launch`.

### 4c. Behavior checks
- Tapping a bottom-bar icon animates to that page.
- Swiping left/right moves one page and does not wrap at the ends.
- The bottom bar is hidden on Settings/detail.
- Lab Hub's inner `LazyRow` still scrolls horizontally without switching tabs.
- Back from a non-first tab returns to Vitals; back again leaves the app.
- Settings/detail navigation preserves the selected pager page.

---

## Verification before reporting

Run and report exact results for:

```
rg -n "onAvatarClick|onSettingsClick|DataSourceDialog|WearableConnectionDialog" app/src/main/java
rg -n "subtitle" app/src/main/java/io/biotrace/app/ui/common/BioTraceTopBar.kt
rg -n "shadowElevation|\.shadow\(|cardElevation|tonalElevation" app/src/main/java
rg -n "Welcome to BioTrace" app/src/main/java
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
```

All four greps must be empty (no matches). The Gradle command must succeed. Also confirm every `BioTraceTopBar(` call site passes only `onProfileClick`/`onBackClick`/`modifier`.

Return the structured report (status, summary, filesChanged, commandsRun, testsPassed, blockers).
