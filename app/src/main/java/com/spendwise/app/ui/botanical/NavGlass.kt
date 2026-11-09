package com.spendwise.app.ui.botanical

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView

internal class NavBackdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Unspecified)
}

@Composable
internal fun rememberNavBackdrop(): NavBackdrop? {
    if (Build.VERSION.SDK_INT < 33 || !LocalView.current.isHardwareAccelerated) return null
    val layer = rememberGraphicsLayer()
    return remember(layer) { NavBackdrop(layer) }
}

internal fun Modifier.recordNavBackdrop(backdrop: NavBackdrop?): Modifier {
    if (backdrop == null) return this
    return onGloballyPositioned { backdrop.origin = it.positionInRoot() }
        .drawWithContent {
            backdrop.layer.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop.layer)
        }
}

@Composable
internal fun Modifier.navGlass(backdrop: NavBackdrop?): Modifier {
    val tint = Bot.NavBg
    if (backdrop == null || Build.VERSION.SDK_INT < 33) {
        return drawWithContent {
            drawRect(tint.copy(alpha = .94f))
            drawContent()
        }
    }
    return refractedNavGlass(backdrop, tint)
}

@RequiresApi(33)
@Composable
private fun Modifier.refractedNavGlass(backdrop: NavBackdrop, tint: Color): Modifier {
    val lens = rememberGraphicsLayer()
    val shader = remember { runCatching { RuntimeShader(NAV_LENS_SHADER) }.getOrNull() }
    val effect = remember(shader) {
        shader?.let { RenderEffect.createRuntimeShaderEffect(it, "backdrop").asComposeRenderEffect() }
    }
    var origin by remember { mutableStateOf(Offset.Unspecified) }
    return onGloballyPositioned { origin = it.positionInRoot() }.drawWithContent {
        val hasBackdrop = shader != null && effect != null &&
            origin != Offset.Unspecified && backdrop.origin != Offset.Unspecified &&
            backdrop.layer.size.width > 0 && backdrop.layer.size.height > 0
        if (hasBackdrop && shader != null) {
            val shift = backdrop.origin - origin
            shader.setFloatUniform("extent", size.width, size.height)
            lens.renderEffect = effect
            lens.record {
                translate(shift.x, shift.y) { drawLayer(backdrop.layer) }
            }
            drawLayer(lens)
        }
        drawRect(tint.copy(alpha = if (hasBackdrop) .76f else .94f))
        drawContent()
    }
}

// Sample the actual page layer. The capsule's rim bends samples inward,
// with a small blur in the interior and a specular edge toward the top.
private const val NAV_LENS_SHADER = """
    uniform shader backdrop;
    uniform float2 extent;

    half4 main(float2 p) {
        float r = extent.y * 0.5;
        float2 centered = p - extent * 0.5;
        float axis = max(0.0, extent.x * 0.5 - r);
        float2 rim = centered - float2(clamp(centered.x, -axis, axis), 0.0);
        float radius = length(rim);
        float2 normal = rim / max(1.0, radius);
        float edge = smoothstep(r * 0.48, r, radius);
        float2 q = clamp(p - normal * edge * edge * r * 0.29, float2(2.0), extent - 2.0);
        half4 color = backdrop.eval(q) * 0.40
            + (backdrop.eval(q + float2(2.0, 0.0)) + backdrop.eval(q - float2(2.0, 0.0))
            + backdrop.eval(q + float2(0.0, 2.0)) + backdrop.eval(q - float2(0.0, 2.0))) * 0.15;
        half light = half(smoothstep(r * 0.86, r, radius) * (0.5 - normal.y * 0.45) * 0.22);
        return mix(color, half4(1.0), light);
    }
"""
