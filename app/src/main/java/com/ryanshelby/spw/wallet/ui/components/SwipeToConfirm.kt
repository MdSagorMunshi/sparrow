package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.spw.wallet.security.HapticUtil
import com.ryanshelby.spw.wallet.ui.theme.CyanNeon
import com.ryanshelby.spw.wallet.ui.theme.DarkBackground
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToConfirm(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var width by remember { mutableFloatStateOf(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isConfirmed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val density = LocalDensity.current
    val thumbSize = 56.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(SurfaceElevated)
            .onSizeChanged { width = it.width.toFloat() },
        contentAlignment = Alignment.CenterStart
    ) {
        // Background Text
        Text(
            text = if (isConfirmed) "Confirmed" else "Swipe to Pay",
            color = if (isConfirmed) CyanNeon else TextPrimary.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        // Progress Fill
        if (dragOffset > 0f && !isConfirmed) {
            Box(
                modifier = Modifier
                    .width(with(density) { (dragOffset + thumbSizePx / 2).toDp() })
                    .fillMaxHeight()
                    .background(CyanNeon.copy(alpha = 0.2f))
            )
        }

        // Draggable Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffset.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(if (isConfirmed) CyanNeon else DarkBackground)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (!isConfirmed) {
                            val newOffset = dragOffset + delta
                            val maxOffset = width - thumbSizePx
                            dragOffset = newOffset.coerceIn(0f, maxOffset)
                        }
                    },
                    onDragStopped = {
                        if (isConfirmed) return@draggable
                        val maxOffset = width - thumbSizePx
                        if (dragOffset > maxOffset * 0.8f) { // 80% threshold
                            dragOffset = maxOffset
                            isConfirmed = true
                            HapticUtil.performSuccess(context)
                            onConfirm()
                        } else {
                            // Snap back
                            scope.launch {
                                Animatable(dragOffset).animateTo(0f) {
                                    dragOffset = value
                                }
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isConfirmed) {
                Icon(Icons.Default.Check, contentDescription = null, tint = DarkBackground)
            } else {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = CyanNeon)
            }
        }
    }
}
