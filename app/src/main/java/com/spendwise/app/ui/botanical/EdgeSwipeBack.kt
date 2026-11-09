package com.spendwise.app.ui.botanical

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun Modifier.edgeSwipeBack(
    enabled: Boolean,
    page: String?,
    offset: MutableState<Float>,
    scope: CoroutineScope,
    onBack: () -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(page, offset) {
        var settling = false
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (settling || down.position.x > 32.dp.toPx()) return@awaitEachGesture
            val width = size.width.toFloat()
            if (width <= 0f) return@awaitEachGesture
            val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                if (overSlop > 0f) {
                    offset.value = (change.position.x - down.position.x).coerceIn(0f, width)
                    change.consume()
                }
            } ?: return@awaitEachGesture
            val released = horizontalDrag(start.id) { change ->
                offset.value = (offset.value + change.positionChange().x).coerceIn(0f, width)
                change.consume()
            }
            val commit = released && offset.value >= minOf(width * .30f, 128.dp.toPx())
            settling = true
            scope.launch {
                try {
                    Animatable(offset.value).animateTo(if (commit) width else 0f, BotMotion.smooth(.24f)) {
                        offset.value = value
                    }
                    if (commit) onBack()
                } finally {
                    offset.value = 0f
                    settling = false
                }
            }
        }
    }
}
