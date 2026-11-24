package com.comixon.editor.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.comixon.editor.rendering.FrameScene
import com.comixon.editor.rendering.drawCoverImage
import com.comixon.editor.rendering.drawEditorFrame
import kotlin.io.encoding.Base64

/** Renders the artwork without selection handles, using exactly the canvas scene. */
fun renderFrame(
    scene: FrameScene,
    width: Int = 1080,
    drawLayers: Boolean = true,
): ImageBitmap {
    require(width in 64..4096)
    val height = width * 4 / 3
    val bitmap = ImageBitmap(width, height)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
        scale(width / 1080f, width / 1080f, Offset.Zero) { drawEditorFrame(scene, drawLayers) }
    }
    return bitmap
}

fun renderStrip(
    scenes: List<FrameScene>,
    width: Int = 1080,
): ImageBitmap {
    require(scenes.isNotEmpty())
    require(width in 64..4096)
    val frameHeight = width * 4 / 3
    val height = frameHeight.toLong() * scenes.size
    require(width.toLong() * height <= 64_000_000) { "Strip exceeds the export pixel limit" }
    val bitmap = ImageBitmap(width, height.toInt())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
        scenes.forEachIndexed { index, scene ->
            translate(top = (index * frameHeight).toFloat()) {
                scale(width / 1080f, width / 1080f, Offset.Zero) { drawEditorFrame(scene) }
            }
        }
    }
    return bitmap
}

enum class ImageEncoding { PNG, JPEG }

/** The only platform-specific rendering boundary: encoding an already drawn bitmap. */
expect fun encodeImage(
    bitmap: ImageBitmap,
    encoding: ImageEncoding = ImageEncoding.PNG,
    quality: Int = 95,
): ByteArray

/** JPEG payload sizes and MIME aliases match the existing Comixon endpoints. */
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
fun generationImage(
    scene: FrameScene,
    speech: Boolean,
): String = generationPayload(speech) { renderFrame(scene, it, drawLayers = false) }

/** Normalizes the second image used by the blending filter to the same server format. */
fun generationImage(image: ImageBitmap): String =
    generationPayload(speech = false) { width ->
        val height = width * 4 / 3
        val bitmap = ImageBitmap(width, height)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
            drawCoverImage(image, Rect(Offset.Zero, size))
        }
        bitmap
    }

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
private fun generationPayload(
    speech: Boolean,
    render: (Int) -> ImageBitmap,
): String {
    val initialWidth = if (speech) 768 else 1080
    val maxBytes = if (speech) 180_000 else 400_000
    for (scale in listOf(1f, .85f, .7f, .6f, .5f)) {
        val bitmap = render((initialWidth * scale).toInt().coerceAtLeast(360))
        for (quality in listOf(98, 96, 94, 92, 88, 84, 80, 74, 68)) {
            val bytes = encodeImage(bitmap, ImageEncoding.JPEG, quality)
            if (bytes.size <= maxBytes) {
                val mime = if (speech) "jpg" else "jpeg"
                return "data:image/" + mime + ";base64," + Base64.encode(bytes).chunked(76).joinToString("\n")
            }
        }
    }
    error("The image could not be compressed to the server payload limit")
}
