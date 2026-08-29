package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary
import com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.SurfaceSubtle
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * High-precision cryptographic scanning visual card with animated radar pulse waves,
 * sweeping laser beam, and live blockchain scan phase tickers.
 */
@Composable
fun StealthScanStatusCard(
    isScanning: Boolean,
    statusMessage: String?,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StealthScanInfinite")

    // Continuous rotation for scanning icon
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanRotation"
    )

    // Sweeping laser animation (0f -> 1f -> 0f)
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserProgress"
    )

    // Sonar pulse waves expansion (0f to 1f)
    val sonarPulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarPulse1"
    )

    val sonarPulse2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarPulse2"
    )

    // Animated dots for progress indicator
    var dotCount by remember { mutableIntStateOf(1) }
    LaunchedEffect(isScanning) {
        while (isScanning) {
            delay(350)
            dotCount = (dotCount % 3) + 1
        }
    }

    // Dynamic phase text while scanning
    var phaseIndex by remember { mutableIntStateOf(0) }
    val phases = listOf(
        "Connecting to SPW validator node...",
        "Deriving Diffie-Hellman view keys (ECDH)...",
        "Scanning unspent outputs for stealth tags...",
        "Verifying one-time output commitments..."
    )

    LaunchedEffect(isScanning) {
        while (isScanning) {
            delay(1200)
            phaseIndex = (phaseIndex + 1) % phases.size
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Trigger Button
        Button(
            onClick = {
                if (!isScanning) {
                    onScanClick()
                }
            },
            enabled = !isScanning,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScanning) SurfaceElevated else ButtonPrimary,
                contentColor = if (isScanning) TextPrimary else ButtonPrimaryText,
                disabledContainerColor = SurfaceElevated,
                disabledContentColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isScanning) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Scanning",
                        tint = AccentPrimary,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(rotationAngle)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Scanning Blockchain" + ".".repeat(dotCount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan",
                        tint = ButtonPrimaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan Stealth Outputs",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ButtonPrimaryText
                    )
                }
            }
        }

        // Active Scanning Radar & Laser Panel
        AnimatedVisibility(
            visible = isScanning,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfacePrimary)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sonar Radar Node
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulse Ring 1
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = (size.minDimension / 2f) * (sonarPulse1 % 1f)
                            val alpha = (1f - (sonarPulse1 % 1f)).coerceIn(0f, 1f) * 0.7f
                            drawCircle(
                                color = AccentPrimary.copy(alpha = alpha),
                                radius = radius,
                                center = Offset(size.width / 2, size.height / 2)
                            )
                        }

                        // Pulse Ring 2
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val pulse = sonarPulse2 % 1f
                            val radius = (size.minDimension / 2f) * pulse
                            val alpha = (1f - pulse).coerceIn(0f, 1f) * 0.7f
                            drawCircle(
                                color = AccentPrimary.copy(alpha = alpha),
                                radius = radius,
                                center = Offset(size.width / 2, size.height / 2)
                            )
                        }

                        // Core Node
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(SurfaceElevated)
                                .border(1.dp, AccentPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CRYPTOGRAPHIC SCANNING ACTIVE",
                                color = AccentPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "LIVE",
                                color = AccentPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = phases[phaseIndex],
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sweeping Laser Beam Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SurfaceSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.35f)
                            .align(Alignment.CenterStart)
                            .offset(x = (laserProgress * 220).dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        AccentPrimary,
                                        Color.White,
                                        AccentPrimary,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }

        // Result Status Badge
        if (!isScanning && statusMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            val isSuccess = statusMessage.contains("Found", ignoreCase = true)
            val isError = statusMessage.contains("error", ignoreCase = true)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isSuccess -> AccentPrimary.copy(alpha = 0.12f)
                            isError -> SemanticError.copy(alpha = 0.12f)
                            else -> SurfaceSubtle
                        }
                    )
                    .border(
                        0.8.dp,
                        when {
                            isSuccess -> AccentPrimary.copy(alpha = 0.5f)
                            isError -> SemanticError.copy(alpha = 0.5f)
                            else -> BorderSubtle
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        isSuccess -> Icons.Default.CheckCircle
                        isError -> Icons.Default.ErrorOutline
                        else -> Icons.Default.Shield
                    },
                    contentDescription = null,
                    tint = when {
                        isSuccess -> SemanticPositive
                        isError -> SemanticError
                        else -> TextSecondary
                    },
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusMessage,
                    color = when {
                        isSuccess -> SemanticPositive
                        isError -> SemanticError
                        else -> TextPrimary
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
