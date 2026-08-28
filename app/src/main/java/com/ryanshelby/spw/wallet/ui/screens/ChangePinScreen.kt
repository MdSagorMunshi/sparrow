package com.ryanshelby.spw.wallet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.components.PinPadView
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon
import com.ryanshelby.spw.wallet.ui.theme.RedCoral
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary

enum class ChangePinStage {
    ENTER_CURRENT,
    ENTER_NEW,
    CONFIRM_NEW
}

@Composable
fun ChangePinScreen(
    isScramblePin: Boolean,
    onVerifyCurrentPin: (String) -> Boolean,
    onSaveNewPin: (String) -> Unit,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentStage by remember { mutableStateOf(ChangePinStage.ENTER_CURRENT) }
    var enteredPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = when (currentStage) {
                        ChangePinStage.ENTER_CURRENT -> "Enter Current PIN"
                        ChangePinStage.ENTER_NEW -> "Enter New PIN"
                        ChangePinStage.CONFIRM_NEW -> "Confirm New PIN"
                    },
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (currentStage) {
                        ChangePinStage.ENTER_CURRENT -> "Please verify your existing master PIN"
                        ChangePinStage.ENTER_NEW -> "Choose a new 6-digit master passcode"
                        ChangePinStage.CONFIRM_NEW -> "Re-enter the new PIN to confirm"
                    },
                    color = TextSecondary,
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
                        val pin = enteredPin + digit
                        enteredPin = pin

                        if (pin.length == 6) {
                            when (currentStage) {
                                ChangePinStage.ENTER_CURRENT -> {
                                    if (onVerifyCurrentPin(pin)) {
                                        HapticUtil.performSuccess(context)
                                        enteredPin = ""
                                        currentStage = ChangePinStage.ENTER_NEW
                                    } else {
                                        HapticUtil.performError(context)
                                        errorMessage = "Incorrect PIN"
                                        enteredPin = ""
                                    }
                                }
                                ChangePinStage.ENTER_NEW -> {
                                    HapticUtil.performKeyClick(context)
                                    newPin = pin
                                    enteredPin = ""
                                    currentStage = ChangePinStage.CONFIRM_NEW
                                }
                                ChangePinStage.CONFIRM_NEW -> {
                                    if (pin == newPin) {
                                        HapticUtil.performSuccess(context)
                                        onSaveNewPin(pin)
                                        onSuccess()
                                    } else {
                                        HapticUtil.performError(context)
                                        errorMessage = "PINs do not match"
                                        enteredPin = ""
                                        newPin = ""
                                        currentStage = ChangePinStage.ENTER_NEW
                                    }
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
                onBiometricClick = null
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
