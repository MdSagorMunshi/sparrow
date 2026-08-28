package com.ryanshelby.spw.wallet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.data.model.TranslationHelper
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.components.PinPadView
import com.ryanshelby.spw.wallet.ui.theme.CyanGlow
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.DarkSurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.GlassCardBackground
import com.ryanshelby.spw.wallet.ui.theme.GlassCardBorder
import com.ryanshelby.spw.wallet.ui.theme.GreenEmerald
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon
import com.ryanshelby.spw.wallet.ui.theme.RedCoral
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun PinLockScreen(
    isFirstTimeSetup: Boolean = false,
    activeLanguage: AppLanguage,
    isBiometricAvailable: Boolean,
    isScramblePin: Boolean,
    onPinSuccess: () -> Unit,
    onBiometricRequest: (onSuccess: () -> Unit) -> Unit,
    onSaveNewPin: (String) -> Unit = {},
    onVerifyPin: (String) -> Boolean = { true }
) {
    val context = LocalContext.current
    val strings = remember(activeLanguage) { TranslationHelper.getStrings(activeLanguage) }

    var enteredPin by remember { mutableStateOf("") }
    var confirmPinStep by remember { mutableStateOf(false) }
    var firstPinEntry by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isShaking by remember { mutableStateOf(false) }

    // Auto trigger biometric on start if available and not first time setup
    LaunchedEffect(Unit) {
        if (!isFirstTimeSetup && isBiometricAvailable) {
            delay(300)
            onBiometricRequest {
                HapticUtil.performSuccess(context)
                onPinSuccess()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Holographic cyber background ambient glow
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CyanNeon.copy(alpha = 0.15f),
                            PurpleNeon.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                // Security Icon with animated halo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CyanNeon.copy(alpha = 0.2f), PurpleNeon.copy(alpha = 0.2f))
                            )
                        )
                        .border(1.dp, CyanNeon.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFirstTimeSetup) Icons.Default.Shield else Icons.Default.Lock,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "SPARROW NETWORK",
                    color = CyanNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isFirstTimeSetup) {
                        if (confirmPinStep) "Confirm Security PIN" else strings.createPin
                    } else {
                        strings.enterPin
                    },
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isFirstTimeSetup) {
                        if (confirmPinStep) "Re-enter the 6-digit PIN to confirm" else "Protect your wallet keys and transactions"
                    } else {
                        "Enter your 6-digit master passcode"
                    },
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                // Error message
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = RedCoral,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }

            // PIN Keypad View
            PinPadView(
                enteredPin = enteredPin,
                errorMessage = errorMessage,
                isScrambled = isScramblePin,
                onDigitClick = { digit ->
                    if (enteredPin.length < 6) {
                        HapticUtil.performKeyClick(context)
                        errorMessage = null
                        val newPin = enteredPin + digit
                        enteredPin = newPin

                        if (newPin.length == 6) {
                            if (isFirstTimeSetup) {
                                if (!confirmPinStep) {
                                    firstPinEntry = newPin
                                    confirmPinStep = true
                                    enteredPin = ""
                                } else {
                                    if (newPin == firstPinEntry) {
                                        HapticUtil.performSuccess(context)
                                        onSaveNewPin(newPin)
                                        onPinSuccess()
                                    } else {
                                        HapticUtil.performError(context)
                                        errorMessage = "PINs do not match. Try again."
                                        enteredPin = ""
                                        confirmPinStep = false
                                        firstPinEntry = ""
                                    }
                                }
                            } else {
                                // Verification
                                val isValid = onVerifyPin(newPin)
                                if (isValid) {
                                    HapticUtil.performSuccess(context)
                                    onPinSuccess()
                                } else {
                                    HapticUtil.performError(context)
                                    errorMessage = "Incorrect PIN. Please try again."
                                    enteredPin = ""
                                }
                            }
                        }
                    }
                },
                onBackspaceClick = {
                    if (enteredPin.isNotEmpty()) {
                        HapticUtil.performKeyClick(context)
                        enteredPin = enteredPin.dropLast(1)
                        errorMessage = null
                    }
                },
                onBiometricClick = if (!isFirstTimeSetup && isBiometricAvailable) {
                    {
                        onBiometricRequest {
                            HapticUtil.performSuccess(context)
                            onPinSuccess()
                        }
                    }
                } else null
            )

            // Footer Biometric prompt or reset helper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isFirstTimeSetup && isBiometricAvailable) {
                    TextButton(
                        onClick = {
                            onBiometricRequest {
                                HapticUtil.performSuccess(context)
                                onPinSuccess()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.biometricLogin,
                            color = CyanNeon,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (isFirstTimeSetup && confirmPinStep) {
                    TextButton(
                        onClick = {
                            confirmPinStep = false
                            enteredPin = ""
                            firstPinEntry = ""
                            errorMessage = null
                        }
                    ) {
                        Text(
                            text = "Back to previous step",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
