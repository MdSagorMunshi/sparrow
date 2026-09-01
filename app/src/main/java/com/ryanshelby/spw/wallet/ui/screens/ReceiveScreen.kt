package com.ryanshelby.spw.wallet.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import com.ryanshelby.spw.wallet.data.model.TranslationHelper
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.ui.components.FinanceCard
import com.ryanshelby.spw.wallet.ui.components.GlowingQrCodeView
import com.ryanshelby.spw.wallet.ui.theme.AccentMuted
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderStrong
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon
import com.ryanshelby.spw.wallet.ui.theme.RedCoral
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ReceiveScreen(
    walletAddress: String,
    walletName: String,
    spendPubHex: String = "",
    viewPubHex: String = "",
    viewKeyHex: String = "",
    network: NetworkConfig,
    activeLanguage: AppLanguage,
    onBack: () -> Unit,
    isNfcSupported: Boolean = false,
    isNfcEnabled: Boolean = false,
    onNavigateToRequestPayment: () -> Unit = {},
    onNavigateToBurnerInvoice: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val strings = remember(activeLanguage) { TranslationHelper.getStrings(activeLanguage) }

    var requestedAmount by remember { mutableStateOf("") }
    var selectedReceiveMode by remember { mutableIntStateOf(0) } // 0: Standard Address, 1: Sparrow Paynym (BIP-47)

    var copiedAddressRecently by remember { mutableStateOf(false) }
    var copiedPaynymRecently by remember { mutableStateOf(false) }
    var copiedSpendPubRecently by remember { mutableStateOf(false) }
    var copiedViewPubRecently by remember { mutableStateOf(false) }
    var copiedBothRecently by remember { mutableStateOf(false) }

    val effectiveSpendPub = spendPubHex.ifBlank { "" }
    val effectiveViewPub = viewPubHex.ifBlank { viewKeyHex }

    val paynymCode = remember(effectiveSpendPub, effectiveViewPub) {
        if (effectiveSpendPub.isNotBlank() && effectiveViewPub.isNotBlank()) {
            try {
                SPWCrypto.generatePaynymCode(effectiveSpendPub, effectiveViewPub)
            } catch (e: Exception) { "" }
        } else {
            ""
        }
    }
    val paynymAlias = remember(effectiveSpendPub) {
        if (effectiveSpendPub.isNotBlank()) SPWCrypto.generatePaynymAlias(effectiveSpendPub) else "+sparrow"
    }

    val qrPayload = remember(walletAddress, requestedAmount, selectedReceiveMode, paynymCode) {
        if (selectedReceiveMode == 1 && paynymCode.isNotBlank()) {
            "paynym:$paynymCode"
        } else if (requestedAmount.isNotBlank()) {
            "spw:$walletAddress?amount=$requestedAmount"
        } else {
            walletAddress
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
                    text = "Receive SPW",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${network.name} Address",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Switcher: Standard Address vs Sparrow Paynym (BIP-47)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfacePrimary)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedReceiveMode == 0) SurfaceElevated else Color.Transparent)
                    .border(1.dp, if (selectedReceiveMode == 0) BorderStrong else Color.Transparent, RoundedCornerShape(8.dp))
                    .bouncyClickable {
                        HapticUtil.lightTap(context)
                        selectedReceiveMode = 0
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Standard Address",
                    color = if (selectedReceiveMode == 0) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedReceiveMode == 1) PurpleNeon.copy(alpha = 0.15f) else Color.Transparent)
                    .border(1.dp, if (selectedReceiveMode == 1) PurpleNeon.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .bouncyClickable {
                        HapticUtil.lightTap(context)
                        selectedReceiveMode = 1
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (selectedReceiveMode == 1) PurpleNeon else TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Paynym (BIP-47)",
                        color = if (selectedReceiveMode == 1) PurpleNeon else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main QR Display Card
        FinanceCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (selectedReceiveMode == 1) "Reusable Paynym Code" else "Your Address QR Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = if (selectedReceiveMode == 1) "Permanent reusable stealth payment code ($paynymAlias)" else "Scan with any SPW-compatible wallet to pay",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedReceiveMode == 1) PurpleNeon else TextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Clean high-contrast QR code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlowingQrCodeView(
                        data = qrPayload,
                        sizeDp = 210.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedReceiveMode == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isNfcSupported) {
                            Text("Your phone doesn't have NFC", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else if (!isNfcEnabled) {
                            Text(
                                "Turn on NFC",
                                color = CyanNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
                                }
                            )
                        } else {
                            com.ryanshelby.spw.wallet.ui.components.NfcRippleAnimation()
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("NFC Tap-to-Receive Ready", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Tap sender's phone to receive", color = TextSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onNavigateToRequestPayment() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary, contentColor = ButtonPrimaryText),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Request Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Address Box (Clickable copy)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceSubtle)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .bouncyClickable {
                                clipboardManager.setText(AnnotatedString(walletAddress))
                                HapticUtil.performSuccess(context)
                                copiedAddressRecently = true
                                scope.launch {
                                    delay(2000)
                                    copiedAddressRecently = false
                                }
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = walletAddress,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (copiedAddressRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (copiedAddressRecently) SemanticPositive else TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (copiedAddressRecently) "Copied to clipboard!" else "Tap to copy address",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (copiedAddressRecently) SemanticPositive else TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    // Paynym Code Box (Clickable copy)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceSubtle)
                            .border(1.dp, PurpleNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .bouncyClickable {
                                if (paynymCode.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(paynymCode))
                                    HapticUtil.performSuccess(context)
                                    copiedPaynymRecently = true
                                    scope.launch {
                                        delay(2000)
                                        copiedPaynymRecently = false
                                    }
                                }
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = paynymCode.ifBlank { "Generating Paynym..." },
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                color = if (paynymCode.isNotBlank()) TextPrimary else TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (copiedPaynymRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (copiedPaynymRecently) SemanticPositive else PurpleNeon,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (copiedPaynymRecently) "Copied Paynym Code!" else "Tap to copy Paynym code",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (copiedPaynymRecently) SemanticPositive else PurpleNeon
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Share this permanent Paynym code publicly. Senders can pay you repeatedly; each transaction cryptographically generates a brand-new, unlinkable one-time stealth address on the SPW blockchain.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row (Share / Copy)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val copyPayload = if (selectedReceiveMode == 1) paynymCode else walletAddress
            val shareTitle = if (selectedReceiveMode == 1) "My Sparrow Paynym ($paynymAlias)" else "My SPW Address"
            val copyLabel = if (selectedReceiveMode == 1) "Copy Paynym" else "Copy Address"

            Button(
                onClick = {
                    if (copyPayload.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(copyPayload))
                        HapticUtil.performSuccess(context)
                        if (selectedReceiveMode == 1) copiedPaynymRecently = true else copiedAddressRecently = true
                        scope.launch {
                            delay(2000)
                            copiedPaynymRecently = false
                            copiedAddressRecently = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = TextPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(copyLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    if (copyPayload.isNotBlank()) {
                        HapticUtil.lightTap(context)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                            putExtra(Intent.EXTRA_TEXT, copyPayload)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, shareTitle))
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedReceiveMode == 1) PurpleNeon else com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary,
                    contentColor = if (selectedReceiveMode == 1) DarkBackground else com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = if (selectedReceiveMode == 1) DarkBackground else com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stealth Public Keys Card (ECDH Dual-Key)
        FinanceCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                    Text("Dual-Key Stealth Receiving", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "To receive private, untraceable stealth payments, share your Spend Public Key and View Public Key with the sender.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                // 1. Spend Public Key
                if (effectiveSpendPub.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SPEND PUBLIC KEY (HEX)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        if (copiedSpendPubRecently) {
                            Text("Copied!", color = SemanticPositive, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceSubtle)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .bouncyClickable {
                                clipboardManager.setText(AnnotatedString(effectiveSpendPub))
                                HapticUtil.performSuccess(context)
                                copiedSpendPubRecently = true
                                scope.launch {
                                    delay(2000)
                                    copiedSpendPubRecently = false
                                }
                            }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = effectiveSpendPub,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (copiedSpendPubRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy Spend Public Key",
                                tint = if (copiedSpendPubRecently) SemanticPositive else TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // 2. View Public Key
                if (effectiveViewPub.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("VIEW PUBLIC KEY (ECDH)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        if (copiedViewPubRecently) {
                            Text("Copied!", color = SemanticPositive, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceSubtle)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .bouncyClickable {
                                clipboardManager.setText(AnnotatedString(effectiveViewPub))
                                HapticUtil.performSuccess(context)
                                copiedViewPubRecently = true
                                scope.launch {
                                    delay(2000)
                                    copiedViewPubRecently = false
                                }
                            }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = effectiveViewPub,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (copiedViewPubRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy View Public Key",
                                tint = if (copiedViewPubRecently) SemanticPositive else TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Action buttons for stealth keys
                if (effectiveSpendPub.isNotBlank() && effectiveViewPub.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val stealthShareText = "SPW Stealth Payment Keys:\nSpend Public Key: $effectiveSpendPub\nView Public Key: $effectiveViewPub"
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(stealthShareText))
                                HapticUtil.performSuccess(context)
                                copiedBothRecently = true
                                scope.launch {
                                    delay(2000)
                                    copiedBothRecently = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceElevated,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(
                                imageVector = if (copiedBothRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = if (copiedBothRecently) SemanticPositive else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (copiedBothRecently) "Copied Both!" else "Copy Both",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                HapticUtil.lightTap(context)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "My SPW Stealth Keys")
                                    putExtra(Intent.EXTRA_TEXT, stealthShareText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share SPW Stealth Keys"))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceElevated,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Both", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Requester
        FinanceCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Specify Amount (Optional)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = requestedAmount,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            val parts = filtered.split('.')
                            if (parts.size <= 1 || parts[1].length <= 8) {
                                requestedAmount = filtered
                            }
                        }
                    },
                    placeholder = { Text("0.00 SPW", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceSubtle,
                        unfocusedContainerColor = SurfaceSubtle,
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onNavigateToBurnerInvoice,
                    colors = ButtonDefaults.buttonColors(containerColor = RedCoral.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = RedCoral, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Single-Use Burner Invoice", color = RedCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
