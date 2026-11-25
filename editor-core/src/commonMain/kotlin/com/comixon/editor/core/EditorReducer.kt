package com.comixon.editor.core

import kotlin.math.abs

object EditorReducer {
    fun addDefaultBubble(
        project: EditorProject,
        text: String,
    ): EditorProject =
        addBubble(
            project = project,
            style = StyleCatalog.defaultBubbleStyle,
            text = text,
        )

    fun addBubble(
        project: EditorProject,
        style: BubbleStyle,
        text: String,
    ): EditorProject {
        val selectedFrame = project.selectedFrame() ?: return project
        val layerId = nextLayerId(selectedFrame, prefix = "bubble")
        val bubble =
            EditorBubbleLayer(
                id = layerId,
                bounds =
                    EditorRect(
                        x = DEFAULT_BUBBLE_X,
                        y = DEFAULT_BUBBLE_Y,
                        width = DEFAULT_BUBBLE_WIDTH,
                        height = DEFAULT_BUBBLE_HEIGHT,
                    ),
                type = style.type,
                bubbleIndex = style.index,
                text = text,
                fontSizeName = FontSize.M,
                tailEndPoint =
                    EditorPoint(
                        x = DEFAULT_BUBBLE_X + DEFAULT_BUBBLE_WIDTH / 2,
                        y = DEFAULT_BUBBLE_Y + DEFAULT_BUBBLE_HEIGHT + EditorBubbleDimensions.DEFAULT_TAIL_LENGTH,
                    ),
                isTailEnabled = true,
            )
        return project
            .updateSelectedFrame {
                it.copy(layers = it.layers + bubble)
            }.copy(selectedLayerId = layerId)
    }

    fun addLettering(
        project: EditorProject,
        style: LetteringStyle,
        text: String,
    ): EditorProject {
        val selectedFrame = project.selectedFrame() ?: return project
        val layerId = nextLayerId(selectedFrame, prefix = "letter")
        val letter =
            EditorLetterLayer(
                id = layerId,
                bounds =
                    EditorRect(
                        x = DEFAULT_LETTER_X,
                        y = DEFAULT_LETTER_Y,
                        width = DEFAULT_LETTER_WIDTH,
                        height = DEFAULT_LETTER_HEIGHT,
                    ),
                type = style.type,
                letterIndex = style.index,
                text = text,
                fontSizeName = FontSize.L,
            )
        return project
            .updateSelectedFrame {
                it.copy(layers = it.layers + letter)
            }.copy(selectedLayerId = layerId)
    }

    fun updateBubbleStyle(
        project: EditorProject,
        layerId: String,
        style: BubbleStyle,
    ): EditorProject =
        project
            .updateSelectedFrame { frame ->
                frame.copy(
                    layers =
                        frame.layers.map { layer ->
                            if (layer is EditorBubbleLayer && layer.id == layerId) {
                                layer.copy(
                                    type = style.type,
                                    bubbleIndex = style.index,
                                    tailBasePercent = null,
                                    bounds =
                                        EditorBubbleDimensions.ensureMinimum(
                                            layer.bounds,
                                            style.type,
                                            style.index,
                                        ),
                                )
                            } else {
                                layer
                            }
                        },
                )
            }.copy(selectedLayerId = layerId)

    fun updateLetteringStyle(
        project: EditorProject,
        layerId: String,
        style: LetteringStyle,
    ): EditorProject =
        project
            .updateSelectedFrame { frame ->
                frame.copy(
                    layers =
                        frame.layers.map { layer ->
                            if (layer is EditorLetterLayer && layer.id == layerId) {
                                layer.copy(
                                    type = style.type,
                                    letterIndex = style.index,
                                )
                            } else {
                                layer
                            }
                        },
                )
            }.copy(selectedLayerId = layerId)

    fun ensureDefaultBubble(
        project: EditorProject,
        text: String,
    ): EditorProject {
        val selectedFrame = project.selectedFrame() ?: return project
        val existingBubble = selectedFrame.layers.filterIsInstance<EditorBubbleLayer>().firstOrNull()
        return if (existingBubble != null) {
            project.copy(selectedLayerId = existingBubble.id)
        } else {
            addDefaultBubble(project, text)
        }
    }

