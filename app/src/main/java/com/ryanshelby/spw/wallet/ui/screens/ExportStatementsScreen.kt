package com.ryanshelby.spw.wallet.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.data.model.AppLanguage
import com.ryanshelby.spw.wallet.data.model.NetworkConfig
import com.ryanshelby.spw.wallet.data.model.TransactionItem
import com.ryanshelby.spw.wallet.data.model.TransactionType
import com.ryanshelby.spw.wallet.security.CsvExportUtil
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.PdfStatementExportUtil
import com.ryanshelby.spw.wallet.ui.components.FinanceCard
import com.ryanshelby.spw.wallet.ui.theme.BorderStrong
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import com.ryanshelby.spw.wallet.ui.theme.bouncyClickable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExportFormat(val title: String, val subtitle: String) {
    PDF("Official Bank Statement (PDF)", "Bank-grade audited vector PDF for accounting & records"),
    CSV("Accounting Ledger (CSV)", "Raw spreadsheet for Excel, CoinTracker & tax preparation")
}

enum class ExportPeriod(val id: String, val label: String) {
    ALL("all", "All Time"),
    LAST_24H("24h", "Last 24 Hours"),
    LAST_7D("7d", "Last 7 Days"),
    LAST_30D("30d", "Last 30 Days"),
    LAST_90D("90d", "Last 90 Days"),
    YEAR_2026("2026", "Year 2026"),
    YEAR_2025("2025", "Year 2025"),
    CUSTOM("custom", "Custom Range")
}

enum class ExportTxTypeFilter(val label: String) {
    ALL("All Types"),
    INFLOW_ONLY("Received Only (+IN)"),
    OUTFLOW_ONLY("Sent Only (-OUT)"),
    STEALTH_ONLY("Stealth Only")
}

