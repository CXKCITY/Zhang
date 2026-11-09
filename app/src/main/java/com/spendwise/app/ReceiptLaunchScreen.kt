package com.spendwise.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.spendwise.app.ui.botanical.LedgerAppearance

@Composable
internal fun ReceiptLaunchScreen(start: Boolean, onFinished: () -> Unit) {
    val paper = Color(0xFFF8FAFC)
    val charcoal = Color(0xFF111827)
    val background = if (LedgerAppearance.dark) charcoal else paper
    val ink = if (LedgerAppearance.dark) paper else charcoal
    val bodyData = stringResource(R.string.receipt_icon_body)
    val checkData = stringResource(R.string.receipt_icon_check)
    val body = remember(bodyData) {
        requireNotNull(PathParser.createPathFromPathData(bodyData)).asComposePath().apply {
            fillType = PathFillType.EvenOdd
        }
    }
    val check = remember(checkData) {
        PathMeasure().apply {
            setPath(requireNotNull(PathParser.createPathFromPathData(checkData)).asComposePath(), false)
        }
    }
    val checkSegment = remember { Path() }
    val time = remember { Animatable(0f) }
    LaunchedEffect(start) {
        if (start) {
            time.animateTo(510f, tween(510, easing = LinearEasing))
            onFinished()
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val elapsed = time.value
        val entrance = FastOutSlowInEasing.transform((elapsed / 190f).coerceIn(0f, 1f))
        val progress = FastOutSlowInEasing.transform(((elapsed - 105f) / 235f).coerceIn(0f, 1f))
        val exit = FastOutSlowInEasing.transform(((elapsed - 375f) / 135f).coerceIn(0f, 1f))
        drawRect(background, alpha = 1f - exit)

        // Center the combined silhouette of the receipt and the check.
        val unit = 246.dp.toPx() / 108f
        val markScale = unit * (0.76f + 0.24f * entrance - 0.06f * exit)
        translate(center.x - 53.9f * markScale, center.y - 50.2f * markScale) {
            scale(markScale, markScale, pivot = Offset.Zero) {
                drawPath(body, ink, alpha = entrance * (1f - exit))
                checkSegment.reset()
                if (check.getSegment(0f, progress * check.length, checkSegment)) {
                    drawPath(
                        path = checkSegment,
                        color = ink,
                        alpha = 1f - exit,
                        style = Stroke(width = 5.1f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}