    fun selectLayer(
        project: EditorProject,
        layerId: String?,
    ): EditorProject = project.copy(selectedLayerId = layerId)

    fun selectFrame(
        project: EditorProject,
        frameId: String,
    ): EditorProject {
        val exists = project.frames.any { it.id == frameId }
        return if (exists) {
            project.copy(
                selectedFrameId = frameId,
                selectedLayerId = null,
            )
        } else {
            project
        }
    }

    fun addFrame(
        project: EditorProject,
        imageUrl: String? = null,
    ): EditorProject {
        val frameId = nextFrameId(project.frames)
        val frame =
            EditorFrame(
                id = frameId,
                originalImageUrl = imageUrl,
            )
        return project.copy(
            frames = project.frames + frame,
            selectedFrameId = frameId,
            selectedLayerId = null,
        )
    }

    fun deleteFrame(
        project: EditorProject,
        frameId: String,
    ): EditorProject {
        val index = project.frames.indexOfFirst { it.id == frameId }
        if (index == -1) return project

        val updatedFrames = project.frames.filterNot { it.id == frameId }
        val nextSelectedFrameId =
            if (project.selectedFrameId == frameId) {
                updatedFrames.getOrNull(index)?.id ?: updatedFrames.getOrNull(index - 1)?.id
            } else {
                project.selectedFrameId
            }
        return project.copy(
            frames = updatedFrames,
            selectedFrameId = nextSelectedFrameId,
            selectedLayerId = if (project.selectedFrameId == frameId) null else project.selectedLayerId,
        )
    }

    fun moveFrame(
        project: EditorProject,
        frameId: String,
        offset: Int,
    ): EditorProject {
        if (offset == 0) return project

        val fromIndex = project.frames.indexOfFirst { it.id == frameId }
        if (fromIndex == -1) return project

        val toIndex = (fromIndex + offset).coerceIn(0, project.frames.lastIndex)
        if (fromIndex == toIndex) return project

        val reorderedFrames = project.frames.toMutableList()
        val frame = reorderedFrames.removeAt(fromIndex)
        reorderedFrames.add(toIndex, frame)
        return project.copy(frames = reorderedFrames)
    }

    fun updateSelectedFrameImage(
        project: EditorProject,
        imageUrl: String,
    ): EditorProject =
        project.updateSelectedFrame { frame ->
            frame.copy(originalImageUrl = imageUrl)
        }

    fun recordGeneratedImageForSelectedFrame(
        project: EditorProject,
        imageUrl: String,
    ): EditorProject =
        project.updateSelectedFrame { frame ->
            frame.copy(
                originalImageUrl = imageUrl,
                crop = EditorCropState(),
                instantFilterName = ImageFilter.NONE,
                generatedImageUrls =
                    (listOf(imageUrl) + frame.generatedImageUrls)
                        .distinct()
                        .take(MAX_GENERATED_HISTORY),
            )
        }

    fun applyGeneratedImage(
        project: EditorProject,
        imageUrl: String,
    ): EditorProject =
        project.updateSelectedFrame { frame ->
            val knownImage = imageUrl in frame.generatedImageUrls
            if (knownImage) {
                frame.copy(
                    originalImageUrl = imageUrl,
                    crop = EditorCropState(),
                    instantFilterName = ImageFilter.NONE,
                )
            } else {
                frame
            }
        }

    fun applyInstantFilter(
        project: EditorProject,
        filter: ImageFilter,
    ): EditorProject = project.updateSelectedFrame { it.copy(instantFilterName = filter) }

