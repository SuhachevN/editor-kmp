package com.comixon.editor.rendering

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comixon.editor.compose.style
import com.comixon.editor.compose.weight
import com.comixon.editor.core.EditorLetterLayer
import com.comixon.editor.core.LetteringFamily
import kotlin.math.min
import kotlin.math.sin

internal fun prepareLettering(
    layer: EditorLetterLayer,
    family: FontFamily,
    measurer: TextMeasurer,
): LayerScene.Letter {
    val spec = letteringStyle(layer.type, layer.letterIndex)
    val parts = layer.text.unicodeGlyphs()
    val textStyle =
        TextStyle(
            fontFamily = family,
            fontSize =
                (
                    (22f + layer.fontSizeName.ordinal * 8f) *
                        BubbleReferenceMetrics.POINT_TO_EXPORT
                ).sp,
            fontWeight = layer.font.weight,
            fontStyle = layer.font.style,
        )
    val layouts = parts.map { measurer.measure(AnnotatedString(it), textStyle, softWrap = false) }
    val naturalWidth = layouts.sumOf { it.size.width }.coerceAtLeast(1)
    val naturalHeight = layouts.maxOfOrNull { it.size.height }?.toFloat()?.coerceAtLeast(1f) ?: 1f
    val factor =
        min(
            1f,
            min(
                layer.bounds.width.toFloat() / naturalWidth,
                layer.bounds.height.toFloat() / (naturalHeight + spec.curve.amplitude * BubbleGeometry.EXPORT_SCALE),
            ),
        )
    var x = (layer.bounds.width.toFloat() - naturalWidth * factor) / 2f
    val glyphs =
        layouts.mapIndexed { index, layout ->
            val fraction = if (layouts.size < 2) .5f else index.toFloat() / (layouts.size - 1)
            val position =
                Offset(
                    x,
                    (layer.bounds.height.toFloat() - layout.size.height * factor) / 2f +
                        spec.offsetAt(fraction) * BubbleGeometry.EXPORT_SCALE * factor,
                )
            x += layout.size.width * factor
            Glyph(layout, position, spec.scaleAt(fraction))
        }
    return LayerScene.Letter(layer, glyphs, spec, factor)
}

internal fun DrawScope.drawLettering(scene: LayerScene.Letter) {
    val style = scene.style
    scene.glyphs.forEach { glyph ->
        withTransform({
            translate(glyph.position.x, glyph.position.y)
            scale(scene.scale * glyph.scale, scene.scale * glyph.scale, Offset.Zero)
        }) {
            val offset = Offset(style.shadowOffsetX, style.shadowOffsetY) * BubbleGeometry.EXPORT_SCALE
            drawText(
                glyph.layout,
                color = Color.Black,
                topLeft = offset,
                drawStyle = Stroke(style.strokeWidth.value * BubbleGeometry.EXPORT_SCALE),
            )
            drawText(glyph.layout, color = Color.White)
        }
    }
}

private fun String.unicodeGlyphs(): List<String> {
    val result = mutableListOf<String>()
    var index = 0
    while (index < length) {
        val size = if (this[index].isHighSurrogate() && index + 1 < length && this[index + 1].isLowSurrogate()) 2 else 1
        result += substring(index, index + size)
        index += size
    }
    return result
}

