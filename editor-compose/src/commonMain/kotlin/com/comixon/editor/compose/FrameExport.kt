package com.comixon.editor.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.comixon.editor.rendering.FrameScene
import com.comixon.editor.rendering.drawEditorFrame
import kotlin.io.encoding.Base64
import kotlin.math.roundToInt

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

expect fun encodeImage(
    bitmap: ImageBitmap,
    encoding: ImageEncoding = ImageEncoding.PNG,
    quality: Int = 95,
): ByteArray

// The endpoints require different JPEG dimensions, size limits and MIME aliases.
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
fun generationImage(
    scene: FrameScene,
    speech: Boolean,
): String =
    generationPayload(speech) { width, height ->
        val frame = renderFrame(scene, width, drawLayers = false)
        if (frame.height == height) frame else resizeGenerationImage(frame, width, height)
    }

fun generationImage(
    image: ImageBitmap,
    speech: Boolean = false,
): String = generationPayload(speech) { width, height -> resizeGenerationImage(image, width, height) }

private fun resizeGenerationImage(
    image: ImageBitmap,
    width: Int,
    height: Int,
): ImageBitmap {
    val bitmap = ImageBitmap(width, height)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
        drawRect(Color.White)
        drawImage(image, dstSize = IntSize(width, height))
    }
    return bitmap
}

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
private fun generationPayload(
    speech: Boolean,
    render: (Int, Int) -> ImageBitmap,
): String {
    val initialWidth = if (speech) 768 else 1080
    val initialHeight = if (speech) 1024 else 1440
    val maxBytes = if (speech) 180_000 else 400_000
    for (scale in listOf(1f, .85f, .7f, .6f, .5f)) {
        val bitmap =
            render(
                (initialWidth * scale).roundToInt().coerceAtLeast(480),
                (initialHeight * scale).roundToInt().coerceAtLeast(480),
            )
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
