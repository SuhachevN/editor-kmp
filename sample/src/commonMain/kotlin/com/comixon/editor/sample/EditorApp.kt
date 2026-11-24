package com.comixon.editor.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.comixon.editor.compose.EditorCanvas
import com.comixon.editor.compose.editorFontFamilies
import com.comixon.editor.compose.rememberEditorTextMeasurer
import com.comixon.editor.compose.rememberFrameScene
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.rendering.prepareFrameScene

@Composable
fun EditorApp() {
    val scope = rememberCoroutineScope()
    val workspace = remember(scope) { EditorWorkspace(scope) }
    val editor = workspace.editor
    val families = editorFontFamilies()
    val measurer = rememberEditorTextMeasurer()
    val scene = rememberFrameScene(editor, workspace.images, families, measurer)
    var cropMode by remember { mutableStateOf(false) }
    val sources =
        editor.project.frames.flatMap { frame ->
            listOfNotNull(frame.originalImageUrl) +
                frame.layers.filterIsInstance<EditorBubbleLayer>().mapNotNull { it.imageUrl }
        }
    LaunchedEffect(sources) { workspace.resolveImages(editor.project) }
    DisposableEffect(workspace) { onDispose { workspace.close() } }

    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = Color(0xFF245855),
                secondary = Color(0xFFB85736),
                surface = Color(0xFFFCFAF6),
                background = Color(0xFFF1EEE7),
            ),
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                EditorHeader(workspace, onExport = { scene?.let(workspace::exportFrame) }, onExportStrip = {
                    workspace.exportStrip(editor.project.frames.map { prepareFrameScene(it, workspace.images, families, measurer) })
                })
                HorizontalDivider()
                LazyRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(editor.project.frames, key = { _, frame -> frame.id }) { index, frame ->
                        FilterChip(
                            selected = frame.id == editor.project.selectedFrameId,
                            onClick = { editor.select { EditorReducer.selectFrame(it, frame.id) } },
                            label = { Text("Frame " + (index + 1)) },
                        )
                    }
                    item { TextButton(onClick = { editor.edit { EditorReducer.addFrame(it) } }) { Text("+ Frame") } }
                }
                BoxWithConstraints(Modifier.weight(1f)) {
                    val wide = maxWidth >= 900.dp
                    if (wide) {
                        Row(Modifier.fillMaxSize()) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(24.dp),
                            ) {
                                EditorCanvas(editor, scene, Modifier.fillMaxSize(), cropMode)
                            }
                            EditorTools(workspace, scene, cropMode, { cropMode = it }, Modifier.width(340.dp).fillMaxSize())
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(12.dp),
                            ) {
                                EditorCanvas(editor, scene, Modifier.fillMaxSize(), cropMode)
                            }
                            EditorTools(workspace, scene, cropMode, { cropMode = it }, Modifier.fillMaxWidth().height(280.dp))
                        }
                    }
                }
                if (workspace.isGenerating) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Text(workspace.generationLabel, Modifier.weight(1f))
                        TextButton(onClick = workspace::cancelGeneration) { Text("Cancel") }
                    }
                }
            }
            workspace.message?.let { message ->
                AlertDialog(
                    onDismissRequest = { workspace.message = null },
                    title = { Text("Comixon Editor") },
                    text = { Text(message) },
                    confirmButton = { TextButton(onClick = { workspace.message = null }) { Text("OK") } },
                )
            }
        }
    }
}

@Composable
private fun EditorHeader(
    workspace: EditorWorkspace,
    onExport: () -> Unit,
    onExportStrip: () -> Unit,
) {
    val state = workspace.editor
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("COMIXON / EDITOR", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = state::undo, enabled = state.canUndo) { Text("Undo") }
            TextButton(onClick = state::redo, enabled = state.canRedo) { Text("Redo") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedButton(onClick = { workspace.openProject() }) { Text("Open") } }
            item { OutlinedButton(onClick = { workspace.saveProject() }) { Text("Save project") } }
            item { OutlinedButton(onClick = { workspace.importImage() }) { Text("Import image") } }
            item { Button(onClick = onExport, enabled = state.project.selectedFrame != null) { Text("Export PNG") } }
            item { OutlinedButton(onClick = onExportStrip, enabled = state.project.frames.isNotEmpty()) { Text("Export strip") } }
        }
    }
}
