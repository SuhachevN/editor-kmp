package com.comixon.editor.network

import com.comixon.editor.core.SpeechGeneration
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StoryGenerationServiceTest {
    @Test
    fun startGenerationSendsTwoDirectRequestsWithDifferentImages() =
        runTest {
            val filterImage = "data:image/jpeg;base64,Y3JvcA=="
            val speechImage = "data:image/jpg;base64,c291cmNl"
            val engine =
                MockEngine { request ->
                    assertEquals("session-token", request.headers["Token"])
                    val body = Json.decodeFromString<JsonObject>(request.body.toByteArray().decodeToString())
                    val response =
                        when (request.url.encodedPath) {
                            "/v2/filter" -> {
                                assertEquals("anime", body["filter"]?.jsonPrimitive?.content)
                                assertEquals(filterImage, body["image_base64"]?.jsonPrimitive?.content)
                                """{"image":["https://example.test/anime.jpg"]}"""
                            }
                            "/v2/get_speech" -> {
                                assertEquals(speechImage, body["image"]?.jsonPrimitive?.content)
                                """{"speech":[[{"text":"Hello"}]],"image_desc":"A street"}"""
                            }
                            else -> error("Unexpected endpoint")
                        }
                    respond(response, headers = headersOf("Content-Type", "application/json"))
                }
            val client = HttpClient(engine) { install(HttpTimeout) }
            try {
                val service =
                    StoryGenerationService(ComixonGenerationService(client, ServerConfig(token = { "session-token" })))
                val result = service.generate(filterImage, speechImage, true, null)
                assertEquals(
                    "https://example.test/anime.jpg",
                    assertIs<GenerationResult.Success<String>>(result.image).value,
                )
                assertEquals(
                    "Hello",
                    assertIs<GenerationResult.Success<SpeechGeneration>>(result.speech)
                        .value.suggestions
                        .single()
                        .text,
                )
                assertEquals(
                    setOf("/v2/filter", "/v2/get_speech"),
                    engine.requestHistory.map { it.url.encodedPath }.toSet(),
                )
                assertEquals(2, engine.requestHistory.size)
            } finally {
                client.close()
            }
        }

    @Test
    fun requestsRunConcurrentlyAndKeepPartialSuccess() =
        runTest {
            val imageStarted = CompletableDeferred<Unit>()
            val speechStarted = CompletableDeferred<Unit>()
            val fake =
                object : EditorGenerationService {
                    override suspend fun filter(
                        imageDataUrl: String,
                        filter: String,
                        backImage: String?,
                    ): GenerationResult<String> {
                        imageStarted.complete(Unit)
                        speechStarted.await()
                        return GenerationResult.Success("result")
                    }

                    override suspend fun speech(
                        imageDataUrl: String,
                        firstGeneration: Boolean,
                        imageDescription: String?,
                    ): GenerationResult<SpeechGeneration> {
                        speechStarted.complete(Unit)
                        imageStarted.await()
                        return GenerationResult.Failure(FailureKind.SERVER, "Speech failed")
                    }
                }
            val result = StoryGenerationService(fake).generate("filter", "speech", true, null)
            assertEquals("result", assertIs<GenerationResult.Success<String>>(result.image).value)
            assertIs<GenerationResult.Failure>(result.speech)
            assertTrue(imageStarted.isCompleted && speechStarted.isCompleted)
        }
}
