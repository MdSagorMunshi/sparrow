package com.ryanshelby.spw.wallet.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = FinanceBackground,
    primaryContainer = AccentMuted,
    onPrimaryContainer = AccentPrimary,
    secondary = SurfaceElevated,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceSubtle,
    onSecondaryContainer = TextSecondary,
    tertiary = SemanticNeutral,
    onTertiary = TextPrimary,
    background = FinanceBackground,
    onBackground = TextPrimary,
    surface = SurfacePrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderStrong,
    error = SemanticError,
    onError = TextPrimary,
    errorContainer = SemanticErrorMuted,
    onErrorContainer = SemanticError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = FinanceBackground.toArgb()
            window.navigationBarColor = FinanceBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
