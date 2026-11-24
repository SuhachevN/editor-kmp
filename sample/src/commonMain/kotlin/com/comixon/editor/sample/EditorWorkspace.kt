package com.comixon.editor.sample

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.comixon.editor.compose.EditorState
import com.comixon.editor.compose.ImageEncoding
import com.comixon.editor.compose.encodeImage
import com.comixon.editor.compose.generationImage
import com.comixon.editor.compose.renderFrame
import com.comixon.editor.compose.renderStrip
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorProject
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.FrameSpeechState
import com.comixon.editor.core.ProjectCodec
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.network.ComixonGenerationService
import com.comixon.editor.network.GenerationResult
import com.comixon.editor.network.ServerConfig
import com.comixon.editor.network.createEditorHttpClient
import com.comixon.editor.rendering.FrameScene
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.coroutines.coroutineContext
import kotlin.io.encoding.Base64

@Stable
internal class EditorWorkspace(
    private val scope: CoroutineScope,
) {
    val editor = EditorState(demoProject())
    val client = createEditorHttpClient()
    var images: Map<String, ImageBitmap> by mutableStateOf(emptyMap())
        private set
    var message: String? by mutableStateOf(null)
    var isGenerating: Boolean by mutableStateOf(false)
        private set
    var generationLabel: String by mutableStateOf("")
        private set
    private var generationJob: Job? = null
    private val speechStates = mutableMapOf<String, FrameSpeechState>()

    suspend fun resolveImages(project: EditorProject) {
        val sources =
            project.frames
                .flatMap { frame ->
                    listOfNotNull(frame.originalImageUrl) + frame.layers.filterIsInstance<EditorBubbleLayer>().mapNotNull { it.imageUrl }
                }.toSet()
        for (source in sources - images.keys) {
            try {
                val image = withContext(Dispatchers.Default) { loadImage(client, source) }
                images = images + (source to image)
            } catch (
                error: CancellationException,
            ) {
                throw error
            } catch (_: Exception) {
                message = "An image could not be loaded. Check your connection or import it again."
            }
        }
        images = images.filterKeys { it in sources }
    }

    fun saveProject() =
        task {
            val bytes = ProjectCodec.encode(editor.project).encodeToByteArray()
            FileKit.openFileSaver("comixon-project", "json")?.write(bytes)
        }

    fun openProject() =
        task {
            val file = FileKit.openFilePicker(FileKitType.File(setOf("json"))) ?: return@task
            val project = ProjectCodec.decode(file.readBytes().decodeToString())
            generationJob?.cancel()
            speechStates.clear()
            editor.replace(project)
        }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    fun importImage(asBubble: Boolean = false) =
        task {
            val file = FileKit.openFilePicker(FileKitType.Image) ?: return@task
            val bytes = file.readBytes()
            val mime =
                when (file.extension.lowercase()) {
                    "jpg", "jpeg" -> "jpeg"
                    "webp" -> "webp"
                    else -> "png"
                }
            val data = "data:image/" + mime + ";base64," + Base64.encode(bytes)
            val image = loadImage(client, data)
            images = images + (data to image)
            editor.edit { project ->
                val id = project.selectedLayerId
                if (asBubble && id != null) {
                    EditorReducer.updateBubbleImage(project, id, data)
                } else {
                    EditorReducer.addFrame(project, data)
                }
            }
        }

    fun pickBlendImage(onSelected: (String) -> Unit) =
        task {
            val file = FileKit.openFilePicker(FileKitType.Image) ?: return@task
            val image = file.readBytes().decodeToImageBitmap()
            val payload = withContext(Dispatchers.Default) { generationImage(image) }
            onSelected(payload)
        }

    fun exportFrame(scene: FrameScene) =
        task {
            val bytes = withContext(Dispatchers.Default) { encodeImage(renderFrame(scene), ImageEncoding.PNG) }
            FileKit.openFileSaver("comixon-frame", "png")?.write(bytes)
        }

    fun exportStrip(scenes: List<FrameScene>) =
        task {
            val bytes = withContext(Dispatchers.Default) { encodeImage(renderStrip(scenes), ImageEncoding.PNG) }
            FileKit.openFileSaver("comixon-strip", "png")?.write(bytes)
        }

    fun generate(
        scene: FrameScene,
        settings: ServerConfig,
        filter: String? = null,
        backImage: String? = null,
    ) {
        if (isGenerating) return
        val frame = editor.project.selectedFrame ?: return
        if (frame.originalImageUrl == null) {
            message = "Import an image first."
            return
        }
        generationJob =
            scope.launch {
                isGenerating = true
                generationLabel = if (filter == null) "Writing dialogue…" else "Applying AI filter…"
                try {
                    val payload = withContext(Dispatchers.Default) { generationImage(scene, speech = filter == null) }
                    val service = ComixonGenerationService(client, settings)
                    if (filter != null) {
                        when (val result = service.filter(payload, filter, backImage)) {
                            is GenerationResult.Success -> {
                                val image = withContext(Dispatchers.Default) { loadImage(client, result.value) }
                                coroutineContext.ensureActive()
                                images = images + (result.value to image)
                                applyToFrame(
                                    frame.id,
                                    frame.originalImageUrl,
                                ) { EditorReducer.recordGeneratedImageForSelectedFrame(it, result.value) }
                            }
                            is GenerationResult.Failure -> message = result.message
                        }
                    } else {
                        val previous = speechStates[frame.id]?.takeIf { it.previousImageSource == frame.originalImageUrl }
                        when (
                            val result =
                                service.speech(
                                    payload,
                                    firstGeneration = previous == null,
                                    imageDescription = previous?.imageDesc,
                                )
                        ) {
                            is GenerationResult.Success -> {
                                coroutineContext.ensureActive()
                                val generation = result.value
                                speechStates[frame.id] =
                                    FrameSpeechState(
                                        true,
                                        generation.imageDesc,
                                        frame.originalImageUrl,
                                        previous?.generations.orEmpty() + listOf(generation.suggestions),
                                    )
                                applyToFrame(
                                    frame.id,
                                    frame.originalImageUrl,
                                ) { EditorReducer.applySpeechSuggestions(it, generation.suggestions) }
                            }
                            is GenerationResult.Failure -> message = result.message
                        }
                    }
                } catch (
                    error: CancellationException,
                ) {
                    throw error
                } catch (
                    _: Exception,
                ) {
                    message = "Generation could not be completed. Check the server settings and image."
                } finally {
                    isGenerating = false
                }
            }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
    }

    fun close() {
        generationJob?.cancel()
        client.close()
    }

    private fun applyToFrame(
        id: String,
        source: String?,
        transform: (EditorProject) -> EditorProject,
    ) {
        val target = editor.project.frames.firstOrNull { it.id == id && it.originalImageUrl == source }
        if (target == null) {
            message = "The source frame changed. The generation was not applied."
            return
        }
        editor.edit { current ->
            val updated = transform(current.copy(selectedFrameId = id)).frames.first { it.id == id }
            current.copy(frames = current.frames.map { if (it.id == id) updated else it })
        }
    }

    private fun task(block: suspend () -> Unit) =
        scope.launch {
            try {
                block()
            } catch (
                error: CancellationException,
            ) {
                throw error
            } catch (_: Exception) {
                message = "The file operation failed. Check the file format and destination."
            }
        }
}
