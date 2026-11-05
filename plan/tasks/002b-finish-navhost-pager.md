# Task 002b — finish the pager shell in BioTraceNavHost

Task 002 was partially applied: all UI screens, the top bar, the bottom bar, and Settings were updated, but `ui/navigation/BioTraceNavHost.kt` was NOT changed, so the project currently fails to compile. Finish workstream 4 exactly as specified below.

Do not commit or push. Only edit `app/src/main/java/io/biotrace/app/ui/navigation/BioTraceNavHost.kt`, except that you may fix compile errors caused by the already-applied task 002 edits (unused/incorrect imports or call sites). Do not revert any task 002 visual changes.

## Required file content

Replace the whole file with the equivalent of:

```kotlin
package io.biotrace.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.biotrace.app.ui.chat.ChatScreen
import io.biotrace.app.ui.common.BioTraceFloatingBottomBar
import io.biotrace.app.ui.common.bioTraceBottomTabs
import io.biotrace.app.ui.insights.BiometricInsightsScreen
import io.biotrace.app.ui.labs.LabsScreen
import io.biotrace.app.ui.nutrition.FoodScanScreen
import io.biotrace.app.ui.onboarding.OnboardingScreen
import io.biotrace.app.ui.permissions.HealthConnectPermissionScreen
import io.biotrace.app.ui.settings.SettingsScreen
import io.biotrace.app.ui.theme.AppCanvasBackground
import io.biotrace.app.ui.vitals.VitalDetailScreen
import io.biotrace.app.ui.vitals.VitalsScreen
import kotlinx.coroutines.launch

@Composable
fun BioTraceNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Main.route
    val pagerState = rememberPagerState(pageCount = { bioTraceBottomTabs.size })
    val scope = rememberCoroutineScope()
    val onMainShell = currentRoute == Screen.Main.route

    BackHandler(enabled = onMainShell && pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    Scaffold(
        containerColor = AppCanvasBackground,
        bottomBar = {
            if (onMainShell) {
                BioTraceFloatingBottomBar(
                    selectedIndex = pagerState.currentPage,
                    onSelect = { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Main.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGetStarted = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Permissions.route) {
                HealthConnectPermissionScreen(
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Screen.Main.route) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 3,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> VitalsScreen(
                            onNavigateToDetail = { metricType ->
                                navController.navigate(
                                    Screen.VitalDetail.createRoute(metricType.name)
                                )
                            },
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onNavigateToPermissions = {
                                navController.navigate(Screen.Permissions.route)
                            }
                        )
                        1 -> LabsScreen(
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            }
                        )
                        2 -> FoodScanScreen(
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            }
                        )
                        3 -> BiometricInsightsScreen(
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onNavigateToLabs = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            }
                        )
                    }
                }
            }
            composable(
                route = Screen.VitalDetail.route,
                arguments = listOf(navArgument("metricType") { type = NavType.StringType })
            ) {
                VitalDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Copilot.route) {
                ChatScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) }
                )
            }
        }
    }
}
```

## Then fix compile errors

Run `./gradlew :app:compileDebugKotlin`. If errors exist in the task-002 files (for example an import removed that is still used, or a screen call site not updated), fix them minimally. Do not undo the intended behavior (no shadows, no wordmark, unified profile navigation, pager tabs).

Then run `./gradlew :app:testDebugUnitTest`.

Finally run:

```
rg -n "onAvatarClick|onSettingsClick|DataSourceDialog|WearableConnectionDialog" app/src/main/java
rg -n "subtitle" app/src/main/java/io/biotrace/app/ui/common/BioTraceTopBar.kt
rg -n "shadowElevation|\.shadow\(|cardElevation|tonalElevation" app/src/main/java
```

All three must be empty.

Return the structured report.
