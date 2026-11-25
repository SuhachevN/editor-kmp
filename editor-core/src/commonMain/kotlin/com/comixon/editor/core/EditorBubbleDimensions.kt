package com.comixon.editor.core

/** Bubble minimum sizes use the original editor's 320-point coordinate system. */
object EditorBubbleDimensions {
    data class Minimum(
        val width: Double,
        val height: Double,
    )

    // Dimensions are in reference points; saved geometry uses document pixels.
    const val IOS_CANVAS_POINTS = 414f
    const val FIXED_POINT_SCALE = EditorCanvasSpec.LOGICAL_WIDTH / IOS_CANVAS_POINTS
    const val TAIL_FORBIDDEN_INSET = 6.0 * FIXED_POINT_SCALE

    // A 16-point handle radius plus a 2-point gap keeps the tail control outside the bubble.
    const val TAIL_PUSH_OUT = 18.0 * FIXED_POINT_SCALE

    const val DEFAULT_WIDTH = 140.0 * EditorCanvasSpec.LOGICAL_WIDTH / 320.0
    const val DEFAULT_HEIGHT = 105.0 * EditorCanvasSpec.LOGICAL_HEIGHT / 320.0
    const val DEFAULT_TAIL_LENGTH = 50.0 * EditorCanvasSpec.LOGICAL_WIDTH / 320.0

    fun minimum(
        type: BubbleFamily,
        index: Int,
    ): Minimum {
        val i = ((index % 4 + 4) % 4)
        val (width, height) =
            when (type) {
                BubbleFamily.CLASSIC_TWO -> listOf(80 to 44, 125 to 80, 125 to 75, 80 to 37)[i]
                BubbleFamily.ROCKS -> 90 to if (i == 0 || i == 2) 50 else 45
                BubbleFamily.RECTANGLES -> 82 to 35
                BubbleFamily.SCREAMO -> 90 to 55
                BubbleFamily.CORNER -> 85 to if (i == 0 || i == 3) 45 else 40
                else -> listOf(80 to 44, 125 to 75, 125 to 75, 80 to 40)[i]
            }
        return Minimum(
            width * EditorCanvasSpec.LOGICAL_WIDTH / 320.0,
            height * EditorCanvasSpec.LOGICAL_HEIGHT / 320.0,
        )
    }

    fun ensureMinimum(
        rect: EditorRect,
        type: BubbleFamily,
        index: Int,
    ): EditorRect {
        val minimum = minimum(type, index)
        val width = rect.width.coerceIn(minimum.width, EditorCanvasSpec.LOGICAL_WIDTH)
        val height = rect.height.coerceIn(minimum.height, EditorCanvasSpec.LOGICAL_HEIGHT)
        return EditorRect(
            (rect.x + (rect.width - width) / 2).coerceIn(0.0, EditorCanvasSpec.LOGICAL_WIDTH - width),
            (rect.y + (rect.height - height) / 2).coerceIn(0.0, EditorCanvasSpec.LOGICAL_HEIGHT - height),
            width,
            height,
        )
    }
}
