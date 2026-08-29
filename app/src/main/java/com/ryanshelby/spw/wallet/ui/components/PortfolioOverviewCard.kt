package com.ryanshelby.spw.wallet.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import com.ryanshelby.spw.wallet.data.model.TransactionItem
import com.ryanshelby.spw.wallet.data.model.TransactionType
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.FinancialSubBalanceStyle
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Institutional Portfolio Balance Card with 3D Flippable Backside.
 * Swiping the card in any direction (left/right or up/down) or tapping the 3D flip button
 * reveals comprehensive financial statistics: Total Received, Total Spent, Total Transactions,
 * Net Flow, and Shielded activity.
 */
@Composable
fun PortfolioOverviewCard(
    isSyncing: Boolean = false,
    walletName: String,
    walletAddress: String,
    totalBalanceSpw: Double,
    totalBalanceFeathers: Long,
    transactions: List<TransactionItem> = emptyList(),
    network: NetworkConfig,
    hideBalance: Boolean,
    onToggleHideBalance: () -> Unit,
    onCopyAddress: (String) -> Unit,
    onShowQr: () -> Unit,
    onScanQr: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copiedRecently by remember { mutableStateOf(false) }

    LaunchedEffect(copiedRecently) {
        if (copiedRecently) {
            delay(1800)
            copiedRecently = false
        }
    }

    // ── 3D Card Flip & Swipe State ──────────────────────────────────────────
    var isFlipped by remember { mutableStateOf(false) }
    var flipAxisHorizontal by remember { mutableStateOf(true) }
    var dragAccumulatorX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatorY by remember { mutableFloatStateOf(0f) }

    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped && flipAxisHorizontal) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardFlipY"
    )

    val rotationX by animateFloatAsState(
        targetValue = if (isFlipped && !flipAxisHorizontal) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardFlipX"
    )

    val showBack = if (flipAxisHorizontal) (rotationY > 90f) else (rotationX > 90f)

    // ── Financial Ledger Calculations ───────────────────────────────────────
    val totalReceived = remember(transactions, walletAddress) {
        transactions.filter { tx ->
            when (tx.type) {
                TransactionType.RECEIVE -> true
                TransactionType.SEND -> false
                TransactionType.STEALTH -> {
                    if (walletAddress.isNotBlank() && tx.fromAddress.equals(walletAddress, ignoreCase = true)) false else true
                }
            }
        }.sumOf { it.amountSpw }
    }

    val totalSpent = remember(transactions, walletAddress) {
        transactions.filter { tx ->
            when (tx.type) {
                TransactionType.RECEIVE -> false
                TransactionType.SEND -> true
                TransactionType.STEALTH -> {
                    if (walletAddress.isNotBlank() && tx.fromAddress.equals(walletAddress, ignoreCase = true)) true else false
                }
            }
        }.sumOf { it.amountSpw }
    }

    val receivedCount = remember(transactions, walletAddress) {
        transactions.count { tx ->
            when (tx.type) {
                TransactionType.RECEIVE -> true
                TransactionType.SEND -> false
                TransactionType.STEALTH -> {
                    if (walletAddress.isNotBlank() && tx.fromAddress.equals(walletAddress, ignoreCase = true)) false else true
                }
            }
        }
    }

    val spentCount = remember(transactions, walletAddress) {
        transactions.count { tx ->
            when (tx.type) {
                TransactionType.RECEIVE -> false
                TransactionType.SEND -> true
                TransactionType.STEALTH -> {
                    if (walletAddress.isNotBlank() && tx.fromAddress.equals(walletAddress, ignoreCase = true)) true else false
                }
            }
        }
    }

    val stealthCount = remember(transactions) {
        transactions.count { it.type == TransactionType.STEALTH }
    }

    val netFlow = totalReceived - totalSpent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatorX += dragAmount.x
                        dragAccumulatorY += dragAmount.y
                    },
                    onDragEnd = {
                        val absX = abs(dragAccumulatorX)
                        val absY = abs(dragAccumulatorY)
                        val threshold = 35f
                        if (absX > threshold || absY > threshold) {
                            flipAxisHorizontal = absX >= absY
                            HapticUtil.performKeyClick(context)
                            isFlipped = !isFlipped
                        }
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                    },
                    onDragCancel = {
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                    }
                )
            }
            .graphicsLayer {
                this.rotationY = rotationY
                this.rotationX = rotationX
                cameraDistance = 16f * density
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SurfacePrimary,
                        Color(0xFF161B24)
                    )
                )
            )
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        if (!showBack) {
            // ═════════════════════════════════════════════════════════════════
            // FRONT SIDE: Portfolio Balance & Node Overview
            // ═════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Top Row: Wallet Name & Network Chip & Action Icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SemanticPositive)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = walletName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceElevated)
                                .border(0.8.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = network.symbol,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 3D Flip Card Action Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevated)
                                .border(0.8.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                .bouncyClickable {
                                    HapticUtil.performKeyClick(context)
                                    flipAxisHorizontal = true
                                    isFlipped = !isFlipped
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipToBack,
                                contentDescription = "Flip to Financial Stats",
                                tint = TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        if (onScanQr != null) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceElevated)
                                    .border(0.8.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .bouncyClickable {
                                        HapticUtil.lightTap(context)
                                        onScanQr()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevated)
                                .border(0.8.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                .bouncyClickable {
                                    HapticUtil.lightTap(context)
                                    onShowQr()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "Receive QR",
                                tint = TextSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Middle: Balance Display with Animated Counter
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TOTAL BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = {
                                HapticUtil.lightTap(context)
                                onToggleHideBalance()
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = if (hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Balance",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isSyncing && totalBalanceSpw == 0.0) {
                        Text(
                            text = "Syncing Node...",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Querying live block headers…",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        AnimatedBalanceCounter(
                            targetBalance = totalBalanceSpw,
                            hideBalance = hideBalance,
                            unit = "SPW"
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        if (!hideBalance) {
                            val formattedFeathers = NumberFormat.getNumberInstance(Locale.US).format(
                                if (totalBalanceFeathers > 0) totalBalanceFeathers else (totalBalanceSpw * SPWCrypto.FEATHERS_PER_SPW).toLong()
                            )
                            Text(
                                text = "≈ $formattedFeathers feathers",
                                style = FinancialSubBalanceStyle,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Bottom Bar: Address Chip with Copy & Node Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Short Address Pill (Clickable)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceSubtle)
                            .border(0.8.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .bouncyClickable {
                                HapticUtil.performKeyClick(context)
                                onCopyAddress(walletAddress)
                                copiedRecently = true
                                Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (walletAddress.length > 14) {
                                "${walletAddress.take(6)}...${walletAddress.takeLast(6)}"
                            } else walletAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AnimatedVisibility(
                            visible = copiedRecently,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Copied",
                                tint = SemanticPositive,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        if (!copiedRecently) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Address",
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Live Node status pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated)
                            .border(0.8.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SemanticPositive)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "LIVE NODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        } else {
            // ═════════════════════════════════════════════════════════════════
            // BACK SIDE: Financial Ledger Statistics (3D Counter-Rotated)
            // ═════════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        if (flipAxisHorizontal) {
                            this.rotationY = 180f
                        } else {
                            this.rotationX = 180f
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Back Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "FINANCIAL LEDGER",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
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
                                    text = "3D STATS",
                                    color = TextSecondary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Flip to Front button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevated)
                                .border(0.8.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                .bouncyClickable {
                                    HapticUtil.performKeyClick(context)
                                    flipAxisHorizontal = true
                                    isFlipped = false
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipToFront,
                                contentDescription = "Flip Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Front",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 2-Column Summary Cards: Total Received & Total Spent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Received Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceSubtle)
                                .border(0.8.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallReceived,
                                        contentDescription = null,
                                        tint = SemanticPositive,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TOTAL RECEIVED",
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+" + String.format(Locale.US, "%.8f", totalReceived)
                                        .trimEnd('0')
                                        .let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                                    color = SemanticPositive,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$receivedCount deposit${if (receivedCount != 1) "s" else ""}",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Total Spent Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceSubtle)
                                .border(0.8.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        tint = SemanticError,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TOTAL SPENT",
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "-" + String.format(Locale.US, "%.8f", totalSpent)
                                        .trimEnd('0')
                                        .let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                                    color = SemanticError,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$spentCount transfer${if (spentCount != 1) "s" else ""}",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Key Ledger Insights: Net Flow, Total Transactions, Shielded Activity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated)
                            .border(0.8.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NET FLOW", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            val netColor = when {
                                netFlow > 0 -> SemanticPositive
                                netFlow < 0 -> SemanticError
                                else -> TextSecondary
                            }
                            Text(
                                text = (if (netFlow > 0) "+" else "") + String.format(Locale.US, "%.4f", netFlow) + " SPW",
                                color = netColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL TXS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${transactions.size}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("SHIELDED", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$stealthCount stealth",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Bottom Bar: Swipe Prompt & Node Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Swipe card to flip back",
                            color = TextMuted,
                            fontSize = 10.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SemanticPositive)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${network.name.uppercase()} SYNCED",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
