package com.ryanshelby.spw.wallet.ui.components

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

/**
 * Full-featured transaction detail dialog showing all blockchain fields:
 * hash, from/to, amount, fee, merkle root, bits, block, confirmations, nonce, time, memo.
 */
@Composable
fun TransactionDetailDialog(
    tx: TransactionItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val isIncoming = tx.type == TransactionType.RECEIVE
    val isStealth = tx.type == TransactionType.STEALTH

    val accentColor = when {
        isStealth -> CyanNeon
        isIncoming -> GreenEmerald
        else -> RedCoral
    }

    val typeLabel = when {
        isStealth -> "Stealth Shielded"
        isIncoming -> "Received"
        else -> "Sent"
    }

    val statusIcon = when (tx.status) {
        TransactionStatus.CONFIRMED -> Icons.Default.CheckCircle
        TransactionStatus.PENDING -> Icons.Default.Schedule
        TransactionStatus.FAILED -> Icons.Default.ErrorOutline
    }

    val statusColor = when (tx.status) {
        TransactionStatus.CONFIRMED -> GreenEmerald
        TransactionStatus.PENDING -> Color(0xFFFFB300)
        TransactionStatus.FAILED -> RedCoral
    }

    val dateFormatted = remember(tx.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date(tx.timestamp))
    }

    val amountFormatted = remember(tx.amountSpw) {
        String.format(Locale.US, "%.8f", tx.amountSpw).trimEnd('0').let {
            if (it.endsWith('.')) "${it}00" else it
        }
    }

    // Animated glow pulse
    val transition = rememberInfiniteTransition(label = "detail_glow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D1117),
                            Color(0xFF0A0F19),
                            Color(0xFF080C14)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = glowAlpha),
                            CyanGlow.copy(alpha = 0.2f),
                            accentColor.copy(alpha = glowAlpha * 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Transaction Details",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = typeLabel,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.08f))
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = (if (isIncoming) "+" else "-") + amountFormatted,
                        color = accentColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = tx.tokenSymbol,
                        color = accentColor.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                                fontSize = 10.sp
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
                accentColor = CyanNeon,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(tx.txHash))
                    Toast.makeText(context, "TXID copied", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))
            CyberDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // ── Addresses ───────────────────────────────────────
            SectionLabel("FROM")
            CopyableMonoField(
                value = tx.fromAddress,
                accentColor = RedCoral.copy(alpha = 0.8f),
                onCopy = {
                    clipboardManager.setText(AnnotatedString(tx.fromAddress))
                    Toast.makeText(context, "From address copied", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SectionLabel("TO")
            CopyableMonoField(
                value = tx.toAddress,
                accentColor = GreenEmerald.copy(alpha = 0.8f),
                onCopy = {
                    clipboardManager.setText(AnnotatedString(tx.toAddress))
                    Toast.makeText(context, "To address copied", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))
            CyberDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // ── Blockchain Details Grid ─────────────────────────
            SectionLabel("BLOCKCHAIN DATA")
            Spacer(modifier = Modifier.height(8.dp))

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

            // ── Merkle Root ─────────────────────────────────────
            if (tx.merkleRoot.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                CyberDivider()
                Spacer(modifier = Modifier.height(14.dp))

                SectionLabel("MERKLE ROOT")
                CopyableMonoField(
                    value = tx.merkleRoot,
                    accentColor = PurpleNeon.copy(alpha = 0.8f),
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(tx.merkleRoot))
                        Toast.makeText(context, "Merkle root copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // ── Bits ────────────────────────────────────────────
            if (tx.bits.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                SectionLabel("BITS (DIFFICULTY TARGET)")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, GlassCardBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = tx.bits,
                        color = Color(0xFFFFB300),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── TX Public Key ───────────────────────────────────
            if (!tx.txPubkey.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                CyberDivider()
                Spacer(modifier = Modifier.height(14.dp))

                SectionLabel("TX PUBLIC KEY")
                CopyableMonoField(
                    value = tx.txPubkey,
                    accentColor = CyanGlow,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(tx.txPubkey))
                        Toast.makeText(context, "TX pubkey copied", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "TXID copied!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy TXID", fontSize = 12.sp)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkBackground)
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
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun CopyableMonoField(
    value: String,
    accentColor: Color,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, GlassCardBorder, RoundedCornerShape(10.dp))
            .clickable { onCopy() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = value,
            color = accentColor,
            fontSize = 10.sp,
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
            modifier = Modifier.size(14.dp)
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
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun CyberDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = GlassCardBorder
    )
}
