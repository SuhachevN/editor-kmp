package com.comixon.editor.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EditorProject(
    val frames: List<EditorFrame> = emptyList(),
    val selectedFrameId: String? = frames.firstOrNull()?.id,
    val selectedLayerId: String? = null,
)

@Serializable
data class EditorFrame(
    val id: String,
    val originalImageUrl: String? = null,
    val bubblesImageUrl: String? = null,
    val instantFilterName: ImageFilter = ImageFilter.NONE,
    val generatedImageUrls: List<String> = emptyList(),
    val crop: EditorCropState = EditorCropState(),
    val layers: List<EditorLayer> = emptyList(),
)

@Serializable
data class EditorCropState(
    val scale: Double = 1.0,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val rotationDegrees: Int = 0,
    val isFlippedHorizontally: Boolean = false,
)

@Serializable
sealed interface EditorLayer {
    val id: String
    val bounds: EditorRect
}

@Serializable
@SerialName("bubble")
data class EditorBubbleLayer(
    override val id: String,
    override val bounds: EditorRect,
    val type: BubbleFamily,
    val bubbleIndex: Int,
    val text: String,
    val fontSizeName: FontSize,
    val tailEndPoint: EditorPoint,
    val isTailEnabled: Boolean,
    val imageUrl: String? = null,
    // Editing history, like iOS currentDistanceProcent. Local drafts persist it outside BubbleObject.
    val tailBasePercent: Float? = null,
    val font: EditorFont = EditorFont.COMIXON,
) : EditorLayer

@Serializable
@SerialName("lettering")
data class EditorLetterLayer(
    override val id: String,
    override val bounds: EditorRect,
    val type: LetteringFamily,
    val letterIndex: Int,
    val text: String,
    val fontSizeName: FontSize,
    val font: EditorFont = EditorFont.LETTERING,
) : EditorLayer

@Serializable
data class EditorRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

@Serializable
data class EditorPoint(
    val x: Double,
    val y: Double,
)

object EditorCanvasSpec {
    const val LOGICAL_WIDTH = 1080.0
    const val LOGICAL_HEIGHT = 1440.0
    const val ASPECT_RATIO = LOGICAL_WIDTH / LOGICAL_HEIGHT
}
