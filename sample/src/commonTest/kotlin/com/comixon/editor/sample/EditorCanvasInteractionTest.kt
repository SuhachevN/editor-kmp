package com.comixon.editor.sample

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.comixon.editor.compose.EditorCanvas
import com.comixon.editor.compose.EditorState
import com.comixon.editor.compose.rememberEditorTextMeasurer
import com.comixon.editor.compose.rememberFrameScene
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorFrame
import com.comixon.editor.core.EditorProject
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.selectedFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class EditorCanvasInteractionTest {
    @Test
    fun draggingABubbleIsOneUndoableEdit() =
        withCanvas { state, initial ->
            onNodeWithContentDescription("Comic editor canvas").performTouchInput {
                swipe(Offset(180f, 180f), Offset(220f, 220f), durationMillis = 500)
            }
            runOnIdle {
                val before = initial.selectedFrame!!.layers.single() as EditorBubbleLayer
                val after =
                    state.project.selectedFrame!!
                        .layers
                        .single() as EditorBubbleLayer
                assertTrue(after.bounds.x > before.bounds.x + 50)
                assertTrue(after.bounds.y > before.bounds.y + 50)
                state.undo()
                assertEquals(initial, state.project)
                assertTrue(!state.canUndo)
                state.redo()
                assertEquals(
                    after,
                    state.project.selectedFrame!!
                        .layers
                        .single(),
                )
            }
        }

    @Test
    fun draggingResizeHandleKeepsTheBubbleMinimum() =
        withCanvas { state, initial ->
            val bubble = initial.selectedFrame!!.layers.single() as EditorBubbleLayer
            val handle =
                Offset(
                    ((bubble.bounds.x + bubble.bounds.width) / 3).toFloat(),
                    ((bubble.bounds.y + bubble.bounds.height) / 3).toFloat(),
                )
            onNodeWithContentDescription("Comic editor canvas").performTouchInput {
                swipe(handle, handle - Offset(130f, 130f), durationMillis = 500)
            }
            runOnIdle {
                val resized =
                    state.project.selectedFrame!!
                        .layers
                        .single() as EditorBubbleLayer
                assertTrue(resized.bounds.width < bubble.bounds.width)
                assertTrue(resized.bounds.width >= 270.0)
                assertTrue(resized.bounds.height >= 198.0 - .0001)
                assertEquals(bubble.bounds.x, resized.bounds.x)
                state.undo()
                assertEquals(initial, state.project)
            }
        }

    @Test
    fun draggingTheTailKeepsTheBodyInPlace() =
        withCanvas { state, initial ->
            val before = initial.selectedFrame!!.layers.single() as EditorBubbleLayer
            val tip = Offset((before.tailEndPoint.x / 3).toFloat(), (before.tailEndPoint.y / 3).toFloat())
            onNodeWithContentDescription("Comic editor canvas").performTouchInput {
                swipe(tip, tip + Offset(35f, 20f), durationMillis = 500)
            }
            runOnIdle {
                val after =
                    state.project.selectedFrame!!
                        .layers
                        .single() as EditorBubbleLayer
                assertEquals(before.bounds, after.bounds)
                assertTrue(after.tailEndPoint.x > before.tailEndPoint.x)
                state.undo()
                assertEquals(initial, state.project)
            }
        }

    @Test
    fun cropDragIsOneUndoableEdit() =
        withCanvas(cropMode = true) { state, initial ->
            val before = state.project
            onNodeWithContentDescription("Comic editor canvas").performTouchInput {
                swipe(Offset(180f, 200f), Offset(240f, 250f), durationMillis = 500)
            }
            runOnIdle {
                assertTrue(
                    state.project.selectedFrame!!
                        .crop.offsetX > 50,
                )
                state.undo()
                assertEquals(before, state.project)
                state.undo()
                assertEquals(initial, state.project)
            }
        }

    private fun withCanvas(
        cropMode: Boolean = false,
        block: ComposeUiTest.(EditorState, EditorProject) -> Unit,
    ) = runComposeUiTest {
        val initial = EditorReducer.addDefaultBubble(EditorProject(listOf(EditorFrame("frame"))), "Hello, world!")
        val state = EditorState(initial)
        if (cropMode) state.edit { EditorReducer.transformSelectedFrameCrop(it, 2.0, 0.0, 0.0) }
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val measurer = rememberEditorTextMeasurer()
                val scene = rememberFrameScene(state, emptyMap(), emptyMap(), measurer)
                EditorCanvas(state, scene, Modifier.size(360.dp, 480.dp), cropMode)
            }
        }
        block(state, initial)
    }
}
