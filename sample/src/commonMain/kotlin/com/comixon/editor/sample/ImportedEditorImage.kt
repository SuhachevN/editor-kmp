package com.comixon.editor.sample

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.comixon.editor.compose.encodeImage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.math.roundToInt

internal data class ImportedEditorImage(
    val source: String,
    val bitmap: ImageBitmap,
)

internal suspend fun pickEditorImages(multiple: Boolean): List<ImportedEditorImage>? {
    val files =
        if (multiple) {
            FileKit.openFilePicker(type = FileKitType.Image, mode = FileKitMode.Multiple(maxItems = 12))
        } else {
            FileKit.openFilePicker(type = FileKitType.Image)?.let(::listOf)
        } ?: return null
    return files.take(12).map { file ->
        val original = file.toImageBitmap()
        withContext(Dispatchers.Default) { importedEditorImage(original) }
    }
}

/** Store oriented, bounded images so saved projects do not depend on temporary picker URIs. */
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
internal fun importedEditorImage(original: ImageBitmap): ImportedEditorImage {
    val ratio = (2048.0 / maxOf(original.width, original.height)).coerceAtMost(1.0)
    val bitmap =
        if (ratio < 1.0) {
            val width = (original.width * ratio).roundToInt().coerceAtLeast(1)
            val height = (original.height * ratio).roundToInt().coerceAtLeast(1)
            ImageBitmap(width, height).also { target ->
                CanvasDrawScope().draw(
                    Density(1f),
                    LayoutDirection.Ltr,
                    Canvas(target),
                    Size(width.toFloat(), height.toFloat()),
                ) {
                    drawImage(original, dstSize = IntSize(width, height), filterQuality = FilterQuality.High)
                }
            }
        } else {
            original
        }
    return ImportedEditorImage("data:image/png;base64," + Base64.encode(encodeImage(bitmap)), bitmap)
}
