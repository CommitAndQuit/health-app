package io.biotrace.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import io.biotrace.app.ui.theme.BioRed
import io.biotrace.app.ui.theme.DarkBackground
import io.biotrace.app.ui.theme.DarkSurfaceBorder
import io.biotrace.app.ui.theme.TextGray

data class BottomNavItem(
    val titleResId: Int,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_vitals, Icons.Default.MonitorHeart, Screen.Vitals.route),
    BottomNavItem(R.string.nav_labs, Icons.Default.Biotech, Screen.Labs.route),
    BottomNavItem(R.string.nav_food, Icons.Default.Restaurant, Screen.Nutrition.route),
    BottomNavItem(R.string.nav_copilot, Icons.Default.Psychology, Screen.Copilot.route)
)

@Composable
fun BioTraceNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelRoute = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            if (isTopLevelRoute) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground)
                        .navigationBarsPadding()
                ) {
                    // Subtle top border divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkSurfaceBorder)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentRoute == item.route
                            val label = stringResource(item.titleResId)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (!isSelected) {
                                            navController.navigate(item.route) {
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
                                    imageVector = item.icon,
                                    contentDescription = label,
                                    tint = if (isSelected) BioRed else TextGray,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BioRed else TextGray
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                // Active glowing red dot indicator beneath the label
                                if (isSelected) {
                                    Surface(
                                        color = BioRed,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(4.5.dp)
                                            .shadow(elevation = 4.dp, shape = CircleShape, ambientColor = BioRed, spotColor = BioRed)
                                    ) {}
                                } else {
                                    Spacer(modifier = Modifier.size(4.5.dp))
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
                FoodScanScreen()
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
