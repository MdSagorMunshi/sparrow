package com.ryanshelby.spw.wallet.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.ryanshelby.spw.wallet.ui.theme.BorderSubtle
import com.ryanshelby.spw.wallet.ui.theme.ShimmerBase
import com.ryanshelby.spw.wallet.ui.theme.ShimmerHighlight
import com.ryanshelby.spw.wallet.ui.theme.SurfaceElevated
import com.ryanshelby.spw.wallet.ui.theme.SurfacePrimary

/**
 * Premium Shimmer Placeholder Modifier
 * High-performance slate-to-charcoal shimmer effect with zero dropped frames.
 */
fun Modifier.shimmerPlaceholder(
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = ShimmerBase,
    highlightColor: Color = ShimmerHighlight
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 500f, translateAnim + 500f)
    )

    this
        .clip(shape)
        .background(brush = shimmerBrush, shape = shape)
}

/**
 * Skeleton placeholder matching the exact layout of TransactionRowCard.
 */
@Composable
fun TransactionRowSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfacePrimary)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Circular icon skeleton
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .shimmerPlaceholder(CircleShape, baseColor = SurfaceElevated)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    // Type title skeleton
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(14.dp)
                            .shimmerPlaceholder(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // TX Hash skeleton
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(10.dp)
                            .shimmerPlaceholder(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Timestamp skeleton
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(9.dp)
                            .shimmerPlaceholder(RoundedCornerShape(4.dp))
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                // Amount skeleton
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(14.dp)
                        .shimmerPlaceholder(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Status pill skeleton
                Box(
                    modifier = Modifier
                        .width(55.dp)
                        .height(12.dp)
                        .shimmerPlaceholder(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

/**
 * Skeleton placeholder matching the exact layout of the portfolio balance card.
 */
@Composable
fun PortfolioCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfacePrimary)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(16.dp)
                        .shimmerPlaceholder(RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                        .shimmerPlaceholder(RoundedCornerShape(10.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(12.dp)
                        .shimmerPlaceholder(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(30.dp)
                        .shimmerPlaceholder(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .shimmerPlaceholder(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(24.dp)
                        .shimmerPlaceholder(RoundedCornerShape(12.dp))
                )
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(14.dp)
                        .shimmerPlaceholder(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

/**
 * Skeleton placeholder matching the asset row.
 */
@Composable
fun AssetRowSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfacePrimary)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .shimmerPlaceholder(CircleShape, baseColor = SurfaceElevated)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(16.dp)
                            .shimmerPlaceholder(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(12.dp)
                            .shimmerPlaceholder(RoundedCornerShape(4.dp))
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .shimmerPlaceholder(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .shimmerPlaceholder(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
