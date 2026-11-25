package com.comixon.editor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathOperation
import com.comixon.editor.core.BubbleFamily
import kotlin.math.hypot
import kotlin.math.min

/**
 * Geometry uses a 320-point reference canvas. Resizing a bubble preserves the size of its decorations.
 */
internal object BubbleGeometry {
    const val REFERENCE_WIDTH = 320f
    const val EXPORT_SCALE = 1080f / REFERENCE_WIDTH

    fun outline(
        type: BubbleFamily,
        index: Int,
        rect: Rect,
        detailScale: Float,
        tailEnd: Offset?,
        frameRect: Rect = rect,
        basePercent: Float? = null,
    ): Path {
        val body = path(type, index, rect, detailScale, tailEnd, frameRect, basePercent)
        if (tailEnd == null) return body
        val tail = BubbleTailGeometry.path(type, index, frameRect, tailEnd, detailScale, basePercent)
        if (tail.isEmpty) return body
        return Path().apply {
            // Joining the filled regions removes the black closing edge across the tail base.
            if (!op(body, tail, PathOperation.Union)) {
                addPath(body)
                addPath(tail)
            }
        }
    }

    fun path(
        type: BubbleFamily,
        index: Int,
        rect: Rect,
        detailScale: Float,
        tailEnd: Offset? = null,
        frameRect: Rect = rect,
        basePercent: Float? = null,
    ): Path {
        require(detailScale > 0f && detailScale.isFinite())
        val r =
            Rect(
                rect.left / detailScale,
                rect.top / detailScale,
                rect.right / detailScale,
                rect.bottom / detailScale,
            )
        val frame =
            Rect(
                frameRect.left / detailScale,
                frameRect.top / detailScale,
                frameRect.right / detailScale,
                frameRect.bottom / detailScale,
            )
        val end = tailEnd?.let { Offset(it.x / detailScale, it.y / detailScale) }
        val i = ((index % 4 + 4) % 4)
        val path =
            when (type) {
                BubbleFamily.ROCKS ->
                    when (i) {
                        0 -> BubblePaths.inReferenceSpace(r, BubblePaths::rock1)
                        1 -> BubblePaths.inReferenceSpace(r, BubblePaths::rock2)
                        2 -> BubblePaths.inReferenceSpace(r, BubblePaths::rock3)
                        else -> BubblePaths.inReferenceSpace(r, BubblePaths::rock4)
                    }
                BubbleFamily.RECTANGLES ->
                    when (i) {
                        0 -> BubblePaths.inReferenceSpace(r, BubblePaths::polygon1)
                        1 -> BubblePaths.inReferenceSpace(r, BubblePaths::polygon2)
                        2 -> BubblePaths.inReferenceSpace(r, BubblePaths::polygon3)
                        else -> BubblePaths.inReferenceSpace(r, BubblePaths::polygon4)
                    }
                BubbleFamily.SCREAMO ->
                    when (i) {
                        0 -> BubblePaths.inReferenceSpace(r, BubblePaths::screamo1)
                        1 -> BubblePaths.inReferenceSpace(r, BubblePaths::screamo2)
                        2 -> BubblePaths.inReferenceSpace(r, BubblePaths::screamo3)
                        else -> BubblePaths.inReferenceSpace(r, BubblePaths::screamo4)
                    }
                BubbleFamily.CORNER ->
                    when (i) {
                        0 -> BubblePaths.inReferenceSpace(r, BubblePaths::rightCorner)
                        1 -> BubblePaths.inReferenceSpace(r, BubblePaths::diagonalCorner)
                        2 -> BubblePaths.inReferenceSpace(r, BubblePaths::ellipseCorner)
                        else -> BubblePaths.inReferenceSpace(r, BubblePaths::bezierCorner)
                    }
                else ->
                    when (i) {
                        1, 2 -> segmentedPath(r, frame, type, i, end, basePercent)
                        3 -> Path().apply { addRect(r, Path.Direction.Clockwise) }
                        else -> BubblePaths.inReferenceSpace(r, BubblePaths::rectangleEllipse)
                    }
            }
        path.close()
        path.transform(scaleMatrix(detailScale, detailScale))
        return path
    }

