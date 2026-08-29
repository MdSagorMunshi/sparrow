package com.ryanshelby.spw.wallet.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Supported Theme Modes in SPARROW Wallet.
 */
enum class ThemeMode(val displayName: String, val description: String) {
    DARK(
        displayName = "Dark (Default)",
        description = "Deep charcoal & graphite financial palette"
    ),
    LIGHT(
        displayName = "Light",
        description = "Crisp financial-grade white & clean slate"
    ),
    OLED(
        displayName = "OLED Pure Black",
        description = "100% True Black (#000000) for OLED/AMOLED panels"
    )
}

/**
 * Immutable palette tokens per theme mode.
 */
data class WalletColors(
    val background: Color,
    val surfacePrimary: Color,
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val buttonPrimary: Color,
    val buttonPrimaryText: Color,
    val buttonSecondary: Color,
    val buttonSecondaryText: Color,
    val accentPrimary: Color,
    val semanticPositive: Color,
    val semanticError: Color,
    val semanticWarning: Color,
    val semanticNeutral: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val isDark: Boolean
)

val DarkWalletPalette = WalletColors(
    background = Color(0xFF0C0E12),
    surfacePrimary = Color(0xFF14171F),
    surfaceElevated = Color(0xFF1C212B),
    surfaceSubtle = Color(0xFF222836),
    borderSubtle = Color(0xFF1E2430),
    borderStrong = Color(0xFF333B4D),
    textPrimary = Color(0xFFF9FAFB),
    textSecondary = Color(0xFF9CA3AF),
    textMuted = Color(0xFF64748B),
    buttonPrimary = Color(0xFFFFFFFF),
    buttonPrimaryText = Color(0xFF0C0E12),
    buttonSecondary = Color(0xFF1C212B),
    buttonSecondaryText = Color(0xFFF9FAFB),
    accentPrimary = Color(0xFF10B981),
    semanticPositive = Color(0xFF10B981),
    semanticError = Color(0xFFEF4444),
    semanticWarning = Color(0xFFF59E0B),
    semanticNeutral = Color(0xFF64748B),
    shimmerBase = Color(0xFF14171F),
    shimmerHighlight = Color(0xFF222938),
    isDark = true
)

val LightWalletPalette = WalletColors(
    background = Color(0xFFF8FAFC),
    surfacePrimary = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F5F9),
    surfaceSubtle = Color(0xFFE2E8F0),
    borderSubtle = Color(0xFFE2E8F0),
    borderStrong = Color(0xFFCBD5E1),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    buttonPrimary = Color(0xFF0F172A),
    buttonPrimaryText = Color(0xFFFFFFFF),
    buttonSecondary = Color(0xFFE2E8F0),
    buttonSecondaryText = Color(0xFF0F172A),
    accentPrimary = Color(0xFF059669),
    semanticPositive = Color(0xFF059669),
    semanticError = Color(0xFFDC2626),
    semanticWarning = Color(0xFFD97706),
    semanticNeutral = Color(0xFF94A3B8),
    shimmerBase = Color(0xFFE2E8F0),
    shimmerHighlight = Color(0xFFF8FAFC),
    isDark = false
)

val OledWalletPalette = WalletColors(
    background = Color(0xFF000000),       // Pure 0x000000 true black (OLED subpixel shutoff)
    surfacePrimary = Color(0xFF000000),   // Pure black card backgrounds
    surfaceElevated = Color(0xFF0A0A0A),  // Ultra-subtle elevated black
    surfaceSubtle = Color(0xFF121212),
    borderSubtle = Color(0xFF1F1F1F),     // Crisp hairline border
    borderStrong = Color(0xFF303030),
    textPrimary = Color(0xFFFFFFFF),      // 100% stark white
    textSecondary = Color(0xFFA3A3A3),
    textMuted = Color(0xFF525252),
    buttonPrimary = Color(0xFFFFFFFF),
    buttonPrimaryText = Color(0xFF000000),
    buttonSecondary = Color(0xFF121212),
    buttonSecondaryText = Color(0xFFFFFFFF),
    accentPrimary = Color(0xFF10B981),
    semanticPositive = Color(0xFF10B981),
    semanticError = Color(0xFFEF4444),
    semanticWarning = Color(0xFFF59E0B),
    semanticNeutral = Color(0xFF525252),
    shimmerBase = Color(0xFF0A0A0A),
    shimmerHighlight = Color(0xFF181818),
    isDark = true
)

/**
 * Global reactive theme state.
 * Any Compose component reading [Color.kt] tokens automatically recomposes on change.
 */
object AppThemeState {
    var currentTheme by mutableStateOf(ThemeMode.DARK)
        private set

    var colors by mutableStateOf(DarkWalletPalette)
        private set

    fun setTheme(theme: ThemeMode) {
        currentTheme = theme
        colors = when (theme) {
            ThemeMode.DARK -> DarkWalletPalette
            ThemeMode.LIGHT -> LightWalletPalette
            ThemeMode.OLED -> OledWalletPalette
        }
    }
}
