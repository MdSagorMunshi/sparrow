package com.ryanshelby.spw.wallet.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Sleek Next-Gen Cyber Obsidian & Glassmorphism Colors
val DarkBackground = Color(0xFF070B14)
val DarkSurface = Color(0xFF0D1424)
val DarkSurfaceElevated = Color(0xFF141D34)
val GlassCardBackground = Color(0xCC121B30)
val GlassCardBorder = Color(0x3300E5FF)
val GlassCardBorderSecondary = Color(0x227C4DFF)

// Neon & Accent Brand Colors
val CyanNeon = Color(0xFF00F0FF)
val CyanGlow = Color(0xFF00E5FF)
val PurpleNeon = Color(0xFF7C4DFF)
val PurpleGlow = Color(0xFF9E00FF)
val GreenEmerald = Color(0xFF00E676)
val RedCoral = Color(0xFFFF3D71)
val AmberGold = Color(0xFFFFB300)
val BlueCobalt = Color(0xFF2979FF)

// Neutral & Text Tokens
val TextPrimary = Color(0xFFF0F4FF)
val TextSecondary = Color(0xFF8E9BB5)
val TextMuted = Color(0xFF53627C)
val DividerColor = Color(0x1F8E9BB5)

// Gradients
val HolographicCardGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1A2645),
        Color(0xFF0D162C),
        Color(0xFF18183B),
        Color(0xFF0B2B3F)
    )
)

val CyanPurpleGradient = Brush.linearGradient(
    colors = listOf(CyanNeon, PurpleNeon)
)

val GlowBackgroundGradient = Brush.radialGradient(
    colors = listOf(
        Color(0x3300F0FF),
        Color(0x187C4DFF),
        Color(0x00000000)
    )
)
