package com.comixon.editor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathHitTester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.comixon.editor.core.BubbleFamily
import com.comixon.editor.core.FontSize
import kotlin.math.min

/** Uses the shared text shaper and style paths in logical export pixels on every platform. */
internal object BubbleTextLayout {
    data class Line(
        val layout: TextLayoutResult,
        val position: Offset,
    )

    data class Result(
        val lines: List<Line>,
        val fits: Boolean,
    )

    fun strokeWidth(size: FontSize): Float = 1.5f + (BubbleReferenceMetrics.fixedPoints(size.points) - 12f) / 24f * .7f

    fun create(
        type: BubbleFamily,
        index: Int,
        text: String,
        requested: FontSize,
        width: Float,
        height: Float,
        family: FontFamily,
        weight: FontWeight,
        fontStyle: FontStyle,
        measurer: TextMeasurer,
    ): Result {
        if (text.isEmpty()) return Result(emptyList(), true)
        val style = Style.of(type, index)
        var fontSize = requested.points
        var result = measure(style, text, fontSize, width, height, family, weight, fontStyle, measurer, 0f)
        while (!result.fits && fontSize > BubbleReferenceMetrics.MINIMUM_FONT_POINTS) {
            fontSize = (fontSize - 1f).coerceAtLeast(BubbleReferenceMetrics.MINIMUM_FONT_POINTS)
            result = measure(style, text, fontSize, width, height, family, weight, fontStyle, measurer, 0f)
        }
        if (!result.fits) return result
        var low = 0f
        var high = min(width, height) / BubbleGeometry.EXPORT_SCALE
        repeat(8) {
            val extra = (low + high) / 2f
            val candidate = measure(style, text, fontSize, width, height, family, weight, fontStyle, measurer, extra)
            if (candidate.fits) {
                low = extra
                result = candidate
            } else {
                high = extra
            }
        }
        return result
    }

    private fun measure(
        style: Style,
        text: String,
        fontSize: Float,
        width: Float,
        height: Float,
        family: FontFamily,
        weight: FontWeight,
        fontStyle: FontStyle,
        measurer: TextMeasurer,
        extra: Float,
    ): Result {
        val scale = BubbleGeometry.EXPORT_SCALE
        val w = (width / scale - style.basis * 2f - style.offsetX).coerceAtLeast(.3f)
        val h = (height / scale - style.basis * 2f - style.offsetY - extra).coerceAtLeast(.3f)
        val rect = Rect(0f, 0f, w, h)
        val path =
            when (style.path) {
                1 -> BubblePaths.inReferenceSpace(rect, BubblePaths::rectangleEllipse)
                2 -> BubblePaths.inReferenceSpace(rect, BubblePaths::polygon1TextPath)
                3 -> BubblePaths.inReferenceSpace(rect, BubblePaths::polygon2TextPath)
                4 -> BubblePaths.inReferenceSpace(rect, BubblePaths::polygon3TextPath)
                5 -> BubblePaths.inReferenceSpace(rect, BubblePaths::polygon4TextPath)
                else -> Path().apply { addRect(rect) }
            }
        path.transform(scaleMatrix(scale, scale))
        val bounds = path.getBounds()
        val origin = Offset((width - bounds.width) / 2f, (height - bounds.height) / 2f)
        path.translate(-bounds.topLeft)
        val tester = PathHitTester(path)
        val textStyle =
            TextStyle(
                color = Color.Black,
                fontFamily = family,
                fontWeight = weight,
                fontStyle = fontStyle,
                fontSize = (fontSize * BubbleReferenceMetrics.POINT_TO_EXPORT).sp,
            )
        val lineHeight =
            measurer
                .measure(AnnotatedString("Ag"), textStyle)
                .size.height
                .toFloat()
                .coerceAtLeast(1f)
        val capacity = (bounds.height / lineHeight).toInt().coerceAtLeast(0)
        val lines = mutableListOf<Line>()
        var consumed = 0
        for (line in 0 until capacity) {
            if (consumed >= text.length) break
            val mid = (line + .5f) * lineHeight
            var left = 0f
            var right = bounds.width
            while (left < right && !tester.contains(Offset(left, mid))) left += 1f
            while (right > left && !tester.contains(Offset(right, mid))) right -= 1f
            if (right - left < 1f) continue
            val remaining = text.substring(consumed)
            val layout =
                measurer.measure(
                    AnnotatedString(remaining),
                    textStyle,
                    constraints = Constraints(maxWidth = (right - left).toInt().coerceAtLeast(1)),
                    maxLines = 1,
                )
            val length = layout.getLineEnd(0, visibleEnd = false).coerceAtLeast(1)
            val visibleText = remaining.take(length).trimEnd('\n', '\r')
            val visible = measurer.measure(AnnotatedString(visibleText), textStyle, softWrap = false, maxLines = 1)
            lines += Line(visible, origin + Offset(left + (right - left - visible.size.width) / 2f, line * lineHeight))
            consumed += length
        }
        return Result(lines, consumed >= text.length)
    }

    private data class Style(
        val basis: Float,
        val offsetX: Float = 0f,
        val offsetY: Float = offsetX,
        val path: Int = 0,
    ) {
        companion object {
            fun of(
                type: BubbleFamily,
                index: Int,
            ): Style {
                val i = ((index % 4 + 4) % 4)
                val basis = BubbleStyleMetrics.basisOffset(type, i)
                return when (type) {
                    BubbleFamily.CLASSIC_TWO ->
                        when (i) {
                            0 -> Style(basis, BubbleReferenceMetrics.fixedPoints(30f))
                            1 -> Style(basis, 22f, 22f * 4f / 3f)
                            2 -> Style(basis, 25f, 25f * 4f / 3f)
                            else -> Style(basis, 15f, 20f)
                        }
                    BubbleFamily.ROCKS ->
                        Style(
                            basis,
                            BubbleReferenceMetrics.fixedPoints(floatArrayOf(47f, 40f, 45f, 40f)[i]),
                        )
                    BubbleFamily.RECTANGLES -> Style(basis, path = i + 2)
                    BubbleFamily.SCREAMO -> Style(basis)
                    BubbleFamily.CORNER -> Style(basis)
                    else ->
                        when (i) {
                            0 -> Style(8f, BubbleReferenceMetrics.fixedPoints(30f))
                            1 -> Style(basis, BubbleReferenceMetrics.fixedPoints(15f))
                            2 -> Style(basis, BubbleReferenceMetrics.fixedPoints(18f), path = 1)
                            else -> Style(basis, 15f, 20f)
                        }
                }
            }
        }
    }
}
