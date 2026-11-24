package com.comixon.editor.network

import com.comixon.editor.core.EditorCanvasSpec
import com.comixon.editor.core.EditorRect
import com.comixon.editor.core.SpeechGeneration
import com.comixon.editor.core.SpeechSuggestion

internal fun SpeechResponseDto.toGeneration(): SpeechGeneration {
    val legacy = firstGeneration ?: nextGeneration
    val items =
        speech.firstOrNull().orEmpty().ifEmpty {
            legacy
                ?.speech
                ?.values
                .orEmpty()
                .toList()
        }
    return SpeechGeneration(
        suggestions =
            items.mapNotNull { item ->
                item.text?.takeIf { it.isNotBlank() }?.let { SpeechSuggestion(it, item.bubble?.toEditorRect()) }
            },
        imageDesc = imageDesc ?: legacy?.imageDesc,
        hasFirstGeneration = true,
    )
}

private fun List<Double>.toEditorRect(): EditorRect? {
    if (size < 4 || take(4).any { !it.isFinite() }) return null
    val width = EditorCanvasSpec.LOGICAL_WIDTH
    val height = EditorCanvasSpec.LOGICAL_HEIGHT
    val x = (this[0] * width).coerceIn(0.0, width - 70.0)
    val y = (this[1] * height).coerceIn(0.0, height - 70.0)
    return EditorRect(
        x,
        y,
        (this[2] * width).coerceIn(70.0, width - x),
        (this[3] * height).coerceIn(70.0, height - y),
    )
}
