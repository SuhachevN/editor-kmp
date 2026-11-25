package com.comixon.editor.sample

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import com.comixon.editor.compose.rememberEditorTextMeasurer
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorFrame
import com.comixon.editor.core.EditorProject
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.SpeechGeneration
import com.comixon.editor.core.SpeechSuggestion
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.network.EditorGenerationService
import com.comixon.editor.network.FailureKind
import com.comixon.editor.network.GenerationResult
import com.comixon.editor.network.ServerConfig
import com.comixon.editor.rendering.prepareFrameScene
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class)
class StoryWorkflowTest {
    @Test
    fun generationCannotOverwriteAFrameWhoseSourceWasChangedWhileWaiting() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val gate = CompletableDeferred<Unit>()
            val service = StoryService(gate = gate)
            val workspace = EditorWorkspace(scope) { _, _ -> service }
            lateinit var measurer: TextMeasurer
            setContent { measurer = rememberEditorTextMeasurer() }
            try {
                val before = project(service.source)
                runOnIdle {
                    workspace.editor.replace(before)
                    workspace.generate(
                        scene(before, service.source, measurer),
                        ServerConfig(token = { "test-session" }),
                    )
                }
                waitUntil(timeoutMillis = 15_000) { service.speechCalls == 1 }
                lateinit var changed: EditorProject
                runOnIdle {
                    workspace.editor.edit {
                        it.copy(
                            frames =
                                it.frames.map { frame ->
                                    frame.copy(originalImageUrl = "replacement")
                                },
                        )
                    }
                    changed = workspace.editor.project
                    gate.complete(Unit)
                }
                waitUntil(timeoutMillis = 15_000) { !workspace.isGenerating }
                runOnIdle {
                    assertEquals(changed, workspace.editor.project)
                    assertNull(workspace.selectedSpeechState)
                    assertEquals("The source frame changed. The generation was not applied.", workspace.message)
                }
            } finally {
                gate.complete(Unit)
                workspace.close()
                scope.cancel()
            }
        }

    @Test
    fun imageAndDialogueApplyTogetherAndRegenerationKeepsHistory() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val service = StoryService()
            val workspace = EditorWorkspace(scope) { _, _ -> service }
            lateinit var measurer: TextMeasurer
            setContent { measurer = rememberEditorTextMeasurer() }
            try {
                val before = project(service.source)
                runOnIdle {
                    workspace.editor.replace(before)
                    workspace.generate(
                        scene(before, service.source, measurer),
                        ServerConfig(token = { "test-session" }),
                    )
                }
                waitUntil(timeoutMillis = 15_000) { service.speechCalls == 1 && !workspace.isGenerating }
                runOnIdle {
                    val generated = workspace.editor.project
                    assertEquals(service.generated, generated.selectedFrame!!.originalImageUrl)
                    assertEquals(
                        "Dialogue 1",
                        generated.selectedFrame!!
                            .layers
                            .filterIsInstance<EditorBubbleLayer>()
                            .first()
                            .text,
                    )
                    assertNotNull(workspace.selectedSpeechState)
                    assertNull(workspace.message)
                    workspace.editor.undo()
                    assertEquals(before, workspace.editor.project, "Both server results must be one undo entry")
                    workspace.editor.redo()
                    assertEquals(generated, workspace.editor.project)
                    workspace.generate(
                        scene(generated, service.generated, measurer),
                        ServerConfig(token = { "test-session" }),
                    )
                }
                waitUntil(timeoutMillis = 15_000) { service.speechCalls == 2 && !workspace.isGenerating }
                runOnIdle {
                    assertEquals(listOf(true, false), service.firstGeneration)
                    assertEquals(listOf(null, "A city"), service.descriptions)
                    assertEquals(2, workspace.selectedSpeechState!!.generations.size)
                    workspace.selectSpeechGeneration(-1)
                    assertEquals(
                        "Dialogue 1",
                        workspace.editor.project.selectedFrame!!
                            .layers
                            .filterIsInstance<EditorBubbleLayer>()
                            .first()
                            .text,
                    )
                    workspace.selectSpeechGeneration(1)
                    assertEquals(
                        "Dialogue 2",
                        workspace.editor.project.selectedFrame!!
                            .layers
                            .filterIsInstance<EditorBubbleLayer>()
                            .first()
                            .text,
                    )
                }
            } finally {
                workspace.close()
                scope.cancel()
            }
        }

    @Test
    fun aSpeechFailureKeepsTheSuccessfulImageAndExistingText() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val service = StoryService(failSpeech = true)
            val workspace = EditorWorkspace(scope) { _, _ -> service }
            lateinit var measurer: TextMeasurer
            setContent { measurer = rememberEditorTextMeasurer() }
            try {
                val before = project(service.source)
                runOnIdle {
                    workspace.editor.replace(before)
                    workspace.generate(
                        scene(before, service.source, measurer),
                        ServerConfig(token = { "test-session" }),
                    )
                }
                waitUntil(timeoutMillis = 15_000) { service.speechCalls == 1 && !workspace.isGenerating }
                runOnIdle {
                    val frame = workspace.editor.project.selectedFrame!!
                    assertEquals(service.generated, frame.originalImageUrl)
                    assertEquals(before.selectedFrame!!.layers, frame.layers)
                    assertEquals("Speech failed", workspace.message)
                    assertNull(workspace.selectedSpeechState)
                    workspace.editor.undo()
                    assertEquals(before, workspace.editor.project)
                }
            } finally {
                workspace.close()
                scope.cancel()
            }
        }

    private fun project(source: String): EditorProject =
        EditorReducer.addDefaultBubble(
            EditorProject(listOf(EditorFrame("frame", originalImageUrl = source))),
            "Original",
        )

    private fun scene(
        project: EditorProject,
        source: String,
        measurer: TextMeasurer,
    ) = prepareFrameScene(project.selectedFrame!!, mapOf(source to bitmap(source)), emptyMap(), measurer)

    private fun bitmap(source: String): ImageBitmap =
        Base64.decode(source.substringAfter(',').filterNot(Char::isWhitespace)).decodeToImageBitmap()

    private class StoryService(
        private val failSpeech: Boolean = false,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : EditorGenerationService {
        val source = demoImage()
        val generated = source + "\n"
        var speechCalls = 0
        val firstGeneration = mutableListOf<Boolean>()
        val descriptions = mutableListOf<String?>()

        override suspend fun filter(
            imageDataUrl: String,
            filter: String,
            backImage: String?,
        ): GenerationResult<String> {
            assertEquals("anime", filter)
            assertTrue(imageDataUrl.startsWith("data:image/jpeg;base64,"))
            gate?.await()
            return GenerationResult.Success(generated)
        }

        override suspend fun speech(
            imageDataUrl: String,
            firstGeneration: Boolean,
            imageDescription: String?,
        ): GenerationResult<SpeechGeneration> {
            assertTrue(imageDataUrl.startsWith("data:image/jpg;base64,"))
            this.firstGeneration += firstGeneration
            descriptions += imageDescription
            speechCalls++
            gate?.await()
            return if (failSpeech) {
                GenerationResult.Failure(FailureKind.SERVER, "Speech failed")
            } else {
                GenerationResult.Success(
                    SpeechGeneration(listOf(SpeechSuggestion("Dialogue " + speechCalls)), "A city", true),
                )
            }
        }
    }
}