    private data class Segment(
        val width: Float,
        val height: Float,
        val strength: Float = 1.5f,
    )

    private fun segmentedPath(
        rect: Rect,
        frame: Rect,
        type: BubbleFamily,
        index: Int,
        end: Offset?,
        basePercent: Float?,
    ): Path {
        val second = type == BubbleFamily.CLASSIC_TWO
        val cloud = index == 2
        val offset =
            min(
                if (second) {
                    (if (cloud) 20f else 20f * 4f / 3f)
                } else if (cloud) {
                    25f
                } else {
                    BubbleReferenceMetrics.fixedPoints(25f)
                },
                min(rect.width, rect.height).coerceAtLeast(2f) / 2f - 0.5f,
            )
        val basisRect = rect.deflate(offset)
        val basis =
            if (!second && !cloud) {
                BubblePaths.inReferenceSpace(basisRect, BubblePaths::rectangleEllipse)
            } else {
                Path().apply { addOval(basisRect, Path.Direction.Clockwise) }
            }
        val measure = PathMeasure().apply { setPath(basis, true) }
        val length = measure.length
        if (length <= 0f) return Path()
        val recipe =
            when {
                !second && !cloud -> listOf(Segment(1f, 1f), Segment(1f, 1f))
                !second ->
                    listOf(1f, 3f, 3f, 2f, 3f, 3f, 2f, 1f, 2f, 2f, 3f)
                        .map { Segment(it, if (it == 1f) 0.5f else it - 1f) }
                !cloud ->
                    listOf(1f, .5f, 2f, 2f, .5f, 2f, 2f, 2f, .5f)
                        .map { Segment(it, if (it == .5f) .5f else 1f) }
                else ->
                    listOf(
                        Segment(1f, 2f),
                        Segment(2f, 2f, 1.2f),
                        Segment(2f, 2f, 1.2f),
                        Segment(1f, 2f),
                    )
            }
        val baseWidth = BubbleTailAnchor.baseSize(frame)
        val attached = (second || !cloud) && end != null && !frame.contains(Offset(end.x, end.y))
        val anchor = if (attached) BubbleTailAnchor(frame, type, index, requireNotNull(end), basePercent) else null
        val begin = if (anchor != null) anchor.percent * length + baseWidth / 2f else 0f
        val drawLength = if (anchor != null) (length - baseWidth).coerceAtLeast(1f) else length * .999f
        var total = 0f
        var count = 0
        while (count < 1024) {
            val next = recipe[count % recipe.size].width * baseWidth
            if (total + next > drawLength) {
                if (drawLength - total > total + next - drawLength || count == 0) {
                    total += next
                    count++
                }
                break
            }
            total += next
            count++
        }

        fun point(distance: Float): Offset {
            val wrapped = ((distance % length) + length) % length
            return measure.getPosition(wrapped)
        }
        var start = point(begin)
        val path = Path().apply { moveTo(start.x, start.y) }
        var distance = begin
        repeat(count) { n ->
            val segment = recipe[n % recipe.size]
            distance += drawLength * segment.width * baseWidth / total
            val end = point(distance)
            val dx = end.x - start.x
            val dy = end.y - start.y
            val chord = hypot(dx, dy).coerceAtLeast(.001f)
            val nx = dy / chord
            val ny = -dx / chord
            if (cloud) {
                val radius = chord / segment.strength
                path.cubicTo(
                    start.x + nx * radius,
                    start.y + ny * radius,
                    end.x + nx * radius,
                    end.y + ny * radius,
                    end.x,
                    end.y,
                )
            } else {
                val height = offset * segment.height
                path.lineTo(
                    (start.x + end.x) / 2f + nx * height,
                    (start.y + end.y) / 2f + ny * height,
                )
                path.lineTo(end.x, end.y)
            }
            start = end
        }
        path.close()
        return path
    }
}
