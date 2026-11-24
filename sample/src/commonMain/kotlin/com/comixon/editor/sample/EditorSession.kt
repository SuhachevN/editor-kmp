package com.comixon.editor.sample

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.comixon.editor.compose.EditorState
import com.comixon.editor.core.EditorFont
import com.comixon.editor.core.EditorLayer
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.FontSize
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.core.selectedLayer

internal data class TextDraft(
    val layer: EditorLayer,
    val text: String,
    val font: EditorFont,
    val size: FontSize,
)

/** Temporary panel edits become one document change only after confirmation. */
@Stable
internal class EditorSession(
    val editor: EditorState,
) {
    var tool by mutableStateOf(EditorTool.FRAME)
        private set
    var cropDraft by mutableStateOf<EditorState?>(null)
        private set
    var textDraft by mutableStateOf<TextDraft?>(null)
        private set
    var expanded by mutableStateOf(true)

    val isPicker: Boolean
        get() = tool in listOf(EditorTool.BUBBLE, EditorTool.LETTERING, EditorTool.FILTER)
    val isContextual: Boolean
        get() = textDraft != null || cropDraft != null || isPicker || editor.project.selectedLayer != null
    val canvasState: EditorState
        get() = cropDraft ?: editor

    fun selectTool(next: EditorTool) {
        cropDraft = if (next == EditorTool.CROP) EditorState(editor.project.copy(selectedLayerId = null)) else null
        if (next in listOf(EditorTool.FRAME, EditorTool.CROP, EditorTool.FILTER)) editor.select { EditorReducer.selectLayer(it, null) }
        tool = next
        expanded = true
    }

    fun showFrameTools() {
        tool = EditorTool.FRAME
    }

    fun editText(layer: EditorLayer) {
        textDraft = TextDraft(layer, layer.editorText(), layer.editorFont, layer.editorSize)
    }

    fun updateTextDraft(
        text: String,
        font: EditorFont,
        size: FontSize,
    ) {
        textDraft = textDraft?.copy(text = text, font = font, size = size)
    }

    fun finishEditing(save: Boolean): Boolean {
        val text = textDraft
        val crop = cropDraft?.project?.selectedFrame
        when {
            text != null -> {
                if (save) editor.edit { EditorReducer.updateLayerText(it, text.layer.id, text.text, text.size, text.font) }
                textDraft = null
            }
            crop != null -> {
                if (save) {
                    editor.edit { project ->
                        project.copy(frames = project.frames.map { if (it.id == crop.id) it.copy(crop = crop.crop) else it })
                    }
                }
                cropDraft = null
                tool = EditorTool.FRAME
            }
            isPicker -> tool = EditorTool.FRAME
            editor.project.selectedLayer != null -> editor.select { EditorReducer.selectLayer(it, null) }
            else -> return false
        }
        return true
    }
}
