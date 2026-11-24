package com.comixon.editor.compose

import com.comixon.editor.core.EditorFrame
import com.comixon.editor.core.EditorProject
import com.comixon.editor.core.EditorReducer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorStateTest {
    private fun initial() = EditorProject(listOf(EditorFrame("frame")))

    @Test fun aGestureIsOneUndoStepAndDoesNotMutateSnapshots() {
        val state = EditorState(EditorReducer.addDefaultBubble(initial(), "Text"))
        val before = state.project
        state.beginGesture()
        repeat(10) { state.edit { EditorReducer.moveLayer(it, "bubble_0", 2.0, 1.0) } }
        state.endGesture()
        val after = state.project
        assertTrue(state.canUndo)
        state.undo()
        assertEquals(before, state.project)
        assertFalse(state.canUndo)
        state.redo()
        assertEquals(after, state.project)
    }

    @Test fun editingAfterUndoDropsTheRedoBranch() {
        val state = EditorState(initial())
        state.edit { EditorReducer.addDefaultBubble(it, "First") }
        state.undo()
        state.edit { EditorReducer.addDefaultBubble(it, "Second") }
        assertFalse(state.canRedo)
    }

    @Test fun selectionAndNoOpDoNotCreateUndoSteps() {
        val state = EditorState(initial())
        state.select { EditorReducer.selectFrame(it, "frame") }
        state.edit { it }
        assertFalse(state.canUndo)
    }

    @Test fun historyIsBoundedAndReplacingADocumentClearsIt() {
        val state = EditorState(initial(), historyLimit = 2)
        repeat(4) { number -> state.edit { EditorReducer.addDefaultBubble(it, number.toString()) } }
        state.undo()
        state.undo()
        assertFalse(state.canUndo)
        assertEquals(
            2,
            state.project.frames
                .single()
                .layers.size,
        )
        state.replace(initial())
        assertFalse(state.canRedo)
    }
}
