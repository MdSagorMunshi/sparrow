package com.ryanshelby.spw.wallet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Send
import android.widget.Toast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import com.ryanshelby.spw.wallet.data.model.TransactionItem
import com.ryanshelby.spw.wallet.data.model.TransactionStatus
import com.ryanshelby.spw.wallet.data.model.TransactionType
import com.ryanshelby.spw.wallet.security.CsvExportUtil
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.components.FinanceCard
import com.ryanshelby.spw.wallet.ui.components.Identicon
import com.ryanshelby.spw.wallet.ui.components.TransactionDetailDialog
import com.ryanshelby.spw.wallet.ui.components.TransactionRowSkeleton
import com.ryanshelby.spw.wallet.ui.theme.AccentMuted
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderStrong
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    transactions: List<TransactionItem>,
    activeLanguage: AppLanguage,
    network: NetworkConfig,
    walletAddress: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTxForDetails by remember { mutableStateOf<TransactionItem?>(null) }
    var isFilterTransitioning by remember { mutableStateOf(false) }

    val filterOptions = listOf("ALL", "RECEIVED", "SENT", "STEALTH")

    LaunchedEffect(selectedFilter) {
        isFilterTransitioning = true
        delay(120)
        isFilterTransitioning = false
    }

    val filteredTransactions = remember(transactions, selectedFilter, searchQuery) {
        transactions.filter { tx ->
            val matchesFilter = when (selectedFilter) {
                "SENT" -> tx.type == TransactionType.SEND
                "RECEIVED" -> tx.type == TransactionType.RECEIVE
                "STEALTH" -> tx.type == TransactionType.STEALTH
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                tx.txHash.contains(searchQuery, ignoreCase = true) ||
                tx.toAddress.contains(searchQuery, ignoreCase = true) ||
                tx.fromAddress.contains(searchQuery, ignoreCase = true) ||
                tx.memo.contains(searchQuery, ignoreCase = true)
            }

            matchesFilter && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FinanceBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
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
                        text = "Transaction History",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${network.name} Ledger",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Export CSV Ledger Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfacePrimary)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .bouncyClickable {
                            HapticUtil.performKeyClick(context)
                            if (transactions.isEmpty()) {
                                Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                            } else {
                                val ok = CsvExportUtil.exportAndShareCsv(context, transactions)
                                if (!ok) {
                                    Toast.makeText(context, "Failed to export CSV", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export CSV Ledger",
                        tint = TextPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by TXID, address, or memo", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SurfaceElevated else SurfacePrimary)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) BorderStrong else BorderSubtle,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .bouncyClickable {
                                HapticUtil.lightTap(context)
                                selectedFilter = filter
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(TextPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = filter,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transactions list or loading skeleton
            if (isFilterTransitioning) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TransactionRowSkeleton()
                    TransactionRowSkeleton()
                    TransactionRowSkeleton()
                }
            } else if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    FinanceCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No transactions found",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No records match your search criteria." else "There are no confirmed transactions in this filter.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredTransactions) { index, tx ->
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

                        FinanceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredEntrance(index, baseDelayMs = 30),
                            onClick = {
                                HapticUtil.lightTap(context)
                                selectedTxForDetails = tx
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Identicon with action badge overlay
                                    Box(modifier = Modifier.size(40.dp)) {
                                        Identicon(
                                            address = if (isIncoming) tx.fromAddress else tx.toAddress,
                                            size = 40.dp,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(SurfacePrimary)
                                                .border(0.8.dp, BorderSubtle, CircleShape),
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
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = when {
                                                isStealth -> if (isIncoming) "Stealth Received" else "Stealth Sent"
                                                isIncoming -> "Received SPW"
                                                else -> "Sent SPW"
                                            },
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
                                            text = dateFormatter.format(Date(tx.timestamp)),
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
                    item { Spacer(modifier = Modifier.height(30.dp)) }
                }
            }
        }

        // Transaction Details Dialog
        selectedTxForDetails?.let { tx ->
            TransactionDetailDialog(
                tx = tx,
                walletAddress = walletAddress,
                onDismiss = { selectedTxForDetails = null }
            )
        }
    }
}