private fun letteringStyle(
    type: LetteringFamily,
    index: Int,
): LetteringStyleSpec =
    when (type) {
        LetteringFamily.STAIRS ->
            when (index.floorMod(4)) {
                0 ->
                    LetteringStyleSpec(
                        strokeWidth = 2.7.dp,
                        curve = LetteringCurve(startY = 0.3f, controlY = 0.15f, endY = 0f, centerY = 0.15f),
                        shadowOffsetX = -1.2f,
                        shadowOffsetY = -1.2f,
                    )
                1 ->
                    LetteringStyleSpec(
                        strokeWidth = 2.7.dp,
                        curve = LetteringCurve(startY = 0f, controlY = 0.15f, endY = 0.3f, centerY = 0.15f),
                        shadowOffsetX = 0.3f,
                        shadowOffsetY = 1.7f,
                    )
                2 ->
                    LetteringStyleSpec(
                        strokeWidth = 2.7.dp,
                        curve = LetteringCurve(startY = 0f, controlY = 0.3f, endY = 0f, centerY = 0.15f),
                        shadowOffsetX = 0.3f,
                        shadowOffsetY = 1.7f,
                    )
                else ->
                    LetteringStyleSpec(
                        strokeWidth = 2.7.dp,
                        curve = LetteringCurve(startY = 0.3f, controlY = 0f, endY = 0.3f, centerY = 0.15f),
                        shadowOffsetX = -1.2f,
                        shadowOffsetY = -1.2f,
                    )
            }
        LetteringFamily.BRIDGE ->
            when (index.floorMod(5)) {
                0 ->
                    LetteringStyleSpec(
                        strokeWidth = 2.dp,
                        curve = LetteringCurve(startY = 0f, controlY = 0f, endY = 0f, centerY = 0f),
                        ovalSized = true,
                    )
                1 ->
                    LetteringStyleSpec(
                        strokeWidth = 2.dp,
                        curve = LetteringCurve(startY = 0f, controlY = -0.5f, endY = 0f, centerY = 0f, amplitude = 28f),
                        shadowOffsetY = -1f,
                    )
                2 ->
                    LetteringStyleSpec(
                        strokeWidth = 2.dp,
                        curve = LetteringCurve(startY = 0f, controlY = 0.5f, endY = 0f, centerY = 0f, amplitude = 28f),
                        shadowOffsetY = 1f,
                    )
                3 ->
                    LetteringStyleSpec(
                        strokeWidth = 2.7.dp,
                        curve = LetteringCurve(startY = 0f, controlY = -0.5f, endY = 0f, centerY = 0f, amplitude = 28f),
                        shadowOffsetX = 1.2f,
                        shadowOffsetY = 1.2f,
                    )
                else ->
                    LetteringStyleSpec(
                        strokeWidth = 2.7.dp,
                        curve = LetteringCurve(startY = 0f, controlY = 0.5f, endY = 0f, centerY = 0f, amplitude = 28f),
                        shadowOffsetX = -1.2f,
                        shadowOffsetY = 1.2f,
                    )
            }
        else ->
            when (index.floorMod(5)) {
                1 ->
                    LetteringStyleSpec(
                        strokeWidth = 2.7.dp,
                        curve = LetteringCurve.Flat,
                        shadowOffsetX = -1.2f,
                        shadowOffsetY = -1.2f,
                    )
                2 ->
                    LetteringStyleSpec(
                        strokeWidth = 5.dp,
                        curve = LetteringCurve.Flat,
                        shadowOffsetY = -3f,
                    )
                3 ->
                    LetteringStyleSpec(
                        strokeWidth = 5.dp,
                        curve = LetteringCurve.Flat,
                        shadowOffsetY = 3f,
                    )
                4 ->
                    LetteringStyleSpec(
                        strokeWidth = 7.dp,
                        curve = LetteringCurve.Flat,
                    )
                else ->
                    LetteringStyleSpec(
                        strokeWidth = 2.7.dp,
                        curve = LetteringCurve.Flat,
                        shadowOffsetX = 0.3f,
                        shadowOffsetY = 1.7f,
                    )
            }
    }

@Immutable
internal data class LetteringStyleSpec(
    val strokeWidth: Dp,
    val curve: LetteringCurve,
    val rotation: Float = 0f,
    val ovalSized: Boolean = false,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 0f,
) {
    fun offsetAt(fraction: Float): Float {
        val inverse = 1f - fraction
        val y =
            inverse * inverse * curve.startY +
                2f * inverse * fraction * curve.controlY +
                fraction * fraction * curve.endY
        return (y - curve.centerY) * curve.amplitude
    }

    fun scaleAt(fraction: Float): Float {
        if (!ovalSized) return 1f
        return 0.72f + sin(fraction * kotlin.math.PI).toFloat() * 0.34f
    }
}

@Immutable
internal data class LetteringCurve(
    val startY: Float,
    val controlY: Float,
    val endY: Float,
    val centerY: Float,
    val amplitude: Float = 60f,
) {
    companion object {
        val Flat = LetteringCurve(startY = 0.5f, controlY = 0.5f, endY = 0.5f, centerY = 0.5f)
    }
}

private fun Int.floorMod(mod: Int): Int = ((this % mod) + mod) % mod
