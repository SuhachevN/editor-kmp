package com.comixon.editor.rendering

import com.comixon.editor.core.BubbleFamily

/** DrawConfigurator.basisOffset: shared by the text container and the inset image mask. */
internal object BubbleStyleMetrics {
    fun basisOffset(
        type: BubbleFamily,
        index: Int,
    ): Float {
        val i = ((index % 4 + 4) % 4)
        return when (type) {
            BubbleFamily.CLASSIC_TWO -> floatArrayOf(8f, 30f, 25f, 5f)[i]
            BubbleFamily.ROCKS -> floatArrayOf(5f, 4f, 1f, 4f)[i]
            BubbleFamily.RECTANGLES -> 14f
            BubbleFamily.SCREAMO -> BubbleReferenceMetrics.fixedPoints(30f)
            BubbleFamily.CORNER ->
                if (i == 2) {
                    18f
                } else {
                    BubbleReferenceMetrics.fixedPoints(floatArrayOf(25f, 20f, 18f, 22f)[i])
                }
            else -> floatArrayOf(8f, 35f, 35f, 5f * 4f / 3f)[i]
        }
    }
}
