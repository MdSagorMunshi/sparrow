package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.ryanshelby.spw.wallet.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Transaction status states for the send-coin flow animation overlay.
 */
enum class TxOverlayState {
    HIDDEN,
    BROADCASTING,   // Pending — coin pulsing + progress ring
    SUCCESS,         // Checkmark morph + particle burst
    FAILURE          // X morph + shake + red pulse
}

/**
 * Full-screen animated overlay for the send-coin transaction flow.
 *
 * Shows:
 * - BROADCASTING: SPW coin icon with soft pulse + thin progress ring
 * - SUCCESS: Coin morphs to checkmark, radial particle burst, details slide in
 * - FAILURE: Coin morphs to X, horizontal shake, red glow, error text
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

    // ── Entry animation ─────────────────────────────────────
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state) { visible = true }

    // ── Fire haptics on result ──────────────────────────────
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
                .background(DarkBackground.copy(alpha = 0.96f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
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

// ══════════════════════════════════════════════════════════════
//  BROADCASTING — coin pulse + progress ring
// ══════════════════════════════════════════════════════════════
@Composable
private fun BroadcastingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Soft scale pulse 0.92 -> 1.08
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coinPulse"
    )

    // Glow alpha pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Progress ring sweep angle (loops from 0 to 360)
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progressSweep"
    )

    // Ring start rotation
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow circle
        Canvas(modifier = Modifier.size(140.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CyanNeon.copy(alpha = glowAlpha),
                        PurpleNeon.copy(alpha = glowAlpha * 0.5f),
                        Color.Transparent
                    ),
                    radius = size.minDimension / 2
                )
            )
        }

        // Progress ring
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { rotationZ = ringRotation }
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        CyanNeon.copy(alpha = 0.8f),
                        PurpleNeon.copy(alpha = 0.6f),
                        CyanNeon.copy(alpha = 0.1f)
                    )
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle * 0.7f, // never quite fills completely
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // SPW Coin Icon (real app asset)
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "SPW Coin",
            modifier = Modifier
                .size(72.dp)
                .scale(scale),
            tint = Color.Unspecified
        )
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
        text = "Broadcasting Transaction",
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Signing & submitting to SPW mempool…",
        color = TextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
}

// ══════════════════════════════════════════════════════════════
//  SUCCESS — checkmark morph + particles + details slide-in
// ══════════════════════════════════════════════════════════════
@Composable
private fun SuccessContent(
    txHash: String?,
    sentAmount: Double,
    recipientAddress: String,
    onDismiss: () -> Unit
) {
    // Phase control — icon morphs then details slide in
    var phase by remember { mutableIntStateOf(0) } // 0 = morph, 1 = show details
    LaunchedEffect(Unit) {
        delay(1200) // checkmark morph + particles play out
        phase = 1
    }

    // Icon morph: starts as coin scale -> 0, checkmark scale 0 -> 1
    val morphProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "morph"
    )

    // Circle stroke draw-in
    val circleSweep by animateFloatAsState(
        targetValue = 360f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "circleStroke"
    )

    // Particle burst
    val particleAlpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(1000, delayMillis = 300, easing = LinearEasing),
        label = "particleFade"
    )

    val particleScale by animateFloatAsState(
        targetValue = 2.5f,
        animationSpec = tween(1000, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "particleScale"
    )

    // Generate consistent particles
    val particles = remember {
        List(16) {
            val angle = (it * 22.5f) + Random.nextFloat() * 10f
            val speed = 0.6f + Random.nextFloat() * 0.4f
            val size = 3f + Random.nextFloat() * 4f
            val colorIndex = it % 3
            Triple(angle, speed, Pair(size, colorIndex))
        }
    }

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Particle burst canvas
        Canvas(
            modifier = Modifier
                .size(140.dp)
                .scale(particleScale)
                .alpha(1f - morphProgress * 0.3f + particleAlpha)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val baseRadius = size.minDimension * 0.25f

            particles.forEach { (angle, speed, sizeAndColor) ->
                val (dotSize, colorIndex) = sizeAndColor
                val radians = Math.toRadians(angle.toDouble())
                val distance = baseRadius * speed * particleScale
                val x = centerX + (cos(radians) * distance).toFloat()
                val y = centerY + (sin(radians) * distance).toFloat()

                val color = when (colorIndex) {
                    0 -> CyanNeon
                    1 -> PurpleNeon
                    else -> GreenEmerald
                }.copy(alpha = (1f - particleAlpha.coerceIn(0f, 1f)).coerceIn(0f, 0.9f))

                drawCircle(
                    color = color,
                    radius = dotSize,
                    center = Offset(x, y)
                )
            }
        }

        // Circle stroke around checkmark
        Canvas(modifier = Modifier.size(100.dp)) {
            drawArc(
                color = GreenEmerald,
                startAngle = -90f,
                sweepAngle = circleSweep,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Morph: Coin fades out as checkmark fades in
        // Coin icon (fading out)
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .alpha((1f - morphProgress * 1.5f).coerceIn(0f, 1f))
                .scale(1f - morphProgress * 0.3f),
            tint = Color.Unspecified
        )

        // Checkmark (fading in)
        Text(
            text = "✓",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = GreenEmerald.copy(alpha = (morphProgress * 2f - 0.5f).coerceIn(0f, 1f)),
            modifier = Modifier
                .scale((morphProgress * 1.2f).coerceIn(0.5f, 1f))
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Details slide in after morph completes
    AnimatedVisibility(
        visible = phase >= 1,
        enter = fadeIn(tween(400)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Transfer Successful",
                color = GreenEmerald,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sent amount
            Text(
                text = String.format(Locale.US, "%.8f", sentAmount)
                    .trimEnd('0')
                    .let { if (it.endsWith('.')) "${it}00" else it } + " SPW",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Recipient
            if (recipientAddress.isNotBlank()) {
                Text(
                    text = "To",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = recipientAddress,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // TX Hash
            if (!txHash.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "TXID: $txHash",
                        color = CyanNeon,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenEmerald,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Done", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
//  FAILURE — X morph + shake + red pulse + error message
// ══════════════════════════════════════════════════════════════
@Composable
private fun FailureContent(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    // Horizontal shake
    val shakeOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = keyframes {
            durationMillis = 500
            0f at 0
            -18f at 60
            18f at 120
            -14f at 180
            14f at 240
            -8f at 300
            8f at 360
            -4f at 420
            0f at 500
        },
        label = "shake"
    )

    // Red glow pulse
    val redGlowAlpha by animateFloatAsState(
        targetValue = 0.1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "redGlow"
    )
    val redGlowInitial by animateFloatAsState(
        targetValue = 0f,
        animationSpec = keyframes {
            durationMillis = 800
            0.6f at 100
            0.35f at 400
            0.1f at 800
        },
        label = "redGlowFlash"
    )

    // Morph progress
    val morphProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "failMorph"
    )

    // Details visibility
    var showDetails by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(700)
        showDetails = true
    }

    Box(
        modifier = Modifier
            .size(140.dp)
            .graphicsLayer { translationX = shakeOffset },
        contentAlignment = Alignment.Center
    ) {
        // Red glow background
        Canvas(modifier = Modifier.size(140.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RedCoral.copy(alpha = redGlowInitial.coerceAtLeast(redGlowAlpha)),
                        Color.Transparent
                    ),
                    radius = size.minDimension / 2
                )
            )
        }

        // Circle stroke (red)
        Canvas(modifier = Modifier.size(100.dp)) {
            drawArc(
                color = RedCoral,
                startAngle = -90f,
                sweepAngle = morphProgress * 360f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Coin fading out
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .alpha((1f - morphProgress * 1.5f).coerceIn(0f, 1f))
                .scale(1f - morphProgress * 0.3f),
            tint = Color.Unspecified
        )

        // X mark fading in
        Text(
            text = "✕",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = RedCoral.copy(alpha = (morphProgress * 2f - 0.5f).coerceIn(0f, 1f)),
            modifier = Modifier.scale((morphProgress * 1.2f).coerceIn(0.5f, 1f))
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    AnimatedVisibility(
        visible = showDetails,
        enter = fadeIn(tween(400)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Transaction Failed",
                color = RedCoral,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Error reason box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RedCoral.copy(alpha = 0.08f))
                    .padding(14.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = RedCoral.copy(alpha = 0.9f),
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
                    containerColor = DarkSurfaceElevated,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Dismiss", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
