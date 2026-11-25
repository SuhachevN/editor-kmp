package com.comixon.editor.network

import com.comixon.editor.core.SpeechGeneration
import io.ktor.http.URLProtocol
import io.ktor.http.Url

/** The token provider is evaluated for each request. Tokens are not saved in project files. */
class ServerConfig(
    val baseUrl: String = "https://dev.comixon.com",
    val language: String = "ru-RU",
    val token: suspend () -> String? = { null },
) {
    init {
        val authority =
            baseUrl
                .substringAfter("https://", "")
                .substringBefore('/')
                .substringBefore('?')
                .substringBefore('#')
        require(authority.isNotBlank()) { "The server URL must include a host" }
        val url = Url(baseUrl)
        require(url.protocol == URLProtocol.HTTPS && url.host.isNotBlank()) {
            "The server URL must use HTTPS and include a host"
        }
        require(
            url.parameters.isEmpty() && url.fragment.isEmpty(),
        ) { "The server URL must not include a query or fragment" }
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

// Filter names are part of the server contract.
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