    fun applySpeechSuggestions(
        project: EditorProject,
        suggestions: List<SpeechSuggestion>,
    ): EditorProject {
        if (suggestions.isEmpty()) return project

        return project
            .updateSelectedFrame { frame ->
                val existingBubbles = frame.layers.filterIsInstance<EditorBubbleLayer>()
                val nonBubbleLayers = frame.layers.filterNot { it is EditorBubbleLayer }
                val updatedBubbles =
                    suggestions.mapIndexed { index, suggestion ->
                        val existing = existingBubbles.getOrNull(index)
                        val bounds = suggestion.bubbleRect ?: existing?.bounds ?: defaultSpeechBubbleBounds(index)
                        val tailEndPoint = existing?.tailEndPoint ?: defaultSpeechBubbleTail(bounds)
                        existing?.copy(
                            text = suggestion.text,
                            bounds = bounds,
                            tailEndPoint = suggestion.bubbleRect?.let(::defaultSpeechBubbleTail) ?: tailEndPoint,
                            tailBasePercent = if (suggestion.bubbleRect != null) null else existing.tailBasePercent,
                            isTailEnabled = true,
                            imageUrl = null,
                        ) ?: EditorBubbleLayer(
                            id = nextLayerId(frame, prefix = "bubble", extraOffset = index),
                            bounds = bounds,
                            type = BubbleFamily.CLASSIC,
                            bubbleIndex = 0,
                            text = suggestion.text,
                            fontSizeName = FontSize.S,
                            tailEndPoint = defaultSpeechBubbleTail(bounds),
                            isTailEnabled = true,
                        )
                    }

                frame.copy(
                    layers = nonBubbleLayers + updatedBubbles + existingBubbles.drop(suggestions.size),
                )
            }.copy(selectedLayerId = null)
    }

    fun updateBubbleImage(
        project: EditorProject,
        layerId: String,
        imageUrl: String,
    ): EditorProject =
        project
            .updateSelectedFrame { frame ->
                frame.copy(
                    layers =
                        frame.layers.map { layer ->
                            if (layer is EditorBubbleLayer && layer.id == layerId) {
                                layer.copy(
                                    text = "",
                                    imageUrl = imageUrl,
                                )
                            } else {
                                layer
                            }
                        },
                )
            }.copy(selectedLayerId = layerId)

    fun transformSelectedFrameCrop(
        project: EditorProject,
        zoomChange: Double,
        panX: Double,
        panY: Double,
    ): EditorProject =
        project.updateSelectedFrame { frame ->
            val nextScale = (frame.crop.scale * zoomChange).coerceIn(MIN_CROP_SCALE, MAX_CROP_SCALE)
            val maxOffsetX = EditorCanvasSpec.LOGICAL_WIDTH * (nextScale - 1.0) / 2.0
            val maxOffsetY = EditorCanvasSpec.LOGICAL_HEIGHT * (nextScale - 1.0) / 2.0
            frame.copy(
                crop =
                    frame.crop.copy(
                        scale = nextScale,
                        offsetX = (frame.crop.offsetX + panX).coerceIn(-maxOffsetX, maxOffsetX),
                        offsetY = (frame.crop.offsetY + panY).coerceIn(-maxOffsetY, maxOffsetY),
                    ),
            )
        }

    fun resetSelectedFrameCrop(project: EditorProject): EditorProject =
        project.updateSelectedFrame { frame ->
            frame.copy(crop = EditorCropState())
        }

    fun rotateSelectedFrameCrop(
        project: EditorProject,
        degrees: Int,
    ): EditorProject =
        project.updateSelectedFrame { frame ->
            frame.copy(
                crop =
                    frame.crop.copy(
                        rotationDegrees = (frame.crop.rotationDegrees + degrees).floorMod(360),
                    ),
            )
        }

    fun flipSelectedFrameCrop(project: EditorProject): EditorProject =
        project.updateSelectedFrame { frame ->
            frame.copy(
                crop =
                    frame.crop.copy(
                        isFlippedHorizontally = !frame.crop.isFlippedHorizontally,
                    ),
            )
        }

    fun moveLayer(
        project: EditorProject,
        layerId: String,
        deltaX: Double,
        deltaY: Double,
    ): EditorProject =
        project
            .updateSelectedFrame { frame ->
                frame.copy(
                    layers =
                        frame.layers.map { layer ->
                            if (layer.id == layerId) layer.moveBy(deltaX, deltaY) else layer
                        },
                )
            }.copy(selectedLayerId = layerId)

    fun resizeLayer(
        project: EditorProject,
        layerId: String,
        leftDelta: Double,
        topDelta: Double,
        rightDelta: Double,
        bottomDelta: Double,
    ): EditorProject =
        project
            .updateSelectedFrame { frame ->
                frame.copy(
                    layers =
                        frame.layers.map { layer ->
                            if (layer.id == layerId) {
                                layer.resizeBy(
                                    leftDelta = leftDelta,
                                    topDelta = topDelta,
                                    rightDelta = rightDelta,
                                    bottomDelta = bottomDelta,
                                )
                            } else {
                                layer
                            }
                        },
                )
            }.copy(selectedLayerId = layerId)

