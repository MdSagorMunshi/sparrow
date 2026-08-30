package com.ryanshelby.spw.wallet.ui.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.ryanshelby.spw.wallet.ui.theme.AccentPrimary
import com.ryanshelby.spw.wallet.ui.theme.BorderStrong
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.FinanceBackground
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary

/**
 * Institutional High-Contrast QR Code View
 * Renders an optimized matrix with clean financial framing, removing neon/laser distractions.
 */
@Composable
fun GlowingQrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 220.dp,
    showScannerEffect: Boolean = false
) {
    // Generate a high-contrast QR code matrix using ZXing with H error correction
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
            .clip(RoundedCornerShape(20.dp))
            .background(SurfacePrimary)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (matrix != null) {
                val gridSize = matrix.size
                val badgeRadiusFraction = 0.09f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val cellSize = canvasWidth / gridSize.toFloat()

                    val centerX = gridSize / 2f
                    val centerY = gridSize / 2f
                    val badgeRadius = gridSize * badgeRadiusFraction

                    // High-contrast clean white modules on dark background
                    for (r in 0 until gridSize) {
                        for (c in 0 until gridSize) {
                            val dx = c + 0.5f - centerX
                            val dy = r + 0.5f - centerY
                            if (dx * dx + dy * dy < badgeRadius * badgeRadius) continue

                            if (matrix[r][c]) {
                                val topLeft = Offset(c * cellSize + 0.5f, r * cellSize + 0.5f)
                                drawRoundRect(
                                    color = TextPrimary,
                                    topLeft = topLeft,
                                    size = Size(cellSize - 0.8f, cellSize - 0.8f),
                                    cornerRadius = CornerRadius(2.5f, 2.5f)
                                )
                            }
                        }
                    }
                }

                // Clean center SPW badge
                Box(
                    modifier = Modifier
                        .size((sizeDp.value * 0.2f).dp)
                        .clip(CircleShape)
                        .background(FinanceBackground)
                        .border(1.dp, BorderStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SPW",
                        color = AccentPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
