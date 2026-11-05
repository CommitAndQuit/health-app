package io.biotrace.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object BioTraceTokens {
    // Shapes
    val HeroCardShape = RoundedCornerShape(28.dp)
    val BentoCardShape = RoundedCornerShape(24.dp)
    val InnerWellShape = RoundedCornerShape(18.dp)
    val InnerWellSmallShape = RoundedCornerShape(14.dp)
    val RecordCardShape = RoundedCornerShape(20.dp)
    val FloatingBarShape = RoundedCornerShape(36.dp)
    val PillShape = RoundedCornerShape(50)
    val IconBadgeShape = CircleShape

    // Elevations & Shadows
    val HeroCardElevation = 6.dp
    val BentoCardElevation = 3.dp
    val FloatingBarElevation = 12.dp
    val PillElevation = 2.dp

    // Borders
    val CardHairlineBorder = BorderStroke(1.dp, Color(0xFFF1F5F9))
    val WellHairlineBorder = BorderStroke(1.dp, Color(0xFFE2E8F0))

    // Shadow Spot & Ambient Colors
    val ShadowSpotColor = Color(0x140F172A)
    val ShadowAmbientColor = Color(0x0A0F172A)
}
