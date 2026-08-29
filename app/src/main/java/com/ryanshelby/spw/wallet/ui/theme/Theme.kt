package com.ryanshelby.spw.wallet.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val themeColors = AppThemeState.colors
    val isDark = themeColors.isDark

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = themeColors.accentPrimary,
            onPrimary = themeColors.background,
            primaryContainer = themeColors.surfaceSubtle,
            onPrimaryContainer = themeColors.accentPrimary,
            secondary = themeColors.surfaceElevated,
            onSecondary = themeColors.textPrimary,
            secondaryContainer = themeColors.surfaceSubtle,
            onSecondaryContainer = themeColors.textSecondary,
            tertiary = themeColors.semanticNeutral,
            onTertiary = themeColors.textPrimary,
            background = themeColors.background,
            onBackground = themeColors.textPrimary,
            surface = themeColors.surfacePrimary,
            onSurface = themeColors.textPrimary,
            surfaceVariant = themeColors.surfaceElevated,
            onSurfaceVariant = themeColors.textSecondary,
            outline = themeColors.borderSubtle,
            outlineVariant = themeColors.borderStrong,
            error = themeColors.semanticError,
            onError = themeColors.textPrimary,
            errorContainer = themeColors.semanticError.copy(alpha = 0.12f),
            onErrorContainer = themeColors.semanticError
        )
    } else {
        lightColorScheme(
            primary = themeColors.accentPrimary,
            onPrimary = themeColors.textPrimary,
            primaryContainer = themeColors.surfaceSubtle,
            onPrimaryContainer = themeColors.accentPrimary,
            secondary = themeColors.surfaceElevated,
            onSecondary = themeColors.textPrimary,
            secondaryContainer = themeColors.surfaceSubtle,
            onSecondaryContainer = themeColors.textSecondary,
            tertiary = themeColors.semanticNeutral,
            onTertiary = themeColors.textPrimary,
            background = themeColors.background,
            onBackground = themeColors.textPrimary,
            surface = themeColors.surfacePrimary,
            onSurface = themeColors.textPrimary,
            surfaceVariant = themeColors.surfaceElevated,
            onSurfaceVariant = themeColors.textSecondary,
            outline = themeColors.borderSubtle,
            outlineVariant = themeColors.borderStrong,
            error = themeColors.semanticError,
            onError = themeColors.textPrimary,
            errorContainer = themeColors.semanticError.copy(alpha = 0.12f),
            onErrorContainer = themeColors.semanticError
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = themeColors.background.toArgb()
                window.navigationBarColor = themeColors.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
