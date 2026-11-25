package com.comixon.editor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.comixon.editor.core.BubbleFamily
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorFrame
import com.comixon.editor.core.EditorPoint
import com.comixon.editor.core.EditorProject
import com.comixon.editor.core.EditorRect
import com.comixon.editor.core.FontSize
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BubbleTailEditingTest {
    private val frame = Rect(0f, 0f, 140f, 140f)

    @Test fun allStylesUseTheFinalIosInclineOverrideInsteadOfTheirBuilderValues() {
        val types =
            listOf(
                BubbleFamily.CLASSIC,
                BubbleFamily.CLASSIC_TWO,
                BubbleFamily.ROCKS,
                BubbleFamily.RECTANGLES,
                BubbleFamily.SCREAMO,
                BubbleFamily.CORNER,
            )
        val angle = (15.0 * kotlin.math.PI / 180.0)
        types.forEach { type ->
            repeat(4) { index ->
                val initial = BubbleTailAnchor(frame, type, index, Offset(70f, 230f))
                // The settled attachment uses the inset path; Noise has a separate creation state.
                val anchor = BubbleTailAnchor(frame, type, index, Offset(70f, 230f), initial.percent)
                val n = anchor.normal
                val dx = (n.x * kotlin.math.cos(angle) - n.y * kotlin.math.sin(angle)).toFloat()
                val dy = (n.x * kotlin.math.sin(angle) + n.y * kotlin.math.cos(angle)).toFloat()
                val end = Offset(anchor.point.x + 110f * dx, anchor.point.y + 110f * dy)
                assertEquals(
                    anchor.percent,
                    anchor.draggedPercent(end),
                    .00001f,
                    "$type/$index must retain a 15-degree tilt",
                )
            }
        }
    }

    @Test fun firstNoiseTailEditSettlesOntoInsetPathAtTheRequestedIncline() {
        val initial = BubbleTailAnchor(frame, BubbleFamily.CLASSIC, 1, Offset(70f, 230f))
        assertEquals(140f, initial.point.y, .1f)
        val end = Offset(initial.point.x - 28.47f, initial.point.y + 106.25f)
        val updated =
            BubbleTailAnchor(
                frame,
                BubbleFamily.CLASSIC,
                1,
                end,
                initial.draggedPercent(end),
            )
        assertTrue(updated.point.y < 120f)
        assertEquals(15f, kotlin.math.abs(updated.incline), 1.1f)
        assertTrue(kotlin.math.abs(updated.percent - initial.percent) > .005f)
    }

    @Test fun smallEndMotionKeepsTheBaseInsteadOfStraighteningTheTail() {
        val original = BubbleTailAnchor(frame, BubbleFamily.CLASSIC, 0, Offset(70f, 230f))
        val end = Offset(100f, 230f)
        val percent = original.draggedPercent(end)
        val current = BubbleTailAnchor(frame, BubbleFamily.CLASSIC, 0, end, percent)
        val nearest = BubbleTailAnchor(frame, BubbleFamily.CLASSIC, 0, end)
        assertEquals(original.percent, percent, .00001f)
        assertEquals(original.point.x, current.point.x, .01f)
        assertTrue(abs(current.incline) > 10f)
        assertTrue(
            abs(nearest.point.x - current.point.x) > 3f,
            "Fixture must distinguish history from nearest-point rendering",
        )
    }

    @Test fun largeEndMotionSlidesTheBaseAtTheAllowedIncline() {
        val original = BubbleTailAnchor(frame, BubbleFamily.CLASSIC, 0, Offset(70f, 230f))
        val end = Offset(280f, 150f)
        val current = BubbleTailAnchor(frame, BubbleFamily.CLASSIC, 0, end, original.draggedPercent(end))
        assertTrue(abs(current.percent - original.percent) > .02f)
        assertEquals(30f, abs(current.incline), 1.1f)
    }

    @Test fun endCanGoAroundTheBubbleAndAttachToEverySide() {
        var current = BubbleTailAnchor(frame, BubbleFamily.SCREAMO, 0, Offset(70f, 230f))
        for (end in listOf(Offset(230f, 70f), Offset(70f, -90f), Offset(-90f, 70f), Offset(70f, 230f))) {
            current = BubbleTailAnchor(frame, BubbleFamily.SCREAMO, 0, end, current.draggedPercent(end))
            assertTrue(current.percent in 0f..1f)
            assertTrue(abs(current.incline) < 31.1f, "Base failed to follow end $end: ${current.incline}")
            assertTrue(hypot(current.point.x - end.x, current.point.y - end.y).isFinite())
        }
    }

    @Test fun resizeKeepsTheTipFixedLikeTheVideoAndRepositionsItsBase() {
        val layer =
            EditorBubbleLayer(
                "bubble",
                EditorRect(100.0, 100.0, 472.5, 472.5),
                BubbleFamily.SCREAMO,
                0,
                "ТЕКСТ",
                FontSize.M,
                EditorPoint(820.0, 350.0),
                true,
            )
        val project = EditorProject(listOf(EditorFrame("frame", layers = listOf(layer))))
        val after =
            BubbleTailEditing
                .resizeLayer(project, layer.id, 0.0, 0.0, 40.0, 200.0)
                .frames
                .single()
                .layers
                .single() as EditorBubbleLayer
        assertEquals(layer.tailEndPoint, after.tailEndPoint)
        assertEquals(672.5, after.bounds.height, .001)
        assertNotNull(after.tailBasePercent)
        val r = Rect(0f, 0f, (after.bounds.width / 3.375).toFloat(), (after.bounds.height / 3.375).toFloat())
        val end =
            Offset(
                ((after.tailEndPoint.x - after.bounds.x) / 3.375).toFloat(),
                ((after.tailEndPoint.y - after.bounds.y) / 3.375).toFloat(),
            )
        val attachment = BubbleTailAnchor(r, after.type, after.bubbleIndex, end, after.tailBasePercent)
        assertTrue(abs(attachment.incline) <= 31.1f)
        assertTrue(attachment.point.x > r.center.x)
    }

    @Test fun movingUneditedNoisePreservesTheCreationContour() {
        val layer =
            EditorBubbleLayer(
                "noise",
                EditorRect(100.0, 100.0, 472.5, 472.5),
                BubbleFamily.CLASSIC,
                1,
                "ТЕКСТ",
                FontSize.M,
                EditorPoint(240.0, 750.0),
                true,
            )
        val project = EditorProject(listOf(EditorFrame("frame", layers = listOf(layer))))
        val moved =
            BubbleTailEditing
                .moveLayer(project, layer.id, 80.0, 55.0)
                .frames
                .single()
                .layers
                .single() as EditorBubbleLayer
        assertNull(moved.tailBasePercent, "Body translation must not settle the initial Noise attachment")
        assertEquals(
            layer.tailEndPoint.x - layer.bounds.x,
            moved.tailEndPoint.x - moved.bounds.x,
            .001,
        )
        assertEquals(
            layer.tailEndPoint.y - layer.bounds.y,
            moved.tailEndPoint.y - moved.bounds.y,
            .001,
        )
    }

    @Test fun immutableProjectSnapshotsKeepTheirOwnAttachmentForUndo() {
        val layer =
            EditorBubbleLayer(
                "bubble",
                EditorRect(100.0, 100.0, 472.5, 472.5),
                BubbleFamily.CLASSIC,
                0,
                "ТЕКСТ",
                FontSize.M,
                EditorPoint(336.25, 850.0),
                true,
            )
        val original = EditorProject(listOf(EditorFrame("frame", layers = listOf(layer))))
        val first = BubbleTailEditing.moveBubbleTail(original, layer.id, 20.0, 0.0)
        val snapshot =
            first.frames
                .single()
                .layers
                .single() as EditorBubbleLayer
        val second = BubbleTailEditing.moveBubbleTail(first, layer.id, 400.0, -250.0)
        val changed =
            second.frames
                .single()
                .layers
                .single() as EditorBubbleLayer
        assertNull(layer.tailBasePercent)
        assertNotNull(snapshot.tailBasePercent)
        assertNotEquals(snapshot.tailBasePercent, changed.tailBasePercent)
        assertEquals(
            snapshot,
            first.frames
                .single()
                .layers
                .single(),
        )
    }
}
