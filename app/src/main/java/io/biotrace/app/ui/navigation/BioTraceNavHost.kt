package io.biotrace.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.biotrace.app.R
import io.biotrace.app.ui.chat.ChatScreen
import io.biotrace.app.ui.dashboard.DashboardScreen
import io.biotrace.app.ui.labs.LabsScreen
import io.biotrace.app.ui.nutrition.FoodScanScreen
import io.biotrace.app.ui.onboarding.OnboardingScreen
import io.biotrace.app.ui.permissions.HealthConnectPermissionScreen
import io.biotrace.app.ui.settings.SettingsScreen
import io.biotrace.app.ui.theme.BioOrange
import io.biotrace.app.ui.theme.CardBorder
import io.biotrace.app.ui.theme.LightBackground
import io.biotrace.app.ui.theme.PureWhite
import io.biotrace.app.ui.theme.TextDark
import io.biotrace.app.ui.theme.TextMuted
import io.biotrace.app.ui.theme.TextSecondary

data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val isHero: Boolean = false
)

val bottomNavTabs = listOf(
    NavTabItem("Vitals", Icons.Default.FavoriteBorder, Screen.Vitals.route),
    NavTabItem("Lab Hub", Icons.Default.Science, Screen.Labs.route),
    NavTabItem("Macro Cam", Icons.Default.CameraAlt, Screen.Nutrition.route, isHero = true),
    NavTabItem("AI Copilot", Icons.Default.Psychology, Screen.Copilot.route)
)

@Composable
fun BioTraceNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Nutrition.route

    val isTopLevelRoute = bottomNavTabs.any { it.route == currentRoute }

    Scaffold(
        containerColor = LightBackground,
        bottomBar = {
            if (isTopLevelRoute) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = PureWhite,
                        shape = RoundedCornerShape(32.dp),
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomNavTabs.forEach { tab ->
                                val isSelected = currentRoute == tab.route

                                if (tab.isHero) {
                                    // Hero Center Button (Macro Cam)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .clickable {
                                                if (!isSelected) {
                                                    navController.navigate(tab.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(BioOrange)
                                                .shadow(elevation = 6.dp, shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                tint = PureWhite,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = tab.title,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BioOrange
                                        )
                                    }
                                } else {
                                    // Regular Navigation Tab
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (!isSelected) {
                                                    navController.navigate(tab.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) TextDark else TextMuted,
                                            modifier = Modifier.size(22.dp)
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Text(
                                            text = tab.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) TextDark else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Nutrition.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onGetStarted = {
                        navController.navigate(Screen.Nutrition.route) {
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
                DashboardScreen(
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToCopilot = {
                        navController.navigate(Screen.Copilot.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
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
