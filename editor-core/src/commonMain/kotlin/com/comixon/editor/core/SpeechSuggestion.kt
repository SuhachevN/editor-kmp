package com.comixon.editor.core

data class SpeechGeneration(
    val suggestions: List<SpeechSuggestion>,
    val imageDesc: String?,
    val hasFirstGeneration: Boolean,
)

data class SpeechSuggestion(
    val text: String,
    val bubbleRect: EditorRect? = null,
)

data class FrameSpeechState(
    val hasFirstGeneration: Boolean = false,
    val imageDesc: String? = null,
    val previousImageSource: String? = null,
    val generations: List<List<SpeechSuggestion>> = emptyList(),
    val currentGenerationIndex: Int = generations.lastIndex.coerceAtLeast(0),
) {
    fun isFirstGenerationFor(imageSource: String?): Boolean = !hasFirstGeneration || previousImageSource != imageSource

    fun hasGenerationFor(imageSource: String?): Boolean = hasFirstGeneration && previousImageSource == imageSource

    val canShowPreviousGeneration: Boolean
        get() = generations.isNotEmpty() && currentGenerationIndex > 0

    val canShowNextGeneration: Boolean
        get() = generations.isNotEmpty() && currentGenerationIndex < generations.lastIndex
}
