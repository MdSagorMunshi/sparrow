package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import com.ryanshelby.spw.wallet.data.model.TransactionItem

/**
 * Compatibility delegate for Holographic3DCard -> PortfolioOverviewCard.
 * Discards AI neon glows and glassmorphism in favor of financial-grade architecture.
 */
@Composable
fun Holographic3DCard(
    isSyncing: Boolean = false,
    isOnline: Boolean = true,
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
        onShowQr = onShowQr,
        onScanQr = onScanQr,
        modifier = modifier
    )
}
