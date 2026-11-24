package com.comixon.editor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import com.comixon.editor.core.BubbleFamily
import kotlin.math.atan2
import kotlin.math.hypot

/** Tail recipes shared by preview and export, in the same 320-point reference space as bodies. */
internal object BubbleTailGeometry {
    fun path(
        type: BubbleFamily,
        index: Int,
        rect: Rect,
        end: Offset,
        scale: Float,
        basePercent: Float? = null,
    ): Path {
        val r = Rect(rect.left / scale, rect.top / scale, rect.right / scale, rect.bottom / scale)
        val tip = Offset(end.x / scale, end.y / scale)
        if (r.contains(Offset(tip.x, tip.y))) return Path()
        val i = ((index % 4 + 4) % 4)
        val attachment = BubbleTailAnchor(r, type, i, tip, basePercent)
        val anchor = attachment.point
        val base = attachment.baseSize
        val left = attachment.basePoint(base / 2f, true)
        val right = attachment.basePoint(base / 2f, false)
        val dx = tip.x - anchor.x
        val dy = tip.y - anchor.y
        val tailLength = hypot(dx, dy).coerceAtLeast(.001f)
        val p = BubbleReferenceMetrics.POINT_TO_REFERENCE
        val result =
            when {
                type == BubbleFamily.CLASSIC_TWO && i == 0 || type == BubbleFamily.CORNER && i == 2 ->
                    segments(attachment, listOf(20f to 20f, 13f to 13f, 8f to 8f, 5f to 5f), 15f, false, false)
                type == BubbleFamily.CLASSIC_TWO && i == 3 ->
                    segments(attachment, listOf(35f to 18f, 28f to 11f, 21f to 8f, 14f to 6f), 25f, true, false)
                type == BubbleFamily.CLASSIC && i == 2 ->
                    segments(attachment, listOf(40f to 40f, 30f to 30f, 20f to 20f), 35f, false, true)
                type == BubbleFamily.SCREAMO -> {
                    val width = base + 10f * p
                    Path().apply {
                        // TailDrawPathCreateMethods.lightning, before rotation from the downward axis.
                        moveTo((.46312f - .5f) * width, 1.37f * p)
                        lineTo((.06847f - .5f) * width, tailLength - 25.11f * p)
                        lineTo((.48167f - .5f) * width, tailLength - 26.26f * p)
                        lineTo((.17861f - .5f) * width, tailLength - .44f * p)
                        lineTo((.94642f - .5f) * width, tailLength - 33.14f * p)
                        lineTo((.44292f - .5f) * width, tailLength - 31.09f * p)
                        lineTo((.98817f - .5f) * width, 1.37f * p)
                        transform(rotationMatrix(degrees(atan2(dy, dx)) - 90f))
                        translate(Offset(anchor.x, anchor.y))
                    }
                }
                type == BubbleFamily.CORNER ->
                    Path().apply {
                        moveTo(left.x, left.y)
                        attachment.basePoint(base / 2f, true, -5f * p).let { lineTo(it.x, it.y) }
                        lineTo(tip.x, tip.y)
                        attachment.basePoint(base / 2f, false, -5f * p).let { lineTo(it.x, it.y) }
                        lineTo(right.x, right.y)
                    }
                type == BubbleFamily.CLASSIC && i == 0 -> BubbleFlexibleTail.path(attachment)
                else ->
                    Path().apply {
                        moveTo(left.x, left.y)
                        lineTo(tip.x, tip.y)
                        lineTo(right.x, right.y)
                    }
            }
        result.transform(scaleMatrix(scale, scale))
        return result
    }

    private fun segments(
        attachment: BubbleTailAnchor,
        sizes: List<Pair<Float, Float>>,
        minimumDistance: Float,
        rectangles: Boolean,
        cloud: Boolean,
    ): Path {
        val anchor = attachment.point
        val tip = attachment.end
        val centerline =
            Path().apply {
                moveTo(anchor.x, anchor.y)
                if (!rectangles) {
                    val p = attachment.smoothPoints(0f, false)
                    cubicTo(p[1].x, p[1].y, p[2].x, p[2].y, p[3].x, p[3].y)
                } else {
                    lineTo(tip.x, tip.y)
                }
            }
        val measure = PathMeasure().apply { setPath(centerline, false) }
        val shift = if (cloud) .3f else 0f

        fun distribute(count: Int): List<Offset> =
            (0 until count).map { index ->
                val fraction = if (count == 1 && !cloud) 0f else (index + shift) / (count - 1 + shift)
                measure.getPosition((measure.length * fraction).coerceIn(0f, measure.length))
            }
        var visible = sizes
        var centers = distribute(visible.size)
        while (visible.size > 1 && centers.zipWithNext().any { (a, b) -> hypot(a.x - b.x, a.y - b.y) < minimumDistance }) {
            visible = visible.drop(1)
            centers = distribute(visible.size)
        }
        val result = Path()
        visible.forEachIndexed { index, (w, rawHeight) ->
            val center = centers[index]
            val h = if (cloud) rawHeight else rawHeight * 4f / 3f
            val r = Rect(center.x - w / 2f, center.y - h / 2f, center.x + w / 2f, center.y + h / 2f)
            val segment =
                when {
                    cloud && index == visible.lastIndex && sizes.last() == visible[index] ->
                        BubblePaths.inReferenceSpace(
                            r,
                            BubblePaths::smallCloudTail,
                        )
                    cloud -> BubblePaths.inReferenceSpace(r, BubblePaths::largeCloudTail)
                    rectangles && index == 0 ->
                        Path().apply {
                            moveTo(r.left, center.y - BubbleReferenceMetrics.fixedPoints(3f))
                            lineTo(r.left, r.bottom)
                            lineTo(r.right, r.bottom)
                            lineTo(r.right, center.y - BubbleReferenceMetrics.fixedPoints(3f))
                        }
                    rectangles -> Path().apply { addRect(r, Path.Direction.Clockwise) }
                    index == 0 ->
                        Path().apply {
                            val y = center.y - BubbleReferenceMetrics.fixedPoints(4f)
                            moveTo(r.right, y)
                            cubicTo(r.right, center.y + h / 2f, r.left, center.y + h / 2f, r.left, y)
                        }
                    else -> Path().apply { addOval(r, Path.Direction.Clockwise) }
                }
            val bounds = segment.getBounds()
            segment.transform(rotationMatrix(attachment.rotation, bounds.center.x, bounds.center.y))
            result.addPath(segment)
        }
        return result
    }
}
