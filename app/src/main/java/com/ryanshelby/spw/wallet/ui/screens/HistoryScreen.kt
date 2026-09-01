package com.ryanshelby.spw.wallet.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import com.ryanshelby.spw.wallet.data.model.TransactionItem
import com.ryanshelby.spw.wallet.data.model.TransactionStatus
import com.ryanshelby.spw.wallet.data.model.TransactionType
import com.ryanshelby.spw.wallet.data.model.TranslationHelper
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
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
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

enum class TimeframeOption(val id: String, val seconds: Long) {
    ALL("ALL", 0L),
    DAY("24H", 86400L),
    WEEK("7D", 7 * 86400L),
    MONTH("30D", 30 * 86400L),
    YEAR("1Y", 365 * 86400L)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    transactions: List<TransactionItem>,
    activeLanguage: AppLanguage,
    network: NetworkConfig,
    walletAddress: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = remember(activeLanguage) { TranslationHelper.getStrings(activeLanguage) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    var selectedTimeframe by remember { mutableStateOf(TimeframeOption.ALL) }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showAnalytics by remember { mutableStateOf(true) }
    var showExportSheet by remember { mutableStateOf(false) }
    var selectedTxForDetails by remember { mutableStateOf<TransactionItem?>(null) }
    var isFilterTransitioning by remember { mutableStateOf(false) }

    val typeFilterOptions = listOf("ALL", "RECEIVED", "SENT", "STEALTH")

    LaunchedEffect(selectedTimeframe, selectedTypeFilter) {
        isFilterTransitioning = true
        delay(80)
        isFilterTransitioning = false
    }

    val nowSec = remember { System.currentTimeMillis() / 1000L }

    // Filter transactions by timeframe, type, and search query
    val filteredTransactions = remember(transactions, selectedTimeframe, selectedTypeFilter, searchQuery) {
        transactions.filter { tx ->
            val matchesTime = if (selectedTimeframe.seconds == 0L) true else {
                val txSec = if (tx.timestamp > 1000000000000L) tx.timestamp / 1000L else tx.timestamp
                txSec >= (nowSec - selectedTimeframe.seconds)
            }

            val matchesType = when (selectedTypeFilter) {
                "SENT" -> tx.type == TransactionType.SEND
                "RECEIVED" -> tx.type == TransactionType.RECEIVE
                "STEALTH" -> tx.type == TransactionType.STEALTH
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                tx.txHash.contains(searchQuery, ignoreCase = true) ||
                tx.toAddress.contains(searchQuery, ignoreCase = true) ||
                tx.fromAddress.contains(searchQuery, ignoreCase = true) ||
                tx.memo.contains(searchQuery, ignoreCase = true) ||
                tx.amountSpw.toString().contains(searchQuery)
            }

            matchesTime && matchesType && matchesSearch
        }
    }

    // Compute Financial Telemetry Metrics over active filtered list
    val totalInflow = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.RECEIVE }.sumOf { it.amountSpw }
    }
    val totalOutflow = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.SEND || it.type == TransactionType.STEALTH }.sumOf { it.amountSpw }
    }
    val totalFeesPaid = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.feeSpw }
    }
    val netFlow = totalInflow - totalOutflow
    val totalVolume = totalInflow + totalOutflow
    val inflowRatio = if (totalVolume > 0) (totalInflow / totalVolume).toFloat() else 0.5f

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

            // ── Header ──────────────────────────────────────────
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.history,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${network.name} Ledger • ${filteredTransactions.size} records",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

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
                                showExportSheet = true
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

            Spacer(modifier = Modifier.height(14.dp))

            // ── Financial Telemetry Card ────────────────────────
            FinanceCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.lightTap(context)
                                showAnalytics = !showAnalytics
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PORTFOLIO LEDGER TELEMETRY",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Icon(
                            imageVector = if (showAnalytics) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showAnalytics,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            // 4 Metrics Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Inflow
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceSubtle)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(strings.inflows, color = TextSecondary, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "+${String.format(Locale.US, "%.4f", totalInflow)}",
                                            color = SemanticPositive,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Outflow
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceSubtle)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(strings.outflows, color = TextSecondary, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "-${String.format(Locale.US, "%.4f", totalOutflow)}",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Net Flow
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceSubtle)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(strings.netFlow, color = TextSecondary, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = (if (netFlow >= 0) "+" else "") + String.format(Locale.US, "%.4f", netFlow),
                                            color = if (netFlow >= 0) SemanticPositive else SemanticError,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Fees
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceSubtle)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(strings.totalFees, color = TextSecondary, fontSize = 10.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.4f", totalFeesPaid)} SPW",
                                            color = SemanticWarning,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Volume Ratio Bar
                            if (totalVolume > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceSubtle)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(inflowRatio.coerceIn(0.01f, 0.99f))
                                            .fillMaxSize()
                                            .background(SemanticPositive)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight((1f - inflowRatio).coerceIn(0.01f, 0.99f))
                                            .fillMaxSize()
                                            .background(CyanNeon)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Search Input ────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search TXID, address, memo or amount", color = TextMuted, fontSize = 12.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
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

            Spacer(modifier = Modifier.height(10.dp))

            // ── Timeframe & Type Filter Chips ───────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeframe Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(TimeframeOption.entries.toTypedArray()) { tf ->
                        val isSelected = selectedTimeframe == tf
                        val label = when (tf) {
                            TimeframeOption.ALL -> strings.allTime
                            TimeframeOption.DAY -> strings.last24h
                            TimeframeOption.WEEK -> strings.last7d
                            TimeframeOption.MONTH -> strings.last30d
                            TimeframeOption.YEAR -> strings.thisYear
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SurfaceElevated else SurfacePrimary)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BorderStrong else BorderSubtle,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .bouncyClickable {
                                    HapticUtil.lightTap(context)
                                    selectedTimeframe = tf
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Type Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(typeFilterOptions) { filter ->
                    val isSelected = selectedTypeFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) SurfaceElevated else SurfacePrimary)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) BorderStrong else BorderSubtle,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .bouncyClickable {
                                HapticUtil.lightTap(context)
                                selectedTypeFilter = filter
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) CyanNeon else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Transactions List ───────────────────────────────
            if (isFilterTransitioning) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TransactionRowSkeleton()
                    TransactionRowSkeleton()
                    TransactionRowSkeleton()
                }
            } else if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedTypeFilter != "ALL" || selectedTimeframe != TimeframeOption.ALL) {
                                "No transactions match your active filters"
                            } else {
                                "No transaction history yet"
                            },
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
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
                        val accentColor = if (isIncoming) SemanticPositive else SemanticError

                        FinanceCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredEntrance(index)
                                .bouncyClickable {
                                    HapticUtil.lightTap(context)
                                    selectedTxForDetails = tx
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
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
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = when {
                                                    isStealth -> "Stealth Transfer"
                                                    isIncoming -> "Received"
                                                    else -> "Sent"
                                                },
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            if (tx.status != TransactionStatus.CONFIRMED) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${tx.status.name})",
                                                    color = when (tx.status) {
                                                        TransactionStatus.PENDING -> SemanticWarning
                                                        TransactionStatus.FAILED -> SemanticError
                                                        else -> TextMuted
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        val counterparty = if (isIncoming) tx.fromAddress else tx.toAddress
                                        Text(
                                            text = if (counterparty.isNotBlank()) counterparty.take(8) + "..." + counterparty.takeLast(6) else "On-Chain",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        Text(
                                            text = dateFormatter.format(Date(if (tx.timestamp > 1000000000000L) tx.timestamp else tx.timestamp * 1000L)),
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = (if (isIncoming) "+" else "-") + String.format(Locale.US, "%.4f", tx.amountSpw) + " ${tx.tokenSymbol}",
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (tx.feeSpw > 0) {
                                        Text(
                                            text = "Fee: ${String.format(Locale.US, "%.4f", tx.feeSpw)}",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // ── Export ModalBottomSheet (PDF Statement & CSV Ledger) ──
    if (showExportSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var exportScopeIsFiltered by remember { mutableStateOf(true) }
        val targetTxs = if (exportScopeIsFiltered) filteredTransactions else transactions
        val targetPeriodLabel = if (exportScopeIsFiltered) "Filtered (${selectedTimeframe.id})" else "Full Ledger"
        val targetCsvLabel = if (exportScopeIsFiltered) "Filtered_${selectedTimeframe.id}" else "Full_Ledger"

        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = sheetState,
            containerColor = DarkBackground,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FINANCIAL EXPORTS",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Bank-grade PDF statements & CSV accounting",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { showExportSheet = false }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── SCOPE SELECTOR TABS ─────────────────────────
                Text(
                    text = "SELECT DATA SCOPE",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (exportScopeIsFiltered) SurfaceElevated else SurfacePrimary)
                            .border(1.dp, if (exportScopeIsFiltered) BorderStrong else BorderSubtle, RoundedCornerShape(10.dp))
                            .bouncyClickable {
                                HapticUtil.lightTap(context)
                                exportScopeIsFiltered = true
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Filtered (${filteredTransactions.size} txs)",
                            color = if (exportScopeIsFiltered) CyanNeon else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!exportScopeIsFiltered) SurfaceElevated else SurfacePrimary)
                            .border(1.dp, if (!exportScopeIsFiltered) BorderStrong else BorderSubtle, RoundedCornerShape(10.dp))
                            .bouncyClickable {
                                HapticUtil.lightTap(context)
                                exportScopeIsFiltered = false
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All History (${transactions.size} txs)",
                            color = if (!exportScopeIsFiltered) CyanNeon else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── OPTION 1: OFFICIAL PDF STATEMENT ────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfacePrimary)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Official Statement (PDF)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Institutional formatted bank-grade audit document", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    showExportSheet = false
                                    com.ryanshelby.spw.wallet.security.PdfStatementExportUtil.exportAndSavePdfToDevice(
                                        context = context,
                                        transactions = targetTxs,
                                        walletAddress = walletAddress,
                                        networkName = network.name,
                                        periodLabel = targetPeriodLabel
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary, contentColor = ButtonPrimaryText),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Device", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    showExportSheet = false
                                    com.ryanshelby.spw.wallet.security.PdfStatementExportUtil.exportAndSharePdf(
                                        context = context,
                                        transactions = targetTxs,
                                        walletAddress = walletAddress,
                                        networkName = network.name,
                                        periodLabel = targetPeriodLabel
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = TextPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── OPTION 2: RAW SPREADSHEET (CSV) ────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfacePrimary)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Accounting Ledger (CSV)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Raw tabular spreadsheet for tax software & Excel", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showExportSheet = false
                                    CsvExportUtil.exportAndSaveCsvToDevice(context, targetTxs, label = targetCsvLabel)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Device", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    showExportSheet = false
                                    CsvExportUtil.exportAndShareCsv(context, targetTxs, label = targetCsvLabel)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ── Transaction Details Dialog ──────────────────────────────
    if (selectedTxForDetails != null) {
        TransactionDetailDialog(
            tx = selectedTxForDetails!!,
            walletAddress = walletAddress,
            onDismiss = { selectedTxForDetails = null }
        )
    }
}
