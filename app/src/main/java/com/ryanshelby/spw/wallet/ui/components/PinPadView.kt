package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.theme.CyanGlow
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkSurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.GlassCardBackground
import com.ryanshelby.spw.wallet.ui.theme.GlassCardBorder
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon
import com.ryanshelby.spw.wallet.ui.theme.RedCoral
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary

@Composable
fun PinPadView(
    enteredPin: String,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    isScrambled: Boolean = false,
    errorMessage: String? = null,
    maxDigits: Int = 6,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val digits = remember(isScrambled) {
        val list = (0..9).map { it.toString() }.toMutableList()
        if (isScrambled) list.shuffle() else {
            // standard order: 1-9 then 0 at the end
            list.remove("0")
            list.add("0")
        }
        list
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN Dot Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            for (i in 0 until maxDigits) {
                val isFilled = i < enteredPin.length
                val animatedScale by animateFloatAsState(
                    targetValue = if (isFilled) 1.25f else 1.0f,
                    animationSpec = tween(150, easing = FastOutSlowInEasing),
                    label = "dotScale"
                )

                Box(
                    modifier = Modifier
                        .size((16 * animatedScale).dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) {
                                Brush.radialGradient(
                                    colors = listOf(CyanNeon, PurpleNeon)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(Color(0x338E9BB5), Color(0x118E9BB5))
                                )
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFilled) CyanGlow else GlassCardBorder,
                            shape = CircleShape
                        )
                )
            }
        }

        // Error message if any
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + scaleIn()
        ) {
            errorMessage?.let {
                Text(
                    text = it,
                    color = RedCoral,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Numpad 3x4 Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rows 1-3
            for (row in 0..2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (col in 0..2) {
                        val digit = digits[row * 3 + col]
                        NumpadButton(
                            text = digit,
                            onClick = {
                                HapticUtil.lightTap(context)
                                if (enteredPin.length < maxDigits) {
                                    onDigitClick(digit)
                                }
                            }
                        )
                    }
                }
            }

            // Bottom Row: Biometric / Empty, '0', Backspace
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left slot: Biometric button or empty
                if (onBiometricClick != null) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(GlassCardBackground)
                            .border(1.dp, GlassCardBorder, CircleShape)
                            .clickable {
                                HapticUtil.heavyClick(context)
                                onBiometricClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Login",
                            tint = CyanNeon,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(72.dp))
                }

                // Center bottom digit
                val bottomDigit = digits[9]
                NumpadButton(
                    text = bottomDigit,
                    onClick = {
                        HapticUtil.lightTap(context)
                        if (enteredPin.length < maxDigits) {
                            onDigitClick(bottomDigit)
                        }
                    }
                )

                // Backspace button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(GlassCardBackground)
                        .border(1.dp, GlassCardBorder, CircleShape)
                        .clickable {
                            HapticUtil.lightTap(context)
                            onBackspaceClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Backspace",
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NumpadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(DarkSurfaceElevated.copy(alpha = 0.8f))
            .border(1.dp, GlassCardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 24.sp
        )
    }
}
