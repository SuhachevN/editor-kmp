package com.comixon.editor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

internal object BubbleFlexibleTail {
    fun path(anchor: BubbleTailAnchor): Path {
        val base = anchor.point
        val end = anchor.end
        val length = anchor.length.coerceAtLeast(.001f)
        val dx = (end.x - base.x) / length
        val dy = (end.y - base.y) / length
        val centerDx = anchor.rect.center.x - base.x
        val centerDy = anchor.rect.center.y - base.y
        val centerDistance = hypot(centerDx, centerDy).coerceAtLeast(.001f)
        val hint = (dx * centerDy - dy * centerDx) / centerDistance
        val sign = if (hint < -.12f) -1f else 1f
        val inc = abs(anchor.incline)
        val p = BubbleReferenceMetrics.POINT_TO_REFERENCE
        val contribution = if (inc < 6f) 0f else 22f * p * (1f - exp(-inc / 22f))
        val maxAxis = min(80f * p, min(length * .75f, max(36f * p, anchor.baseSize * 1.1f)))
        val bend = (length * .18f + contribution).coerceIn(0f, maxAxis)
        val knee =
            Offset(
                (base.x + end.x) / 2f - dy * bend * sign,
                (base.y + end.y) / 2f + dx * bend * sign,
            )
        val retreat = 10f * p / length.coerceAtLeast(10f * p)
        val drawEnd = Offset(end.x + (base.x - end.x) * retreat, end.y + (base.y - end.y) * retreat)
        val centers = sample(listOf(base, knee, drawEnd))
        val left = mutableListOf(anchor.basePoint(anchor.baseSize / 2f, true))
        val right = mutableListOf(anchor.basePoint(anchor.baseSize / 2f, false))
        for (i in 1 until centers.size) {
            val p = centers[i]
            val previous = centers[i - 1]
            val tx = p.x - previous.x
            val ty = p.y - previous.y
            val len = hypot(tx, ty).coerceAtLeast(.00001f)
            val halfWidth = anchor.baseSize * (1f - i.toFloat() / (centers.size - 1)).coerceAtLeast(0f).pow(1.2f) / 2f
            left += Offset(p.x - ty / len * halfWidth, p.y + tx / len * halfWidth)
            right += Offset(p.x + ty / len * halfWidth, p.y - tx / len * halfWidth)
        }
        return Path().apply {
            moveTo(left.first().x, left.first().y)
            left.drop(1).forEach { lineTo(it.x, it.y) }
            right.asReversed().forEach { lineTo(it.x, it.y) }
        }
    }

    private fun sample(points: List<Offset>): List<Offset> {
        val result = mutableListOf<Offset>()
        for (i in 0 until points.lastIndex) {
            val p0 = points[max(0, i - 1)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[min(points.lastIndex, i + 2)]
            val t1 = sqrt(hypot(p1.x - p0.x, p1.y - p0.y))
            val t2 = t1 + sqrt(hypot(p2.x - p1.x, p2.y - p1.y))
            val t3 = t2 + sqrt(hypot(p3.x - p2.x, p3.y - p2.y))
            val dt = (t2 - t1).coerceAtLeast(.0001f)
            val m1 = dt / t2.coerceAtLeast(.0001f) / 3f
            val m2 = dt / (t3 - t1).coerceAtLeast(.0001f) / 3f
            val c1 = Offset(p1.x + (p2.x - p0.x) * m1, p1.y + (p2.y - p0.y) * m1)
            val c2 = Offset(p2.x - (p3.x - p1.x) * m2, p2.y - (p3.y - p1.y) * m2)
            for (s in 0 until 14) {
                if (i > 0 && s == 0) continue
                val t = s / 13f
                val u = 1f - t
                result +=
                    Offset(
                        u * u * u * p1.x + 3f * u * u * t * c1.x + 3f * u * t * t * c2.x + t * t * t * p2.x,
                        u * u * u * p1.y + 3f * u * u * t * c1.y + 3f * u * t * t * c2.y + t * t * t * p2.y,
                    )
            }
        }
        return result
    }
}
