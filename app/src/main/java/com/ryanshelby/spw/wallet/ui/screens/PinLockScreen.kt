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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.data.model.TranslationHelper
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.components.PinPadView
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
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
    onVerifyPin: (String) -> Boolean = { true },
    getRemainingLockoutSeconds: () -> Long = { 0L }
) {
    val context = LocalContext.current
    val strings = remember(activeLanguage) { TranslationHelper.getStrings(activeLanguage) }

    var enteredPin by remember { mutableStateOf("") }
    var confirmPinStep by remember { mutableStateOf(false) }
    var firstPinEntry by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { androidx.compose.runtime.mutableLongStateOf(getRemainingLockoutSeconds()) }

    LaunchedEffect(lockoutSeconds) {
        while (lockoutSeconds > 0L) {
            errorMessage = "Too many failed attempts. Try again in ${lockoutSeconds}s"
            delay(1000L)
            val rem = getRemainingLockoutSeconds()
            lockoutSeconds = rem
            if (rem <= 0L) {
                errorMessage = null
            }
        }
    }

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
            .background(FinanceBackground)
    ) {
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
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SurfacePrimary)
                        .border(1.dp, BorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFirstTimeSetup) Icons.Default.Shield else Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "SPARROW WALLET",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isFirstTimeSetup) {
                        if (confirmPinStep) "Confirm Security PIN" else strings.createPin
                    } else {
                        strings.enterPin
                    },
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isFirstTimeSetup) {
                        if (confirmPinStep) "Re-enter your 6-digit PIN to confirm" else "Protect your keys and asset transfers"
                    } else {
                        if (lockoutSeconds > 0L) "Device temporarily locked" else "Enter your 6-digit passcode"
                    },
                    color = if (lockoutSeconds > 0L) SemanticError else TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = SemanticError,
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
                isEnabled = lockoutSeconds <= 0L,
                onDigitClick = { digit ->
                    if (lockoutSeconds > 0L) return@PinPadView
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
                                val pinChars = newPin.toCharArray()
                                val isValid: Boolean
                                try {
                                    isValid = onVerifyPin(newPin)
                                } finally {
                                    pinChars.fill('\u0000')
                                }

                                if (isValid) {
                                    HapticUtil.performSuccess(context)
                                    onPinSuccess()
                                } else {
                                    HapticUtil.performError(context)
                                    val rem = getRemainingLockoutSeconds()
                                    if (rem > 0L) {
                                        lockoutSeconds = rem
                                        errorMessage = "Too many failed attempts. Try again in ${rem}s"
                                    } else {
                                        errorMessage = "Incorrect PIN. Please try again."
                                    }
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
                onBiometricClick = if (!isFirstTimeSetup && isBiometricAvailable && lockoutSeconds <= 0L) {
                    {
                        onBiometricRequest {
                            HapticUtil.performSuccess(context)
                            onPinSuccess()
                        }
                    }
                } else null
            )

            // Footer Section
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
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.biometricLogin,
                            color = TextPrimary,
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
