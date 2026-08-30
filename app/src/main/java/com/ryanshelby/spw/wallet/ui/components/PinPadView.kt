package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable

/**
 * Institutional PIN Entry & Keypad Component
 * Features tactile physics spring feedback and minimalist indicator dots.
 */
@Composable
fun PinPadView(
    enteredPin: String,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    isScrambled: Boolean = false,
    errorMessage: String? = null,
    maxDigits: Int = 6,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val digits = remember(isScrambled) {
        val list = (0..9).map { it.toString() }.toMutableList()
        if (isScrambled) list.shuffle() else {
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
            modifier = Modifier.padding(vertical = 18.dp)
        ) {
            for (i in 0 until maxDigits) {
                val isFilled = i < enteredPin.length
                val animatedScale by animateFloatAsState(
                    targetValue = if (isFilled) 1.2f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "dotScale"
                )

                Box(
                    modifier = Modifier
                        .size((14 * animatedScale).dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) TextPrimary else SurfaceSubtle,
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFilled) TextPrimary else BorderSubtle,
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
                    color = SemanticError,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3x4 Numerical Keypad Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                            isEnabled = isEnabled,
                            onClick = {
                                if (isEnabled) {
                                    HapticUtil.lightTap(context)
                                    if (enteredPin.length < maxDigits) {
                                        onDigitClick(digit)
                                    }
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
                // Left slot: Biometric button or spacer
                if (onBiometricClick != null) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) SurfacePrimary else SurfaceSubtle)
                            .border(1.dp, BorderSubtle, CircleShape)
                            .then(
                                if (isEnabled) Modifier.bouncyClickable {
                                    HapticUtil.heavyClick(context)
                                    onBiometricClick()
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Login",
                            tint = if (isEnabled) TextPrimary else com.ryanshelby.spw.wallet.ui.theme.TextMuted,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(72.dp))
                }

                // Center bottom digit
                val bottomDigit = digits[9]
                NumpadButton(
                    text = bottomDigit,
                    isEnabled = isEnabled,
                    onClick = {
                        if (isEnabled) {
                            HapticUtil.lightTap(context)
                            if (enteredPin.length < maxDigits) {
                                onDigitClick(bottomDigit)
                            }
                        }
                    }
                )

                // Backspace button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (isEnabled) SurfacePrimary else SurfaceSubtle)
                        .border(1.dp, BorderSubtle, CircleShape)
                        .then(
                            if (isEnabled) Modifier.bouncyClickable {
                                HapticUtil.lightTap(context)
                                onBackspaceClick()
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Backspace",
                        tint = if (isEnabled) TextSecondary else com.ryanshelby.spw.wallet.ui.theme.TextMuted,
                        modifier = Modifier.size(22.dp)
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
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (isEnabled) SurfaceElevated else SurfaceSubtle)
            .border(1.dp, BorderSubtle, CircleShape)
            .then(
                if (isEnabled) Modifier.bouncyClickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isEnabled) TextPrimary else com.ryanshelby.spw.wallet.ui.theme.TextMuted,
            fontSize = 22.sp
        )
    }
}
