package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryanshelby.spw.wallet.security.SPWCrypto
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import java.security.MessageDigest
import kotlin.math.abs

/**
 * Deterministic Financial Blockie / Identicon generator.
 * Creates an institutional 5x5 symmetrical visual avatar unique to any SPW address or pubkey.
 */
@Composable
fun Identicon(
    address: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    shape: Shape = CircleShape
) {
    val cleanAddress = address.trim()
    val visualHash = remember(cleanAddress) {
        if (cleanAddress.isEmpty()) {
            ByteArray(16) { 0 }
        } else {
            try {
                MessageDigest.getInstance("SHA-256").digest(cleanAddress.toByteArray(Charsets.UTF_8))
            } catch (e: Exception) {
                ByteArray(16) { (cleanAddress.hashCode() shr (it * 2)).toByte() }
            }
        }
    }

    // Derive deterministic colors from hash bytes
    val bgColor = remember(visualHash) {
        val r = 18 + (abs(visualHash[0].toInt()) % 20)
        val g = 22 + (abs(visualHash[1].toInt()) % 22)
        val b = 30 + (abs(visualHash[2].toInt()) % 26)
        Color(r, g, b)
    }

    val primaryColor = remember(visualHash) {
        // High-contrast, vibrant but refined color palette
        val hue = (abs(visualHash[3].toInt() shl 8 or abs(visualHash[4].toInt())) % 360).toFloat()
        hslToColor(hue, 0.65f, 0.60f)
    }

    val secondaryColor = remember(visualHash) {
        val hue = ((abs(visualHash[3].toInt() shl 8 or abs(visualHash[4].toInt())) + 60) % 360).toFloat()
        hslToColor(hue, 0.50f, 0.45f)
    }

    // 5x5 grid with horizontal symmetry (columns 0..2 mirrored to 4..3)
    val grid = remember(visualHash) {
        val matrix = Array(5) { BooleanArray(5) }
        var byteIdx = 5
        for (r in 0 until 5) {
            for (c in 0 until 3) {
                val byteVal = abs(visualHash[byteIdx % visualHash.size].toInt())
                val fill = (byteVal and (1 shl (c + 1))) != 0
                matrix[r][c] = fill
                matrix[r][4 - c] = fill // horizontal mirror
                byteIdx++
            }
        }
        matrix
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgColor)
            .border(0.8.dp, BorderSubtle, shape)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val cellWidth = this.size.width / 5f
            val cellHeight = this.size.height / 5f

            for (r in 0 until 5) {
                for (c in 0 until 5) {
                    if (grid[r][c]) {
                        val color = if ((r + c) % 2 == 0) primaryColor else secondaryColor
                        drawRect(
                            color = color,
                            topLeft = Offset(c * cellWidth, r * cellHeight),
                            size = Size(cellWidth, cellHeight)
                        )
                    }
                }
            }
        }
    }
}

/**
 * HSL to Compose Color conversion helper.
 */
private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f

    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f)
    )
}
