package com.comixon.editor.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.comixon.editor.core.EditorProject

/** Single-owner editor session. A drag is recorded as one undo step. */
@Stable
class EditorState(
    initialProject: EditorProject,
    private val historyLimit: Int = 100,
) {
    var project: EditorProject by mutableStateOf(initialProject)
        private set
    var canUndo: Boolean by mutableStateOf(false)
        private set
    var canRedo: Boolean by mutableStateOf(false)
        private set
    private val past = ArrayDeque<EditorProject>()
    private val future = ArrayDeque<EditorProject>()
    private var gestureStart: EditorProject? = null

    init {
        require(historyLimit > 0)
    }

    fun select(transform: (EditorProject) -> EditorProject) {
        project = transform(project)
    }

    fun edit(transform: (EditorProject) -> EditorProject) {
        val before = project
        val after = transform(before)
        if (before == after) return
        if (gestureStart == null) remember(before)
        project = after
        future.clear()
        refresh()
    }

    fun beginGesture() {
        if (gestureStart == null) gestureStart = project
    }

    fun endGesture() {
        gestureStart?.takeIf { it != project }?.let(::remember)
        gestureStart = null
        refresh()
    }

    fun undo() {
        endGesture()
        if (past.isEmpty()) return
        future.addLast(project)
        project = past.removeLast()
        refresh()
    }

    fun redo() {
        endGesture()
        if (future.isEmpty()) return
        past.addLast(project)
        project = future.removeLast()
        refresh()
    }

    fun replace(project: EditorProject) {
        gestureStart = null
        past.clear()
        future.clear()
        this.project = project
        refresh()
    }

    private fun remember(project: EditorProject) {
        past.addLast(project)
        while (past.size > historyLimit) past.removeFirst()
    }

    private fun refresh() {
        canUndo = past.isNotEmpty()
        canRedo = future.isNotEmpty()
    }
}
