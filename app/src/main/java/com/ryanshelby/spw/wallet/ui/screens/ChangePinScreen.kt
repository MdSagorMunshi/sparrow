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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.components.PinPadView
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable

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
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfacePrimary)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .bouncyClickable {
                                HapticUtil.performKeyClick(context)
                                onBack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SurfacePrimary)
                        .border(1.dp, BorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (currentStage) {
                        ChangePinStage.ENTER_CURRENT -> "Enter Current PIN"
                        ChangePinStage.ENTER_NEW -> "Enter New PIN"
                        ChangePinStage.CONFIRM_NEW -> "Confirm New PIN"
                    },
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
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
                onDigitClick = { digit ->
                    if (enteredPin.length < 6) {
                        HapticUtil.performKeyClick(context)
                        errorMessage = null
                        val updated = enteredPin + digit
                        enteredPin = updated

                        if (updated.length == 6) {
                            when (currentStage) {
                                ChangePinStage.ENTER_CURRENT -> {
                                    if (onVerifyCurrentPin(updated)) {
                                        HapticUtil.performSuccess(context)
                                        currentStage = ChangePinStage.ENTER_NEW
                                        enteredPin = ""
                                    } else {
                                        HapticUtil.performError(context)
                                        errorMessage = "Incorrect current PIN"
                                        enteredPin = ""
                                    }
                                }
                                ChangePinStage.ENTER_NEW -> {
                                    HapticUtil.performSuccess(context)
                                    newPin = updated
                                    currentStage = ChangePinStage.CONFIRM_NEW
                                    enteredPin = ""
                                }
                                ChangePinStage.CONFIRM_NEW -> {
                                    if (updated == newPin) {
                                        HapticUtil.performSuccess(context)
                                        onSaveNewPin(newPin)
                                        onSuccess()
                                    } else {
                                        HapticUtil.performError(context)
                                        errorMessage = "PINs do not match. Try again."
                                        enteredPin = ""
                                        currentStage = ChangePinStage.ENTER_NEW
                                        newPin = ""
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
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
