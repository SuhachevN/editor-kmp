package com.comixon.editor.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EditorReducerTest {
    @Test
    fun addBubbleAddsSelectedStyleAndSelectsBubble() {
        val style =
            BubbleStyle(
                type = BubbleFamily.SCREAMO,
                index = 2,
            )
        val updated = EditorReducer.addBubble(sampleProject(), style, "Text")
        val layer =
            updated.frames
                .single()
                .layers
                .single()

        assertIs<EditorBubbleLayer>(layer)
        assertEquals(layer.id, updated.selectedLayerId)
        assertEquals("Text", layer.text)
        assertEquals(BubbleFamily.SCREAMO, layer.type)
        assertEquals(2, layer.bubbleIndex)
    }

    @Test
    fun addLetteringAddsSelectedStyleAndSelectsLetter() {
        val style =
            LetteringStyle(
                type = LetteringFamily.BRIDGE,
                index = 1,
            )
        val updated = EditorReducer.addLettering(sampleProject(), style, "WOW")
        val layer =
            updated.frames
                .single()
                .layers
                .single()

        assertIs<EditorLetterLayer>(layer)
        assertEquals(layer.id, updated.selectedLayerId)
        assertEquals("WOW", layer.text)
        assertEquals(LetteringFamily.BRIDGE, layer.type)
        assertEquals(1, layer.letterIndex)
    }

    @Test
    fun updateBubbleStyleChangesTypeAndIndexWithoutAddingLayer() {
        val withBubble = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val updated =
            EditorReducer.updateBubbleStyle(
                project = withBubble,
                layerId = "bubble_0",
                style =
                    BubbleStyle(
                        type = BubbleFamily.RECTANGLES,
                        index = 3,
                    ),
            )
        val bubble =
            updated.frames
                .single()
                .layers
                .single() as EditorBubbleLayer

        assertEquals(BubbleFamily.RECTANGLES, bubble.type)
        assertEquals(3, bubble.bubbleIndex)
        assertEquals("bubble_0", updated.selectedLayerId)
    }

    @Test
    fun ensureDefaultBubbleSelectsExistingBubble() {
        val withBubble = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val updated = EditorReducer.ensureDefaultBubble(withBubble.copy(selectedLayerId = null), "Ignored")

        assertEquals(
            1,
            updated.frames
                .single()
                .layers.size,
        )
        assertEquals("bubble_0", updated.selectedLayerId)
    }

    @Test
    fun selectFrameChangesFrameAndClearsLayerSelection() {
        val updated =
            EditorReducer.selectFrame(
                project = sampleMultiFrameProject().copy(selectedLayerId = "bubble_0"),
                frameId = "frame_1",
            )

        assertEquals("frame_1", updated.selectedFrameId)
        assertEquals(null, updated.selectedLayerId)
    }

    @Test
    fun selectFrameIgnoresUnknownFrame() {
        val project = sampleMultiFrameProject()
        val updated = EditorReducer.selectFrame(project, "missing")

        assertEquals(project, updated)
    }

    @Test
    fun addFrameAppendsNewFrameAndSelectsIt() {
        val updated = EditorReducer.addFrame(sampleMultiFrameProject(), "image_2")

        assertEquals(3, updated.frames.size)
        assertEquals("frame_2", updated.frames.last().id)
        assertEquals("image_2", updated.frames.last().originalImageUrl)
        assertEquals("frame_2", updated.selectedFrameId)
        assertEquals(null, updated.selectedLayerId)
    }

    @Test
    fun addFrameAvoidsExistingIdsAfterDeletion() {
        val project =
            sampleMultiFrameProject().copy(
                frames =
                    listOf(
                        EditorFrame(id = "frame_0", originalImageUrl = "image_0"),
                        EditorFrame(id = "frame_2", originalImageUrl = "image_2"),
                    ),
            )
        val updated = EditorReducer.addFrame(project, "image_3")

        assertEquals("frame_3", updated.frames.last().id)
    }

    @Test
    fun deleteSelectedFrameSelectsNextFrameAndClearsLayerSelection() {
        val updated =
            EditorReducer.deleteFrame(
                project = sampleMultiFrameProject().copy(selectedLayerId = "bubble_0"),
                frameId = "frame_0",
            )

        assertEquals(listOf("frame_1"), updated.frames.map { it.id })
        assertEquals("frame_1", updated.selectedFrameId)
        assertEquals(null, updated.selectedLayerId)
    }

    @Test
    fun deleteSelectedLastFrameSelectsPreviousFrame() {
        val project = sampleMultiFrameProject().copy(selectedFrameId = "frame_1")
        val updated = EditorReducer.deleteFrame(project, "frame_1")

        assertEquals(listOf("frame_0"), updated.frames.map { it.id })
        assertEquals("frame_0", updated.selectedFrameId)
    }

    @Test
    fun deleteOnlyFrameClearsFrameSelection() {
        val updated = EditorReducer.deleteFrame(sampleProject(), "frame_0")

        assertEquals(emptyList(), updated.frames)
        assertEquals(null, updated.selectedFrameId)
        assertEquals(null, updated.selectedLayerId)
    }

    @Test
    fun deleteUnknownFrameKeepsProject() {
        val project = sampleMultiFrameProject()
        val updated = EditorReducer.deleteFrame(project, "missing")

        assertEquals(project, updated)
    }

    @Test
    fun moveFrameReordersFramesAndKeepsSelection() {
        val project = sampleThreeFrameProject().copy(selectedFrameId = "frame_1")
        val updated = EditorReducer.moveFrame(project, "frame_1", -1)

        assertEquals(listOf("frame_1", "frame_0", "frame_2"), updated.frames.map { it.id })
        assertEquals("frame_1", updated.selectedFrameId)
    }

    @Test
    fun moveFrameClampsAtEdges() {
        val project = sampleThreeFrameProject()
        val leftEdge = EditorReducer.moveFrame(project, "frame_0", -1)
        val rightEdge = EditorReducer.moveFrame(project, "frame_2", 1)

        assertEquals(project, leftEdge)
        assertEquals(project, rightEdge)
    }

    @Test
    fun moveFrameIgnoresUnknownFrame() {
        val project = sampleThreeFrameProject()
        val updated = EditorReducer.moveFrame(project, "missing", 1)

        assertEquals(project, updated)
    }

    @Test
    fun updateSelectedFrameImageReplacesCurrentFrameSource() {
        val updated = EditorReducer.updateSelectedFrameImage(sampleProject(), "https://example.com/generated.jpg")

        assertEquals("https://example.com/generated.jpg", updated.frames.single().originalImageUrl)
        assertEquals("frame_0", updated.selectedFrameId)
    }

    @Test
    fun recordGeneratedImageStoresHistoryAndUpdatesSelectedFrameSource() {
        val updated = EditorReducer.recordGeneratedImageForSelectedFrame(sampleProject(), "generated_1")
        val frame = updated.frames.single()

        assertEquals("generated_1", frame.originalImageUrl)
        assertEquals(listOf("generated_1"), frame.generatedImageUrls)
        assertEquals("frame_0", updated.selectedFrameId)
    }

    @Test
    fun recordGeneratedImageKeepsNewestUniqueHistoryFirst() {
        val project =
            sampleProject().copy(
                frames =
                    listOf(
                        EditorFrame(
                            id = "frame_0",
                            originalImageUrl = "generated_1",
                            generatedImageUrls = listOf("generated_1", "generated_0"),
                        ),
                    ),
            )
        val updated = EditorReducer.recordGeneratedImageForSelectedFrame(project, "generated_0")

        assertEquals(listOf("generated_0", "generated_1"), updated.frames.single().generatedImageUrls)
    }

    @Test
    fun applyGeneratedImageUsesKnownHistoryItem() {
        val project =
            sampleProject().copy(
                frames =
                    listOf(
                        EditorFrame(
                            id = "frame_0",
                            originalImageUrl = "generated_1",
                            generatedImageUrls = listOf("generated_1", "generated_0"),
                        ),
                    ),
            )
        val updated = EditorReducer.applyGeneratedImage(project, "generated_0")

        assertEquals("generated_0", updated.frames.single().originalImageUrl)
    }

    @Test
    fun applyInstantFilterUpdatesSelectedFrame() {
        val updated = EditorReducer.applyInstantFilter(sampleProject(), ImageFilter.OLDFILM)

        assertEquals(ImageFilter.OLDFILM, updated.frames.single().instantFilterName)
    }

    @Test
    fun applySpeechSuggestionsCreatesBubblesFromRelativeRects() {
        val updated =
            EditorReducer.applySpeechSuggestions(
                project = sampleProject(),
                suggestions =
                    listOf(
                        SpeechSuggestion(
                            text = "First generated speech text.",
                            bubbleRect =
                                EditorRect(
                                    x = 54.0,
                                    y = 1080.0,
                                    width = 270.0,
                                    height = 216.0,
                                ),
                        ),
                    ),
            )
        val bubble =
            updated.frames
                .single()
                .layers
                .single() as EditorBubbleLayer

        assertEquals("First generated speech text.", bubble.text)
        assertEquals(EditorRect(54.0, 1080.0, 270.0, 216.0), bubble.bounds)
        assertEquals(BubbleFamily.CLASSIC, bubble.type)
        assertEquals(null, updated.selectedLayerId)
    }

    @Test
    fun applySpeechSuggestionsWithoutRectsUpdatesExistingBubbleTextOnly() {
        val withBubble =
            EditorReducer.applySpeechSuggestions(
                project = sampleProject(),
                suggestions =
                    listOf(
                        SpeechSuggestion(
                            text = "First text",
                            bubbleRect = EditorRect(54.0, 1080.0, 270.0, 216.0),
                        ),
                    ),
            )

        val updated =
            EditorReducer.applySpeechSuggestions(
                project = withBubble,
                suggestions = listOf(SpeechSuggestion(text = "Next text")),
            )
        val bubble =
            updated.frames
                .single()
                .layers
                .single() as EditorBubbleLayer

        assertEquals("Next text", bubble.text)
        assertEquals(EditorRect(54.0, 1080.0, 270.0, 216.0), bubble.bounds)
    }

    @Test
    fun applyGeneratedImageIgnoresUnknownHistoryItem() {
        val project =
            sampleProject().copy(
                frames =
                    listOf(
                        EditorFrame(
                            id = "frame_0",
                            originalImageUrl = "generated_1",
                            generatedImageUrls = listOf("generated_1"),
                        ),
                    ),
            )
        val updated = EditorReducer.applyGeneratedImage(project, "external")

        assertEquals(project, updated)
    }

    @Test
    fun transformSelectedFrameCropUpdatesScaleAndOffset() {
        val updated =
            EditorReducer.transformSelectedFrameCrop(
                project = sampleProject(),
                zoomChange = 2.0,
                panX = 120.0,
                panY = -180.0,
            )
        val crop = updated.frames.single().crop

        assertEquals(2.0, crop.scale)
        assertEquals(120.0, crop.offsetX)
        assertEquals(-180.0, crop.offsetY)
    }

    @Test
    fun transformSelectedFrameCropClampsScaleAndOffset() {
        val updated =
            EditorReducer.transformSelectedFrameCrop(
                project = sampleProject(),
                zoomChange = 10.0,
                panX = 10_000.0,
                panY = -10_000.0,
            )
        val crop = updated.frames.single().crop

        assertEquals(4.0, crop.scale)
        assertEquals(1620.0, crop.offsetX)
        assertEquals(-2160.0, crop.offsetY)
    }

    @Test
    fun resetSelectedFrameCropRestoresDefaultCrop() {
        val cropped =
            EditorReducer.transformSelectedFrameCrop(
                project = sampleProject(),
                zoomChange = 2.0,
                panX = 120.0,
                panY = 180.0,
            )
        val updated = EditorReducer.resetSelectedFrameCrop(cropped)

        assertEquals(EditorCropState(), updated.frames.single().crop)
    }

    @Test
    fun moveLayerMovesSelectedLayerInsideCanvas() {
        val withBubble = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val moved =
            EditorReducer.moveLayer(
                project = withBubble,
                layerId = "bubble_0",
                deltaX = 50.0,
                deltaY = 75.0,
            )
        val bubble =
            moved.frames
                .single()
                .layers
                .single() as EditorBubbleLayer

        assertEquals(353.75, bubble.bounds.x)
        assertEquals(391.8, bubble.bounds.y)
        assertEquals("bubble_0", moved.selectedLayerId)
    }

    @Test
    fun moveLayerClampsBoundsInsideCanvas() {
        val withBubble = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val moved =
            EditorReducer.moveLayer(
                project = withBubble,
                layerId = "bubble_0",
                deltaX = 10_000.0,
                deltaY = 10_000.0,
            )
        val bubble =
            moved.frames
                .single()
                .layers
                .single() as EditorBubbleLayer

        assertEquals(EditorCanvasSpec.LOGICAL_WIDTH - bubble.bounds.width, bubble.bounds.x)
        assertEquals(EditorCanvasSpec.LOGICAL_HEIGHT - bubble.bounds.height, bubble.bounds.y)
    }

    @Test
    fun updateBubbleTextChangesOnlySelectedBubble() {
        val withBubble = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val updated = EditorReducer.updateLayerText(withBubble, "bubble_0", "Updated")
        val bubble =
            updated.frames
                .single()
                .layers
                .single() as EditorBubbleLayer

        assertEquals("Updated", bubble.text)
        assertEquals("bubble_0", updated.selectedLayerId)
    }

    @Test
    fun updateLayerTextChangesLetteringText() {
        val withLetter =
            EditorReducer.addLettering(
                project = sampleProject(),
                style = StyleCatalog.defaultLetteringStyle,
                text = "WOW",
            )
        val updated = EditorReducer.updateLayerText(withLetter, "letter_0", "BANG")
        val letter =
            updated.frames
                .single()
                .layers
                .single() as EditorLetterLayer

        assertEquals("BANG", letter.text)
        assertEquals("letter_0", updated.selectedLayerId)
    }

    @Test
    fun moveBubbleTailMovesTailWithoutMovingBounds() {
        val withBubble = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val moved = EditorReducer.moveBubbleTail(withBubble, "bubble_0", -40.0, 20.0)
        val bubble =
            moved.frames
                .single()
                .layers
                .single() as EditorBubbleLayer

        assertEquals(303.75, bubble.bounds.x)
        assertEquals(500.0, bubble.tailEndPoint.x)
        assertTrue(bubble.tailEndPoint.y > bubble.bounds.y + bubble.bounds.height)
    }

    @Test
    fun moveBubbleTailKeepsTailEndOutsideBubbleForbiddenArea() {
        val withBubble = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val moved = EditorReducer.moveBubbleTail(withBubble, "bubble_0", -180.0, -160.0)
        val bubble =
            moved.frames
                .single()
                .layers
                .single() as EditorBubbleLayer

        assertEquals(360.0, bubble.tailEndPoint.x)
        assertTrue(
            bubble.tailEndPoint.x < bubble.bounds.x ||
                bubble.tailEndPoint.x > bubble.bounds.x + bubble.bounds.width ||
                bubble.tailEndPoint.y < bubble.bounds.y ||
                bubble.tailEndPoint.y > bubble.bounds.y + bubble.bounds.height,
        )
    }

    @Test
    fun movingTailIntoTheBodyUsesIosControlClearanceOnEverySide() {
        val layer =
            EditorBubbleLayer(
                "bubble",
                EditorRect(150.0, 180.0, 472.5, 472.5),
                BubbleFamily.CLASSIC,
                0,
                "Текст",
                FontSize.M,
                EditorPoint(800.0, 900.0),
                true,
            )
        val project = EditorProject(listOf(EditorFrame("frame", layers = listOf(layer))))
        // iOS forbidden inset=6pt, push out=max(32pt/2+2pt,6pt)=18pt.
        val clearance = 24.0 * 1080.0 / 414.0
        val cases =
            listOf(
                EditorPoint(155.0, 400.0) to EditorPoint(150.0 - clearance, 400.0),
                EditorPoint(617.5, 400.0) to EditorPoint(622.5 + clearance, 400.0),
                EditorPoint(400.0, 185.0) to EditorPoint(400.0, 180.0 - clearance),
                EditorPoint(400.0, 647.5) to EditorPoint(400.0, 652.5 + clearance),
            )
        cases.forEach { (candidate, expected) ->
            val result =
                EditorReducer
                    .moveBubbleTail(
                        project,
                        layer.id,
                        candidate.x - layer.tailEndPoint.x,
                        candidate.y - layer.tailEndPoint.y,
                    ).frames
                    .single()
                    .layers
                    .single() as EditorBubbleLayer
            assertEquals(expected.x, result.tailEndPoint.x, .00001)
            assertEquals(expected.y, result.tailEndPoint.y, .00001)
        }
    }

    @Test
    fun resizeLayerUpdatesBoundsAndKeepsSelection() {
        val withBubble = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val resized =
            EditorReducer.resizeLayer(
                project = withBubble,
                layerId = "bubble_0",
                leftDelta = 0.0,
                topDelta = 0.0,
                rightDelta = 120.0,
                bottomDelta = 80.0,
            )
        val bubble =
            resized.frames
                .single()
                .layers
                .single() as EditorBubbleLayer

        assertEquals(303.75, bubble.bounds.x)
        assertEquals(316.8, bubble.bounds.y)
        assertEquals(592.5, bubble.bounds.width)
        assertEquals(552.5, bubble.bounds.height, absoluteTolerance = 0.0001)
        assertEquals("bubble_0", resized.selectedLayerId)
    }

    @Test
    fun resizeLayerClampsToMinimumSizeWhenDraggingLeadingEdges() {
        val withBubble = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val resized =
            EditorReducer.resizeLayer(
                project = withBubble,
                layerId = "bubble_0",
                leftDelta = 10_000.0,
                topDelta = 10_000.0,
                rightDelta = 0.0,
                bottomDelta = 0.0,
            )
        val bubble =
            resized.frames
                .single()
                .layers
                .single() as EditorBubbleLayer

        assertEquals(270.0, bubble.bounds.width)
        assertEquals(198.0, bubble.bounds.height)
        assertTrue(bubble.bounds.x >= 0.0)
        assertTrue(bubble.bounds.y >= 0.0)
    }

    @Test
    fun toggleBubbleTailFlipsTailEnabledFlag() {
        val withBubble = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val updated = EditorReducer.toggleBubbleTail(withBubble, "bubble_0")
        val bubble =
            updated.frames
                .single()
                .layers
                .single() as EditorBubbleLayer

        assertEquals(false, bubble.isTailEnabled)
        assertEquals("bubble_0", updated.selectedLayerId)
    }

    @Test
    fun deleteLayerRemovesLayerAndClearsSelection() {
        val withBubble = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val updated = EditorReducer.deleteLayer(withBubble, "bubble_0")

        assertEquals(emptyList(), updated.frames.single().layers)
        assertEquals(null, updated.selectedLayerId)
    }

    @Test
    fun blankFrameHasNoImageSource() {
        val frame = EditorReducer.addFrame(sampleProject()).selectedFrame!!
        assertEquals(null, frame.originalImageUrl)
        assertTrue(frame.layers.isEmpty())
    }

    @Test
    fun blockedMoveDoesNotPullTheTailAwayFromItsBubble() {
        val initial = EditorReducer.ensureDefaultBubble(sampleProject(), "Text")
        val againstEdge = EditorReducer.moveLayer(initial, "bubble_0", -10000.0, 0.0)
        val before = againstEdge.selectedFrame!!.layers.single() as EditorBubbleLayer
        val after =
            EditorReducer
                .moveLayer(againstEdge, before.id, -100.0, 0.0)
                .selectedFrame!!
                .layers
                .single() as EditorBubbleLayer
        assertEquals(before.bounds, after.bounds)
        assertEquals(before.tailEndPoint, after.tailEndPoint)
    }

    @Test
    fun generatedImageDoesNotApplyTheInputCropTwice() {
        val cropped = EditorReducer.transformSelectedFrameCrop(sampleProject(), 2.0, 120.0, 180.0)
        val filtered = EditorReducer.applyInstantFilter(cropped, ImageFilter.MONO)
        val generated = EditorReducer.recordGeneratedImageForSelectedFrame(filtered, "generated")
        assertEquals(EditorCropState(), generated.selectedFrame!!.crop)
        assertEquals(ImageFilter.NONE, generated.selectedFrame!!.instantFilterName)
        val croppedAgain = EditorReducer.transformSelectedFrameCrop(generated, 3.0, 100.0, 200.0)
        val restored = EditorReducer.applyGeneratedImage(croppedAgain, "generated")
        assertEquals(EditorCropState(), restored.selectedFrame!!.crop)
    }

    private fun sampleProject(): EditorProject =
        EditorProject(
            frames =
                listOf(
                    EditorFrame(
                        id = "frame_0",
                        originalImageUrl = "image",
                    ),
                ),
            selectedFrameId = "frame_0",
        )

    private fun sampleMultiFrameProject(): EditorProject =
        EditorProject(
            frames =
                listOf(
                    EditorFrame(
                        id = "frame_0",
                        originalImageUrl = "image_0",
                        layers =
                            listOf(
                                EditorBubbleLayer(
                                    id = "bubble_0",
                                    bounds = EditorRect(10.0, 10.0, 100.0, 80.0),
                                    type = BubbleFamily.CLASSIC,
                                    bubbleIndex = 0,
                                    text = "Text",
                                    fontSizeName = FontSize.M,
                                    tailEndPoint = EditorPoint(120.0, 90.0),
                                    isTailEnabled = true,
                                ),
                            ),
                    ),
                    EditorFrame(
                        id = "frame_1",
                        originalImageUrl = "image_1",
                    ),
                ),
            selectedFrameId = "frame_0",
        )

    private fun sampleThreeFrameProject(): EditorProject =
        EditorProject(
            frames =
                listOf(
                    EditorFrame(id = "frame_0", originalImageUrl = "image_0"),
                    EditorFrame(id = "frame_1", originalImageUrl = "image_1"),
                    EditorFrame(id = "frame_2", originalImageUrl = "image_2"),
                ),
            selectedFrameId = "frame_0",
        )
}
