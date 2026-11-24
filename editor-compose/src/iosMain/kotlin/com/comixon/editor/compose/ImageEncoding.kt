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
    val image = Image.makeFromBitmap(bitmap.asSkiaBitmap())
    try {
        val data = image.encodeToData(format, quality) ?: error("Image encoding failed")
        try {
            return data.bytes
        } finally {
            data.close()
        }
    } finally {
        image.close()
    }
}
