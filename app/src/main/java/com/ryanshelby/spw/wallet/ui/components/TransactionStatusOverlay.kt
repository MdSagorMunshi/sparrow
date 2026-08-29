package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.R
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.SemanticError
import com.ryanshelby.spw.wallet.ui.theme.SemanticErrorMuted
import com.ryanshelby.spw.wallet.ui.theme.SemanticPositive
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.TextMuted
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import com.ryanshelby.spw.wallet.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Transaction status states for the send-coin flow animation overlay.
 */
enum class TxOverlayState {
    HIDDEN,
    BROADCASTING,
    SUCCESS,
    FAILURE
}

/**
 * Full-screen animated overlay for the financial transaction broadcast flow.
 * Precision engineered with high-trust confirmation visuals.
 */
@Composable
fun TransactionStatusOverlay(
    state: TxOverlayState,
    txHash: String? = null,
    sentAmount: Double = 0.0,
    recipientAddress: String = "",
    errorMessage: String? = null,
    onDismiss: () -> Unit
) {
    if (state == TxOverlayState.HIDDEN) return

    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state) { visible = true }

    LaunchedEffect(state) {
        if (state == TxOverlayState.SUCCESS) {
            HapticUtil.performSuccess(context)
        } else if (state == TxOverlayState.FAILURE) {
            HapticUtil.errorVibrate(context)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FinanceBackground.copy(alpha = 0.97f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            ) {
                when (state) {
                    TxOverlayState.BROADCASTING -> BroadcastingContent()
                    TxOverlayState.SUCCESS -> SuccessContent(
                        txHash = txHash,
                        sentAmount = sentAmount,
                        recipientAddress = recipientAddress,
                        onDismiss = onDismiss
                    )
                    TxOverlayState.FAILURE -> FailureContent(
                        errorMessage = errorMessage ?: "Transaction broadcast failed.",
                        onDismiss = onDismiss
                    )
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun BroadcastingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "broadcastPulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coinScale"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringSpin"
    )

    Box(
        modifier = Modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle spinning progress ring with emerald accent
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { rotationZ = ringRotation }
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        AccentPrimary,
                        BorderSubtle,
                        Color.Transparent
                    )
                ),
                startAngle = -90f,
                sweepAngle = 240f,
                useCenter = false,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "SPW Coin",
            modifier = Modifier
                .size(64.dp)
                .scale(scale),
            tint = Color.Unspecified
        )
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
        text = "Broadcasting Transaction",
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Signing & submitting to SPW consensus network…",
        color = TextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SuccessContent(
    txHash: String?,
    sentAmount: Double,
    recipientAddress: String,
    onDismiss: () -> Unit
) {
    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(900)
        phase = 1
    }

    val morphProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "successMorph"
    )

    val circleSweep by animateFloatAsState(
        targetValue = 360f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "successCircle"
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(90.dp)) {
            drawArc(
                color = SemanticPositive,
                startAngle = -90f,
                sweepAngle = circleSweep,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Text(
            text = "✓",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = SemanticPositive.copy(alpha = (morphProgress * 2f - 0.5f).coerceIn(0f, 1f)),
            modifier = Modifier.scale((morphProgress * 1.2f).coerceIn(0.5f, 1f))
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    AnimatedVisibility(
        visible = phase >= 1,
        enter = fadeIn(tween(350)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Transaction Broadcasted",
                color = SemanticPositive,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sent amount card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfacePrimary)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AMOUNT SENT",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-" + String.format(Locale.US, "%.8f", sentAmount)
                            .trimEnd('0')
                            .let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                        color = SemanticError,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )

                    if (recipientAddress.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "To: ${if (recipientAddress.length > 20) "${recipientAddress.take(8)}...${recipientAddress.takeLast(8)}" else recipientAddress}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (!txHash.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "TXID: ${if (txHash.length > 22) "${txHash.take(10)}...${txHash.takeLast(10)}" else txHash}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimary,
                    contentColor = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = com.ryanshelby.spw.wallet.ui.theme.ButtonPrimaryText)
            }
        }
    }
}

@Composable
private fun FailureContent(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    val shakeOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = keyframes {
            durationMillis = 450
            0f at 0
            -14f at 60
            14f at 120
            -10f at 180
            10f at 240
            -6f at 300
            6f at 360
            0f at 450
        },
        label = "errorShake"
    )

    Box(
        modifier = Modifier
            .size(110.dp)
            .graphicsLayer { translationX = shakeOffset },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(90.dp)) {
            drawArc(
                color = SemanticError,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Text(
            text = "✕",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = SemanticError
        )
    }

    Spacer(modifier = Modifier.height(22.dp))

    Text(
        text = "Transaction Failed",
        color = SemanticError,
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(14.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SemanticErrorMuted)
            .border(1.dp, SemanticError.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = errorMessage,
            color = TextPrimary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            lineHeight = 18.sp
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceElevated,
            contentColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text("Dismiss", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}
