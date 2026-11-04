package io.biotrace.app.ui.theme

import androidx.compose.ui.graphics.Color

// BioTrace Luminous Clinical Palette (Modeled after reference UI)
val LightBackground = Color(0xFFF6F8FA)
val PureWhite = Color(0xFFFFFFFF)
val CardBorder = Color(0xFFEEF2F6)
val SoftInputBg = Color(0xFFF1F5F9)

// Hero Vibrant Orange (Macro Cam, Caloric Ring, Action Buttons)
val BioOrange = Color(0xFFFF5722)
val BioOrangeBright = Color(0xFFFF6D00)
val BioOrangeLight = Color(0xFFFFF3EB)
val BioOrangeDark = Color(0xFFE64A19)

// Clinical Mint / Emerald (Restorative HRV, Protein Goals, Verified Badges)
val ClinicalEmerald = Color(0xFF10B981)
val ClinicalEmeraldDark = Color(0xFF059669)
val MintBadgeBg = Color(0xFFE6F9F0)
val MintBadgeText = Color(0xFF059669)

// Macro Distribution Colors
val MacroProteinColor = Color(0xFF10B981)
val MacroCarbsColor = Color(0xFFFF9800)
val MacroFatsColor = Color(0xFF475569)

// Pillar 5 Wearable Correlation Card
val CorrelationPeachBg = Color(0xFFFFF8F5)
val CorrelationPeachBorder = Color(0xFFFFE8DC)

// Text Colors (High contrast, clean typography)
val TextDark = Color(0xFF0F172A)
val TextSecondary = Color(0xFF64748B)
val TextMuted = Color(0xFF94A3B8)
val TextWhite = Color(0xFFFFFFFF)

// Alerts & Out of Range
val AlertRed = Color(0xFFEF4444)
val AlertRedLight = Color(0xFFFEE2E2)
val AlertRedText = Color(0xFFDC2626)

// Compatibility aliases for existing dark/cyber components
val DarkBackground = LightBackground
val DarkSurface = PureWhite
val DarkSurfaceElevated = PureWhite
val DarkSurfaceBorder = CardBorder
val DarkInputBg = SoftInputBg
val BioRed = BioOrange
val BioRedBright = BioOrangeBright
val BioRedDark = BioOrangeDark
val BioRedGlow = Color(0x33FF5722)
val StatusGreen = ClinicalEmerald
val StatusOptimalBadge = MintBadgeBg
val StatusOptimalText = MintBadgeText
val TextGray = TextSecondary
val HeroCardBg = PureWhite
val HeroCardTextPrimary = TextDark
val HeroCardTextSecondary = TextSecondary
val HeroCardTrack = Color(0xFFE2E8F0)
val HeroCardButtonSecondary = SoftInputBg

// Core Material theme mappings
val Primary = BioOrange
val OnPrimary = PureWhite
val PrimaryContainer = BioOrangeLight
val OnPrimaryContainer = BioOrangeDark

val Secondary = TextSecondary
val OnSecondary = PureWhite
val SecondaryContainer = Color(0xFFF1F5F9)
val OnSecondaryContainer = TextDark

val Tertiary = ClinicalEmerald
val OnTertiary = PureWhite
val TertiaryContainer = MintBadgeBg
val OnTertiaryContainer = MintBadgeText

val Error = AlertRed
val OnError = PureWhite
val ErrorContainer = AlertRedLight
val OnErrorContainer = AlertRedText

val Background = LightBackground
val OnBackground = TextDark
val Surface = PureWhite
val OnSurface = TextDark
val SurfaceVariant = PureWhite
val OnSurfaceVariant = TextSecondary
val Outline = CardBorder

// Metric indicators
val MetricHeartRate = BioOrange
val MetricHrv = Color(0xFF6366F1)
val MetricSteps = ClinicalEmerald
val MetricSleep = Color(0xFF8B5CF6)
val MetricCalories = BioOrange
val MetricOxygen = Color(0xFF0EA5E9)
