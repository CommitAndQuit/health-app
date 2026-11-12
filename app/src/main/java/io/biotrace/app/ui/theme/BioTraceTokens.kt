package io.biotrace.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    // Borders
    val CardHairlineBorder = BorderStroke(1.dp, CardBorder)
    val WellHairlineBorder = BorderStroke(1.dp, DividerSubtle)
}
