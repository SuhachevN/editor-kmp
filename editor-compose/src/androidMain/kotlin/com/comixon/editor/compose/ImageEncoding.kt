package com.comixon.editor.compose

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun encodeImage(
    bitmap: ImageBitmap,
    encoding: ImageEncoding,
    quality: Int,
): ByteArray {
    require(quality in 0..100)
    val format = if (encoding == ImageEncoding.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
    return ByteArrayOutputStream().use { output ->
        check(bitmap.asAndroidBitmap().compress(format, quality, output)) { "Image encoding failed" }
        output.toByteArray()
    }
}
