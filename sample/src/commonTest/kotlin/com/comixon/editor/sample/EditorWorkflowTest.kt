package com.comixon.editor.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.core.selectedLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class EditorWorkflowTest {
    @Test
    fun generationWithoutAuthorizationShowsANotificationWithoutCreatingAService() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            var serviceCreated = false
            val workspace =
                EditorWorkspace(scope) { _, _ ->
                    serviceCreated = true
                    error("Anonymous generation must not create a network service")
                }
            try {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        Box(Modifier.size(400.dp, 800.dp)) { EditorScreen(workspace) }
                    }
                }
                onNodeWithTag("editor_generate").performClick()
                waitUntil(timeoutMillis = 5_000) { workspace.notice != null }
                onNodeWithTag("editor_generation_notice").assertIsDisplayed()
                runOnIdle {
                    assertEquals(EditorLabel.AuthorizationRequired, workspace.notice!!.label)
                    assertEquals(false, serviceCreated)
                    assertEquals(false, workspace.isGenerating)
                    assertEquals(false, workspace.editor.canUndo)
                }
            } finally {
                workspace.close()
                scope.cancel()
            }
        }

    @Test
    fun catalogTextEditingAndUndoWorkTogether() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val workspace = EditorWorkspace(scope)
            try {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        Box(Modifier.size(400.dp, 800.dp)) { EditorScreen(workspace) }
                    }
                }
                val originalCount =
                    workspace.editor.project.selectedFrame!!
                        .layers.size
                onNodeWithTag("editor_tool_BUBBLE").performClick()
                onNodeWithTag("editor_style_CLASSIC_0").performClick()
                var insertedText = ""
                runOnIdle {
                    assertEquals(
                        originalCount + 1,
                        workspace.editor.project.selectedFrame!!
                            .layers.size,
                    )
                    insertedText = (workspace.editor.project.selectedLayer as EditorBubbleLayer).text
                }
                onNodeWithTag("editor_action_Text").performClick()
                onNode(hasSetTextAction()).performTextReplacement("Привет, мир!")
                onNodeWithTag("editor_done").performClick()
                runOnIdle {
                    assertEquals("Привет, мир!", (workspace.editor.project.selectedLayer as EditorBubbleLayer).text)
                    workspace.editor.undo()
                    assertEquals(insertedText, (workspace.editor.project.selectedLayer as EditorBubbleLayer).text)
                    workspace.editor.redo()
                    assertEquals("Привет, мир!", (workspace.editor.project.selectedLayer as EditorBubbleLayer).text)
                }
            } finally {
                workspace.close()
                scope.cancel()
            }
        }

    @Test
    fun cropIsPreviewedUntilConfirmationAndCanBeCancelled() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val workspace = EditorWorkspace(scope)
            try {
                setContent {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        Box(Modifier.size(400.dp, 800.dp)) { EditorScreen(workspace) }
                    }
                }
                val before = workspace.editor.project
                onNodeWithTag("editor_tool_CROP").performClick()
                onNodeWithTag("editor_crop_zoom").performTouchInput { click(center) }
                onNodeWithContentDescription("Comic editor canvas").performTouchInput {
                    swipe(Offset(160f, 200f), Offset(220f, 250f), durationMillis = 500)
                }
                onNodeWithTag("editor_back").performClick()
                runOnIdle {
                    assertEquals(before, workspace.editor.project)
                    assertEquals(false, workspace.editor.canUndo)
                }
                onNodeWithTag("editor_tool_CROP").performClick()
                onNodeWithTag("editor_crop_zoom").performTouchInput { click(center) }
                onNodeWithTag("editor_done").performClick()
                runOnIdle {
                    assertTrue(
                        workspace.editor.project.selectedFrame!!
                            .crop.scale in 2.4..2.6,
                    )
                    workspace.editor.undo()
                    assertEquals(before, workspace.editor.project)
                }
            } finally {
                workspace.close()
                scope.cancel()
            }
        }
}