    fun moveBubbleTail(
        project: EditorProject,
        layerId: String,
        deltaX: Double,
        deltaY: Double,
    ): EditorProject =
        project
            .updateSelectedFrame { frame ->
                frame.copy(
                    layers =
                        frame.layers.map { layer ->
                            if (layer is EditorBubbleLayer && layer.id == layerId) {
                                val nextTailEndPoint =
                                    layer.correctedTailEndPoint(
                                        deltaX = deltaX,
                                        deltaY = deltaY,
                                    )
                                layer.copy(
                                    tailEndPoint = nextTailEndPoint,
                                    isTailEnabled = true,
                                )
                            } else {
                                layer
                            }
                        },
                )
            }.copy(selectedLayerId = layerId)

    fun toggleBubbleTail(
        project: EditorProject,
        layerId: String,
    ): EditorProject =
        project
            .updateSelectedFrame { frame ->
                frame.copy(
                    layers =
                        frame.layers.map { layer ->
                            if (layer is EditorBubbleLayer && layer.id == layerId) {
                                layer.copy(isTailEnabled = !layer.isTailEnabled)
                            } else {
                                layer
                            }
                        },
                )
            }.copy(selectedLayerId = layerId)

    @Suppress("unused")
    fun updateBubbleText(
        project: EditorProject,
        layerId: String,
        text: String,
    ): EditorProject = updateLayerText(project, layerId, text)

    fun updateLayerText(
        project: EditorProject,
        layerId: String,
        text: String,
        fontSizeName: FontSize? = null,
        font: EditorFont? = null,
    ): EditorProject =
        project
            .updateSelectedFrame { frame ->
                frame.copy(
                    layers =
                        frame.layers.map { layer ->
                            when {
                                layer is EditorBubbleLayer && layer.id == layerId ->
                                    layer.copy(
                                        text = text,
                                        fontSizeName = fontSizeName ?: layer.fontSizeName,
                                        font = font ?: layer.font,
                                        imageUrl = if (text.isNotBlank()) null else layer.imageUrl,
                                    )
                                layer is EditorLetterLayer && layer.id == layerId ->
                                    layer.copy(
                                        text = text,
                                        fontSizeName = fontSizeName ?: layer.fontSizeName,
                                        font = font ?: layer.font,
                                    )
                                else -> layer
                            }
                        },
                )
            }.copy(selectedLayerId = layerId)

    fun deleteLayer(
        project: EditorProject,
        layerId: String,
    ): EditorProject =
        project
            .updateSelectedFrame { frame ->
                frame.copy(layers = frame.layers.filterNot { it.id == layerId })
            }.copy(selectedLayerId = null)

    private fun EditorProject.selectedFrame(): EditorFrame? =
        frames.firstOrNull { it.id == selectedFrameId } ?: frames.firstOrNull()

    private fun EditorProject.updateSelectedFrame(transform: (EditorFrame) -> EditorFrame): EditorProject {
        val frame = selectedFrame() ?: return this
        return copy(
            frames =
                frames.map {
                    if (it.id == frame.id) transform(it) else it
                },
            selectedFrameId = frame.id,
        )
    }

    private fun EditorLayer.moveBy(
        deltaX: Double,
        deltaY: Double,
    ): EditorLayer {
        val movedBounds = bounds.moveBy(deltaX, deltaY)
        return when (this) {
            is EditorBubbleLayer ->
                copy(
                    bounds = movedBounds,
                    tailEndPoint =
                        tailEndPoint.copy(
                            x =
                                (tailEndPoint.x + movedBounds.x - bounds.x).coerceIn(
                                    0.0,
                                    EditorCanvasSpec.LOGICAL_WIDTH,
                                ),
                            y =
                                (tailEndPoint.y + movedBounds.y - bounds.y).coerceIn(
                                    0.0,
                                    EditorCanvasSpec.LOGICAL_HEIGHT,
                                ),
                        ),
                )
            is EditorLetterLayer -> copy(bounds = movedBounds)
        }
    }

