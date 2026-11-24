package com.comixon.editor.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorBubbleDimensionsTest {
    private val families =
        listOf(
            BubbleFamily.CLASSIC,
            BubbleFamily.CLASSIC_TWO,
            BubbleFamily.ROCKS,
            BubbleFamily.RECTANGLES,
            BubbleFamily.SCREAMO,
            BubbleFamily.CORNER,
        )

    private fun project() = EditorProject(frames = listOf(EditorFrame(id = "frame_0")), selectedFrameId = "frame_0")

    @Test
    fun newBubblesUseIosAxisAdaptationAndStartWithAnOutsideTail() {
        families.forEach { type ->
            repeat(4) { index ->
                val p = EditorReducer.addBubble(project(), BubbleStyle(type, index), "Test")
                val bubble =
                    p.frames
                        .single()
                        .layers
                        .single() as EditorBubbleLayer
                assertEquals(472.5, bubble.bounds.width)
                assertEquals(472.5, bubble.bounds.height)
                assertEquals(bubble.bounds.x + bubble.bounds.width / 2, bubble.tailEndPoint.x)
                assertTrue(bubble.tailEndPoint.y > bubble.bounds.y + bubble.bounds.height)
            }
        }
    }

    @Test
    fun resizeKeepsOppositeEdgesAndUsesStyleMinimums() {
        families.forEach { type ->
            repeat(4) { index ->
                val p = EditorReducer.addBubble(project(), BubbleStyle(type, index), "Test")
                val original =
                    p.frames
                        .single()
                        .layers
                        .single() as EditorBubbleLayer
                val resized =
                    EditorReducer
                        .resizeLayer(p, original.id, 10000.0, 10000.0, 0.0, 0.0)
                        .frames
                        .single()
                        .layers
                        .single() as EditorBubbleLayer
                val minimum = EditorBubbleDimensions.minimum(type, index)
                assertEquals(minimum.width, resized.bounds.width)
                assertEquals(minimum.height, resized.bounds.height)
                assertEquals(original.bounds.x + original.bounds.width, resized.bounds.x + resized.bounds.width)
                assertEquals(original.bounds.y + original.bounds.height, resized.bounds.y + resized.bounds.height)
            }
        }
    }

    @Test
    fun switchingToComplexStyleExpandsSmallBoundsInsideCanvas() {
        val rect = EditorRect(1000.0, 1370.0, 70.0, 70.0)
        val result = EditorBubbleDimensions.ensureMinimum(rect, BubbleFamily.CLASSIC_TWO, 1)
        assertEquals(421.875, result.width)
        assertEquals(360.0, result.height)
        assertTrue(result.x >= 0 && result.x + result.width <= EditorCanvasSpec.LOGICAL_WIDTH)
        assertTrue(result.y >= 0 && result.y + result.height <= EditorCanvasSpec.LOGICAL_HEIGHT)
    }
}
