package com.comixon.editor.compose

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontFamily
import com.comixon.editor.core.BubbleStyle
import com.comixon.editor.core.EditorFont
import com.comixon.editor.core.EditorLetterLayer
import com.comixon.editor.core.EditorRect
import com.comixon.editor.core.FontSize
import com.comixon.editor.core.LetteringStyle
import com.comixon.editor.rendering.BubbleGeometry
import com.comixon.editor.rendering.BubbleTailGeometry
import com.comixon.editor.rendering.drawLettering
import com.comixon.editor.rendering.prepareLettering

@Composable
fun BubbleStylePreview(
    style: BubbleStyle,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val factor = size.width / 100f
        val body = Rect(10f, 6f, 90f, 65f)
        val tip = Offset(26f, 86f)
        val outline = BubbleGeometry.outline(style.type, style.index, body, .7f, tip, body)
        val tail = BubbleTailGeometry.path(style.type, style.index, body, tip, .7f)
        scale(factor, factor, Offset.Zero) {
            drawPath(outline, Color.White)
            drawPath(outline, Color.Black, style = Stroke(1.3f))
            drawPath(tail, Color.Black, style = Stroke(1.3f))
        }
    }
}

@Composable
fun LetteringStylePreview(
    style: LetteringStyle,
    modifier: Modifier = Modifier,
) {
    val families = editorFontFamilies()
    val measurer = rememberEditorTextMeasurer()
    val layer =
        remember(style) {
            EditorLetterLayer(
                "preview",
                EditorRect(0.0, 0.0, 600.0, 400.0),
                style.type,
                style.index,
                "WOW!",
                FontSize.XXL,
            )
        }
    val prepared =
        remember(layer, families, measurer) {
            prepareLettering(layer, families[EditorFont.LETTERING] ?: FontFamily.SansSerif, measurer)
        }
    Canvas(modifier) {
        scale(size.width / 600f, size.height / 400f, Offset.Zero) { drawLettering(prepared) }
    }
}
