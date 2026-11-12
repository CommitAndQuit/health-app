package io.biotrace.app.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// BioTrace Design System Color Palette (Reference UI Extraction)
// =============================================================================

// Background & Surfaces
val AppCanvasBackground = Color(0xFF0B101B) // Deep Obsidian slate background
val CardSurface = Color(0xFF141C2B)        // Dark navy card surfaces
val InnerWellSurface = Color(0xFF0F172A)   // Recessed slate container
val CardBorder = Color(0xFF1E293B)         // Hairline slate borders
val DividerSubtle = Color(0xFF1E293B)      // Thin hairline divider

// Brand & Primary Accent (Bio-Orange)
val BioOrangePrimary = Color(0xFFFC5723)   // Primary brand vermilion orange
val BioOrangeSoftTint = Color(0xFF2E1912)  // Soft dark peach background fill
val BioOrangeGradientTop = Color(0x40FC5723) // 25% alpha
val BioOrangeGradientBottom = Color(0x00FC5723) // 0% alpha

// Semantic Metric Accents: Emerald Mint (Synced Status, Relaxed Stress, Positive Deltas)
val EmeraldPrimary = Color(0xFF34D399)
val EmeraldSoftTint = Color(0xFF064E3B).copy(alpha = 0.35f)

// Solar Amber (Daily Steps & Medium Stress / Warnings)
val SolarAmberPrimary = Color(0xFFF19F0F)
val SolarAmberSoftTint = Color(0xFF291E0B)

// Cobalt Blue (Blood Oxygen SpO2 & Normal Stress)
val CobaltBluePrimary = Color(0xFF38BDF8)
val CobaltBlueSoftTint = Color(0xFF172554)

// Indigo Lavender (Sleep Recovery)
val IndigoSleepPrimary = Color(0xFF818CF8)
val IndigoSleepSoftTint = Color(0xFF1E1B4B)

// Coral / Alert Red (High Stress & Out-of-Range Alerts)
val CoralAlertPrimary = Color(0xFFF87171)
val CoralAlertSoftTint = Color(0xFF1F141B)

// Typography Colors
val TextPrimary = Color(0xFFF8FAFC)        // Crisp white text
val TextSecondary = Color(0xFF94A3B8)      // Cool slate secondary text
val TextMuted = Color(0xFF64748B)          // Muted slate captions
val TextWhite = Color(0xFFFFFFFF)

// Semantic & Copilot Tokens (Reference Renders)
val BioTraceDot = Color(0xFF34D399)
val ProfileAvatarBg = Color(0xFFA7F3D0)
val ProfileAvatarIcon = Color(0xFF064E3B)
val CopilotGlowStart = Color(0xFF38BDF8)
val CopilotGlowEnd = Color(0xFFA855F7)
val CopilotBadgeBg = Color(0xFF3B0764)
val CopilotBadgeText = Color(0xFFD8B4FE)
val StrainAlertBg = Color(0xFF1F141B)
val StrainAlertBorder = Color(0xFF381C26)
val StrainAlertText = Color(0xFFF87171)
val SleepDeepColor = Color(0xFF818CF8)
val SleepRemColor = Color(0xFF38BDF8)
val SleepLightColor = Color(0xFF34D399)

// =============================================================================
// Compatibility Aliases for Existing Components
// =============================================================================
val LightBackground = AppCanvasBackground
val PureWhite = CardSurface
val SoftInputBg = InnerWellSurface

val BioOrange = BioOrangePrimary
val BioOrangeBright = Color(0xFFFF6D00)
val BioOrangeLight = BioOrangeSoftTint
val BioOrangeDark = Color(0xFFE64A19)

val ClinicalEmerald = EmeraldPrimary
val ClinicalEmeraldDark = Color(0xFF059669)
val MintBadgeBg = Color(0xFF064E3B).copy(alpha = 0.35f)
val MintBadgeText = BioTraceDot

val MacroProteinColor = EmeraldPrimary
val MacroCarbsColor = SolarAmberPrimary
val MacroFatsColor = Color(0xFF475569)

val CorrelationPeachBg = Color(0xFF241816)
val CorrelationPeachBorder = Color(0xFF381C26)

val TextDark = TextPrimary
val TextGray = TextSecondary

val AlertRed = CoralAlertPrimary
val AlertRedLight = StrainAlertBg
val AlertRedText = StrainAlertText

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
val StatusOptimalBadge = MintBadgeBg
val StatusOptimalText = MintBadgeText

val HeroCardBg = CardSurface
val HeroCardTextPrimary = TextPrimary
val HeroCardTextSecondary = TextSecondary
val HeroCardTrack = DividerSubtle
val HeroCardButtonSecondary = InnerWellSurface

// Core Material theme mappings
val Primary = BioOrangePrimary
val OnPrimary = TextWhite
val PrimaryContainer = BioOrangeSoftTint
val OnPrimaryContainer = BioOrangeBright

val Secondary = TextSecondary
val OnSecondary = TextWhite
val SecondaryContainer = InnerWellSurface
val OnSecondaryContainer = TextPrimary

val Tertiary = EmeraldPrimary
val OnTertiary = TextWhite
val TertiaryContainer = EmeraldSoftTint
val OnTertiaryContainer = EmeraldPrimary

val Error = CoralAlertPrimary
val OnError = TextWhite
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
