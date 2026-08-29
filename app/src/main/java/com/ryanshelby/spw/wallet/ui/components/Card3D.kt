package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ryanshelby.spw.wallet.data.model.NetworkConfig

/**
 * Compatibility delegate for Holographic3DCard -> PortfolioOverviewCard.
 * Discards AI neon glows and glassmorphism in favor of financial-grade architecture.
 */
@Composable
fun Holographic3DCard(
    isSyncing: Boolean = false,
    walletName: String,
    walletAddress: String,
    totalBalanceSpw: Double,
    totalBalanceFeathers: Long,
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
        walletName = walletName,
        walletAddress = walletAddress,
        totalBalanceSpw = totalBalanceSpw,
        totalBalanceFeathers = totalBalanceFeathers,
        network = network,
        hideBalance = hideBalance,
        onToggleHideBalance = onToggleHideBalance,
        onCopyAddress = onCopyAddress,
        onShowQr = onShowQr,
        onScanQr = onScanQr,
        modifier = modifier
    )
}
