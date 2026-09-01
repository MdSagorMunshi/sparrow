package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.data.remote.SpwUtxo
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.theme.AmberGold
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinControlBottomSheet(
    allUtxos: List<SpwUtxo>,
    initiallySelected: List<SpwUtxo>?,
    requiredFeathers: Long,
    onApplySelection: (List<SpwUtxo>?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Selection set of unique UTXO identifiers (txid:vout)
    var selectedKeys by remember {
        mutableStateOf(
            (initiallySelected ?: emptyList()).map { "${it.txid}:${it.vout}" }.toSet()
        )
    }

    val selectedUtxos = remember(selectedKeys, allUtxos) {
        allUtxos.filter { "${it.txid}:${it.vout}" in selectedKeys }
    }
    val totalSelectedFeathers = remember(selectedUtxos) {
        selectedUtxos.sumOf { it.amount }
    }
    val totalSelectedSpw = totalSelectedFeathers.toDouble() / 100_000_000.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceElevated,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Coin Control (UTXO Manager)", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${allUtxos.size} unspent outputs available", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Presets / Quick Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(
                    onClick = {
                        HapticUtil.lightTap(context)
                        selectedKeys = allUtxos.map { "${it.txid}:${it.vout}" }.toSet()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select All", fontSize = 11.sp, color = CyanNeon, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = {
                        HapticUtil.lightTap(context)
                        // Greedy largest first
                        val sorted = allUtxos.sortedByDescending { it.amount }
                        var acc = 0L
                        val chosen = mutableSetOf<String>()
                        for (u in sorted) {
                            chosen.add("${u.txid}:${u.vout}")
                            acc += u.amount
                            if (requiredFeathers > 0 && acc >= requiredFeathers) break
                        }
                        selectedKeys = chosen
                    },
                    modifier = Modifier.weight(1.1f)
                ) {
                    Text("Largest First", fontSize = 11.sp, color = CyanNeon, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = {
                        HapticUtil.lightTap(context)
                        selectedKeys = emptySet()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Auto Mode", fontSize = 11.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // UTXO List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allUtxos, key = { "${it.txid}:${it.vout}" }) { utxo ->
                    val key = "${utxo.txid}:${utxo.vout}"
                    val isChecked = key in selectedKeys
                    val amountSpw = utxo.amount.toDouble() / 100_000_000.0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChecked) CyanNeon.copy(alpha = 0.08f) else SurfacePrimary)
                            .border(1.dp, if (isChecked) CyanNeon.copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(12.dp))
                            .clickable {
                                HapticUtil.lightTap(context)
                                selectedKeys = if (isChecked) {
                                    selectedKeys - key
                                } else {
                                    selectedKeys + key
                                }
                            }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        HapticUtil.lightTap(context)
                                        selectedKeys = if (checked) selectedKeys + key else selectedKeys - key
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = CyanNeon)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.6f", amountSpw)} SPW",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (utxo.isStealth) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(PurpleNeon.copy(alpha = 0.2f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("STEALTH", color = PurpleNeon, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${utxo.txid.take(12)}...:${utxo.vout}",
                                        color = TextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (utxo.blockHeight != null) {
                                Text(
                                    text = "Block #${utxo.blockHeight}",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer Summary & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (selectedKeys.isEmpty()) "AUTOMATIC SELECTION" else "${selectedKeys.size} UTXOs Selected",
                        color = if (selectedKeys.isEmpty()) TextMuted else CyanNeon,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedKeys.isEmpty()) "Algorithm chooses inputs" else "${String.format(Locale.US, "%.6f", totalSelectedSpw)} SPW Total",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            HapticUtil.performSuccess(context)
                            if (selectedKeys.isEmpty()) {
                                onApplySelection(null) // null means auto-selection
                            } else {
                                onApplySelection(selectedUtxos)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                    ) {
                        Text("Apply Selection", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
