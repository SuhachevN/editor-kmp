package com.comixon.editor.sample

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.comixon.editor.compose.ImageEncoding
import com.comixon.editor.compose.encodeImage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64

@OptIn(ExperimentalResourceApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class)
internal suspend fun loadImage(
    client: HttpClient,
    source: String,
): ImageBitmap {
    val bytes =
        if (source.startsWith("data:image/")) {
            Base64.decode(source.substringAfter(";base64,").filterNot(Char::isWhitespace))
        } else {
            require(source.startsWith("https://")) { "Image URLs must use HTTPS" }
            val response = client.get(source)
            check(response.status.value in 200..299) { "Image download failed: " + response.status.value }
            response.readRawBytes()
        }
    return bytes.decodeToImageBitmap()
}

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
internal fun demoImage(): String {
    val bitmap = ImageBitmap(540, 720)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(540f, 720f)) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFFE9D9CC), Color(0xFFF6EEE3))))
        drawCircle(Color(0xFFEAA76F), 130f, Offset(360f, 230f))
        val heights = listOf(310f, 410f, 350f, 470f, 370f, 300f, 430f)
        heights.forEachIndexed { index, height ->
            val x = index * 90f - 35f
            drawRect(Color(0xFF334B50), Offset(x, height), Size(80f, 720f - height))
            repeat(4) { row ->
                repeat(2) { col ->
                    drawRect(Color(0xFFDAC1A3), Offset(x + 16f + col * 30f, height + 24f + row * 45f), Size(10f, 17f))
                }
            }
        }
        drawRect(Color(0xFF172C31), Offset(0f, 660f), Size(540f, 60f))
    }
    return "data:image/png;base64," + Base64.encode(encodeImage(bitmap, ImageEncoding.PNG))
}
