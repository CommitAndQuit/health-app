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
