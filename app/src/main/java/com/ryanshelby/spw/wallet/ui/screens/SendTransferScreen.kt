package com.ryanshelby.spw.wallet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import android.widget.Toast
import com.ryanshelby.spw.wallet.security.QrUriParser
import com.ryanshelby.spw.wallet.ui.components.QrScannerDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ryanshelby.spw.wallet.data.local.ContactEntity
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.data.model.CryptoAsset
import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import com.ryanshelby.spw.wallet.data.model.TranslationHelper
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.ui.components.GlassCard
import com.ryanshelby.spw.wallet.ui.components.PinPadView
import com.ryanshelby.spw.wallet.ui.components.TransactionStatusOverlay
import com.ryanshelby.spw.wallet.ui.components.TxOverlayState
import com.ryanshelby.spw.wallet.ui.theme.CyanGlow
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.DarkSurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.GlassCardBorder
import com.ryanshelby.spw.wallet.ui.theme.GreenEmerald
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon
import com.ryanshelby.spw.wallet.ui.theme.RedCoral
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SendTransferScreen(
    initialTokenSymbol: String? = null,
    initialRecipientAddress: String? = null,
    initialAmount: String? = null,
    tokens: List<CryptoAsset>,
    contacts: List<ContactEntity>,
    network: NetworkConfig,
    activeLanguage: AppLanguage,
    onBack: () -> Unit,
    onConfirmSend: suspend (tokenSymbol: String, toAddress: String, amount: Double, gasFee: Double, memo: String, isStealth: Boolean, recipientViewPubHex: String?) -> Result<String>,
    onVerifyPin: (String) -> Boolean,
    onTriggerBiometric: (onSuccess: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val strings = remember(activeLanguage) { TranslationHelper.getStrings(activeLanguage) }

    val nativeToken = remember(tokens) { tokens.firstOrNull { it.isNative } ?: CryptoAsset() }

    var recipientAddress by remember { mutableStateOf(initialRecipientAddress ?: "") }
    var recipientViewPub by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf(initialAmount ?: "") }
    var memoText by remember { mutableStateOf("") }
    var isStealthTransfer by remember { mutableStateOf(false) }

    var showQrScanner by remember { mutableStateOf(false) }

    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onCodeScanned = { rawCode ->
                showQrScanner = false
                val parsed = QrUriParser.parse(rawCode)
                if (parsed.address.isNotBlank()) {
                    recipientAddress = parsed.address
                    if (!parsed.amount.isNullOrBlank()) {
                        amountText = parsed.amount
                    }
                    if (!parsed.memo.isNullOrBlank()) {
                        memoText = parsed.memo
                    }
                    Toast.makeText(context, "Recipient Address Scanned", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Invalid QR code format", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    var selectedFeeSpeed by remember { mutableStateOf("Standard") } // Economy, Standard, Fast
    val gasFeeSpw = when (selectedFeeSpeed) {
        "Economy" -> 0.00005
        "Fast" -> 0.0005
        else -> 0.0001
    }

    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showContactsDialog by remember { mutableStateOf(false) }

    // Animated overlay state
    var txOverlayState by remember { mutableStateOf(TxOverlayState.HIDDEN) }
    var overlayTxHash by remember { mutableStateOf<String?>(null) }
    var overlayError by remember { mutableStateOf<String?>(null) }

    val amountValue = amountText.toDoubleOrNull() ?: 0.0
    val amountFeathers = (amountValue * SPWCrypto.FEATHERS_PER_SPW).toLong()
    val isAmountValid = amountValue > 0 && (amountValue + gasFeeSpw) <= nativeToken.balance

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation Bar with QR Code Scan Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        HapticUtil.lightTap(context)
                        onBack()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Send SPW",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = network.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // QR Code Scan Button in Header
            IconButton(
                onClick = {
                    HapticUtil.lightTap(context)
                    showQrScanner = true
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .border(1.dp, CyanNeon.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR Code",
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Balance Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Available Balance",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format(Locale.US, "%.8f", nativeToken.balance).trimEnd('0').let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.US).format(
                            if (nativeToken.feathers > 0) nativeToken.feathers else (nativeToken.balance * SPWCrypto.FEATHERS_PER_SPW).toLong()
                        ) + " feathers",
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                TextButton(
                    onClick = {
                        val maxSpendable = (nativeToken.balance - gasFeeSpw).coerceAtLeast(0.0)
                        amountText = String.format(Locale.US, "%.8f", maxSpendable).trimEnd('0')
                    }
                ) {
                    Text(
                        text = "USE MAX",
                        color = CyanNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recipient Address Input Header with Quick Scan Chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECIPIENT SPW ADDRESS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Quick Scan QR Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyanNeon.copy(alpha = 0.12f))
                    .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable {
                        HapticUtil.lightTap(context)
                        showQrScanner = true
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan QR", color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = recipientAddress,
            onValueChange = { recipientAddress = it.trim() },
            placeholder = { Text(if (isStealthTransfer) "Recipient Spend Public Key" else "DPqH8kK7yZ6c9M8p7L9a8N4...", color = TextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            HapticUtil.lightTap(context)
                            showQrScanner = true
                        }
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", tint = CyanNeon, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                recipientAddress = clip.trim()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = CyanNeon, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { showContactsDialog = true }
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = "Contacts", tint = GreenEmerald, modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = GlassCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // Stealth Transfer Toggle & Recipient View Key input
        Spacer(modifier = Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Stealth Shielded Transfer (ECDH)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Generate one-time on-chain stealth address", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = isStealthTransfer,
                        onCheckedChange = { isStealthTransfer = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = PurpleNeon)
                    )
                }

                if (isStealthTransfer) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = recipientViewPub,
                        onValueChange = { recipientViewPub = it.trim() },
                        placeholder = { Text("Recipient View Public Key (Hex)", color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleNeon,
                            unfocusedBorderColor = GlassCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Input
        Text(
            text = "TRANSFER AMOUNT (SPW)",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            placeholder = { Text("0.00", color = TextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = GlassCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        if (amountValue > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "= " + NumberFormat.getNumberInstance(Locale.US).format(amountFeathers) + " Feathers",
                color = CyanNeon,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Optional Memo
        Text(
            text = "OP_RETURN MEMO (OPTIONAL, MAX 80 BYTES)",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = memoText,
            onValueChange = { if (it.length <= 80) memoText = it },
            placeholder = { Text("Enter on-chain transaction memo", color = TextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = GlassCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Network Fee Selector
        Text(
            text = "NETWORK MINING FEE",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Economy" to 0.00005, "Standard" to 0.0001, "Fast" to 0.0005).forEach { (speed, fee) ->
                val isSelected = selectedFeeSpeed == speed
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFeeSpeed = speed }
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) CyanNeon else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(speed, color = if (isSelected) CyanNeon else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        val formattedFee = java.text.NumberFormat.getInstance(java.util.Locale.US).apply { maximumFractionDigits = 8 }.format(fee)
                        Text("$formattedFee SPW", color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Confirm Send Button
        Button(
            onClick = {
                HapticUtil.lightTap(context)
                onTriggerBiometric {
                    showPinDialog = false
                    executeBroadcast(
                        onConfirmSend = onConfirmSend,
                        toAddress = recipientAddress,
                        amount = amountValue,
                        gasFee = gasFeeSpw,
                        memo = memoText,
                        isStealth = isStealthTransfer,
                        recipientViewPubHex = recipientViewPub.ifBlank { null },
                        scope = scope,
                        onStart = { txOverlayState = TxOverlayState.BROADCASTING; overlayError = null },
                        onSuccess = { txid ->
                            overlayTxHash = txid
                            txOverlayState = TxOverlayState.SUCCESS
                        },
                        onError = { err ->
                            overlayError = err
                            txOverlayState = TxOverlayState.FAILURE
                        }
                    )
                }
                showPinDialog = true
            },
            enabled = recipientAddress.isNotBlank() && amountValue > 0 && txOverlayState == TxOverlayState.HIDDEN,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanNeon,
                contentColor = DarkBackground,
                disabledContainerColor = DarkSurfaceElevated,
                disabledContentColor = TextMuted
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            val isBtnEnabled = recipientAddress.isNotBlank() && amountValue > 0 && txOverlayState == TxOverlayState.HIDDEN
            Text(
                text = "Authenticate & Broadcast Transfer",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBtnEnabled) DarkBackground else TextMuted
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }



    // Contacts Dialog
    if (showContactsDialog) {
        Dialog(onDismissRequest = { showContactsDialog = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text("Saved Contacts", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (contacts.isEmpty()) {
                        Text("No saved contacts found.", color = TextMuted, fontSize = 12.sp)
                    } else {
                        contacts.forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        recipientAddress = contact.address
                                        showContactsDialog = false
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(contact.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(contact.address.take(12) + "...", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Text("Select", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    // PIN Pad Security Dialog
    if (showPinDialog) {
        Dialog(onDismissRequest = { showPinDialog = false }) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Enter Security PIN", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Authorize on-chain transfer of $amountValue SPW", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    PinPadView(
                        enteredPin = enteredPin,
                        isScrambled = true,
                        errorMessage = pinError,
                        onDigitClick = { digit ->
                            if (enteredPin.length < 6) {
                                enteredPin += digit
                                if (enteredPin.length == 6) {
                                    if (onVerifyPin(enteredPin)) {
                                        showPinDialog = false
                                        executeBroadcast(
                                            onConfirmSend = onConfirmSend,
                                            toAddress = recipientAddress,
                                            amount = amountValue,
                                            gasFee = gasFeeSpw,
                                            memo = memoText,
                                            isStealth = isStealthTransfer,
                                            recipientViewPubHex = recipientViewPub.ifBlank { null },
                                            scope = scope,
                                            onStart = { txOverlayState = TxOverlayState.BROADCASTING; overlayError = null },
                                            onSuccess = { txid ->
                                                overlayTxHash = txid
                                                txOverlayState = TxOverlayState.SUCCESS
                                            },
                                            onError = { err ->
                                                overlayError = err
                                                txOverlayState = TxOverlayState.FAILURE
                                            }
                                        )
                                    } else {
                                        pinError = "Incorrect PIN"
                                        enteredPin = ""
                                    }
                                }
                            }
                        },
                        onBackspaceClick = {
                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                        },
                        onBiometricClick = {
                            onTriggerBiometric {
                                showPinDialog = false
                                executeBroadcast(
                                    onConfirmSend = onConfirmSend,
                                    toAddress = recipientAddress,
                                    amount = amountValue,
                                    gasFee = gasFeeSpw,
                                    memo = memoText,
                                    isStealth = isStealthTransfer,
                                    recipientViewPubHex = recipientViewPub.ifBlank { null },
                                    scope = scope,
                                    onStart = { txOverlayState = TxOverlayState.BROADCASTING; overlayError = null },
                                    onSuccess = { txid ->
                                        overlayTxHash = txid
                                        txOverlayState = TxOverlayState.SUCCESS
                                    },
                                    onError = { err ->
                                        overlayError = err
                                        txOverlayState = TxOverlayState.FAILURE
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // ── Animated Transaction Status Overlay ──────────────────────
    TransactionStatusOverlay(
        state = txOverlayState,
        txHash = overlayTxHash,
        sentAmount = amountValue,
        recipientAddress = recipientAddress,
        errorMessage = overlayError,
        onDismiss = {
            txOverlayState = TxOverlayState.HIDDEN
            if (overlayTxHash != null) {
                onBack()
            }
            overlayTxHash = null
            overlayError = null
        }
    )
    } // close outer Box
}

private fun executeBroadcast(
    onConfirmSend: suspend (String, String, Double, Double, String, Boolean, String?) -> Result<String>,
    toAddress: String,
    amount: Double,
    gasFee: Double,
    memo: String,
    isStealth: Boolean,
    recipientViewPubHex: String?,
    scope: kotlinx.coroutines.CoroutineScope,
    onStart: () -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    scope.launch {
        onStart()
        val result = onConfirmSend("SPW", toAddress, amount, gasFee, memo, isStealth, recipientViewPubHex)
        if (result.isSuccess) {
            onSuccess(result.getOrNull() ?: "")
        } else {
            onError(result.exceptionOrNull()?.message ?: "Transaction broadcast failed.")
        }
    }
}
