package com.comixon.editor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import com.comixon.editor.core.BubbleFamily
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/** The base follows an inset traverse path, not the visible decorative outline. */
internal class BubbleTailAnchor(
    val rect: Rect,
    type: BubbleFamily,
    index: Int,
    val end: Offset,
    basePercent: Float? = null,
) {
    private val spec = Spec.of(type, ((index % 4 + 4) % 4))
    val traverse = Traversal(spec.path(rect), spec.corners(rect))
    private val initialTraversal =
        if (basePercent == null &&
            type == BubbleFamily.CLASSIC &&
            ((index % 4 + 4) % 4) == 1
        ) {
            Traversal(Path().apply { addOval(rect, Path.Direction.Clockwise) })
        } else {
            null
        }
    private val initialSample = initialTraversal?.nearest(end)
    val sample =
        basePercent?.takeIf { it.isFinite() }?.let { traverse.at(it * traverse.length) }
            ?: initialSample ?: traverse.nearest(end, spec.exclusion, spec.maxIncline)
    private val projectedBase by lazy { if (initialSample != null) traverse.nearest(sample.point) else sample }
    val point: Offset get() = sample.point
    val percent: Float get() = sample.distance / (initialTraversal?.length ?: traverse.length).coerceAtLeast(.001f)
    val normal: Offset get() = sample.normal
    val rotation: Float get() = degrees(atan2(normal.y, normal.x)) - 90f
    val length: Float get() = hypot(end.x - point.x, end.y - point.y)
    val incline: Float get() = angle(normal, Offset(end.x - point.x, end.y - point.y))
    val baseSize = baseSize(rect)

    /** Keep the base while the end is within its allowed incline, then slide along the path. */
    fun draggedPercent(target: Offset): Float {
        val requested = angle(normal, Offset(target.x - point.x, target.y - point.y))
        if (initialSample == null && abs(requested) <= spec.maxIncline) return percent
        return traverse
            .follow(
                target,
                percent,
                requested.coerceIn(-spec.maxIncline, spec.maxIncline),
                spec.exclusion,
            ).distance / traverse.length.coerceAtLeast(.001f)
    }

    /** Resize keeps the end fixed and first restores the old percentage on the new path. */
    fun resizedPercent(): Float =
        if (abs(incline) <= spec.maxIncline) {
            percent
        } else {
            traverse.follow(end, percent, 0f, spec.exclusion).distance / traverse.length.coerceAtLeast(.001f)
        }

    fun basePoint(
        distance: Float,
        forward: Boolean,
        pathOffset: Float = 0f,
    ): Offset {
        val path = if (pathOffset == 0f) traverse else Traversal(spec.path(rect, pathOffset))
        val base = if (pathOffset == 0f) projectedBase else path.nearest(point)
        return path.at(base.distance + if (forward) distance else -distance).point
    }

    fun smoothPoints(
        distance: Float,
        forward: Boolean,
    ): List<Offset> {
        val reach = (length - BubbleReferenceMetrics.fixedPoints(abs(incline)) / 2f).coerceAtLeast(0f)
        val retreatDistance = BubbleReferenceMetrics.fixedPoints(10f)
        val retreat = retreatDistance / length.coerceAtLeast(retreatDistance)
        return listOf(
            basePoint(distance, forward),
            basePoint(distance / 1.2f, forward, -reach / 3f),
            basePoint(distance / 1.7f, forward, -reach * 2f / 3f),
            Offset(end.x + (point.x - end.x) * retreat, end.y + (point.y - end.y) * retreat),
        )
    }

    internal data class Sample(
        val distance: Float,
        val point: Offset,
        val normal: Offset,
    )

    internal class Traversal(
        path: Path,
        private val exclusionPoints: List<Offset> = emptyList(),
    ) {
        private val measure = PathMeasure().apply { setPath(path, true) }
        val length = measure.length

        fun at(distance: Float): Sample {
            val d = if (length > 0f) ((distance % length) + length) % length else 0f
            val point = measure.getPosition(d)
            val tangent = measure.getTangent(d)
            return Sample(d, point, Offset(tangent.y, -tangent.x))
        }

        fun nearest(
            target: Offset,
            exclusion: Float = 0f,
            maxIncline: Float = 180f,
        ): Sample {
            // Explicit corner exclusions are supplied as distances in the same reference space.
            // The four bounding corners match PathConfigurator.pathWithCornerExclusionPoints.
            val corners = if (exclusion > 0f) exclusionPoints.map { nearest(it).distance } else emptyList()

            fun allowed(sample: Sample): Boolean {
                if (corners.any {
                        val d = abs(it - sample.distance)
                        min(d, length - d) < exclusion
                    }
                ) {
                    return false
                }
                return abs(angle(sample.normal, Offset(target.x - sample.point.x, target.y - sample.point.y))) <= maxIncline + .1f
            }

            fun score(sample: Sample): Float = hypot(sample.point.x - target.x, sample.point.y - target.y)
            var best = at(0f)
            var bestScore = Float.MAX_VALUE
            val samples = 512
            repeat(samples) { n ->
                val candidate = at(length * n / samples)
                val distance = score(candidate)
                if (allowed(candidate) && distance < bestScore) {
                    best = candidate
                    bestScore = distance
                }
            }
            if (bestScore == Float.MAX_VALUE && maxIncline < 180f) return nearest(target, exclusion, 180f)
            if (bestScore == Float.MAX_VALUE && exclusion > 0f) return nearest(target)
            var step = length / samples
            repeat(12) {
                for (d in listOf(best.distance - step, best.distance + step)) {
                    val candidate = at(d)
                    val distance = score(candidate)
                    if (allowed(candidate) && distance < bestScore) {
                        best = candidate
                        bestScore = distance
                    }
                }
                step /= 2f
            }
            return best
        }

        // TraversePath.nearestPoint(toPoint:cashedPercent:angle:). Search locally from the
        // previous base, preserving the clamped angle instead of minimizing tail length.
        fun follow(
            target: Offset,
            cachedPercent: Float,
            incline: Float,
            exclusion: Float,
        ): Sample {
            fun difference(percent: Float): Float {
                val p = at(percent * length)
                return abs(incline - angle(p.normal, Offset(target.x - p.point.x, target.y - p.point.y)))
            }
            val step = if (difference(cachedPercent + .01f) < difference(cachedPercent - .01f)) .0005f else -.0005f
            var current = cachedPercent + step
            var previousDifference =
                hypot(
                    at(cachedPercent * length).point.x - target.x,
                    at(cachedPercent * length).point.y - target.y,
                )
            var currentDifference = 0f
            var result: Sample? = null
            for (iteration in 0 until 2000) {
                if (currentDifference != 0f) previousDifference = currentDifference
                current += step
                currentDifference = difference(current)
                if (currentDifference > previousDifference && previousDifference < 1f) {
                    result = at((current - step) * length)
                    break
                }
            }
            var candidate = result ?: nearest(target)
            if (exclusion > 0f) {
                for (corner in exclusionPoints) {
                    val center = nearest(corner).distance
                    val delta = ((candidate.distance - center + length * 1.5f) % length) - length / 2f
                    if (abs(delta) < exclusion) candidate = at(center + if (delta < 0f) -exclusion else exclusion)
                }
            }
            return candidate
        }
    }

    private data class Spec(
        val kind: Int,
        val left: Float = 0f,
        val top: Float = left,
        val right: Float = left,
        val bottom: Float = top,
        val exclusion: Float = 0f,
        val maxIncline: Float = 30f,
    ) {
        private fun pathRect(
            rect: Rect,
            offset: Float,
        ): Rect {
            val x = ((left + right) / 2 + offset).coerceAtMost(rect.width / 2 - .5f)
            val y = ((top + bottom) / 2 + offset).coerceAtMost(rect.height / 2 - .5f)
            return Rect(
                rect.center.x + (left - right) / 2 - rect.width / 2 + x,
                rect.center.y + (top - bottom) / 2 - rect.height / 2 + y,
                rect.center.x + (left - right) / 2 + rect.width / 2 - x,
                rect.center.y + (top - bottom) / 2 + rect.height / 2 - y,
            )
        }

        fun corners(rect: Rect): List<Offset> =
            pathRect(rect, 0f).let { r ->
                listOf(Offset(r.left, r.top), Offset(r.right, r.top), Offset(r.right, r.bottom), Offset(r.left, r.bottom))
            }

        fun path(
            rect: Rect,
            offset: Float = 0f,
        ): Path {
            val r = pathRect(rect, offset)
            return when (kind) {
                1 -> BubblePaths.inReferenceSpace(r, BubblePaths::rectangleEllipse)
                2 -> Path().apply { addRect(r, Path.Direction.Clockwise) }
                3 -> BubblePaths.inReferenceSpace(r, BubblePaths::smoothCornersRectangle)
                4 -> BubblePaths.inReferenceSpace(r, BubblePaths::polygon1)
                5 -> BubblePaths.inReferenceSpace(r, BubblePaths::polygon2)
                6 -> BubblePaths.inReferenceSpace(r, BubblePaths::polygon3)
                7 -> BubblePaths.inReferenceSpace(r, BubblePaths::polygon4)
                8 -> BubblePaths.inReferenceSpace(r, BubblePaths::ellipseCorner)
                else -> Path().apply { addOval(r, Path.Direction.Clockwise) }
            }
        }

        companion object {
            fun of(
                type: BubbleFamily,
                i: Int,
            ): Spec {
                val p = BubbleReferenceMetrics.POINT_TO_REFERENCE
                return when (type) {
                    BubbleFamily.CLASSIC_TWO ->
                        when (i) {
                            1 -> Spec(0, 20f * 4f / 3f + 2f * p)
                            2 -> Spec(0, 20f + 2f * p)
                            3 -> Spec(2, exclusion = 30f)
                            else -> Spec(0)
                        }
                    BubbleFamily.ROCKS -> Spec(3, (if (i == 1) 17f else 20f) * p)
                    BubbleFamily.RECTANGLES -> Spec(i + 4, 5f, exclusion = 20f)
                    BubbleFamily.SCREAMO ->
                        when (i) {
                            0 -> Spec(3, 28f * p, 23f * p, 23f * p, 28f * p)
                            1 -> Spec(3, 28f * p, 23f * p, 28f * p, 28f * p)
                            2 -> Spec(3, 26f * p, 25f * p, 26f * p, 32f * p)
                            else -> Spec(3, 30f * p, 27f * p, 31f * p, 32f * p)
                        }
                    BubbleFamily.CORNER -> if (i == 2) Spec(8) else Spec(2, 5f, exclusion = 23f)
                    else ->
                        when (i) {
                            0 -> Spec(0, 8f)
                            1 -> Spec(1, 27f * p)
                            2 -> Spec(0)
                            else -> Spec(2, 5f, exclusion = 20f)
                        }
                }
            }
        }
    }

    companion object {
        fun baseSize(rect: Rect): Float =
            BubbleReferenceMetrics.fixedPoints(
                if (min(rect.width, rect.height) < BubbleReferenceMetrics.fixedPoints(140f)) 12f else 20f,
            )

        private fun angle(
            a: Offset,
            b: Offset,
        ): Float = degrees(atan2(a.x * b.y - a.y * b.x, a.x * b.x + a.y * b.y))
    }
}
