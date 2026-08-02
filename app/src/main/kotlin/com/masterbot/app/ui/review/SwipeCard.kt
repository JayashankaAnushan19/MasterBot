package com.masterbot.app.ui.review

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val SWIPE_THRESHOLD_PX = 350f

/**
 * A single tinder-style swipeable card. Drag right = correct, drag left = incorrect;
 * releasing past [SWIPE_THRESHOLD_PX] commits the answer and flings the card off,
 * otherwise it springs back to center.
 */
@Composable
fun SwipeCard(
    onSwipedRight: () -> Unit,
    onSwipedLeft: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (offsetX: Float) -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = offsetX.value
                rotationZ = (offsetX.value / 40f).coerceIn(-12f, 12f)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val current = offsetX.value
                        scope.launch {
                            when {
                                current > SWIPE_THRESHOLD_PX -> {
                                    offsetX.animateTo(2000f)
                                    onSwipedRight()
                                    offsetX.snapTo(0f)
                                }
                                current < -SWIPE_THRESHOLD_PX -> {
                                    offsetX.animateTo(-2000f)
                                    onSwipedLeft()
                                    offsetX.snapTo(0f)
                                }
                                else -> offsetX.animateTo(0f)
                            }
                        }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                }
            },
    ) {
        content(offsetX.value)
    }
}

/** Green/red tint that fades in as the card is dragged toward correct/incorrect. */
@Composable
fun SwipeHintOverlay(offsetX: Float, modifier: Modifier = Modifier) {
    val alpha = (kotlin.math.abs(offsetX) / SWIPE_THRESHOLD_PX).coerceIn(0f, 1f) * 0.35f
    val color = if (offsetX > 0) Color(0xFF00E5A0) else Color(0xFFFF5470)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(color.copy(alpha = alpha)),
    )
}
