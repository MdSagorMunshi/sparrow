package com.ryanshelby.spw.wallet.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * Institutional Physics-Based Motion Tokens
 * Designed to guarantee consistent 60fps frame rendering with zero dropped frames.
 */
object MotionTokens {
    // Tactile spring for buttons, cards, and interactive chips
    val SpringTactile = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Smooth spring for screen navigations and expanding cards
    val SpringSmooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Bouncy spring for verification checkmarks and badge pops
    val SpringBouncy = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessMedium
    )

    // Material 3 Emphasized Decelerate easing for fluid reveals
    val EmphasizedEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
}

/**
 * Modifier providing immediate physics-based tactile scale feedback upon press.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = MotionTokens.SpringTactile,
        label = "bouncyScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Modifier that staggers list items into view sequentially with smooth spring-physics
 * and slight delay offsets to eliminate sudden pops.
 */
fun Modifier.staggeredEntrance(
    index: Int,
    baseDelayMs: Int = 35,
    initialOffsetY: Float = 30f
): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(initialOffsetY) }

    LaunchedEffect(Unit) {
        val delayTime = (index.coerceAtMost(12) * baseDelayMs).toLong()
        if (delayTime > 0) delay(delayTime)
        
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 280, easing = MotionTokens.EaseOutQuart)
        )
    }

    LaunchedEffect(Unit) {
        val delayTime = (index.coerceAtMost(12) * baseDelayMs).toLong()
        if (delayTime > 0) delay(delayTime)

        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = offsetY.value
    }
}
