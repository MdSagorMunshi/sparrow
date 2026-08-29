package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.ryanshelby.spw.wallet.ui.theme.FinancialBalanceStyle
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import java.util.Locale

/**
 * Institutional Animated Balance Counter
 * Physics-interpolated number transitions that count up and down smoothly when balance changes.
 */
@Composable
fun AnimatedBalanceCounter(
    targetBalance: Double,
    hideBalance: Boolean,
    modifier: Modifier = Modifier,
    unit: String = "SPW",
    style: TextStyle = FinancialBalanceStyle,
    color: Color = TextPrimary
) {
    val animatedBalance = remember { Animatable(targetBalance.toFloat()) }

    LaunchedEffect(targetBalance) {
        animatedBalance.animateTo(
            targetValue = targetBalance.toFloat(),
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    AnimatedContent(
        targetState = hideBalance,
        transitionSpec = {
            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
        },
        label = "balanceVisibilityAnim",
        modifier = modifier
    ) { isHidden ->
        if (isHidden) {
            Text(
                text = "•••••••• $unit",
                style = style,
                color = color
            )
        } else {
            val formatted = formatBalanceString(animatedBalance.value.toDouble())
            Text(
                text = "$formatted $unit",
                style = style,
                color = color
            )
        }
    }
}

/**
 * Format balance cleanly with up to 8 decimals, stripping unnecessary trailing zeroes,
 * keeping at least two decimals for financial clarity.
 */
internal fun formatBalanceString(balance: Double): String {
    if (balance == 0.0) return "0.00"
    val raw = String.format(Locale.US, "%.8f", balance)
    val trimmed = raw.trimEnd('0')
    return if (trimmed.endsWith('.')) {
        "${trimmed}00"
    } else if (trimmed.substringAfter('.').length == 1) {
        "${trimmed}0"
    } else {
        trimmed
    }
}
