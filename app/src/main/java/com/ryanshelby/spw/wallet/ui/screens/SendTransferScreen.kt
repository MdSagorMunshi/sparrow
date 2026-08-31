package com.ryanshelby.spw.wallet.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.ryanshelby.spw.wallet.security.QrUriParser
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.ui.components.FinanceCard
import com.ryanshelby.spw.wallet.ui.components.Identicon
import com.ryanshelby.spw.wallet.ui.components.PinPadView
import com.ryanshelby.spw.wallet.ui.components.QrScannerDialog
import com.ryanshelby.spw.wallet.ui.components.TransactionStatusOverlay
import com.ryanshelby.spw.wallet.ui.components.TxOverlayState
import com.ryanshelby.spw.wallet.ui.theme.AccentMuted
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderStrong
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable
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
    walletAddress: String = "",
    onBack: () -> Unit,
    onConfirmSend: suspend (tokenSymbol: String, toAddress: String, amount: Double, gasFee: Double, memo: String, isStealth: Boolean, recipientViewPubHex: String?) -> Result<String>,
    onVerifyPin: (String) -> Boolean,
    onTriggerBiometric: (onSuccess: () -> Unit) -> Unit,
    isNfcSupported: Boolean = false,
    isNfcEnabled: Boolean = false,
    onNfcTapToPayClick: () -> Unit = {},
    onScannedPaymentInvoice: (com.ryanshelby.spw.wallet.nfc.NfcPaymentInvoice) -> Unit = {}
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
                
                try {
                    val invoice = com.ryanshelby.spw.wallet.nfc.NfcPaymentInvoice.fromJson(rawCode)
                    if (invoice.address.isNotBlank()) {
                        onScannedPaymentInvoice(invoice)
                        return@QrScannerDialog
                    }
                } catch (e: Exception) {
                    // Not an invoice, try standard URI parsing
                }
                
                val parsed = QrUriParser.parse(rawCode)
                if (parsed.address.isNotBlank()) {
                    recipientAddress = parsed.address
                    if (!parsed.amount.isNullOrBlank()) {
                        amountText = parsed.amount
                    }
                    if (parsed.memo != null) {
                        memoText = parsed.memo
                    }
                    Toast.makeText(context, "Scanned: ${parsed.address.take(12)}...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No valid address found in QR code", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    var selectedFeeSpeed by remember { mutableStateOf("Standard") }
    var customGasFeeSpw by remember { mutableDoubleStateOf(0.0002) }
    val gasFeeSpw = when (selectedFeeSpeed) {
        "Economy" -> 0.00005
        "Fast" -> 0.0005
        "Custom" -> customGasFeeSpw
        else -> 0.0001
    }

    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showContactsDialog by remember { mutableStateOf(false) }

    var txOverlayState by remember { mutableStateOf(TxOverlayState.HIDDEN) }
    var overlayTxHash by remember { mutableStateOf<String?>(null) }
    var overlayError by remember { mutableStateOf<String?>(null) }

    val amountValue = amountText.toDoubleOrNull() ?: 0.0
    val amountFeathers = (amountValue * SPWCrypto.FEATHERS_PER_SPW).toLong()

    // Address & Self-Send Validation
    val isSelfSend = remember(recipientAddress, walletAddress) {
        recipientAddress.isNotBlank() && walletAddress.isNotBlank() &&
                recipientAddress.trim().equals(walletAddress.trim(), ignoreCase = true)
    }

    val isAddressValidFormat = remember(recipientAddress, isStealthTransfer) {
        if (recipientAddress.isBlank()) true
        else if (isStealthTransfer) {
            val clean = recipientAddress.trim()
            clean.length == 66 && (clean.startsWith("02") || clean.startsWith("03")) &&
                    clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        } else {
            SPWCrypto.isValidSpwAddress(recipientAddress.trim())
        }
    }

    val isStealthViewKeyValid = remember(recipientViewPub, isStealthTransfer) {
        if (!isStealthTransfer || recipientViewPub.isBlank()) true
        else {
            val clean = recipientViewPub.trim()
            clean.length == 66 && (clean.startsWith("02") || clean.startsWith("03")) &&
                    clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
        }
    }

    val addressErrorMessage = when {
        recipientAddress.isBlank() -> null
        isSelfSend -> "Cannot send tokens to your own wallet address."
        !isAddressValidFormat -> if (isStealthTransfer) {
            "Invalid Spend Public Key (must be 66-character compressed hex starting with 02 or 03)."
        } else {
            "Invalid SPW wallet address. Tokens cannot be sent."
        }
        isStealthTransfer && recipientViewPub.isNotBlank() && !isStealthViewKeyValid -> {
            "Invalid Recipient View Public Key (must be 66-character compressed hex)."
        }
        else -> null
    }

    val isAddressValid = recipientAddress.isNotBlank() && addressErrorMessage == null

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FinanceBackground)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfacePrimary)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .bouncyClickable {
                                HapticUtil.lightTap(context)
                                onBack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Send SPW",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = network.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isNfcSupported) {
                        Text(
                            text = "Your phone doesn't have NFC",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    } else if (!isNfcEnabled) {
                        Text(
                            text = "Turn on NFC",
                            color = CyanNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable {
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
                                }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfacePrimary)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .bouncyClickable {
                                    HapticUtil.lightTap(context)
                                    onNfcTapToPayClick()
                                    Toast.makeText(context, "NFC Reader Enabled. Tap to pay.", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Tap to Pay",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfacePrimary)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .bouncyClickable {
                                HapticUtil.lightTap(context)
                                showQrScanner = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR Code",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Available Balance Card
            FinanceCard(modifier = Modifier.fillMaxWidth()) {
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
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.8f", nativeToken.balance).trimEnd('0').let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceSubtle)
                            .bouncyClickable {
                                val maxSpendable = (nativeToken.balance - gasFeeSpw).coerceAtLeast(0.0)
                                amountText = String.format(Locale.US, "%.8f", maxSpendable).trimEnd('0')
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "MAX",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Send / Saved Recipients Bar
            if (contacts.isNotEmpty()) {
                Text(
                    text = "QUICK SEND / RECENT RECIPIENTS",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(contacts) { contact ->
                        val isSelected = recipientAddress.equals(contact.address, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) SurfaceElevated else SurfacePrimary)
                                .border(0.8.dp, if (isSelected) TextPrimary else BorderSubtle, RoundedCornerShape(20.dp))
                                .bouncyClickable {
                                    HapticUtil.performKeyClick(context)
                                    recipientAddress = contact.address
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Identicon(address = contact.address, size = 18.dp, shape = CircleShape)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = contact.name.ifBlank { contact.address.take(6) + "..." },
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Recipient Section
            Text(
                text = "RECIPIENT ADDRESS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = recipientAddress,
                onValueChange = { recipientAddress = it.trim() },
                placeholder = { Text(if (isStealthTransfer) "Recipient Spend Public Key" else "Recipient address", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                isError = addressErrorMessage != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = if (recipientAddress.isNotBlank()) {
                    {
                        Box(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
                            Identicon(address = recipientAddress, size = 22.dp, shape = RoundedCornerShape(6.dp))
                        }
                    }
                } else null,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    recipientAddress = clip.trim()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { showContactsDialog = true }
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = "Contacts", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfacePrimary,
                    unfocusedContainerColor = SurfacePrimary,
                    focusedBorderColor = if (addressErrorMessage != null) SemanticError else BorderStrong,
                    unfocusedBorderColor = if (addressErrorMessage != null) SemanticError else BorderSubtle,
                    errorBorderColor = SemanticError,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            if (addressErrorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Address Error",
                        tint = SemanticError,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = addressErrorMessage,
                        color = SemanticError,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp
                    )
                }
            }

            // Stealth Transfer Toggle & Recipient View Key input
            Spacer(modifier = Modifier.height(12.dp))
            FinanceCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Stealth Shielded Transfer", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Generates one-time on-chain stealth address", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = isStealthTransfer,
                            onCheckedChange = { isStealthTransfer = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FinanceBackground,
                                checkedTrackColor = ButtonPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceSubtle
                            )
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
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceSubtle,
                                unfocusedContainerColor = SurfaceSubtle,
                                focusedBorderColor = BorderStrong,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Amount Input
            Text(
                text = "TRANSFER AMOUNT",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                placeholder = { Text("0.00", color = TextMuted, fontSize = 18.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    Text("SPW", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(end = 14.dp))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfacePrimary,
                    unfocusedContainerColor = SurfacePrimary,
                    focusedBorderColor = BorderStrong,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            if (amountValue > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "≈ " + NumberFormat.getNumberInstance(Locale.US).format(amountFeathers) + " feathers",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Optional Memo
            Text(
                text = "TRANSACTION MEMO (OPTIONAL)",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = memoText,
                onValueChange = { if (it.length <= 80) memoText = it },
                placeholder = { Text("Enter memo (max 80 chars)", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfacePrimary,
                    unfocusedContainerColor = SurfacePrimary,
                    focusedBorderColor = BorderStrong,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Network Fee Selector
            Text(
                text = "NETWORK MINING SPEED & FEE",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Economy" to 0.00005,
                    "Standard" to 0.0001,
                    "Fast" to 0.0005,
                    "Custom" to customGasFeeSpw
                ).forEach { (speed, fee) ->
                    val isSelected = selectedFeeSpeed == speed
                    FinanceCard(
                        modifier = Modifier
                            .weight(1f)
                            .bouncyClickable { selectedFeeSpeed = speed }
                            .border(
                                width = 1.dp,
                                color = if (isSelected) BorderStrong else BorderSubtle,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        backgroundColor = if (isSelected) SurfaceElevated else SurfacePrimary
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                speed,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val formattedFee = java.text.NumberFormat.getInstance(java.util.Locale.US).apply { maximumFractionDigits = 8 }.format(fee)
                            Text("$formattedFee SPW", color = TextPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Custom Fee Slider
            if (selectedFeeSpeed == "Custom") {
                Spacer(modifier = Modifier.height(10.dp))
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Custom Gas Fee Tuning", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = String.format(Locale.US, "%.6f SPW", customGasFeeSpw),
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = customGasFeeSpw.toFloat(),
                            onValueChange = { customGasFeeSpw = (Math.round(it * 100000.0) / 100000.0).coerceAtLeast(0.00001) },
                            valueRange = 0.00001f..0.00200f,
                            colors = SliderDefaults.colors(
                                thumbColor = TextPrimary,
                                activeTrackColor = TextPrimary,
                                inactiveTrackColor = SurfaceElevated
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ═════════════════════════════════════════════════════════════════
            // Transaction Summary & Net Deduction Breakdown Card
            // ═════════════════════════════════════════════════════════════════
            val totalDebited = amountValue + gasFeeSpw
            val remainingBalance = (nativeToken.balance - totalDebited).coerceAtLeast(0.0)
            val isOverBalance = totalDebited > nativeToken.balance

            FinanceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FINANCIAL DEDUCTION BREAKDOWN",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Live Estimate",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Transfer Amount", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = String.format(Locale.US, "%.8f", amountValue).trimEnd('0').let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Network Gas Fee", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = "- " + String.format(Locale.US, "%.8f", gasFeeSpw).trimEnd('0').let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                            color = SemanticError,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Recipient Receives (Net)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = String.format(Locale.US, "%.8f", amountValue).trimEnd('0').let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                            color = SemanticPositive,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Debited from Wallet", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.US, "%.8f", totalDebited).trimEnd('0').let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                            color = if (isOverBalance) SemanticError else TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Remaining Wallet Balance", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = String.format(Locale.US, "%.8f", if (isOverBalance) 0.0 else remainingBalance).trimEnd('0').let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                            color = if (isOverBalance) SemanticError else TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (isOverBalance && amountValue > 0) {
                        Text(
                            text = "Insufficient balance: Total debited (${String.format(Locale.US, "%.8f", totalDebited)} SPW) exceeds your balance.",
                            color = SemanticError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm Send Button
            val isAmountValid = amountValue > 0 && !isOverBalance
            val isBtnEnabled = isAddressValid && isAmountValid && txOverlayState == TxOverlayState.HIDDEN
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
                enabled = isBtnEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary,
                    contentColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText,
                    disabledContainerColor = SurfaceElevated,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Authorize & Broadcast Transfer",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBtnEnabled) com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Contacts Dialog
        if (showContactsDialog) {
            Dialog(onDismissRequest = { showContactsDialog = false }) {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text("Saved Contacts", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (contacts.isEmpty()) {
                            Text("No saved contacts found.", color = TextMuted, fontSize = 12.sp)
                        } else {
                            contacts.forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .bouncyClickable {
                                            recipientAddress = contact.address
                                            showContactsDialog = false
                                        }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(contact.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(contact.address.take(12) + "...", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Text("Select", color = AccentPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }

        // PIN Pad Security Dialog
        if (showPinDialog) {
            Dialog(onDismissRequest = { showPinDialog = false }) {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SurfaceSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Authorize Transaction", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Confirm transfer of $amountValue SPW", color = TextSecondary, fontSize = 12.sp)
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

        // Animated Transaction Status Overlay
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
    }
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
