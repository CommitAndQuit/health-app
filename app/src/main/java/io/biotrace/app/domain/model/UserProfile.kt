package io.biotrace.app.domain.model

data class UserProfile(
    val userName: String = "BioTrace User",
    val stepsGoal: Int = 8_000,
    val sleepGoalMinutes: Int = 420
)
