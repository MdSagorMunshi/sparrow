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
    sizeDp: Dp = 260.dp,
    showCenterBadge: Boolean = false
) {
    // Generate a high-contrast QR code matrix using ZXing with M error correction for optimal module size & readability
    val matrix = remember(data) {
        if (data.isBlank()) null
        else {
            try {
                val hints = mapOf(
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
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
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (matrix != null) {
                val gridSize = matrix.size

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val cellSize = canvasWidth / gridSize.toFloat()

                    // High-contrast clean modules on dark background
                    for (r in 0 until gridSize) {
                        for (c in 0 until gridSize) {
                            if (matrix[r][c]) {
                                val topLeft = Offset(c * cellSize, r * cellSize)
                                drawRoundRect(
                                    color = TextPrimary,
                                    topLeft = topLeft,
                                    size = Size(cellSize, cellSize),
                                    cornerRadius = CornerRadius(1.5f, 1.5f)
                                )
                            }
                        }
                    }
                }

                if (showCenterBadge) {
                    Box(
                        modifier = Modifier
                            .size((sizeDp.value * 0.14f).dp)
                            .clip(CircleShape)
                            .background(FinanceBackground)
                            .border(1.dp, BorderStrong, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SPW",
                            color = AccentPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
