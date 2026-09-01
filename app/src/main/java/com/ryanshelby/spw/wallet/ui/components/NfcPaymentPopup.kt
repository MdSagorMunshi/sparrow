package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.nfc.NfcPaymentInvoice
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SemanticWarning
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcPaymentPopup(
    invoice: NfcPaymentInvoice?,
    isScanning: Boolean = false,
    isBroadcasting: Boolean = false,
    broadcastTxHash: String? = null,
    broadcastError: String? = null,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var enteredAmount by remember(invoice) { 
        mutableStateOf(invoice?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") 
    }
    
    val parsedAmount = enteredAmount.toDoubleOrNull()
    val isAmountValid = parsedAmount != null && parsedAmount > 0.0
    val tokenSymbol = invoice?.token ?: "SPW"

    ModalBottomSheet(
        onDismissRequest = {
            if (!isBroadcasting) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar with Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC",
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (invoice == null) "NFC TAP-TO-PAY" else "NFC PAYMENT TRANSFER",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                if (!isBroadcasting) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content Area with Animated Transitions
            AnimatedContent(
                targetState = when {
                    broadcastTxHash != null -> "SUCCESS"
                    broadcastError != null -> "ERROR"
                    isBroadcasting -> "BROADCASTING"
                    invoice != null -> "PAYMENT_DETAILS"
                    else -> "SCANNING"
                },
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith fadeOut()
                },
                label = "NfcPopupContent"
            ) { state ->
                when (state) {
                    "SCANNING" -> {
                        ScanningStateView(onCancel = onDismiss)
                    }

                    "PAYMENT_DETAILS" -> {
                        if (invoice != null) {
                            PaymentDetailsStateView(
                                invoice = invoice,
                                enteredAmount = enteredAmount,
                                isAmountValid = isAmountValid,
                                tokenSymbol = tokenSymbol,
                                onDigitClick = { digit ->
                                    if (invoice.amount == null) {
                                        HapticUtil.performKeyClick(context)
                                        if (digit == ".") {
                                            if (!enteredAmount.contains(".") && enteredAmount.isNotEmpty()) {
                                                enteredAmount += "."
                                            } else if (enteredAmount.isEmpty()) {
                                                enteredAmount = "0."
                                            }
                                        } else {
                                            val parts = enteredAmount.split(".")
                                            if (parts.size > 1 && parts[1].length >= 4) {
                                                // Max 4 decimal places
                                                return@PaymentDetailsStateView
                                            }
                                            if (enteredAmount.length < 9) {
                                                if (enteredAmount == "0") {
                                                    enteredAmount = digit
                                                } else {
                                                    enteredAmount += digit
                                                }
                                            }
                                        }
                                    }
                                },
                                onBackspaceClick = {
                                    if (invoice.amount == null && enteredAmount.isNotEmpty()) {
                                        HapticUtil.performKeyClick(context)
                                        enteredAmount = enteredAmount.dropLast(1)
                                    }
                                },
                                onSlideConfirm = {
                                    parsedAmount?.let { amt ->
                                        onConfirmPayment(amt)
                                    }
                                }
                            )
                        }
                    }

                    "BROADCASTING" -> {
                        BroadcastingStateView()
                    }

                    "SUCCESS" -> {
                        SuccessStateView(
                            txHash = broadcastTxHash ?: "",
                            amount = parsedAmount ?: 0.0,
                            tokenSymbol = tokenSymbol,
                            recipientName = invoice?.name ?: "Recipient",
                            onCopyTx = {
                                clipboardManager.setText(AnnotatedString(broadcastTxHash ?: ""))
                                HapticUtil.performSuccess(context)
                            },
                            onDone = onDismiss
                        )
                    }

                    "ERROR" -> {
                        ErrorStateView(
                            error = broadcastError ?: "Transaction failed",
                            onRetry = {
                                parsedAmount?.let { amt ->
                                    onConfirmPayment(amt)
                                }
                            },
                            onClose = onDismiss
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScanningStateView(onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        
        NfcRippleAnimation(modifier = Modifier.size(120.dp))
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Text(
            text = "Ready to Scan",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Hold phone near receiver device or SPW NFC tag to pay",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Cancel Scan", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PaymentDetailsStateView(
    invoice: NfcPaymentInvoice,
    enteredAmount: String,
    isAmountValid: Boolean,
    tokenSymbol: String,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onSlideConfirm: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Recipient Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceElevated)
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(SurfacePrimary)
                        .border(1.dp, BorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.name.ifBlank { "SPW Receiver" },
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatAddress(invoice.address),
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Amount Display
        if (invoice.amount != null) {
            // Fixed Amount from Request Flow
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Requested Payment",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (invoice.amount % 1.0 == 0.0) "${invoice.amount.toLong()}" else "${invoice.amount}",
                        color = TextPrimary,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyanNeon.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tokenSymbol,
                            color = CyanNeon,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Open Amount Entry Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                Text(
                    text = "Enter Transfer Amount",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (enteredAmount.isEmpty()) "0.00" else enteredAmount,
                        color = if (enteredAmount.isEmpty()) TextMuted else TextPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyanNeon.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tokenSymbol,
                            color = CyanNeon,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // In-App Numeric Keypad
            NfcNumericKeypad(
                onDigitClick = onDigitClick,
                onBackspaceClick = onBackspaceClick
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Slide to Pay Control
        if (isAmountValid) {
            SwipeToConfirm(
                onConfirm = onSlideConfirm,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = SurfaceElevated,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Enter valid amount to pay", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = SemanticWarning,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Instant non-reversible on-chain transfer",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun NfcNumericKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "BACK")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .bouncyClickable {
                                if (key == "BACK") {
                                    onBackspaceClick()
                                } else {
                                    onDigitClick(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "BACK") {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastingStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = CyanNeon,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Broadcasting Payment",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Signing transaction and broadcasting to SPW network...",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessStateView(
    txHash: String,
    amount: Double,
    tokenSymbol: String,
    recipientName: String,
    onCopyTx: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(AccentPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = AccentPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Payment Sent!",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Successfully sent to $recipientName",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Transaction Details Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceElevated)
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Amount", color = TextMuted, fontSize = 12.sp)
                    Text("$amount $tokenSymbol", color = AccentPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                if (txHash.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tx Hash", color = TextMuted, fontSize = 12.sp)
                        Text(
                            text = formatAddress(txHash),
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.bouncyClickable { onCopyTx() }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonPrimary,
                contentColor = ButtonPrimaryText
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorStateView(
    error: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(SemanticError.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = SemanticError,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Payment Failed",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = error,
            color = SemanticError,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Close", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    contentColor = ButtonPrimaryText
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatAddress(address: String): String {
    if (address.length <= 14) return address
    return "${address.take(8)}...${address.takeLast(6)}"
}
