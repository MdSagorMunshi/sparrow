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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import com.ryanshelby.spw.wallet.data.model.TranslationHelper
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.components.GlassCard
import com.ryanshelby.spw.wallet.ui.components.GlowingQrCodeView
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.DarkSurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.GlassCardBorder
import com.ryanshelby.spw.wallet.ui.theme.GreenEmerald
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val strings = remember(activeLanguage) { TranslationHelper.getStrings(activeLanguage) }

    var requestedAmount by remember { mutableStateOf("") }
    var copiedAddressRecently by remember { mutableStateOf(false) }
    var copiedSpendPubRecently by remember { mutableStateOf(false) }
    var copiedViewPubRecently by remember { mutableStateOf(false) }
    var copiedBothRecently by remember { mutableStateOf(false) }

    val effectiveSpendPub = spendPubHex.ifBlank { "" }
    val effectiveViewPub = viewPubHex.ifBlank { viewKeyHex }

    val qrPayload = remember(walletAddress, requestedAmount) {
        if (requestedAmount.isNotBlank()) {
            "spw:$walletAddress?amount=$requestedAmount"
        } else {
            walletAddress
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
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
            Text(
                text = "Receive SPW",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Main QR Display Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowing = true
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SPW Address QR Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = network.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanNeon
                )

                Spacer(modifier = Modifier.height(16.dp))

                // High Quality Glowing QR
                GlowingQrCodeView(
                    data = qrPayload,
                    sizeDp = 200.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Address Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, GlassCardBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(walletAddress))
                            HapticUtil.performSuccess(context)
                            copiedAddressRecently = true
                            scope.launch {
                                delay(2000)
                                copiedAddressRecently = false
                            }
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = walletAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanNeon,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (copiedAddressRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = if (copiedAddressRecently) GreenEmerald else TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (copiedAddressRecently) "Copied to clipboard!" else "Tap to copy address",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (copiedAddressRecently) GreenEmerald else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row (Share / Copy)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(walletAddress))
                    HapticUtil.performSuccess(context)
                    copiedAddressRecently = true
                    scope.launch {
                        delay(2000)
                        copiedAddressRecently = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Address", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    HapticUtil.lightTap(context)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "My SPW Address")
                        putExtra(Intent.EXTRA_TEXT, walletAddress)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share SPW Address"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stealth Public Keys Card (ECDH Dual-Key)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dual-Key Stealth Receiving", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
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
                        Text("SPEND PUBLIC KEY (HEX):", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        if (copiedSpendPubRecently) {
                            Text("Copied!", color = GreenEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, if (copiedSpendPubRecently) GreenEmerald.copy(alpha = 0.5f) else GlassCardBorder, RoundedCornerShape(8.dp))
                            .clickable {
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
                                color = CyanNeon,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (copiedSpendPubRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy Spend Public Key",
                                tint = if (copiedSpendPubRecently) GreenEmerald else TextMuted,
                                modifier = Modifier.size(14.dp)
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
                        Text("VIEW PUBLIC KEY (HEX - ECDH):", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        if (copiedViewPubRecently) {
                            Text("Copied!", color = GreenEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, if (copiedViewPubRecently) GreenEmerald.copy(alpha = 0.5f) else GlassCardBorder, RoundedCornerShape(8.dp))
                            .clickable {
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
                                color = PurpleNeon,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (copiedViewPubRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy View Public Key",
                                tint = if (copiedViewPubRecently) GreenEmerald else TextMuted,
                                modifier = Modifier.size(14.dp)
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
                                containerColor = DarkSurfaceElevated,
                                contentColor = PurpleNeon
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(
                                imageVector = if (copiedBothRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = if (copiedBothRecently) GreenEmerald else PurpleNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (copiedBothRecently) "Copied Both!" else "Copy Both Keys",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (copiedBothRecently) GreenEmerald else PurpleNeon
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
                                containerColor = PurpleNeon.copy(alpha = 0.2f),
                                contentColor = PurpleNeon
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = PurpleNeon, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Both Keys", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurpleNeon)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Requester
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Specify Amount (Optional)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = requestedAmount,
                    onValueChange = { requestedAmount = it },
                    placeholder = { Text("0.00 SPW", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = GlassCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
