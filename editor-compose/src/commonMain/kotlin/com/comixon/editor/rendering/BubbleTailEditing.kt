package com.comixon.editor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorProject
import com.comixon.editor.core.EditorReducer

/** Native path calculations stay outside the domain editor; history travels with its immutable state. */
object BubbleTailEditing {
    fun moveLayer(
        project: EditorProject,
        layerId: String,
        deltaX: Double,
        deltaY: Double,
    ): EditorProject = reconcile(project, EditorReducer.moveLayer(project, layerId, deltaX, deltaY))

    fun resizeLayer(
        project: EditorProject,
        layerId: String,
        leftDelta: Double,
        topDelta: Double,
        rightDelta: Double,
        bottomDelta: Double,
    ): EditorProject =
        reconcile(
            project,
            EditorReducer.resizeLayer(project, layerId, leftDelta, topDelta, rightDelta, bottomDelta),
        )

    fun moveBubbleTail(
        project: EditorProject,
        layerId: String,
        deltaX: Double,
        deltaY: Double,
    ): EditorProject = reconcile(project, EditorReducer.moveBubbleTail(project, layerId, deltaX, deltaY))

    private fun reconcile(
        before: EditorProject,
        after: EditorProject,
    ): EditorProject =
        after.copy(
            frames =
                after.frames.map { frame ->
                    val previous = before.frames.firstOrNull { it.id == frame.id } ?: return@map frame
                    frame.copy(
                        layers =
                            frame.layers.map layers@{ layer ->
                                if (layer !is EditorBubbleLayer) return@layers layer
                                val old =
                                    previous.layers.firstOrNull { it.id == layer.id } as? EditorBubbleLayer
                                        ?: return@layers layer
                                if (old == layer) return@layers layer
                                val resized = old.bounds.width != layer.bounds.width || old.bounds.height != layer.bounds.height
                                val moved = old.bounds.x != layer.bounds.x || old.bounds.y != layer.bounds.y
                                // Moving the entire unedited bubble also translates the initial control.
                                // Materializing its percentage here would settle Noise onto the inset path.
                                if (moved &&
                                    !resized &&
                                    old.tailBasePercent == null &&
                                    kotlin.math.abs(
                                        (old.tailEndPoint.x - old.bounds.x) -
                                            (layer.tailEndPoint.x - layer.bounds.x),
                                    ) < .0001 &&
                                    kotlin.math.abs(
                                        (old.tailEndPoint.y - old.bounds.y) -
                                            (layer.tailEndPoint.y - layer.bounds.y),
                                    ) < .0001
                                ) {
                                    return@layers layer
                                }
                                val anchor = anchor(old)
                                val percent =
                                    when {
                                        resized -> anchor(layer, anchor.percent).resizedPercent()
                                        moved -> anchor(layer, anchor.percent).draggedPercent(localEnd(layer))
                                        old.tailEndPoint != layer.tailEndPoint -> anchor.draggedPercent(localEnd(layer))
                                        else -> old.tailBasePercent
                                    }
                                layer.copy(tailBasePercent = percent)
                            },
                    )
                },
        )

    private fun localEnd(layer: EditorBubbleLayer) =
        Offset(
            ((layer.tailEndPoint.x - layer.bounds.x) / BubbleGeometry.EXPORT_SCALE).toFloat(),
            ((layer.tailEndPoint.y - layer.bounds.y) / BubbleGeometry.EXPORT_SCALE).toFloat(),
        )

    private fun anchor(
        layer: EditorBubbleLayer,
        percent: Float? = layer.tailBasePercent,
    ) = BubbleTailAnchor(
        Rect(
            0f,
            0f,
            (layer.bounds.width / BubbleGeometry.EXPORT_SCALE).toFloat(),
            (layer.bounds.height / BubbleGeometry.EXPORT_SCALE).toFloat(),
        ),
        layer.type,
        layer.bubbleIndex,
        localEnd(layer),
        percent,
    )
}