    private fun EditorLayer.resizeBy(
        leftDelta: Double,
        topDelta: Double,
        rightDelta: Double,
        bottomDelta: Double,
    ): EditorLayer {
        val minimum = (this as? EditorBubbleLayer)?.let { EditorBubbleDimensions.minimum(it.type, it.bubbleIndex) }
        val resizedBounds =
            bounds.resizeBy(
                leftDelta = leftDelta,
                topDelta = topDelta,
                rightDelta = rightDelta,
                bottomDelta = bottomDelta,
                minimumWidth = minimum?.width ?: MIN_LAYER_SIZE,
                minimumHeight = minimum?.height ?: MIN_LAYER_SIZE,
            )
        return when (this) {
            is EditorBubbleLayer -> copy(bounds = resizedBounds)
            is EditorLetterLayer -> copy(bounds = resizedBounds)
        }
    }

    private fun EditorBubbleLayer.correctedTailEndPoint(
        deltaX: Double,
        deltaY: Double,
    ): EditorPoint {
        val candidate =
            EditorPoint(
                x = (tailEndPoint.x + deltaX).coerceIn(0.0, EditorCanvasSpec.LOGICAL_WIDTH),
                y = (tailEndPoint.y + deltaY).coerceIn(0.0, EditorCanvasSpec.LOGICAL_HEIGHT),
            )
        return bounds.correctPointOutside(candidate, inset = TAIL_FORBIDDEN_AREA_INSET)
    }

    private fun EditorRect.moveBy(
        deltaX: Double,
        deltaY: Double,
    ): EditorRect =
        copy(
            x = (x + deltaX).coerceIn(0.0, EditorCanvasSpec.LOGICAL_WIDTH - width),
            y = (y + deltaY).coerceIn(0.0, EditorCanvasSpec.LOGICAL_HEIGHT - height),
        )

    private fun EditorRect.correctPointOutside(
        point: EditorPoint,
        inset: Double,
    ): EditorPoint {
        val left = x - inset
        val top = y - inset
        val right = x + width + inset
        val bottom = y + height + inset
        if (point.x !in left..right || point.y !in top..bottom) {
            return point
        }

        val leftDistance = abs(point.x - left)
        val rightDistance = abs(right - point.x)
        val topDistance = abs(point.y - top)
        val bottomDistance = abs(bottom - point.y)
        val minDistance = minOf(leftDistance, rightDistance, topDistance, bottomDistance)
        val pushOutDistance = TAIL_FORBIDDEN_AREA_PUSH_OUT

        return when (minDistance) {
            leftDistance -> point.copy(x = (left - pushOutDistance).coerceIn(0.0, EditorCanvasSpec.LOGICAL_WIDTH))
            rightDistance -> point.copy(x = (right + pushOutDistance).coerceIn(0.0, EditorCanvasSpec.LOGICAL_WIDTH))
            topDistance -> point.copy(y = (top - pushOutDistance).coerceIn(0.0, EditorCanvasSpec.LOGICAL_HEIGHT))
            else -> point.copy(y = (bottom + pushOutDistance).coerceIn(0.0, EditorCanvasSpec.LOGICAL_HEIGHT))
        }
    }

    private fun EditorRect.resizeBy(
        leftDelta: Double,
        topDelta: Double,
        rightDelta: Double,
        bottomDelta: Double,
        minimumWidth: Double,
        minimumHeight: Double,
    ): EditorRect {
        val isLeftEdgeActive = leftDelta != 0.0
        val isTopEdgeActive = topDelta != 0.0
        val isRightEdgeActive = rightDelta != 0.0
        val isBottomEdgeActive = bottomDelta != 0.0
        var left = (x + leftDelta).coerceIn(0.0, EditorCanvasSpec.LOGICAL_WIDTH)
        var top = (y + topDelta).coerceIn(0.0, EditorCanvasSpec.LOGICAL_HEIGHT)
        var right = (x + width + rightDelta).coerceIn(0.0, EditorCanvasSpec.LOGICAL_WIDTH)
        var bottom = (y + height + bottomDelta).coerceIn(0.0, EditorCanvasSpec.LOGICAL_HEIGHT)

        if (right - left < minimumWidth) {
            if (isLeftEdgeActive && !isRightEdgeActive) {
                left = (right - minimumWidth).coerceAtLeast(0.0)
            } else {
                right = (left + minimumWidth).coerceAtMost(EditorCanvasSpec.LOGICAL_WIDTH)
            }
        }
        if (bottom - top < minimumHeight) {
            if (isTopEdgeActive && !isBottomEdgeActive) {
                top = (bottom - minimumHeight).coerceAtLeast(0.0)
            } else {
                bottom = (top + minimumHeight).coerceAtMost(EditorCanvasSpec.LOGICAL_HEIGHT)
            }
        }

        left = left.coerceIn(0.0, EditorCanvasSpec.LOGICAL_WIDTH - minimumWidth)
        top = top.coerceIn(0.0, EditorCanvasSpec.LOGICAL_HEIGHT - minimumHeight)
        right = right.coerceIn(left + minimumWidth, EditorCanvasSpec.LOGICAL_WIDTH)
        bottom = bottom.coerceIn(top + minimumHeight, EditorCanvasSpec.LOGICAL_HEIGHT)

        return copy(
            x = left,
            y = top,
            width = right - left,
            height = bottom - top,
        )
    }

