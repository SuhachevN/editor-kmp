package com.comixon.editor.sample

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.comixon.editor.core.EditorFrame
import com.comixon.editor.core.EditorProject
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.FrameSpeechState
import com.comixon.editor.core.ProjectCodec
import com.comixon.editor.core.SpeechSuggestion
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.network.ComixonGenerationService
import com.comixon.editor.network.EditorGenerationService
import com.comixon.editor.network.GenerationResult
import com.comixon.editor.network.ServerConfig
import com.comixon.editor.network.StoryGenerationService
import com.comixon.editor.network.createEditorHttpClient
import com.comixon.editor.rendering.FrameScene
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.coroutineContext

internal data class EditorNotice(
    val label: EditorLabel,
    val sequence: Int,
)

@Stable
internal class EditorWorkspace(
    private val scope: CoroutineScope,
    private val mediaPicker: suspend (Boolean) -> List<ImportedEditorImage>? = ::pickEditorImages,
    private val serviceFactory: (
        io.ktor.client.HttpClient,
        ServerConfig,
    ) -> EditorGenerationService = ::ComixonGenerationService,
) {
    var isEditorOpen by mutableStateOf(false)
        private set
    var isImporting by mutableStateOf(false)
        private set
    val editor = EditorState(demoProject())
    val client = createEditorHttpClient()
    var images: Map<String, ImageBitmap> by mutableStateOf(emptyMap())
        private set
    var notice: EditorNotice? by mutableStateOf(null)
        private set
    var message: String? by mutableStateOf(null)
    var isGenerating: Boolean by mutableStateOf(false)
        private set
    var generationLabel: String by mutableStateOf("")
        private set
    var storyRevision by mutableStateOf(0)
        private set
    private var generationJob: Job? = null
    private val speechStates = mutableStateMapOf<String, FrameSpeechState>()

    suspend fun resolveImages(project: EditorProject) {
        val sources =
            project.frames
                .flatMap { frame ->
                    listOfNotNull(frame.originalImageUrl) + frame.generatedImageUrls +
                        frame.layers.filterIsInstance<EditorBubbleLayer>().mapNotNull { it.imageUrl }
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
            isEditorOpen = true
        }

    fun importImage(
        asBubble: Boolean = false,
        newProject: Boolean = false,
    ) {
        if (isImporting) return
        isImporting = true
        task {
            try {
                val picked = mediaPicker(!asBubble)?.takeIf { it.isNotEmpty() } ?: return@task
                val imported = picked.associate { it.source to it.bitmap }
                if (asBubble) {
                    val id = editor.project.selectedLayerId ?: return@task
                    editor.edit { EditorReducer.updateBubbleImage(it, id, picked.first().source) }
                } else if (newProject) {
                    val project =
                        picked.fold(
                            EditorProject(emptyList()),
                        ) { document, photo -> EditorReducer.addFrame(document, photo.source) }
                    generationJob?.cancel()
                    speechStates.clear()
                    editor.replace(EditorReducer.selectFrame(project, project.frames.first().id))
                } else {
                    editor.edit { document ->
                        picked.fold(document) { project, photo -> EditorReducer.addFrame(project, photo.source) }
                    }
                }
                images = images + imported
                isEditorOpen = true
            } finally {
                isImporting = false
            }
        }
    }

    fun openDemo() {
        generationJob?.cancel()
        speechStates.clear()
        editor.replace(demoProject())
        isEditorOpen = true
    }

    fun pickBlendImage(onSelected: (String) -> Unit) =
        task {
            val image = mediaPicker(false)?.firstOrNull()?.bitmap ?: return@task
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
                generationLabel = if (filter == null) "Creating your story…" else "Applying AI filter…"
                try {
                    val token = if (filter == null) settings.token() else null
                    if (filter == null && token.isNullOrBlank()) {
                        notice = EditorNotice(EditorLabel.AuthorizationRequired, (notice?.sequence ?: 0) + 1)
                        return@launch
                    }
                    val requestSettings =
                        if (filter ==
                            null
                        ) {
                            ServerConfig(settings.baseUrl, settings.language) { token }
                        } else {
                            settings
                        }
                    val service = serviceFactory(client, requestSettings)
                    if (filter == null) {
                        generateStory(frame, scene, service)
                    } else {
                        generateFilter(frame, scene, service, filter, backImage)
                    }
                } catch (_: TimeoutCancellationException) {
                    message = "Generation timed out. Try again."
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    message = "Generation could not be completed. Check the server settings and image."
                } finally {
                    isGenerating = false
                }
            }
    }

    private suspend fun generateFilter(
        frame: EditorFrame,
        scene: FrameScene,
        service: EditorGenerationService,
        filter: String,
        backImage: String?,
    ) {
        val payload = withContext(Dispatchers.Default) { generationImage(scene, speech = false) }
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
    }

    private suspend fun generateStory(
        frame: EditorFrame,
        scene: FrameScene,
        service: EditorGenerationService,
    ) {
        val source = requireNotNull(frame.originalImageUrl)
        val sourceImage = images[source] ?: withContext(Dispatchers.Default) { loadImage(client, source) }
        val previous = speechStates[frame.id]?.takeIf { it.hasGenerationFor(source) }
        val (filterPayload, speechPayload) =
            withContext(Dispatchers.Default) {
                generationImage(scene, speech = false) to generationImage(sourceImage, speech = true)
            }
        val result =
            withTimeout(310_000) {
                StoryGenerationService(
                    service,
                ).generate(filterPayload, speechPayload, previous == null, previous?.imageDesc)
            }
        val generation = (result.speech as? GenerationResult.Success)?.value
        var imageUrl = (result.image as? GenerationResult.Success)?.value
        if (imageUrl != null) {
            try {
                val bitmap = withContext(Dispatchers.Default) { loadImage(client, imageUrl) }
                images = images + (imageUrl to bitmap)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                imageUrl = null
            }
        }
        coroutineContext.ensureActive()
        val suggestions = generation?.suggestions.orEmpty()
        val applied =
            applyToFrame(frame.id, source) { project ->
                val withSpeech = EditorReducer.applySpeechSuggestions(project, suggestions)
                imageUrl?.let { EditorReducer.recordGeneratedImageForSelectedFrame(withSpeech, it) } ?: withSpeech
            } ?: return
        if (generation != null && suggestions.isNotEmpty()) {
            val materialized =
                applied.layers
                    .filterIsInstance<EditorBubbleLayer>()
                    .take(
                        suggestions.size,
                    ).map { SpeechSuggestion(it.text, it.bounds) }
            val history = (previous?.generations.orEmpty() + listOf(materialized)).takeLast(10)
            speechStates[frame.id] =
                FrameSpeechState(
                    generation.hasFirstGeneration,
                    generation.imageDesc ?: previous?.imageDesc,
                    applied.originalImageUrl,
                    history,
                )
            if (editor.project.selectedFrameId == frame.id) storyRevision++
        }
        message =
            when {
                result.speech is GenerationResult.Failure -> (result.speech as GenerationResult.Failure).message
                suggestions.isEmpty() -> "The server returned no dialogue."
                imageUrl == null ->
                    (result.image as? GenerationResult.Failure)?.message
                        ?: "The generated image could not be loaded."
                else -> null
            }
    }

    val selectedSpeechState: FrameSpeechState?
        get() {
            val frame = editor.project.selectedFrame ?: return null
            return speechStates[frame.id]?.takeIf { it.hasGenerationFor(frame.originalImageUrl) }
        }

    fun selectSpeechGeneration(delta: Int) {
        val frame = editor.project.selectedFrame ?: return
        val history = selectedSpeechState ?: return
        val index = history.currentGenerationIndex + delta
        val suggestions = history.generations.getOrNull(index) ?: return
        speechStates[frame.id] = history.copy(currentGenerationIndex = index)
        editor.edit { EditorReducer.applySpeechSuggestions(it, suggestions) }
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
    ): EditorFrame? {
        val current = editor.project
        if (current.frames.none { it.id == id && it.originalImageUrl == source }) {
            message = "The source frame changed. The generation was not applied."
            return null
        }
        val transformed = transform(current.copy(selectedFrameId = id))
        val updated = transformed.frames.first { it.id == id }
        editor.edit { project ->
            project.copy(
                frames = project.frames.map { if (it.id == id) updated else it },
                selectedLayerId =
                    if (project.selectedFrameId ==
                        id
                    ) {
                        transformed.selectedLayerId
                    } else {
                        project.selectedLayerId
                    },
            )
        }
        return updated
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
