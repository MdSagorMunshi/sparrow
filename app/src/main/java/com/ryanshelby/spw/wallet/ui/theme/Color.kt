package com.ryanshelby.spw.wallet.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================================================
// Financial-Grade Monochrome & Restrained Emerald Design System (Coinbase/Apple style)
// ==========================================================================

// Foundation & Surfaces (Deep, credible near-black / graphite)
val FinanceBackground = Color(0xFF0C0E12)
val SurfacePrimary = Color(0xFF14171F)
val SurfaceElevated = Color(0xFF1C212B)
val SurfaceSubtle = Color(0xFF222836)
val BorderSubtle = Color(0xFF1E2430)
val BorderStrong = Color(0xFF333B4D)

// Typography & Content Hierarchy (Soft ivory / clean neutral slate)
val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF64748B)
val DividerColor = Color(0xFF1E2430)

// Primary Action Buttons (Authoritative pure white on dark graphite, Coinbase/Apple style)
val ButtonPrimary = Color(0xFFFFFFFF)
val ButtonPrimaryText = Color(0xFF0C0E12)
val ButtonSecondary = Color(0xFF1C212B)
val ButtonSecondaryText = Color(0xFFF9FAFB)

// Restrained Accent: Deep Emerald strictly for positive balance changes and live status
val AccentPrimary = Color(0xFF10B981)
val AccentMuted = Color(0x1F10B981) // 12% opacity tint
val AccentHover = Color(0xFF059669)

// Semantic Financial Indicators
val SemanticPositive = Color(0xFF10B981) // Incoming transactions (+SPW), positive gains, confirmed status
val SemanticPositiveMuted = Color(0x1F10B981)
val SemanticError = Color(0xFFEF4444) // Negative values, failed states, destructive actions
val SemanticErrorMuted = Color(0x1FEF4444)
val SemanticWarning = Color(0xFFF59E0B) // Pending confirmations, warnings
val SemanticWarningMuted = Color(0x1FF59E0B)
val SemanticNeutral = Color(0xFF64748B) // Informational pills

// Shimmer gradient colors
val ShimmerBase = Color(0xFF14171F)
val ShimmerHighlight = Color(0xFF222938)

// ==========================================================================
// Compatibility Aliases (Map to clean neutral slate / white rather than green)
// ==========================================================================
val DarkBackground = FinanceBackground
val DarkSurface = SurfacePrimary
val DarkSurfaceElevated = SurfaceElevated
val GlassCardBackground = SurfacePrimary
val GlassCardBorder = BorderSubtle
val GlassCardBorderSecondary = BorderSubtle

val CyanNeon = TextPrimary // Map legacy cyan to clean text/white, NOT green!
val CyanGlow = TextSecondary
val PurpleNeon = SurfaceElevated
val PurpleGlow = SurfaceElevated
val GreenEmerald = SemanticPositive
val RedCoral = SemanticError
val AmberGold = SemanticWarning
val BlueCobalt = SemanticNeutral

// Calm, subtle gradients
val HolographicCardGradient = Brush.linearGradient(
    colors = listOf(
        SurfacePrimary,
        SurfaceElevated
    )
)

val CyanPurpleGradient = Brush.linearGradient(
    colors = listOf(
        SurfaceElevated,
        SurfaceSubtle
    )
)

val GlowBackgroundGradient = Brush.radialGradient(
    colors = listOf(
        Color(0x05FFFFFF),
        Color.Transparent
    )
)
