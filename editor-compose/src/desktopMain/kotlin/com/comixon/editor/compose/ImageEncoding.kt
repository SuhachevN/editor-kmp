package com.comixon.editor.compose

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun encodeImage(
    bitmap: ImageBitmap,
    encoding: ImageEncoding,
    quality: Int,
): ByteArray {
    require(quality in 0..100)
    val format = if (encoding == ImageEncoding.PNG) EncodedImageFormat.PNG else EncodedImageFormat.JPEG
    return Image.makeFromBitmap(bitmap.asSkiaBitmap()).use { image ->
        image.encodeToData(format, quality)?.use { it.bytes } ?: error("Image encoding failed")
    }
}
