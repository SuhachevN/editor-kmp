package com.comixon.editor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import com.comixon.editor.core.BubbleFamily

/** ShapeView.getImageMaskPath scales by (bounds.size - basisOffset), then offsets by half. */
internal object BubbleImageMask {
    fun path(
        type: BubbleFamily,
        index: Int,
        body: Path,
        frame: Rect,
        detailScale: Float,
    ): Path {
        val mask = Path().apply { addPath(body, Offset(-frame.left, -frame.top)) }
        val bounds = mask.getBounds()
        if (bounds.width <= 0f || bounds.height <= 0f) return Path()
        val inset = BubbleStyleMetrics.basisOffset(type, index) * detailScale
        mask.transform(
            scaleMatrix(
                (bounds.width - inset).coerceAtLeast(.01f) / bounds.width,
                (bounds.height - inset).coerceAtLeast(.01f) / bounds.height,
            ),
        )
        mask.translate(Offset(frame.left + inset / 2f, frame.top + inset / 2f))
        return Path.combine(PathOperation.Intersect, mask, body)
    }
}
