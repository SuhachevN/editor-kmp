package com.comixon.editor.network

import com.comixon.editor.core.SpeechGeneration
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Direct HTTP integration with the Comixon generation endpoints. */
class ComixonGenerationService(
    private val client: HttpClient,
    private val config: ServerConfig,
) : EditorGenerationService {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    override suspend fun filter(
        imageDataUrl: String,
        filter: String,
        backImage: String?,
    ): GenerationResult<String> =
        safely {
            require(filter in AiFilters.names) { "Unknown AI filter" }
            require(imageDataUrl.startsWith("data:image/")) { "Expected an image data URL" }
            val payload = json.encodeToString(FilterRequest(imageDataUrl, backImage, filter))
            json.decodeFromString<FilterResponse>(post("/v2/filter", payload, 120_000, config.token())).image.firstOrNull()
                ?: throw ProtocolFailure("The filter response contains no image")
        }

    override suspend fun speech(
        imageDataUrl: String,
        firstGeneration: Boolean,
        imageDescription: String?,
    ): GenerationResult<SpeechGeneration> =
        safely {
            require(imageDataUrl.startsWith("data:image/")) { "Expected an image data URL" }
            val token = config.token()
            val authenticated = !token.isNullOrBlank()
            val request =
                SpeechRequestDto(
                    image = imageDataUrl.takeIf { authenticated || firstGeneration || imageDescription.isNullOrBlank() },
                    firstGen = firstGeneration,
                    langZone = config.language,
                    imageDesc = imageDescription?.takeIf { !authenticated && !firstGeneration },
                )
            val payload = json.encodeToString(request)
            json.decodeFromString<SpeechResponseDto>(post("/v2/get_speech", payload, 300_000, token)).toGeneration()
        }

    private suspend fun post(
        path: String,
        payload: String,
        timeoutMillis: Long,
        token: String?,
    ): String {
        val response =
            client.post(config.baseUrl.trimEnd('/') + path) {
                header("Accept", "application/json")
                header("Content-Type", "application/json; charset=UTF-8")
                header("Accept-Language", config.language)
                token?.takeIf { it.isNotBlank() }?.let { header("Token", it) }
                timeout { requestTimeoutMillis = timeoutMillis }
                setBody(payload)
            }
        if (response.status.value !in 200..299) throw HttpFailure(response.status.value)
        return response.bodyAsText().removePrefix("\uFEFF")
    }

    private suspend fun <T> safely(block: suspend () -> T): GenerationResult<T> =
        try {
            GenerationResult.Success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val kind =
                when (error) {
                    is HttpFailure ->
                        when (error.status) {
                            401, 403 -> FailureKind.AUTHORIZATION
                            429 -> FailureKind.RATE_LIMIT
                            400, 422 -> FailureKind.VALIDATION
                            else -> FailureKind.SERVER
                        }
                    is HttpRequestTimeoutException -> FailureKind.TIMEOUT
                    is kotlinx.serialization.SerializationException -> FailureKind.PROTOCOL
                    is IllegalArgumentException -> FailureKind.VALIDATION
                    is ProtocolFailure -> FailureKind.PROTOCOL
                    else -> FailureKind.NETWORK
                }
            val message =
                when (kind) {
                    FailureKind.AUTHORIZATION -> "Sign in with a valid Comixon token to use this service."
                    FailureKind.RATE_LIMIT -> "The generation limit has been reached. Try again later."
                    FailureKind.VALIDATION -> "The server could not accept this image or filter."
                    FailureKind.TIMEOUT -> "Generation timed out."
                    FailureKind.PROTOCOL -> "The server returned an unsupported response."
                    FailureKind.NETWORK -> "Could not reach the Comixon server."
                    FailureKind.SERVER -> "Generation failed on the Comixon server."
                }
            GenerationResult.Failure(kind, message, (error as? HttpFailure)?.status)
        }
}

private class HttpFailure(
    val status: Int,
) : Exception()

private class ProtocolFailure(
    message: String,
) : Exception(message)

@Serializable
private data class FilterRequest(
    @SerialName("image_base64") val image: String,
    @SerialName("back_image") val backImage: String?,
    val filter: String,
)

@Serializable
private data class FilterResponse(
    val image: List<String> = emptyList(),
)
