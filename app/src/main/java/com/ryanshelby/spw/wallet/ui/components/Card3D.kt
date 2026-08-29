package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.ui.theme.CyanGlow
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.GreenEmerald
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

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
    val context = LocalContext.current
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    val animatedRotationX by animateFloatAsState(
        targetValue = tiltX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "rotX"
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = tiltY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "rotY"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer {
                rotationX = animatedRotationX
                rotationY = animatedRotationY
                cameraDistance = 16f * density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        tiltY = (tiltY + dragAmount.x * 0.15f).coerceIn(-18f, 18f)
                        tiltX = (tiltX - dragAmount.y * 0.15f).coerceIn(-18f, 18f)
                    },
                    onDragEnd = {
                        tiltX = 0f
                        tiltY = 0f
                    },
                    onDragCancel = {
                        tiltX = 0f
                        tiltY = 0f
                    }
                )
            }
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = CyanNeon.copy(alpha = 0.35f),
                spotColor = PurpleNeon.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F1A34),
                        Color(0xFF13112E),
                        Color(0xFF082236),
                        Color(0xFF070D1D)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CyanNeon.copy(alpha = 0.8f),
                        PurpleNeon.copy(alpha = 0.6f),
                        CyanGlow.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.7f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyanNeon.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.2f),
                        radius = size.width * 0.45f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PurpleNeon.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.85f),
                        radius = size.width * 0.4f
                    )
                )
            }
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Wallet Name & Network Chip & Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(GreenEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = walletName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Network Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x3300E5FF))
                            .border(0.8.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = network.symbol,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (onScanQr != null) {
                        IconButton(
                            onClick = {
                                HapticUtil.lightTap(context)
                                onScanQr()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            HapticUtil.lightTap(context)
                            onShowQr()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Show QR",
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Middle: Balance Area
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SPW Network Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
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
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (hideBalance) {
                    Text(
                        text = "•••••••••• SPW",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•••••• feathers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyanNeon
                    )
                } else {
                    if (isSyncing && totalBalanceSpw == 0.0) {
                        Text(
                            text = "Syncing Node...",
                            style = MaterialTheme.typography.displaySmall,
                            color = CyanNeon.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Fetching on-chain balance…",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            text = if (totalBalanceSpw == 0.0) "0.00000000 SPW" else String.format(Locale.US, "%.8f SPW", totalBalanceSpw).trimEnd('0').let { if (it.endsWith('.')) "${it}00" else it },
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        val formattedFeathers = NumberFormat.getNumberInstance(Locale.US).format(
                            if (totalBalanceFeathers > 0) totalBalanceFeathers else (totalBalanceSpw * SPWCrypto.FEATHERS_PER_SPW).toLong()
                        )
                        Text(
                            text = "$formattedFeathers Feathers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyanNeon,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Bottom Bar: Address Pill & Micro Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Short Address Chip (Clickable Copy)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x33000000))
                        .border(0.6.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .clickable {
                            HapticUtil.performKeyClick(context)
                            onCopyAddress(walletAddress)
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
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Address",
                        tint = CyanNeon,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Node status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(GreenEmerald)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE NODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenEmerald,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
