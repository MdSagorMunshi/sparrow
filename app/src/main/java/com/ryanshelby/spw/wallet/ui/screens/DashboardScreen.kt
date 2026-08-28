package com.ryanshelby.spw.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast
import com.ryanshelby.spw.wallet.security.QrUriParser
import com.ryanshelby.spw.wallet.ui.components.QrScannerDialog
import com.ryanshelby.spw.wallet.ui.components.TransactionDetailDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.data.model.CryptoAsset
import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import com.ryanshelby.spw.wallet.data.model.TransactionItem
import com.ryanshelby.spw.wallet.data.model.TransactionType
import com.ryanshelby.spw.wallet.data.model.TranslationHelper
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.ui.components.GlassCard
import com.ryanshelby.spw.wallet.ui.components.Holographic3DCard
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    walletName: String,
    walletAddress: String,
    viewKeyHex: String = "",
    tokens: List<CryptoAsset>,
    transactions: List<TransactionItem>,
    network: NetworkConfig,
    activeLanguage: AppLanguage,
    hideBalance: Boolean,
    onToggleHideBalance: () -> Unit,
    onNavigateToSend: (String?) -> Unit,
    onNavigateToSendWithRecipient: (token: String?, recipient: String?) -> Unit = { t, _ -> onNavigateToSend(t) },
    onNavigateToReceive: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMining: () -> Unit,
    onRefresh: () -> Unit = {},
    onCopyAddress: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = remember(activeLanguage) { TranslationHelper.getStrings(activeLanguage) }

    val nativeToken = remember(tokens) { tokens.firstOrNull { it.isNative } ?: CryptoAsset() }
    val totalBalanceSpw = nativeToken.balance
    val totalBalanceFeathers = nativeToken.feathers

    var showQrScanner by remember { mutableStateOf(false) }
    var selectedTxForDetails by remember { mutableStateOf<TransactionItem?>(null) }

    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onCodeScanned = { rawCode ->
                showQrScanner = false
                val parsed = QrUriParser.parse(rawCode)
                if (parsed.address.isNotBlank()) {
                    Toast.makeText(context, "Scanned: ${parsed.address.take(16)}...", Toast.LENGTH_SHORT).show()
                    onNavigateToSendWithRecipient(null, parsed.address)
                } else {
                    Toast.makeText(context, "No valid address found in QR code", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Transaction Detail Dialog
    selectedTxForDetails?.let { tx ->
        TransactionDetailDialog(
            tx = tx,
            onDismiss = { selectedTxForDetails = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. 3D Holographic Cyber Card (contains sleek QR Scan and Show buttons)
        item {
            Holographic3DCard(
                walletName = walletName,
                walletAddress = walletAddress,
                totalBalanceSpw = totalBalanceSpw,
                totalBalanceFeathers = totalBalanceFeathers,
                network = network,
                hideBalance = hideBalance,
                onToggleHideBalance = onToggleHideBalance,
                onCopyAddress = onCopyAddress,
                onShowQr = onNavigateToReceive,
                onScanQr = { showQrScanner = true }
            )
        }

        // 2. Quick Action Buttons Row (Send, Receive, Cold Vault, History)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionButton(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = strings.send,
                    accentColor = CyanNeon,
                    onClick = {
                        HapticUtil.lightTap(context)
                        onNavigateToSend(null)
                    }
                )
                QuickActionButton(
                    icon = Icons.AutoMirrored.Filled.CallReceived,
                    label = strings.receive,
                    accentColor = GreenEmerald,
                    onClick = {
                        HapticUtil.lightTap(context)
                        onNavigateToReceive()
                    }
                )

                QuickActionButton(
                    icon = Icons.Default.History,
                    label = "Explorer",
                    accentColor = Color(0xFFFFB300),
                    onClick = {
                        HapticUtil.lightTap(context)
                        onNavigateToHistory()
                    }
                )

                QuickActionButton(
                    icon = Icons.Default.Security,
                    label = "Mining",
                    accentColor = CyanNeon,
                    onClick = {
                        HapticUtil.lightTap(context)
                        onNavigateToMining()
                    }
                )
            }
        }

        // 3. SPW Core Network Asset Status
        item {
            Column {
                Text(
                    text = "SPW NATIVE ASSET",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            HapticUtil.lightTap(context)
                            onNavigateToSend("SPW")
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(CyanNeon, Color(0xFF0D253F))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SPW",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = DarkBackground
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Sparrow",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyanNeon.copy(alpha = 0.15f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Layer 1 Native",
                                            color = CyanNeon,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "${network.name} • 8 Decimals",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            if (hideBalance) {
                                Text(
                                    text = "••••••",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = String.format(Locale.US, "%.8f", totalBalanceSpw).trimEnd('0').let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = NumberFormat.getNumberInstance(Locale.US).format(
                                        if (totalBalanceFeathers > 0) totalBalanceFeathers else (totalBalanceSpw * SPWCrypto.FEATHERS_PER_SPW).toLong()
                                    ) + " feathers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. On-Chain Security Features Banner
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, PurpleNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = PurpleNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dual-Key Stealth & Offline Vaults",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "ECDH one-time shielded payments & air-gapped QR signing enabled",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 5. Recent On-Chain Activity Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ON-CHAIN ACTIVITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanNeon,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        HapticUtil.lightTap(context)
                        onNavigateToHistory()
                    }
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No on-chain transactions yet",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Send or receive SPW on the network to see live block activity here.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(transactions.take(5)) { tx ->
                TransactionRowCard(
                    tx = tx,
                    onClick = {
                        HapticUtil.lightTap(context)
                        selectedTxForDetails = tx
                    }
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, GlassCardBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TransactionRowCard(tx: TransactionItem, onClick: () -> Unit = {}) {
    val isIncoming = tx.type == TransactionType.RECEIVE
    val isStealth = tx.type == TransactionType.STEALTH

    val iconColor = when {
        isStealth -> CyanNeon
        isIncoming -> GreenEmerald
        else -> RedCoral
    }

    val typeLabel = when {
        isStealth -> "Stealth Shielded"
        isIncoming -> "Received"
        else -> "Sent"
    }

    val timeFormatted = remember(tx.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        sdf.format(Date(tx.timestamp))
    }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncoming) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (tx.txHash.length > 14) "${tx.txHash.take(6)}...${tx.txHash.takeLast(6)}" else tx.txHash,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncoming) "+" else "-") + String.format(Locale.US, "%.8f", tx.amountSpw).trimEnd('0').let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isIncoming) GreenEmerald else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = tx.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tx.status.name == "CONFIRMED") GreenEmerald else Color(0xFFFFB300),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
