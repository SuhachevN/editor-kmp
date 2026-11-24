package com.comixon.editor.rendering

import com.comixon.editor.core.EditorBubbleDimensions

internal object BubbleReferenceMetrics {
    const val IOS_CANVAS_POINTS = EditorBubbleDimensions.IOS_CANVAS_POINTS
    const val POINT_TO_REFERENCE = BubbleGeometry.REFERENCE_WIDTH / IOS_CANVAS_POINTS
    const val POINT_TO_EXPORT = 1080f / IOS_CANVAS_POINTS
    const val MINIMUM_FONT_POINTS = 12f / POINT_TO_REFERENCE

    fun fixedPoints(value: Float): Float = value * POINT_TO_REFERENCE
}
