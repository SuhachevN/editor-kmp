package com.comixon.editor.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorFont
import com.comixon.editor.core.EditorLayer
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.rendering.BubbleTailEditing
import com.comixon.editor.rendering.FrameScene
import com.comixon.editor.rendering.drawEditorFrame
import com.comixon.editor.rendering.prepareFrameScene
import com.comixon.editor.resources.Res
import com.comixon.editor.resources.editor_bubble_resize
import org.jetbrains.compose.resources.painterResource

@Composable
fun rememberEditorTextMeasurer(): TextMeasurer {
    val resolver = LocalFontFamilyResolver.current
    return remember(resolver) { TextMeasurer(resolver, Density(1f, 1f), LayoutDirection.Ltr, cacheSize = 128) }
}

@Composable
fun rememberFrameScene(
    state: EditorState,
    images: Map<String, ImageBitmap>,
    families: Map<EditorFont, FontFamily>,
    measurer: TextMeasurer,
): FrameScene? {
    val frame = state.project.selectedFrame
    return remember(frame, images, families, measurer) { frame?.let { prepareFrameScene(it, images, families, measurer) } }
}

/** Coordinates and text stay in a 1080 × 1440 document, independent of display density. */
@Composable
fun EditorCanvas(
    state: EditorState,
    scene: FrameScene?,
    modifier: Modifier = Modifier,
    cropMode: Boolean = false,
    alignCanvasTop: Boolean = false,
    preserveCanvasWidth: Boolean = false,
) {
    val resizePainter = painterResource(Res.drawable.editor_bubble_resize)
    BoxWithConstraints(modifier, contentAlignment = if (alignCanvasTop) Alignment.TopCenter else Alignment.Center) {
        val width = if (preserveCanvasWidth) maxWidth else minOf(maxWidth, maxHeight * .75f)
        Canvas(
            Modifier
                .size(width, width / .75f)
                .semantics { contentDescription = "Comic editor canvas" }
                .pointerInput(state, cropMode) {
                    if (!cropMode) return@pointerInput
                    try {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            state.beginGesture()
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                            } while (event.changes.any { it.pressed })
                            state.endGesture()
                        }
                    } finally {
                        state.endGesture()
                    }
                }.pointerInput(state, cropMode) {
                    if (cropMode) return@pointerInput
                    detectTapGestures { position ->
                        val p = position / (size.width / 1080f)
                        val hit =
                            state.project.selectedFrame
                                ?.layers
                                ?.lastOrNull { it.hitBounds().contains(p) }
                        state.select { EditorReducer.selectLayer(it, hit?.id) }
                    }
                }.pointerInput(state, cropMode) {
                    if (cropMode) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val scale = size.width / 1080f
                            state.edit {
                                EditorReducer.transformSelectedFrameCrop(
                                    it,
                                    zoom.toDouble(),
                                    (pan.x / scale).toDouble(),
                                    (pan.y / scale).toDouble(),
                                )
                            }
                        }
                    } else {
                        var active: String? = null
                        var mode = DragMode.MOVE
                        var resizeCorner = ResizeCorner.BOTTOM_END
                        detectDragGestures(
                            onDragStart = { position ->
                                val scale = size.width / 1080f
                                val p = position / scale
                                val radius = 24.dp.toPx() / scale
                                val layers =
                                    state.project.selectedFrame
                                        ?.layers
                                        .orEmpty()
                                val selected = layers.firstOrNull { it.id == state.project.selectedLayerId }
                                val tail = selected as? EditorBubbleLayer
                                val corner =
                                    selected?.let { layer ->
                                        resizeCorners(layer).firstOrNull { (p - it.position(layer.hitBounds())).getDistance() <= radius }
                                    }
                                mode =
                                    when {
                                        tail != null &&
                                            tail.isTailEnabled &&
                                            (p - tail.tailOffset()).getDistance() <= radius -> DragMode.TAIL
                                        corner != null -> DragMode.RESIZE
                                        else -> DragMode.MOVE
                                    }
                                if (corner != null) resizeCorner = corner
                                val hit = if (mode == DragMode.MOVE) layers.lastOrNull { it.hitBounds().contains(p) } else selected
                                active = hit?.id
                                state.select { EditorReducer.selectLayer(it, active) }
                                if (active != null) state.beginGesture()
                            },
                            onDragEnd = {
                                state.endGesture()
                                active = null
                            },
                            onDragCancel = {
                                state.endGesture()
                                active = null
                            },
                        ) { change, delta ->
                            val id = active ?: return@detectDragGestures
                            change.consume()
                            val scale = size.width / 1080f
                            val dx = (delta.x / scale).toDouble()
                            val dy = (delta.y / scale).toDouble()
                            state.edit { project ->
                                when (mode) {
                                    DragMode.MOVE -> BubbleTailEditing.moveLayer(project, id, dx, dy)
                                    DragMode.RESIZE ->
                                        BubbleTailEditing.resizeLayer(
                                            project,
                                            id,
                                            if (resizeCorner.left) dx else 0.0,
                                            if (resizeCorner.top) dy else 0.0,
                                            if (!resizeCorner.left) dx else 0.0,
                                            if (!resizeCorner.top) dy else 0.0,
                                        )
                                    DragMode.TAIL -> BubbleTailEditing.moveBubbleTail(project, id, dx, dy)
                                }
                            }
                        }
                    }
                },
        ) {
            val ratio = size.width / 1080f
            scale(ratio, ratio, Offset.Zero) {
                scene?.let { drawEditorFrame(it) }
                if (!cropMode) {
                    state.project.selectedFrame?.layers?.firstOrNull { it.id == state.project.selectedLayerId }?.let { layer ->
                        val rect = layer.hitBounds()
                        val color = Color(0xFFFF3B30)
                        drawRect(color, rect.topLeft, rect.size, style = Stroke(1.5.dp.toPx() / ratio))
                        if (layer is EditorBubbleLayer) {
                            val handle = 32.dp.toPx() / ratio
                            translate(rect.right - handle / 2, rect.bottom - handle / 2) {
                                with(resizePainter) { draw(Size(handle, handle)) }
                            }
                        } else {
                            resizeCorners(layer).forEach { corner ->
                                drawCircle(Color.White, 6.5.dp.toPx() / ratio, corner.position(rect))
                                drawCircle(color, 4.5.dp.toPx() / ratio, corner.position(rect))
                            }
                        }
                        if (layer is EditorBubbleLayer && layer.isTailEnabled) {
                            drawCircle(Color(.863f, .314f, 0f), 5.dp.toPx() / ratio, layer.tailOffset())
                        }
                    }
                }
                if (cropMode) {
                    repeat(2) { index ->
                        val part = (index + 1) / 3f
                        drawLine(Color.White.copy(alpha = .7f), Offset(1080f * part, 0f), Offset(1080f * part, 1440f), 1.dp.toPx() / ratio)
                        drawLine(Color.White.copy(alpha = .7f), Offset(0f, 1440f * part), Offset(1080f, 1440f * part), 1.dp.toPx() / ratio)
                    }
                }
            }
        }
    }
}

private enum class DragMode { MOVE, RESIZE, TAIL }

private enum class ResizeCorner(
    val left: Boolean,
    val top: Boolean,
) {
    TOP_START(true, true),
    TOP_END(false, true),
    BOTTOM_START(true, false),
    BOTTOM_END(false, false),
    ;

    fun position(rect: Rect): Offset = Offset(if (left) rect.left else rect.right, if (top) rect.top else rect.bottom)
}

private fun resizeCorners(layer: EditorLayer): List<ResizeCorner> =
    if (layer is EditorBubbleLayer) listOf(ResizeCorner.BOTTOM_END) else ResizeCorner.entries

private fun EditorLayer.hitBounds(): Rect =
    Rect(
        bounds.x.toFloat(),
        bounds.y.toFloat(),
        (bounds.x + bounds.width).toFloat(),
        (bounds.y + bounds.height).toFloat(),
    )

private fun EditorBubbleLayer.tailOffset(): Offset = Offset(tailEndPoint.x.toFloat(), tailEndPoint.y.toFloat())
