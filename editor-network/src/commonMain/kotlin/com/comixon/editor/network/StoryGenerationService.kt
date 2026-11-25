package com.comixon.editor.network

import com.comixon.editor.core.SpeechGeneration
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class StoryGenerationService(
    private val service: EditorGenerationService,
) {
    suspend fun generate(
        filterImage: String,
        speechImage: String,
        firstGeneration: Boolean,
        imageDescription: String?,
    ): StoryGenerationResult =
        coroutineScope {
            val image = async { service.filter(filterImage, "anime") }
            val speech = async { service.speech(speechImage, firstGeneration, imageDescription) }
            StoryGenerationResult(speech.await(), image.await())
        }
}

data class StoryGenerationResult(
    val speech: GenerationResult<SpeechGeneration>,
    val image: GenerationResult<String>,
)
