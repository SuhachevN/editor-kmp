package com.comixon.editor.sample

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.comixon.editor.compose.ImageEncoding
import com.comixon.editor.compose.encodeImage
import com.comixon.editor.compose.rememberEditorTextMeasurer
import com.comixon.editor.compose.renderFrame
import com.comixon.editor.compose.renderStrip
import com.comixon.editor.core.EditorCropState
import com.comixon.editor.core.EditorFrame
import com.comixon.editor.core.EditorProject
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.ImageFilter
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.rendering.prepareFrameScene
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FrameExportTest {
    @Test
    fun pngAndJpegContainTheRenderedArtwork() =
        withMeasurer { measurer ->
            val frame = EditorFrame("frame", originalImageUrl = "fixture")
            val decorated = EditorReducer.addDefaultBubble(EditorProject(listOf(frame)), "Привет, мир!").selectedFrame!!
            val images = mapOf("fixture" to fixture())
            val scene = prepareFrameScene(decorated, images, emptyMap(), measurer)
            val artwork = renderFrame(scene, width = 96)
            val bare = renderFrame(scene, width = 96, drawLayers = false)
            val pixels = artwork.toPixelMap()
            val original = bare.toPixelMap()
            var changed = 0
            for (y in 0 until artwork.height) {
                for (x in 0 until artwork.width) {
                    if (pixels[x, y] != original[x, y]) changed++
                }
            }
            assertTrue(changed > 200, "The export must contain the bubble and its text")
            val png = encodeImage(artwork)
            assertContentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), png.copyOf(8))
            val restored = png.decodeToImageBitmap()
            assertEquals(96, restored.width)
            assertEquals(128, restored.height)
            assertTrue(restored.toPixelMap()[10, 10].red > .95f)
            val jpeg = encodeImage(artwork, ImageEncoding.JPEG, 90)
            assertEquals(255, jpeg[0].toInt() and 255)
            assertEquals(216, jpeg[1].toInt() and 255)
            assertEquals(96, jpeg.decodeToImageBitmap().width)
        }

    @Test
    fun cropFlipAndFilterAffectTheExport() =
        withMeasurer { measurer ->
            val images = mapOf("fixture" to fixture())
            val frame = EditorFrame("frame", originalImageUrl = "fixture")
            val original = renderFrame(prepareFrameScene(frame, images, emptyMap(), measurer), 96).toPixelMap()
            val flipped = frame.copy(crop = EditorCropState(isFlippedHorizontally = true))
            val result = renderFrame(prepareFrameScene(flipped, images, emptyMap(), measurer), 96).toPixelMap()
            assertTrue(original[10, 10].red > .95f && original[85, 10].blue > .95f)
            assertTrue(result[10, 10].blue > .95f && result[85, 10].red > .95f)
            val mono = frame.copy(instantFilterName = ImageFilter.MONO)
            val gray = renderFrame(prepareFrameScene(mono, images, emptyMap(), measurer), 96).toPixelMap()[10, 10]
            assertTrue(abs(gray.red - gray.green) < .02f && abs(gray.green - gray.blue) < .02f)
        }

    @Test
    fun stripPreservesFrameOrderAndDimensions() =
        withMeasurer { measurer ->
            val image = fixture()
            val first = EditorFrame("first", originalImageUrl = "fixture")
            val second = first.copy(id = "second", crop = EditorCropState(isFlippedHorizontally = true))
            val scenes =
                listOf(
                    first,
                    second,
                ).map { prepareFrameScene(it, mapOf("fixture" to image), emptyMap(), measurer) }
            val strip = renderStrip(scenes, width = 96)
            assertEquals(96, strip.width)
            assertEquals(256, strip.height)
            val pixels = strip.toPixelMap()
            assertTrue(pixels[10, 10].red > .95f)
            assertTrue(pixels[10, 138].blue > .95f)
        }

    private fun withMeasurer(block: (TextMeasurer) -> Unit) =
        runComposeUiTest {
            lateinit var measurer: TextMeasurer
            setContent { measurer = rememberEditorTextMeasurer() }
            runOnIdle { block(measurer) }
        }

    private fun fixture(): ImageBitmap {
        val image = ImageBitmap(96, 128)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(96f, 128f)) {
            drawRect(Color.Red)
            drawRect(Color.Blue, topLeft = Offset(48f, 0f), size = Size(48f, 128f))
        }
        return image
    }
}
