package io.biotrace.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.ui.chat.ChatScreen
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppCanvasBackground)
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Main.route,
            modifier = Modifier.fillMaxSize()
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
                VitalsScreen(
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
                    },
                    onNavigateToCopilot = {
                        navController.navigate(Screen.Copilot.route)
                    },
                    onNavigateToInsights = {
                        navController.navigate(Screen.BiometricInsights.route)
                    },
                    onNavigateToLabs = {
                        navController.navigate(Screen.Labs.route)
                    },
                    onNavigateToNutrition = {
                        navController.navigate(Screen.Nutrition.route)
                    }
                )
            }
            composable(Screen.Labs.route) {
                LabsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Nutrition.route) {
                FoodScanScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.BiometricInsights.route) {
                BiometricInsightsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLabs = { navController.navigate(Screen.Labs.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
            composable(Screen.Copilot.route) {
                ChatScreen(
                    onNavigateBack = { navController.popBackStack() },
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