    private fun nextLayerId(
        frame: EditorFrame,
        prefix: String,
        extraOffset: Int = 0,
    ): String {
        val usedIds = frame.layers.mapTo(mutableSetOf()) { it.id }
        var nextIndex = frame.layers.count { it.id.startsWith(prefix) } + extraOffset
        while ("${prefix}_$nextIndex" in usedIds) {
            nextIndex += 1
        }
        return "${prefix}_$nextIndex"
    }

    private fun defaultSpeechBubbleBounds(index: Int): EditorRect {
        val column = index % 2
        val row = index / 2
        val width = DEFAULT_BUBBLE_WIDTH
        val height = DEFAULT_BUBBLE_HEIGHT
        return EditorRect(
            x =
                (96.0 + column * (EditorCanvasSpec.LOGICAL_WIDTH - width - 192.0))
                    .coerceIn(0.0, EditorCanvasSpec.LOGICAL_WIDTH - width),
            y =
                (160.0 + row * 220.0)
                    .coerceIn(0.0, EditorCanvasSpec.LOGICAL_HEIGHT - height),
            width = width,
            height = height,
        )
    }

    private fun defaultSpeechBubbleTail(bounds: EditorRect): EditorPoint =
        EditorPoint(
            x = (bounds.x + bounds.width * 0.75).coerceIn(0.0, EditorCanvasSpec.LOGICAL_WIDTH),
            y = (bounds.y + bounds.height * 1.25).coerceIn(0.0, EditorCanvasSpec.LOGICAL_HEIGHT),
        )

    private fun nextFrameId(frames: List<EditorFrame>): String {
        val usedIds = frames.mapTo(mutableSetOf()) { it.id }
        var index = frames.size
        while ("frame_$index" in usedIds) {
            index += 1
        }
        return "frame_$index"
    }

    private fun Int.floorMod(mod: Int): Int = ((this % mod) + mod) % mod

    private const val DEFAULT_BUBBLE_WIDTH = EditorBubbleDimensions.DEFAULT_WIDTH
    private const val DEFAULT_BUBBLE_HEIGHT = EditorBubbleDimensions.DEFAULT_HEIGHT
    private const val DEFAULT_BUBBLE_X = (EditorCanvasSpec.LOGICAL_WIDTH - DEFAULT_BUBBLE_WIDTH) / 2.0
    private const val DEFAULT_BUBBLE_Y = EditorCanvasSpec.LOGICAL_HEIGHT * 0.22
    private const val DEFAULT_LETTER_WIDTH = 420.0
    private const val DEFAULT_LETTER_HEIGHT = 140.0
    private const val DEFAULT_LETTER_X = (EditorCanvasSpec.LOGICAL_WIDTH - DEFAULT_LETTER_WIDTH) / 2.0
    private const val DEFAULT_LETTER_Y = EditorCanvasSpec.LOGICAL_HEIGHT * 0.42
    private const val MAX_GENERATED_HISTORY = 10
    private const val MIN_CROP_SCALE = 1.0
    private const val MAX_CROP_SCALE = 4.0
    private const val MIN_LAYER_SIZE = 70.0
    private const val TAIL_FORBIDDEN_AREA_INSET = EditorBubbleDimensions.TAIL_FORBIDDEN_INSET
    private const val TAIL_FORBIDDEN_AREA_PUSH_OUT = EditorBubbleDimensions.TAIL_PUSH_OUT
}
