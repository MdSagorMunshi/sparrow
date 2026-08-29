package com.ryanshelby.spw.wallet.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ryanshelby.spw.wallet.data.model.TransactionItem
import com.ryanshelby.spw.wallet.data.model.TransactionStatus
import com.ryanshelby.spw.wallet.data.model.TransactionType
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.DividerColor
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Institutional Transaction Detail Modal
 * Calm, financial-grade presentation of all on-chain metadata.
 */
@Composable
fun TransactionDetailDialog(
    tx: TransactionItem,
    walletAddress: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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

    val typeLabel = when {
        isStealth -> if (isIncoming) "Stealth Shielded (Received)" else "Stealth Shielded (Sent)"
        isIncoming -> "Received"
        else -> "Sent"
    }

    val statusIcon = when (tx.status) {
        TransactionStatus.CONFIRMED -> Icons.Default.CheckCircle
        TransactionStatus.PENDING -> Icons.Default.Schedule
        TransactionStatus.FAILED -> Icons.Default.ErrorOutline
    }

    val statusColor = when (tx.status) {
        TransactionStatus.CONFIRMED -> SemanticPositive
        TransactionStatus.PENDING -> SemanticWarning
        TransactionStatus.FAILED -> SemanticError
    }

    val dateFormatted = remember(tx.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date(tx.timestamp))
    }

    val amountFormatted = remember(tx.amountSpw) {
        String.format(Locale.US, "%.8f", tx.amountSpw).trimEnd('0').let {
            if (it.endsWith('.')) "${it}00" else it
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfacePrimary)
                .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // ── Header ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Transaction Details",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = typeLabel,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Amount Hero ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceSubtle)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = (if (isIncoming) "+" else "-") + "$amountFormatted ${tx.tokenSymbol}",
                        color = accentColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tx.status.name,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (tx.confirmations > 0) {
                            Text(
                                text = "  •  ${tx.confirmations} confirmation${if (tx.confirmations != 1) "s" else ""}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── TXID Hash ───────────────────────────────────────
            SectionLabel("TRANSACTION HASH")
            CopyableMonoField(
                value = tx.txHash,
                textColor = TextPrimary,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(tx.txHash))
                    Toast.makeText(context, "TXID copied", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))
            FinancialDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // ── Addresses ───────────────────────────────────────
            SectionLabel("FROM")
            CopyableMonoField(
                value = tx.fromAddress,
                textColor = TextSecondary,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(tx.fromAddress))
                    Toast.makeText(context, "From address copied", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SectionLabel("TO")
            CopyableMonoField(
                value = tx.toAddress,
                textColor = TextSecondary,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(tx.toAddress))
                    Toast.makeText(context, "To address copied", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))
            FinancialDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // ── Blockchain Details Grid ─────────────────────────
            SectionLabel("LEDGER METRICS")
            Spacer(modifier = Modifier.height(6.dp))

            DetailGridRow("Amount", "$amountFormatted ${tx.tokenSymbol}")
            DetailGridRow("Feathers", NumberFormat.getNumberInstance(Locale.US).format(tx.amountFeathers) + " feathers")
            DetailGridRow("Mining Fee", "${tx.feeSpw} ${tx.tokenSymbol}")
            DetailGridRow("Timestamp", dateFormatted)

            if (tx.blockNumber > 0L) {
                DetailGridRow("Block Height", "#${NumberFormat.getNumberInstance(Locale.US).format(tx.blockNumber)}")
            }

            if (tx.confirmations > 0) {
                DetailGridRow("Confirmations", "${tx.confirmations}")
            }

            if (tx.nonce > 0L) {
                DetailGridRow("Nonce", "${tx.nonce}")
            }

            if (tx.memo.isNotBlank()) {
                DetailGridRow("Memo", tx.memo)
            }

            if (tx.merkleRoot.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                FinancialDivider()
                Spacer(modifier = Modifier.height(14.dp))

                SectionLabel("MERKLE ROOT")
                CopyableMonoField(
                    value = tx.merkleRoot,
                    textColor = TextSecondary,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(tx.merkleRoot))
                        Toast.makeText(context, "Merkle root copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Action Buttons ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(tx.txHash))
                        Toast.makeText(context, "TXID copied", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy TXID", fontSize = 12.sp)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary,
                        contentColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText)
                }
            }
        }
    }
}

// ── Sub-Components ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun CopyableMonoField(
    value: String,
    textColor: Color,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceSubtle)
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .clickable { onCopy() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = value,
            color = textColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            Icons.Default.ContentCopy,
            contentDescription = "Copy",
            tint = TextMuted,
            modifier = Modifier.size(13.dp)
        )
    }
}

@Composable
private fun DetailGridRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun FinancialDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = DividerColor
    )
}
