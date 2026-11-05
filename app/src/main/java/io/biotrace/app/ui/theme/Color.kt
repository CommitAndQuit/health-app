package io.biotrace.app.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// BioTrace Design System Color Palette (Reference UI Extraction)
// =============================================================================

// Background & Surfaces
val AppCanvasBackground = Color(0xFFF7F8FC) // Cool porcelain off-white
val CardSurface = Color(0xFFFFFFFF)        // Pure white elevated card surface
val InnerWellSurface = Color(0xFFF6F7FB)   // Subtle recessed grey-blue container
val DividerSubtle = Color(0xFFE2E8F0)      // Thin hairline divider

// Brand & Primary Accent (Bio-Orange)
val BioOrangePrimary = Color(0xFFFC5723)   // Primary brand vermilion orange
val BioOrangeSoftTint = Color(0xFFFFF0EB)  // Soft peach background fill
val BioOrangeGradientTop = Color(0x40FC5723) // 25% alpha
val BioOrangeGradientBottom = Color(0x00FC5723) // 0% alpha

// Semantic Metric Accents: Emerald Mint (Synced Status, Relaxed Stress, Positive Deltas)
val EmeraldPrimary = Color(0xFF24AD84)
val EmeraldSoftTint = Color(0xFFEAFBF5)

// Solar Amber (Daily Steps & Medium Stress / Warnings)
val SolarAmberPrimary = Color(0xFFF19F0F)
val SolarAmberSoftTint = Color(0xFFFFF7E6)

// Cobalt Blue (Blood Oxygen SpO2 & Normal Stress)
val CobaltBluePrimary = Color(0xFF2563EB)
val CobaltBlueSoftTint = Color(0xFFEEF4FF)

// Indigo Lavender (Sleep Recovery)
val IndigoSleepPrimary = Color(0xFF5B4FE9)
val IndigoSleepSoftTint = Color(0xFFF1F0FF)

// Coral / Alert Red (High Stress & Out-of-Range Alerts)
val CoralAlertPrimary = Color(0xFFEF4444)
val CoralAlertSoftTint = Color(0xFFFEE2E2)

// Typography Colors
val TextPrimary = Color(0xFF0F172A)        // Deep slate black
val TextSecondary = Color(0xFF64748B)      // Muted cool slate
val TextMuted = Color(0xFF94A3B8)          // Subtle captions
val TextWhite = Color(0xFFFFFFFF)

// =============================================================================
// Compatibility Aliases for Existing Components
// =============================================================================
val LightBackground = AppCanvasBackground
val PureWhite = CardSurface
val CardBorder = Color(0xFFEEF2F6)
val SoftInputBg = InnerWellSurface

val BioOrange = BioOrangePrimary
val BioOrangeBright = Color(0xFFFF6D00)
val BioOrangeLight = BioOrangeSoftTint
val BioOrangeDark = Color(0xFFE64A19)

val ClinicalEmerald = EmeraldPrimary
val ClinicalEmeraldDark = Color(0xFF059669)
val MintBadgeBg = EmeraldSoftTint
val MintBadgeText = EmeraldPrimary

val MacroProteinColor = EmeraldPrimary
val MacroCarbsColor = SolarAmberPrimary
val MacroFatsColor = Color(0xFF475569)

val CorrelationPeachBg = Color(0xFFFFF8F5)
val CorrelationPeachBorder = Color(0xFFFFE8DC)

val TextDark = TextPrimary
val TextGray = TextSecondary

val AlertRed = CoralAlertPrimary
val AlertRedLight = CoralAlertSoftTint
val AlertRedText = Color(0xFFDC2626)

val DarkBackground = LightBackground
val DarkSurface = PureWhite
val DarkSurfaceElevated = PureWhite
val DarkSurfaceBorder = CardBorder
val DarkInputBg = SoftInputBg
val BioRed = BioOrangePrimary
val BioRedBright = BioOrangeBright
val BioRedDark = BioOrangeDark
val BioRedGlow = Color(0x33FC5723)
val StatusGreen = EmeraldPrimary
val StatusOptimalBadge = EmeraldSoftTint
val StatusOptimalText = EmeraldPrimary

val HeroCardBg = PureWhite
val HeroCardTextPrimary = TextPrimary
val HeroCardTextSecondary = TextSecondary
val HeroCardTrack = Color(0xFFE2E8F0)
val HeroCardButtonSecondary = InnerWellSurface

// Core Material theme mappings
val Primary = BioOrangePrimary
val OnPrimary = PureWhite
val PrimaryContainer = BioOrangeSoftTint
val OnPrimaryContainer = BioOrangeDark

val Secondary = TextSecondary
val OnSecondary = PureWhite
val SecondaryContainer = InnerWellSurface
val OnSecondaryContainer = TextPrimary

val Tertiary = EmeraldPrimary
val OnTertiary = PureWhite
val TertiaryContainer = EmeraldSoftTint
val OnTertiaryContainer = EmeraldPrimary

val Error = CoralAlertPrimary
val OnError = PureWhite
val ErrorContainer = CoralAlertSoftTint
val OnErrorContainer = AlertRedText

val Background = AppCanvasBackground
val OnBackground = TextPrimary
val Surface = CardSurface
val OnSurface = TextPrimary
val SurfaceVariant = CardSurface
val OnSurfaceVariant = TextSecondary
val Outline = CardBorder

// Metric indicators
val MetricHeartRate = BioOrangePrimary
val MetricHrv = IndigoSleepPrimary
val MetricSteps = SolarAmberPrimary
val MetricSleep = IndigoSleepPrimary
val MetricCalories = BioOrangePrimary
val MetricOxygen = CobaltBluePrimary
