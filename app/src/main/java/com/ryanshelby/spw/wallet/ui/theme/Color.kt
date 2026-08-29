package com.ryanshelby.spw.wallet.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================================================
// Dynamic Financial-Grade Design System (Dark, Light, and Pure OLED Black)
// ==========================================================================

// Foundation & Surfaces (Dynamic via AppThemeState)
val FinanceBackground: Color get() = AppThemeState.colors.background
val SurfacePrimary: Color get() = AppThemeState.colors.surfacePrimary
val SurfaceElevated: Color get() = AppThemeState.colors.surfaceElevated
val SurfaceSubtle: Color get() = AppThemeState.colors.surfaceSubtle
val BorderSubtle: Color get() = AppThemeState.colors.borderSubtle
val BorderStrong: Color get() = AppThemeState.colors.borderStrong

// Typography & Content Hierarchy
val TextPrimary: Color get() = AppThemeState.colors.textPrimary
val TextSecondary: Color get() = AppThemeState.colors.textSecondary
val TextMuted: Color get() = AppThemeState.colors.textMuted
val DividerColor: Color get() = AppThemeState.colors.borderSubtle

// Primary Action Buttons
val ButtonPrimary: Color get() = AppThemeState.colors.buttonPrimary
val ButtonPrimaryText: Color get() = AppThemeState.colors.buttonPrimaryText
val ButtonSecondary: Color get() = AppThemeState.colors.buttonSecondary
val ButtonSecondaryText: Color get() = AppThemeState.colors.buttonSecondaryText

// Restrained Accent & Financial Indicators
val AccentPrimary: Color get() = AppThemeState.colors.accentPrimary
val AccentMuted: Color get() = AppThemeState.colors.accentPrimary.copy(alpha = 0.12f)
val AccentHover: Color get() = AppThemeState.colors.accentPrimary

val SemanticPositive: Color get() = AppThemeState.colors.semanticPositive
val SemanticPositiveMuted: Color get() = AppThemeState.colors.semanticPositive.copy(alpha = 0.12f)
val SemanticError: Color get() = AppThemeState.colors.semanticError
val SemanticErrorMuted: Color get() = AppThemeState.colors.semanticError.copy(alpha = 0.12f)
val SemanticWarning: Color get() = AppThemeState.colors.semanticWarning
val SemanticWarningMuted: Color get() = AppThemeState.colors.semanticWarning.copy(alpha = 0.12f)
val SemanticNeutral: Color get() = AppThemeState.colors.semanticNeutral

// Shimmer gradient colors
val ShimmerBase: Color get() = AppThemeState.colors.shimmerBase
val ShimmerHighlight: Color get() = AppThemeState.colors.shimmerHighlight

// ==========================================================================
// Compatibility Aliases
// ==========================================================================
val DarkBackground: Color get() = FinanceBackground
val DarkSurface: Color get() = SurfacePrimary
val DarkSurfaceElevated: Color get() = SurfaceElevated
val GlassCardBackground: Color get() = SurfacePrimary
val GlassCardBorder: Color get() = BorderSubtle
val GlassCardBorderSecondary: Color get() = BorderSubtle

val CyanNeon: Color get() = TextPrimary
val CyanGlow: Color get() = TextSecondary
val PurpleNeon: Color get() = SurfaceElevated
val PurpleGlow: Color get() = SurfaceElevated
val GreenEmerald: Color get() = SemanticPositive
val RedCoral: Color get() = SemanticError
val AmberGold: Color get() = SemanticWarning
val BlueCobalt: Color get() = SemanticNeutral

// Dynamic gradients
val HolographicCardGradient: Brush
    get() = Brush.linearGradient(
        colors = listOf(
            SurfacePrimary,
            SurfaceElevated
        )
    )

val CyanPurpleGradient: Brush
    get() = Brush.linearGradient(
        colors = listOf(
            SurfaceElevated,
            SurfaceSubtle
        )
    )

val GlowBackgroundGradient: Brush
    get() = Brush.radialGradient(
        colors = listOf(
            Color(0x05FFFFFF),
            Color.Transparent
        )
    )
