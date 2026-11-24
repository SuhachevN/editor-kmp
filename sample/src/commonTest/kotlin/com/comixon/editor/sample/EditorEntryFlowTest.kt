package com.comixon.editor.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, kotlin.io.encoding.ExperimentalEncodingApi::class)
class EditorEntryFlowTest {
    @Test
    fun selectedPhotosBecomeFramesInOrderWithoutDemoLayers() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val photos = listOf(importedEditorImage(ImageBitmap(32, 48)), importedEditorImage(ImageBitmap(48, 32)))
            val workspace =
                EditorWorkspace(scope, mediaPicker = { multiple ->
                    assertTrue(multiple)
                    photos
                })
            try {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        Box(Modifier.size(400.dp, 800.dp)) { EditorRoot(workspace) }
                    }
                }
                onNodeWithTag("editor_choose_photos").performClick()
                waitUntil(timeoutMillis = 5_000) { workspace.isEditorOpen }
                onNodeWithTag("editor_tool_FRAME").assertIsDisplayed()
                runOnIdle {
                    val project = workspace.editor.project
                    assertEquals(photos.map { it.source }, project.frames.map { it.originalImageUrl })
                    assertTrue(project.frames.all { it.layers.isEmpty() })
                    assertEquals(project.frames.first().id, project.selectedFrameId)
                    assertEquals(false, workspace.editor.canUndo)
                }
            } finally {
                workspace.close()
                scope.cancel()
            }
        }

    @Test
    fun cancellingThePickerKeepsTheStartScreenAndDocument() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            var calls = 0
            val workspace =
                EditorWorkspace(scope, mediaPicker = {
                    calls++
                    null
                })
            val before = workspace.editor.project
            try {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        Box(Modifier.size(400.dp, 800.dp)) { EditorRoot(workspace) }
                    }
                }
                onNodeWithTag("editor_choose_photos").performClick()
                waitUntil(timeoutMillis = 5_000) { calls == 1 && !workspace.isImporting }
                onNodeWithTag("editor_choose_photos").assertIsDisplayed()
                runOnIdle {
                    assertEquals(before, workspace.editor.project)
                    assertEquals(false, workspace.isEditorOpen)
                }
            } finally {
                workspace.close()
                scope.cancel()
            }
        }

    @Test
    fun addingMultiplePhotosToAnExistingProjectIsOneUndoStep() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val photos = listOf(importedEditorImage(ImageBitmap(32, 48)), importedEditorImage(ImageBitmap(48, 32)))
            val workspace = EditorWorkspace(scope, mediaPicker = { photos })
            val before = workspace.editor.project
            try {
                setContent { }
                runOnIdle { workspace.importImage() }
                waitUntil(timeoutMillis = 5_000) { workspace.isEditorOpen && !workspace.isImporting }
                runOnIdle {
                    assertEquals(before.frames.size + 2, workspace.editor.project.frames.size)
                    workspace.editor.undo()
                    assertEquals(before, workspace.editor.project)
                    workspace.editor.redo()
                    assertEquals(before.frames.size + 2, workspace.editor.project.frames.size)
                }
            } finally {
                workspace.close()
                scope.cancel()
            }
        }

    @Test
    fun importedImagesPreserveAspectRatioTransparencyAndPortableEncoding() {
        val image = importedEditorImage(ImageBitmap(3000, 1500))
        assertEquals(2048, image.bitmap.width)
        assertEquals(1024, image.bitmap.height)
        val restored = Base64.decode(image.source.substringAfter(',')).decodeToImageBitmap()
        assertEquals(2048, restored.width)
        assertEquals(1024, restored.height)
        assertEquals(0f, restored.toPixelMap()[0, 0].alpha)
    }
}
