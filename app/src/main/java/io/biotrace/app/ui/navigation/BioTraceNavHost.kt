package io.biotrace.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
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

@Composable
fun BioTraceNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Vitals.route

    val isTopLevelRoute = bioTraceBottomTabs.any { it.route == currentRoute }

    Scaffold(
        containerColor = AppCanvasBackground,
        bottomBar = {
            if (isTopLevelRoute) {
                BioTraceFloatingBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Vitals.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGetStarted = {
                        navController.navigate(Screen.Vitals.route) {
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
            composable(Screen.Vitals.route) {
                VitalsScreen(
                    onNavigateToDetail = { metricType ->
                        navController.navigate(Screen.VitalDetail.createRoute(metricType.name))
                    },
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) }
                )
            }
            composable(
                route = Screen.VitalDetail.route,
                arguments = listOf(navArgument("metricType") { type = NavType.StringType })
            ) {
                VitalDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Labs.route) {
                LabsScreen()
            }
            composable(Screen.Nutrition.route) {
                FoodScanScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.BiometricInsights.route) {
                BiometricInsightsScreen(
                    onNavigateToLabs = {
                        navController.navigate(Screen.Labs.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Copilot.route) {
                ChatScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) }
                )
            }
        }
    }
}
