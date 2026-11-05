package io.biotrace.app.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Permissions : Screen("permissions")
    data object Main : Screen("main")
    data object Vitals : Screen("vitals")
    data object Labs : Screen("labs")
    data object Nutrition : Screen("nutrition")
    data object Copilot : Screen("copilot")
    data object Settings : Screen("settings")
    data object BiometricInsights : Screen("insights")
    data object VitalDetail : Screen("vitals_detail/{metricType}") {
        fun createRoute(metricType: String): String = "vitals_detail/$metricType"
    }

    // Aliases for compatibility
    data object Dashboard : Screen("vitals")
    data object Chat : Screen("copilot")
    data object Cardiac3D : Screen("cardiac_3d")
    data object EcgStream : Screen("ecg_stream")
}
