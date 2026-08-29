package com.ryanshelby.spw.wallet

import androidx.compose.ui.graphics.Color
import com.ryanshelby.spw.wallet.ui.theme.AppThemeState
import com.ryanshelby.spw.wallet.ui.theme.DarkWalletPalette
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.LightWalletPalette
import com.ryanshelby.spw.wallet.ui.theme.OledWalletPalette
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {

    @Test
    fun testDefaultThemeIsDark() {
        AppThemeState.setTheme(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, AppThemeState.currentTheme)
        assertEquals(DarkWalletPalette.background, FinanceBackground)
        assertEquals(Color(0xFF0C0E12), FinanceBackground)
        assertEquals(Color(0xFF14171F), SurfacePrimary)
        assertTrue(AppThemeState.colors.isDark)
    }

    @Test
    fun testOledThemeIsPureBlack() {
        AppThemeState.setTheme(ThemeMode.OLED)
        assertEquals(ThemeMode.OLED, AppThemeState.currentTheme)
        // OLED must be pure 0x000000 true black for subpixel power shutoff
        assertEquals(Color(0xFF000000), FinanceBackground)
        assertEquals(Color(0xFF000000), SurfacePrimary)
        assertEquals(Color(0xFFFFFFFF), TextPrimary)
        assertEquals(OledWalletPalette.background, FinanceBackground)
        assertTrue(AppThemeState.colors.isDark)
    }

    @Test
    fun testLightThemeIsCleanLightSlate() {
        AppThemeState.setTheme(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, AppThemeState.currentTheme)
        assertEquals(Color(0xFFF8FAFC), FinanceBackground)
        assertEquals(Color(0xFFFFFFFF), SurfacePrimary)
        assertEquals(Color(0xFF0F172A), TextPrimary)
        assertEquals(LightWalletPalette.background, FinanceBackground)
        org.junit.Assert.assertFalse(AppThemeState.colors.isDark)
    }
}
