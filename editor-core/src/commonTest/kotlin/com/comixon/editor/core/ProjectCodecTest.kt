package com.comixon.editor.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProjectCodecTest {
    @Test fun allStylesAndTailHistorySurviveRoundTrip() {
        val frame =
            EditorFrame(
                "frame",
                originalImageUrl = "data:image/png;base64,dGVzdA==",
                layers =
                    StyleCatalog.bubbles.mapIndexed { index, style ->
                        EditorBubbleLayer(
                            "bubble_" + index,
                            EditorRect(100.0, 100.0, 472.5, 472.5),
                            style.type,
                            style.index,
                            "Привет 👋\nSecond line",
                            FontSize.L,
                            EditorPoint(700.0, 900.0),
                            true,
                            tailBasePercent = .32f,
                            font = EditorFont.PIXEL,
                        )
                    },
            )
        val project = EditorProject(listOf(frame), selectedLayerId = "bubble_12")
        assertEquals(project, ProjectCodec.decode(ProjectCodec.encode(project)))
    }

    @Test fun futureVersionsAndMalformedFilesAreRejected() {
        val source = ProjectCodec.encode(EditorProject())
        assertFailsWith<IllegalArgumentException> { ProjectCodec.decode(source.replace("\"version\":1", "\"version\":2")) }
        assertFailsWith<IllegalArgumentException> { ProjectCodec.decode("not json") }
    }

    @Test fun outOfBoundsLayersAndDuplicateIdsAreRejected() {
        val project = EditorReducer.addDefaultBubble(EditorProject(listOf(EditorFrame("frame"))), "Test")
        val frame = project.frames.single()
        val bubble = frame.layers.single() as EditorBubbleLayer
        val invalid = project.copy(frames = listOf(frame.copy(layers = listOf(bubble.copy(bounds = bubble.bounds.copy(width = 2000.0))))))
        assertFailsWith<IllegalArgumentException> { ProjectCodec.decode(ProjectCodec.encode(invalid)) }
        val duplicate = project.copy(frames = listOf(frame, frame))
        assertFailsWith<IllegalArgumentException> { ProjectCodec.decode(ProjectCodec.encode(duplicate)) }
    }

    @Test fun unknownSelectionIsRepairedOnLoad() {
        val project = EditorProject(listOf(EditorFrame("frame")), "missing", "missing")
        val restored = ProjectCodec.decode(ProjectCodec.encode(project))
        assertEquals("frame", restored.selectedFrameId)
        assertEquals(null, restored.selectedLayerId)
    }
}
