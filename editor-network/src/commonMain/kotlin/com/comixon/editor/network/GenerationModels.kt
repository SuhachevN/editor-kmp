package com.comixon.editor.network

import com.comixon.editor.core.SpeechGeneration

/** Credentials are supplied per request and never persisted by the editor. */
class ServerConfig(
    val baseUrl: String = "https://dev.comixon.com",
    val language: String = "ru-RU",
    val token: suspend () -> String? = { null },
) {
    init {
        require(baseUrl.startsWith("https://")) { "The server URL must use HTTPS" }
    }
}

sealed interface GenerationResult<out T> {
    data class Success<T>(
        val value: T,
    ) : GenerationResult<T>

    data class Failure(
        val kind: FailureKind,
        val message: String,
        val status: Int? = null,
    ) : GenerationResult<Nothing>
}

enum class FailureKind { AUTHORIZATION, RATE_LIMIT, VALIDATION, NETWORK, SERVER, PROTOCOL, TIMEOUT }

interface EditorGenerationService {
    suspend fun filter(
        imageDataUrl: String,
        filter: String,
        backImage: String? = null,
    ): GenerationResult<String>

    suspend fun speech(
        imageDataUrl: String,
        firstGeneration: Boolean = true,
        imageDescription: String? = null,
    ): GenerationResult<SpeechGeneration>
}

/** Names are the wire values accepted by the Comixon API. */
object AiFilters {
    val names: List<String> =
        listOf(
            "beauty",
            "marvel",
            "anime",
            "watercolor",
            "sketch",
            "simpsons",
            "oil",
            "lego",
            "ghibli",
            "manga",
            "noir",
            "soviet",
            "doodle",
            "japanese",
            "mona_lisa",
            "van_gogh",
            "winter",
            "minecraft",
            "playstation",
            "8bit",
            "stone",
            "glass",
            "impressionism",
            "cubism",
            "surrealism",
            "isometric",
            "cyberpunk",
            "apocalyptic",
            "graffiti",
            "embroidery",
            "pastel",
            "paper",
            "disney",
            "lithograph",
            "art_nouveau",
            "byzantine",
            "matrix",
            "cross-stitch",
            "bronze",
            "tarot",
            "fairytale",
            "constellation",
            "blending",
        )
}
