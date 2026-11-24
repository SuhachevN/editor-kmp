package com.comixon.editor.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.comixon.editor.compose.style
import com.comixon.editor.compose.weight
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorCanvasSpec
import com.comixon.editor.core.EditorFont
import com.comixon.editor.core.EditorFrame
import com.comixon.editor.core.EditorLayer
import com.comixon.editor.core.EditorLetterLayer
import kotlin.math.max
import kotlin.math.roundToInt

/** Prepared paths and text are used unchanged by the live canvas and bitmap export. */
class FrameScene internal constructor(
    internal val frame: EditorFrame,
    internal val images: Map<String, ImageBitmap>,
    internal val layers: List<LayerScene>,
)

internal sealed interface LayerScene {
    val layer: EditorLayer

    data class Bubble(
        override val layer: EditorBubbleLayer,
        val outline: Path,
        val tail: Path?,
        val mask: Path?,
        val image: ImageBitmap?,
        val text: BubbleTextLayout.Result,
        val stroke: Float,
    ) : LayerScene

    data class Letter(
        override val layer: EditorLetterLayer,
        val glyphs: List<Glyph>,
        val style: LetteringStyleSpec,
        val scale: Float,
    ) : LayerScene
}

internal data class Glyph(
    val layout: TextLayoutResult,
    val position: Offset,
    val scale: Float,
)

fun prepareFrameScene(
    frame: EditorFrame,
    images: Map<String, ImageBitmap>,
    families: Map<EditorFont, FontFamily>,
    measurer: TextMeasurer,
): FrameScene {
    val layers =
        frame.layers.map { layer ->
            val family = families[layer.font] ?: FontFamily.SansSerif
            when (layer) {
                is EditorBubbleLayer -> {
                    val frameRect = Rect(0f, 0f, layer.bounds.width.toFloat(), layer.bounds.height.toFloat())
                    val stroke = BubbleTextLayout.strokeWidth(layer.fontSizeName) * BubbleGeometry.EXPORT_SCALE
                    val rect = frameRect.deflate(stroke / 2f)
                    val tip =
                        if (layer.isTailEnabled) {
                            Offset(
                                (layer.tailEndPoint.x - layer.bounds.x).toFloat(),
                                (layer.tailEndPoint.y - layer.bounds.y).toFloat(),
                            )
                        } else {
                            null
                        }
                    val outline =
                        BubbleGeometry.outline(
                            layer.type,
                            layer.bubbleIndex,
                            rect,
                            BubbleGeometry.EXPORT_SCALE,
                            tip,
                            frameRect,
                            layer.tailBasePercent,
                        )
                    val tail =
                        tip?.let {
                            BubbleTailGeometry.path(
                                layer.type,
                                layer.bubbleIndex,
                                frameRect,
                                it,
                                BubbleGeometry.EXPORT_SCALE,
                                layer.tailBasePercent,
                            )
                        }
                    val image = layer.imageUrl?.let(images::get)
                    val body =
                        if (image != null) {
                            BubbleGeometry.path(
                                layer.type,
                                layer.bubbleIndex,
                                rect,
                                BubbleGeometry.EXPORT_SCALE,
                                tip,
                                frameRect,
                                layer.tailBasePercent,
                            )
                        } else {
                            null
                        }
                    LayerScene.Bubble(
                        layer,
                        outline,
                        tail,
                        body?.let {
                            BubbleImageMask.path(
                                layer.type,
                                layer.bubbleIndex,
                                it,
                                frameRect,
                                BubbleGeometry.EXPORT_SCALE,
                            )
                        },
                        image,
                        BubbleTextLayout.create(
                            layer.type,
                            layer.bubbleIndex,
                            if (image == null) layer.text else "",
                            layer.fontSizeName,
                            frameRect.width,
                            frameRect.height,
                            family,
                            layer.font.weight,
                            layer.font.style,
                            measurer,
                        ),
                        stroke,
                    )
                }
                is EditorLetterLayer -> prepareLettering(layer, family, measurer)
            }
        }
    return FrameScene(frame, images, layers)
}

private val EditorLayer.font: EditorFont
    get() =
        when (this) {
            is EditorBubbleLayer -> font
            is EditorLetterLayer -> font
        }

fun DrawScope.drawEditorFrame(
    scene: FrameScene,
    drawLayers: Boolean = true,
) {
    clipRect(0f, 0f, EditorCanvasSpec.LOGICAL_WIDTH.toFloat(), EditorCanvasSpec.LOGICAL_HEIGHT.toFloat()) {
        drawRect(Color.White, size = Size(1080f, 1440f))
        val frame = scene.frame
        frame.originalImageUrl?.let(scene.images::get)?.let { image ->
            val crop = frame.crop
            withTransform({
                translate(540f + crop.offsetX.toFloat(), 720f + crop.offsetY.toFloat())
                rotate(crop.rotationDegrees.toFloat(), Offset.Zero)
                scale(
                    crop.scale.toFloat() * if (crop.isFlippedHorizontally) -1f else 1f,
                    crop.scale.toFloat(),
                    Offset.Zero,
                )
            }) { drawCoverImage(image, Rect(-540f, -720f, 540f, 720f), frame.instantFilterName.colorFilter()) }
        }
        if (drawLayers) {
            scene.layers.forEach { prepared ->
                val r = prepared.layer.bounds
                translate(r.x.toFloat(), r.y.toFloat()) {
                    when (prepared) {
                        is LayerScene.Bubble -> {
                            val stroke = Stroke(prepared.stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            drawPath(prepared.outline, Color.White)
                            if (prepared.image != null && prepared.mask != null) {
                                clipPath(prepared.mask) {
                                    drawCoverImage(
                                        prepared.image,
                                        Rect(0f, 0f, r.width.toFloat(), r.height.toFloat()),
                                    )
                                }
                            }
                            drawPath(prepared.outline, Color.Black, style = stroke)
                            prepared.tail?.let { drawPath(it, Color.Black, style = stroke) }
                            prepared.text.lines.forEach { drawText(it.layout, topLeft = it.position) }
                        }
                        is LayerScene.Letter -> drawLettering(prepared)
                    }
                }
            }
        }
    }
}

internal fun DrawScope.drawCoverImage(
    image: ImageBitmap,
    rect: Rect,
    filter: ColorFilter? = null,
) {
    val factor = max(rect.width / image.width, rect.height / image.height)
    val width = (image.width * factor).roundToInt().coerceAtLeast(1)
    val height = (image.height * factor).roundToInt().coerceAtLeast(1)
    drawImage(
        image,
        dstOffset =
            IntOffset(
                (rect.center.x - width / 2f).roundToInt(),
                (rect.center.y - height / 2f).roundToInt(),
            ),
        dstSize = IntSize(width, height),
        colorFilter = filter,
    )
}
