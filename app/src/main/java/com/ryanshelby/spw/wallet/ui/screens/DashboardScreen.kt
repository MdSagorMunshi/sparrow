package com.ryanshelby.spw.wallet.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ryanshelby.spw.wallet.data.model.TransactionStatus
import com.ryanshelby.spw.wallet.data.model.TransactionType
import com.ryanshelby.spw.wallet.data.model.TranslationHelper
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.QrUriParser
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.ui.components.FinanceCard
import com.ryanshelby.spw.wallet.ui.components.PortfolioOverviewCard
import com.ryanshelby.spw.wallet.ui.components.QrScannerDialog
import com.ryanshelby.spw.wallet.ui.components.TransactionDetailDialog
import com.ryanshelby.spw.wallet.ui.components.TransactionRowSkeleton
import com.ryanshelby.spw.wallet.ui.theme.AccentMuted
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.AmberGold
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SemanticWarning
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable
import com.ryanshelby.spw.wallet.ui.theme.staggeredEntrance
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    isSyncing: Boolean = false,
    isOnline: Boolean = true,
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
            walletAddress = walletAddress,
            onDismiss = { selectedTxForDetails = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Institutional Portfolio Balance Card
        item {
            PortfolioOverviewCard(
                isSyncing = isSyncing,
                isOnline = isOnline,
                walletName = walletName,
                walletAddress = walletAddress,
                totalBalanceSpw = totalBalanceSpw,
                totalBalanceFeathers = totalBalanceFeathers,
                transactions = transactions,
                network = network,
                hideBalance = hideBalance,
                onToggleHideBalance = onToggleHideBalance,
                onCopyAddress = onCopyAddress,
                onShowQr = onNavigateToReceive,
                onScanQr = { showQrScanner = true }
            )
        }

        // 1b. Offline Mode Notice Banner (When no internet detected)
        if (!isOnline) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfacePrimary)
                        .border(1.dp, AmberGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Offline • Showing Cached Balance",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Balance & on-chain data will auto-sync once internet reconnects.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        text = "Retry",
                        color = AmberGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .bouncyClickable {
                                HapticUtil.lightTap(context)
                                onRefresh()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 2. Quick Action Buttons Row (Send, Receive, Explorer, Mining)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionButton(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = strings.send,
                    onClick = {
                        HapticUtil.lightTap(context)
                        onNavigateToSend(null)
                    }
                )
                QuickActionButton(
                    icon = Icons.AutoMirrored.Filled.CallReceived,
                    label = strings.receive,
                    onClick = {
                        HapticUtil.lightTap(context)
                        onNavigateToReceive()
                    }
                )
                QuickActionButton(
                    icon = Icons.Default.History,
                    label = "Explorer",
                    onClick = {
                        HapticUtil.lightTap(context)
                        onNavigateToHistory()
                    }
                )
                QuickActionButton(
                    icon = Icons.Default.Security,
                    label = "Mining",
                    onClick = {
                        HapticUtil.lightTap(context)
                        onNavigateToMining()
                    }
                )
            }
        }

        // 3. SPW Core Layer 1 Asset Row
        item {
            Column {
                Text(
                    text = "LAYER 1 ASSET",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                FinanceCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceSubtle)
                                    .border(1.dp, BorderSubtle, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SPW",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Sparrow",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SurfaceElevated)
                                            .border(0.8.dp, BorderSubtle, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Native",
                                            color = TextSecondary,
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
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = String.format(Locale.US, "%.8f", totalBalanceSpw).trimEnd('0').let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.SansSerif
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

        // 4. On-Chain Security Features Card
        item {
            FinanceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceSubtle)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dual-Key Stealth & Offline Vaults",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
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
                    text = "RECENT ACTIVITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .bouncyClickable {
                            HapticUtil.lightTap(context)
                            onNavigateToHistory()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        if (isSyncing && transactions.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TransactionRowSkeleton()
                    TransactionRowSkeleton()
                }
            }
        } else if (transactions.isEmpty()) {
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
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
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No on-chain transactions yet",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Transactions broadcasted or received on the SPW node will appear here.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            itemsIndexed(transactions.take(5)) { index, tx ->
                TransactionRowCard(
                    tx = tx,
                    walletAddress = walletAddress,
                    modifier = Modifier.staggeredEntrance(index),
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
    accent: Color = TextPrimary,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .bouncyClickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfacePrimary)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(22.dp)
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
fun TransactionRowCard(
    tx: TransactionItem,
    modifier: Modifier = Modifier,
    walletAddress: String = "",
    onClick: () -> Unit = {}
) {
    val isIncoming = when (tx.type) {
        TransactionType.RECEIVE -> true
        TransactionType.SEND -> false
        TransactionType.STEALTH -> {
            if (walletAddress.isNotBlank() && tx.fromAddress.equals(walletAddress, ignoreCase = true)) {
                false
            } else {
                true
            }
        }
    }
    val isStealth = tx.type == TransactionType.STEALTH

    val amountColor = if (isIncoming) SemanticPositive else SemanticError
    val iconColor = if (isIncoming) SemanticPositive else SemanticError

    val typeLabel = when {
        isStealth -> if (isIncoming) "Stealth Received" else "Stealth Sent"
        isIncoming -> "Received"
        else -> "Sent"
    }

    val timeFormatted = remember(tx.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        sdf.format(Date(tx.timestamp))
    }

    FinanceCard(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
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
                        .background(SurfaceSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isStealth -> Icons.Default.Shield
                            isIncoming -> Icons.AutoMirrored.Filled.CallReceived
                            else -> Icons.AutoMirrored.Filled.Send
                        },
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
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
                    color = amountColor,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = tx.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tx.status == TransactionStatus.CONFIRMED) SemanticPositive else SemanticWarning,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