@Composable
fun ExportStatementsScreen(
    transactions: List<TransactionItem>,
    walletAddress: String,
    network: NetworkConfig,
    activeLanguage: AppLanguage = AppLanguage.ENGLISH,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedPeriod by remember { mutableStateOf(ExportPeriod.ALL) }
    var selectedTypeFilter by remember { mutableStateOf(ExportTxTypeFilter.ALL) }

    // Custom Date Range State
    val calendar = remember { Calendar.getInstance() }
    var customStartDateMillis by remember {
        mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis)
    }
    var customEndDateMillis by remember {
        mutableStateOf(System.currentTimeMillis())
    }

    var showPreviewList by remember { mutableStateOf(false) }

    val displayDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    // Filter Logic
    val filteredTransactions = remember(transactions, selectedPeriod, selectedTypeFilter, customStartDateMillis, customEndDateMillis) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        transactions.filter { tx ->
            val txMillis = if (tx.timestamp > 1000000000000L) tx.timestamp else tx.timestamp * 1000L

            val matchesPeriod = when (selectedPeriod) {
                ExportPeriod.ALL -> true
                ExportPeriod.LAST_24H -> (now - txMillis) <= 24 * 60 * 60 * 1000L
                ExportPeriod.LAST_7D -> (now - txMillis) <= 7L * 24 * 60 * 60 * 1000L
                ExportPeriod.LAST_30D -> (now - txMillis) <= 30L * 24 * 60 * 60 * 1000L
                ExportPeriod.LAST_90D -> (now - txMillis) <= 90L * 24 * 60 * 60 * 1000L
                ExportPeriod.YEAR_2026 -> {
                    cal.timeInMillis = txMillis
                    cal.get(Calendar.YEAR) == 2026
                }
                ExportPeriod.YEAR_2025 -> {
                    cal.timeInMillis = txMillis
                    cal.get(Calendar.YEAR) == 2025
                }
                ExportPeriod.CUSTOM -> {
                    // Set end of day for custom end date
                    val endDayCal = Calendar.getInstance().apply {
                        timeInMillis = customEndDateMillis
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }
                    val startDayCal = Calendar.getInstance().apply {
                        timeInMillis = customStartDateMillis
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    txMillis in startDayCal.timeInMillis..endDayCal.timeInMillis
                }
            }

            val matchesType = when (selectedTypeFilter) {
                ExportTxTypeFilter.ALL -> true
                ExportTxTypeFilter.INFLOW_ONLY -> tx.type == TransactionType.RECEIVE
                ExportTxTypeFilter.OUTFLOW_ONLY -> tx.type == TransactionType.SEND || tx.type == TransactionType.STEALTH
                ExportTxTypeFilter.STEALTH_ONLY -> tx.type == TransactionType.STEALTH
            }

            matchesPeriod && matchesType
        }
    }

    // Telemetry aggregations
    var totalInflow = 0.0
    var totalOutflow = 0.0
    var totalFees = 0.0
    for (tx in filteredTransactions) {
        if (tx.type == TransactionType.RECEIVE) {
            totalInflow += tx.amountSpw
        } else {
            totalOutflow += tx.amountSpw
        }
        totalFees += tx.feeSpw
    }
    val netVolume = totalInflow - totalOutflow

    val periodLabel = when (selectedPeriod) {
        ExportPeriod.CUSTOM -> "${displayDateFormat.format(Date(customStartDateMillis))} to ${displayDateFormat.format(Date(customEndDateMillis))}"
        else -> selectedPeriod.label
    }

    val exportLabel = "Export_${selectedPeriod.id}_${selectedTypeFilter.name.lowercase()}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Top Navigation Bar ──────────────────────────────────
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
                    .background(SurfacePrimary)
                    .border(1.dp, BorderSubtle, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Export & Statements",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generate audited PDF bank statements & CSV ledgers",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Format Selector ─────────────────────────────────
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DOCUMENT FORMAT",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExportFormat.values().forEach { fmt ->
                                val isSelected = selectedFormat == fmt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) SurfaceElevated else SurfaceSubtle)
                                        .border(1.dp, if (isSelected) CyanNeon else BorderSubtle, RoundedCornerShape(12.dp))
                                        .bouncyClickable {
                                            HapticUtil.lightTap(context)
                                            selectedFormat = fmt
                                        }
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (fmt == ExportFormat.PDF) Icons.Default.PictureAsPdf else Icons.Default.Analytics,
                                                contentDescription = null,
                                                tint = if (isSelected) CyanNeon else TextMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (fmt == ExportFormat.PDF) "PDF Statement" else "CSV Ledger",
                                            color = if (isSelected) TextPrimary else TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (fmt == ExportFormat.PDF) "Bank-grade audit" else "Raw spreadsheet",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Timeframe & Date Filters ────────────────────────
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "REPORTING PERIOD",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Period Grid (Chips)
                        val periodList = ExportPeriod.values().toList()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                periodList.take(4).forEach { period ->
                                    val isSelected = selectedPeriod == period
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) SurfaceElevated else SurfaceSubtle)
                                            .border(1.dp, if (isSelected) BorderStrong else BorderSubtle, RoundedCornerShape(8.dp))
                                        .bouncyClickable {
                                            HapticUtil.lightTap(context)
                                            selectedPeriod = period
                                        }
                                        .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = period.label,
                                            color = if (isSelected) CyanNeon else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                periodList.drop(4).forEach { period ->
                                    val isSelected = selectedPeriod == period
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) SurfaceElevated else SurfaceSubtle)
                                            .border(1.dp, if (isSelected) BorderStrong else BorderSubtle, RoundedCornerShape(8.dp))
                                        .bouncyClickable {
                                            HapticUtil.lightTap(context)
                                            selectedPeriod = period
                                        }
                                        .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = period.label,
                                            color = if (isSelected) CyanNeon else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Date Range Pickers
                        if (selectedPeriod == ExportPeriod.CUSTOM) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Start Date Picker Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceSubtle)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                        .clickable {
                                            calendar.timeInMillis = customStartDateMillis
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    calendar.set(y, m, d)
                                                    customStartDateMillis = calendar.timeInMillis
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("START DATE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = displayDateFormat.format(Date(customStartDateMillis)),
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // End Date Picker Button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceSubtle)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                        .clickable {
                                            calendar.timeInMillis = customEndDateMillis
                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    calendar.set(y, m, d)
                                                    customEndDateMillis = calendar.timeInMillis
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("END DATE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = displayDateFormat.format(Date(customEndDateMillis)),
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Transaction Type Filter ─────────────────────────
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "TRANSACTION TYPE",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExportTxTypeFilter.values().forEach { filter ->
                                val isSelected = selectedTypeFilter == filter
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) SurfaceElevated else SurfaceSubtle)
                                        .border(1.dp, if (isSelected) BorderStrong else BorderSubtle, RoundedCornerShape(8.dp))
                                        .bouncyClickable {
                                            HapticUtil.lightTap(context)
                                            selectedTypeFilter = filter
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (filter) {
                                            ExportTxTypeFilter.ALL -> "ALL"
                                            ExportTxTypeFilter.INFLOW_ONLY -> "INFLOW"
                                            ExportTxTypeFilter.OUTFLOW_ONLY -> "OUTFLOW"
                                            ExportTxTypeFilter.STEALTH_ONLY -> "STEALTH"
                                        },
                                        color = if (isSelected) CyanNeon else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Live Telemetry Audit Card ────────────────────────
            item {
                FinanceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AUDIT RECAP PREVIEW",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "${filteredTransactions.size} Records Found",
                                color = CyanNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TOTAL INFLOW", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("+${String.format(Locale.US, "%.4f", totalInflow)} SPW", color = SemanticPositive, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TOTAL OUTFLOW", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("-${String.format(Locale.US, "%.4f", totalOutflow)} SPW", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("NET FLOW", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text((if (netVolume >= 0) "+" else "") + String.format(Locale.US, "%.4f", netVolume) + " SPW", color = if (netVolume >= 0) SemanticPositive else SemanticError, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("FEES PAID", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.US, "%.4f", totalFees)} SPW", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preview Records Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceSubtle)
                                .clickable { showPreviewList = !showPreviewList }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showPreviewList) "Hide Transaction Preview" else "Show Transaction Preview (${filteredTransactions.size})",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (showPreviewList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = showPreviewList,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (filteredTransactions.isEmpty()) {
                                    Text("No records match the active criteria", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                                } else {
                                    filteredTransactions.take(10).forEach { tx ->
                                        val isIncoming = tx.type == TransactionType.RECEIVE
                                        val isStealth = tx.type == TransactionType.STEALTH
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SurfaceElevated)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when {
                                                        isStealth -> Icons.Default.Shield
                                                        isIncoming -> Icons.AutoMirrored.Filled.CallReceived
                                                        else -> Icons.AutoMirrored.Filled.Send
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isIncoming) SemanticPositive else CyanNeon,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (tx.txHash.length > 10) tx.txHash.take(6) + "..." + tx.txHash.takeLast(4) else tx.txHash,
                                                    color = TextSecondary,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Text(
                                                text = (if (isIncoming) "+" else "-") + String.format(Locale.US, "%.4f", tx.amountSpw) + " SPW",
                                                color = if (isIncoming) SemanticPositive else TextPrimary,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    if (filteredTransactions.size > 10) {
                                        Text("+ ${filteredTransactions.size - 10} more records in final document", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // ── Action Footer Bar ───────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Save to Device Button
                Button(
                    onClick = {
                        HapticUtil.lightTap(context)
                        if (selectedFormat == ExportFormat.PDF) {
                            PdfStatementExportUtil.exportAndSavePdfToDevice(
                                context = context,
                                transactions = filteredTransactions,
                                walletAddress = walletAddress,
                                networkName = network.name,
                                periodLabel = periodLabel
                            )
                        } else {
                            CsvExportUtil.exportAndSaveCsvToDevice(
                                context = context,
                                transactions = filteredTransactions,
                                label = exportLabel
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary, contentColor = ButtonPrimaryText),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Device", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Share Button
                Button(
                    onClick = {
                        HapticUtil.lightTap(context)
                        if (selectedFormat == ExportFormat.PDF) {
                            PdfStatementExportUtil.exportAndSharePdf(
                                context = context,
                                transactions = filteredTransactions,
                                walletAddress = walletAddress,
                                networkName = network.name,
                                periodLabel = periodLabel
                            )
                        } else {
                            CsvExportUtil.exportAndShareCsv(
                                context = context,
                                transactions = filteredTransactions,
                                label = exportLabel
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = TextPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}
