package com.comixon.editor.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Versioned, portable document format. Embedded data URLs keep imported images with the project. */
object ProjectCodec {
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            classDiscriminator = "kind"
        }

    fun encode(project: EditorProject): String = json.encodeToString(SavedProject(project = project))

    fun decode(source: String): EditorProject {
        val saved = json.decodeFromString<SavedProject>(source)
        require(saved.version == 1) { "Unsupported project version: " + saved.version }
        return saved.project.validated()
    }

    @Serializable
    private data class SavedProject(
        val version: Int = 1,
        val project: EditorProject,
    )
}

internal fun EditorProject.validated(): EditorProject {
    require(frames.map { it.id }.distinct().size == frames.size) { "Duplicate frame IDs" }
    frames.forEach { frame ->
        require(
            frame.layers
                .map { it.id }
                .distinct()
                .size == frame.layers.size,
        ) { "Duplicate layer IDs" }
        val crop = frame.crop
        require(crop.scale.isFinite() && crop.scale in 1.0..4.0)
        require(crop.offsetX.isFinite() && crop.offsetY.isFinite())
        frame.layers.forEach { layer ->
            val r = layer.bounds
            require(listOf(r.x, r.y, r.width, r.height).all { it.isFinite() })
            require(r.x >= 0 && r.y >= 0 && r.width > 0 && r.height > 0)
            require(r.x + r.width <= EditorCanvasSpec.LOGICAL_WIDTH + 0.01)
            require(r.y + r.height <= EditorCanvasSpec.LOGICAL_HEIGHT + 0.01)
            if (layer is EditorBubbleLayer) {
                require(layer.bubbleIndex in 0..3)
                require(layer.tailEndPoint.x.isFinite() && layer.tailEndPoint.y.isFinite())
                require(layer.tailBasePercent?.isFinite() != false)
            }
        }
    }
    return copy(
        selectedFrameId = selectedFrameId?.takeIf { id -> frames.any { it.id == id } } ?: frames.firstOrNull()?.id,
        selectedLayerId = selectedLayerId?.takeIf { id -> selectedFrame?.layers?.any { it.id == id } == true },
    )
}

val EditorProject.selectedFrame: EditorFrame?
    get() = frames.firstOrNull { it.id == selectedFrameId } ?: frames.firstOrNull()

val EditorProject.selectedLayer: EditorLayer?
    get() = selectedFrame?.layers?.firstOrNull { it.id == selectedLayerId }
