package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.ryanshelby.spw.wallet.ui.theme.CyanGlow
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.PurpleNeon

@Composable
fun GlowingQrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 220.dp,
    showScannerEffect: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "scan")
    val scanLineY by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanY"
    )

    // Generate a REAL scannable QR code matrix using ZXing
    // Error Correction Level H = 30% recovery, allowing the center SPW badge to safely overlay
    val matrix = remember(data) {
        if (data.isBlank()) null
        else {
            try {
                val hints = mapOf(
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN to 1,
                    EncodeHintType.CHARACTER_SET to "UTF-8"
                )
                val bitMatrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 0, 0, hints)
                val size = bitMatrix.width
                val grid = Array(size) { row ->
                    BooleanArray(size) { col ->
                        bitMatrix.get(col, row)
                    }
                }
                grid
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkBackground)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(CyanNeon, PurpleNeon)),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner white background for perfect QR contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (matrix != null) {
            val gridSize = matrix.size
            // The center badge occupies ~18% of the QR grid (safely within H-level 30% error tolerance)
            val badgeRadiusFraction = 0.09f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val cellSize = canvasWidth / gridSize.toFloat()

                val centerX = gridSize / 2f
                val centerY = gridSize / 2f
                val badgeRadius = gridSize * badgeRadiusFraction

                // Draw QR modules as rounded white rectangles on dark background
                for (r in 0 until gridSize) {
                    for (c in 0 until gridSize) {
                        // Skip modules under the center badge area
                        val dx = c + 0.5f - centerX
                        val dy = r + 0.5f - centerY
                        if (dx * dx + dy * dy < badgeRadius * badgeRadius) continue

                        if (matrix[r][c]) {
                            val topLeft = Offset(c * cellSize, r * cellSize)
                            drawRect(
                                color = DarkBackground,
                                topLeft = topLeft,
                                size = Size(cellSize + 0.5f, cellSize + 0.5f) // Add slight overlap to prevent antialiasing gaps
                            )
                        }
                    }
                }

                // Draw animated scanning beam if requested
                if (showScannerEffect) {
                    val yPos = size.height * scanLineY
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                CyanNeon,
                                Color.White,
                                CyanNeon,
                                Color.Transparent
                            )
                        ),
                        start = Offset(0f, yPos),
                        end = Offset(size.width, yPos),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }

        // Center Sparrow Emblem Badge
        Box(
            modifier = Modifier
                .size((sizeDp.value * 0.22f).dp)
                .clip(CircleShape)
                .background(DarkBackground)
                .border(1.5.dp, CyanGlow, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SPW",
                color = CyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
        } // Close inner Box
    }
}
