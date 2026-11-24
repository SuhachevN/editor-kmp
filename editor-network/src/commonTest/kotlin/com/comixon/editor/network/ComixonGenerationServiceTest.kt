package com.comixon.editor.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComixonGenerationServiceTest {
    private val jsonHeaders = headersOf("Content-Type", "application/json")
    private val image = "data:image/jpeg;base64,dGVzdA=="

    @Test fun filtersUseTheDirectEndpointAndExactWireKeys() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals("/v2/filter", request.url.encodedPath)
                    assertEquals("test-token", request.headers["Token"])
                    assertEquals("ru-RU", request.headers["Accept-Language"])
                    val body = Json.decodeFromString<JsonObject>(request.body.toByteArray().decodeToString())
                    assertEquals(image, body["image_base64"]?.jsonPrimitive?.content)
                    assertEquals("anime", body["filter"]?.jsonPrimitive?.content)
                    assertFalse("back_image" in body)
                    respond("{\"image\":[\"https://example.test/result.jpg\"]}", headers = jsonHeaders)
                }
            val client = HttpClient(engine) { install(HttpTimeout) }
            try {
                val result = ComixonGenerationService(client, ServerConfig(token = { "test-token" })).filter(image, "anime")
                assertEquals("https://example.test/result.jpg", assertIs<GenerationResult.Success<String>>(result).value)
                assertEquals(1, engine.requestHistory.size)
            } finally {
                client.close()
            }
        }

    @Test fun authenticatedSpeechUsesDirectHttpAndRelativeBubbleCoordinates() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals("/v2/get_speech", request.url.encodedPath)
                    val body = Json.decodeFromString<JsonObject>(request.body.toByteArray().decodeToString())
                    assertEquals(image, body["image"]?.jsonPrimitive?.content)
                    assertEquals("true", body["first_gen"]?.jsonPrimitive?.content)
                    assertFalse("image_desc" in body)
                    respond("""{"speech":[[{"text":"Hello","bubble":[0.1,0.2,0.4,0.3]}]],"image_desc":"A street"}""", headers = jsonHeaders)
                }
            val client = HttpClient(engine) { install(HttpTimeout) }
            try {
                val result = ComixonGenerationService(client, ServerConfig(token = { "test-token" })).speech(image)
                val generation = assertIs<GenerationResult.Success<com.comixon.editor.core.SpeechGeneration>>(result).value
                assertEquals("A street", generation.imageDesc)
                val suggestion = generation.suggestions.single()
                assertEquals("Hello", suggestion.text)
                assertEquals(108.0, suggestion.bubbleRect?.x)
                assertEquals(288.0, suggestion.bubbleRect?.y)
                assertEquals(432.0, suggestion.bubbleRect?.width)
                assertEquals(432.0, suggestion.bubbleRect?.height)
            } finally {
                client.close()
            }
        }

    @Test fun anonymousRegenerationReusesDescriptionAndSupportsLegacyResponses() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertNull(request.headers["Token"])
                    val body = Json.decodeFromString<JsonObject>(request.body.toByteArray().decodeToString())
                    assertFalse("image" in body)
                    assertEquals("A street", body["image_desc"]?.jsonPrimitive?.content)
                    respond("""{"next_generation":{"speech":{"0":{"text":"Again"}},"image_desc":"A street"}}""", headers = jsonHeaders)
                }
            val client = HttpClient(engine) { install(HttpTimeout) }
            try {
                val result = ComixonGenerationService(client, ServerConfig()).speech(image, false, "A street")
                assertEquals(
                    "Again",
                    assertIs<GenerationResult.Success<com.comixon.editor.core.SpeechGeneration>>(result)
                        .value.suggestions
                        .single()
                        .text,
                )
            } finally {
                client.close()
            }
        }

    @Test fun authorizationAndRateLimitFailuresAreNotRetried() =
        runTest {
            for ((status, kind) in listOf(
                HttpStatusCode.Unauthorized to FailureKind.AUTHORIZATION,
                HttpStatusCode.TooManyRequests to FailureKind.RATE_LIMIT,
                HttpStatusCode.BadRequest to FailureKind.VALIDATION,
            )) {
                val engine = MockEngine { respond("private server error", status, jsonHeaders) }
                val client = HttpClient(engine) { install(HttpTimeout) }
                try {
                    val result = ComixonGenerationService(client, ServerConfig()).filter(image, "anime")
                    val failure = assertIs<GenerationResult.Failure>(result)
                    assertEquals(kind, failure.kind)
                    assertEquals(status.value, failure.status)
                    assertFalse(failure.message.contains("private server error"))
                    assertEquals(1, engine.requestHistory.size)
                } finally {
                    client.close()
                }
            }
        }

    @Test fun malformedAndMissingResultsAreProtocolFailures() =
        runTest {
            for (body in listOf("not json", "{\"image\":[]}")) {
                val client = HttpClient(MockEngine { respond(body, headers = jsonHeaders) }) { install(HttpTimeout) }
                try {
                    val result = ComixonGenerationService(client, ServerConfig()).filter(image, "anime")
                    assertEquals(FailureKind.PROTOCOL, assertIs<GenerationResult.Failure>(result).kind)
                } finally {
                    client.close()
                }
            }
        }

    @Test fun cancellationPropagatesToTheCaller() =
        runTest {
            val client = HttpClient(MockEngine { throw CancellationException("cancelled") }) { install(HttpTimeout) }
            try {
                assertFailsWith<CancellationException> { ComixonGenerationService(client, ServerConfig()).filter(image, "anime") }
            } finally {
                client.close()
            }
        }

    @Test fun blendingSendsTheBackgroundImage() =
        runTest {
            val client =
                HttpClient(
                    MockEngine { request ->
                        val body = Json.decodeFromString<JsonObject>(request.body.toByteArray().decodeToString())
                        assertEquals(image, body["back_image"]?.jsonPrimitive?.content)
                        respond("{\"image\":[\"https://example.test/result.jpg\"]}", headers = jsonHeaders)
                    },
                ) { install(HttpTimeout) }
            try {
                assertIs<GenerationResult.Success<String>>(
                    ComixonGenerationService(client, ServerConfig()).filter(image, "blending", image),
                )
            } finally {
                client.close()
            }
        }

    @Test fun invalidFiltersNeverReachTheNetwork() =
        runTest {
            val engine = MockEngine { error("No request expected") }
            val client = HttpClient(engine)
            try {
                val result = ComixonGenerationService(client, ServerConfig()).filter(image, "missing")
                assertEquals(FailureKind.VALIDATION, assertIs<GenerationResult.Failure>(result).kind)
                assertTrue(engine.requestHistory.isEmpty())
            } finally {
                client.close()
            }
        }
}
