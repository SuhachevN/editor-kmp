package com.comixon.editor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.comixon.editor.core.BubbleFamily
import com.comixon.editor.core.FontSize
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BubbleGeometryTest {
    @Test fun all24StylesRemainFiniteAcrossAspectRatios() {
        BubbleFamily.entries.forEach { type ->
            repeat(4) { index ->
                listOf(140f to 140f, 240f to 100f, 130f to 230f).forEach { (w, h) ->
                    val path = BubbleGeometry.path(type, index, Rect(0f, 0f, w, h), 1f)
                    val measure = PathMeasure().apply { setPath(path, false) }
                    assertTrue(measure.length > 0, "$type/$index is empty")
                    val b = path.getBounds()
                    assertTrue(listOf(b.left, b.top, b.right, b.bottom).all(Float::isFinite))
                    assertTrue((measure.getPosition(0f) - measure.getPosition(measure.length)).getDistance() < .01f)
                }
            }
        }
    }

    @Test fun thoughtCloudRetainsTheEightReferenceLobes() {
        val frame = Rect(0f, 0f, 140f, 140f)
        val rect = frame.deflate(BubbleTextLayout.strokeWidth(FontSize.M) / 2f)
        val measure = PathMeasure().apply { setPath(BubbleGeometry.path(BubbleFamily.CLASSIC, 2, rect, 1f, frameRect = frame), true) }
        val radii =
            FloatArray(2048) { index ->
                val point = measure.getPosition(measure.length * index / 2048f)
                hypot(point.x - 70f, point.y - 70f)
            }
        val peaks =
            radii.indices.count { index ->
                radii[index] > radii[(index + 2047) % 2048] && radii[index] >= radii[(index + 1) % 2048]
            }
        assertEquals(8, peaks)
    }

    @Test fun referenceAndExportContoursAgreeForAll24StylesInFourDirections() {
        val rect = Rect(80f, 80f, 220f, 220f)
        val tips = listOf(Offset(150f, 300f), Offset(150f, 10f), Offset(10f, 150f), Offset(300f, 150f))
        val scale = BubbleGeometry.EXPORT_SCALE
        BubbleFamily.entries.forEach { type ->
            repeat(4) { index ->
                tips.forEach { tip ->
                    val a = BubbleGeometry.outline(type, index, rect, 1f, tip)
                    val b =
                        BubbleGeometry.outline(
                            type,
                            index,
                            Rect(
                                rect.left * scale,
                                rect.top * scale,
                                rect.right * scale,
                                rect.bottom * scale,
                            ),
                            scale,
                            tip * scale,
                        )
                    b.transform(scaleMatrix(1f / scale, 1f / scale))
                    val pa = raster(a).toPixelMap()
                    val pb = raster(b).toPixelMap()
                    var different = 0
                    for (y in 0 until 320) for (x in 0 until 320) if (abs(pa[x, y].alpha - pb[x, y].alpha) > 8f / 255f) different++
                    assertTrue(different < 100, "$type/$index tip=$tip: $different differing pixels")
                }
            }
        }
    }

    @Test fun rockDecorationUsesAbsoluteOffsetsAfterResize() {
        for (width in listOf(140f, 240f)) {
            val measure =
                PathMeasure().apply {
                    setPath(
                        BubbleGeometry.path(
                            BubbleFamily.ROCKS,
                            0,
                            Rect(10f, 20f, 10f + width, 160f),
                            1f,
                        ),
                        false,
                    )
                }
            val first = measure.getPosition(0f)
            assertEquals(10f + width - BubbleReferenceMetrics.fixedPoints(19.63f), first.x, .001f)
            assertEquals(20f + BubbleReferenceMetrics.fixedPoints(19.49f), first.y, .001f)
        }
    }

    @Test fun continuousTailJoinDoesNotDrawABlackSeam() {
        val path = BubbleGeometry.outline(BubbleFamily.CLASSIC, 0, Rect(40f, 20f, 180f, 160f), 1f, Offset(110f, 230f))
        val bitmap = raster(path, stroke = true).toPixelMap()
        for (y in 155..162) assertEquals(Color.White, bitmap[110, y], "Seam at y=$y")
    }

    @Test fun tailBaseAndCornerExclusionsMatchSourceMetrics() {
        val classic = BubbleTailAnchor(Rect(0f, 0f, 140f, 140f), BubbleFamily.CLASSIC, 0, Offset(70f, 230f))
        assertEquals(70f, classic.point.x, .2f)
        assertEquals(132f, classic.point.y, .2f)
        val rectangle = BubbleTailAnchor(Rect(0f, 0f, 140f, 140f), BubbleFamily.CLASSIC, 3, Offset(220f, 220f))
        assertTrue(hypot(rectangle.point.x - 135f, rectangle.point.y - 135f) >= 19.9f)

        fun points(x: Float) = BubbleReferenceMetrics.fixedPoints(x)

        fun frame(
            w: Float,
            h: Float,
        ) = Rect(0f, 0f, points(w), points(h))
        assertEquals(points(12f), BubbleTailAnchor.baseSize(frame(139f, 200f)), 0f)
        assertEquals(points(20f), BubbleTailAnchor.baseSize(frame(140f, 140f)), 0f)
    }

    @Test fun imageMasksStayInsideAll24BodiesAndKeepAWhiteRim() {
        val frame = Rect(20f, 20f, 240f, 220f)
        BubbleFamily.entries.forEach { type ->
            repeat(4) { index ->
                val body = BubbleGeometry.path(type, index, frame, 1f)
                val mask = BubbleImageMask.path(type, index, body, frame, 1f)
                val a = raster(body).toPixelMap()
                val b = raster(mask).toPixelMap()
                var rim = 0f
                for (y in 0 until 320) {
                    for (x in 0 until 320) {
                        if (b[x, y].alpha > .1f) {
                            // Boolean path operations can move the antialiased edge by one pixel.
                            val coverage =
                                (-1..1).maxOf { dy ->
                                    (-1..1).maxOf { dx ->
                                        a[(x + dx).coerceIn(0, 319), (y + dy).coerceIn(0, 319)].alpha
                                    }
                                }
                            assertTrue(coverage > .1f, "$type/$index image escapes its body at $x,$y")
                        }
                        rim += (a[x, y].alpha - b[x, y].alpha).coerceAtLeast(0f)
                    }
                }
                assertTrue(rim > 50f, "$type/$index has no white rim")
            }
        }
    }

    private fun raster(
        path: Path,
        stroke: Boolean = false,
    ): ImageBitmap {
        val bitmap = ImageBitmap(320, 320)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(320f, 320f)) {
            drawPath(path, Color.White)
            if (stroke) drawPath(path, Color.Black, style = Stroke(2.2f))
        }
        return bitmap
    }
}
