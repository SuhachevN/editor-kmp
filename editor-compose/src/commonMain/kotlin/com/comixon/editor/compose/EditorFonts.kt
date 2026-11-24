package com.comixon.editor.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.comixon.editor.core.EditorFont
import com.comixon.editor.resources.Res
import com.comixon.editor.resources.comixon
import com.comixon.editor.resources.comixon_lettering
import com.comixon.editor.resources.editor_lubrifont
import com.comixon.editor.resources.editor_pixel
import com.comixon.editor.resources.editor_vollkorn
import org.jetbrains.compose.resources.Font

@Composable
fun editorFontFamilies(): Map<EditorFont, FontFamily> {
    val comic = FontFamily(Font(Res.font.comixon))
    val lettering = FontFamily(Font(Res.font.comixon_lettering))
    val pixel = FontFamily(Font(Res.font.editor_pixel))
    val vollkorn = FontFamily(Font(Res.font.editor_vollkorn))
    val lubrifont = FontFamily(Font(Res.font.editor_lubrifont))
    return remember(comic, lettering, pixel, vollkorn, lubrifont) {
        mapOf(
            EditorFont.COMIXON to comic,
            EditorFont.LETTERING to lettering,
            EditorFont.PIXEL to pixel,
            EditorFont.VOLLKORN to vollkorn,
            EditorFont.LUBRIFONT to lubrifont,
            EditorFont.SANS to FontFamily.SansSerif,
            EditorFont.SANS_BOLD to FontFamily.SansSerif,
            EditorFont.SANS_ITALIC to FontFamily.SansSerif,
            EditorFont.MONO to FontFamily.Monospace,
            EditorFont.MONO_BOLD to FontFamily.Monospace,
            EditorFont.MONO_ITALIC to FontFamily.Monospace,
        )
    }
}

internal val EditorFont.weight: FontWeight
    get() = if (this == EditorFont.SANS_BOLD || this == EditorFont.MONO_BOLD) FontWeight.Bold else FontWeight.Normal

internal val EditorFont.style: FontStyle
    get() = if (this == EditorFont.SANS_ITALIC || this == EditorFont.MONO_ITALIC) FontStyle.Italic else FontStyle.Normal
