package com.comixon.editor.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.comixon.editor.compose.EditorCanvas
import com.comixon.editor.compose.editorFontFamilies
import com.comixon.editor.compose.rememberEditorTextMeasurer
import com.comixon.editor.compose.rememberFrameScene
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.core.selectedLayer
import com.comixon.editor.rendering.prepareFrameScene

@Composable
fun EditorApp(startWithDemo: Boolean = false) {
    val scope = rememberCoroutineScope()
    val workspace = remember(scope) { EditorWorkspace(scope) }
    DisposableEffect(workspace) { onDispose { workspace.close() } }
    LaunchedEffect(startWithDemo) { if (startWithDemo) workspace.openDemo() }
    EditorRoot(workspace)
}

@Composable
internal fun EditorScreen(workspace: EditorWorkspace) {
    val snackbar = remember { SnackbarHostState() }
    val notice = workspace.notice
    val noticeText = notice?.label?.text()
    LaunchedEffect(notice) {
        if (noticeText != null) {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(noticeText)
        }
    }
    val editor = workspace.editor
    val session = remember(editor) { EditorSession(editor) }
    val settings = remember { EditorServerSettings() }
    val families = editorFontFamilies()
    val measurer = rememberEditorTextMeasurer()
    var showSettings by remember { mutableStateOf(false) }
    var showLayers by remember { mutableStateOf(false) }
    var showFiles by remember { mutableStateOf(false) }
    LaunchedEffect(workspace.storyRevision) {
        if (workspace.storyRevision > 0) session.selectTool(EditorTool.BUBBLE)
    }
    val originalScene = rememberFrameScene(session.canvasState, workspace.images, families, measurer)
    val draft = session.textDraft
    val scene =
        remember(originalScene, draft, workspace.images, families, measurer) {
            val frame =
                if (draft != null) {
                    EditorReducer.updateLayerText(editor.project, draft.layer.id, draft.text, draft.size, draft.font).selectedFrame
                } else {
                    null
                }
            if (frame != null) prepareFrameScene(frame, workspace.images, families, measurer) else originalScene
        }
    val sources =
        editor.project.frames.flatMap { frame ->
            listOfNotNull(frame.originalImageUrl) + frame.generatedImageUrls +
                frame.layers.filterIsInstance<EditorBubbleLayer>().mapNotNull { it.imageUrl }
        }
    LaunchedEffect(sources) { workspace.resolveImages(editor.project) }
    val exportStrip: () -> Unit = {
        workspace.exportStrip(editor.project.frames.map { prepareFrameScene(it, workspace.images, families, measurer) })
    }
    val fileActions =
        listOf<Pair<EditorLabel, () -> Unit>>(
            EditorLabel.NewProject to { workspace.importImage(newProject = true) },
            EditorLabel.Open to { workspace.openProject() },
            EditorLabel.Save to { workspace.saveProject() },
            EditorLabel.Import to { workspace.importImage() },
            EditorLabel.Export to { scene?.let(workspace::exportFrame) },
            EditorLabel.ExportStrip to exportStrip,
            EditorLabel.Layers to { showLayers = true },
            EditorLabel.Settings to { showSettings = true },
        )
    EditorTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                EditorTopBar(
                    workspace,
                    session.isContextual,
                    labeled = session.isPicker || session.textDraft != null,
                    onBack = {
                        if (workspace.isGenerating) {
                            workspace.cancelGeneration()
                        } else if (!session.finishEditing(false)) {
                            showFiles = true
                        }
                    },
                    onDone = { if (!session.finishEditing(true)) exportStrip() },
                    fileActions = fileActions,
                )
                BoxWithConstraints(Modifier.weight(1f).clipToBounds()) {
                    val wide = maxWidth >= 900.dp
                    val selected = editor.project.selectedLayer
                    val contextual = draft != null || session.isPicker || session.cropDraft != null
                    val panelHeight =
                        when {
                            draft != null -> 340.dp
                            session.isPicker ->
                                (if (session.expanded) maxHeight - 120.dp else 420.dp)
                                    .coerceAtLeast(280.dp)
                                    .coerceAtMost((maxHeight - 40.dp).coerceAtLeast(120.dp))
                            session.cropDraft != null -> 290.dp
                            selected != null -> 100.dp
                            else -> 210.dp
                        }
                    if (wide) {
                        Row(Modifier.fillMaxSize()) {
                            EditorCanvas(
                                session.canvasState,
                                scene,
                                Modifier.weight(1f).fillMaxHeight().padding(24.dp),
                                session.cropDraft != null,
                            )
                            EditorPanel(session, workspace, scene, settings, true, Modifier.width(380.dp).fillMaxHeight())
                        }
                    } else {
                        EditorCanvas(
                            session.canvasState,
                            scene,
                            Modifier.fillMaxSize().padding(bottom = if (contextual) 0.dp else panelHeight).clipToBounds(),
                            cropMode = session.cropDraft != null,
                            alignCanvasTop = contextual || selected != null,
                            preserveCanvasWidth = contextual,
                        )
                        EditorPanel(
                            session,
                            workspace,
                            scene,
                            settings,
                            false,
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(panelHeight),
                        )
                    }
                    if (workspace.isGenerating) GenerationOverlay(workspace)
                    SnackbarHost(
                        snackbar,
                        Modifier.align(Alignment.BottomCenter).testTag("editor_generation_notice").padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom =
                                if (wide) {
                                    24.dp
                                } else {
                                    panelHeight +
                                        12.dp
                                },
                        ),
                    )
                }
            }
            if (showSettings) ServerSettingsDialog(settings) { showSettings = false }
            if (showLayers) LayerList(workspace) { showLayers = false }
            if (showFiles) ProjectActionsDialog(fileActions) { showFiles = false }
            workspace.message?.let { message ->
                AlertDialog(
                    onDismissRequest = { workspace.message = null },
                    title = { Text("Comixon Editor") },
                    text = { Text(message) },
                    confirmButton = { TextButton({ workspace.message = null }) { Text(EditorLabel.Done.text()) } },
                )
            }
        }
    }
}

@Composable
private fun GenerationOverlay(workspace: EditorWorkspace) {
    Box(Modifier.fillMaxSize().background(EditorColors.Background.copy(alpha = .6f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(Modifier.size(54.dp), color = EditorColors.Content)
            Text(workspace.generationLabel)
            TextButton(workspace::cancelGeneration) { Text(EditorLabel.Cancel.text()) }
        }
    }
}

@Composable
private fun ProjectActionsDialog(
    actions: List<Pair<EditorLabel, () -> Unit>>,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Comixon Editor") },
        text = {
            Column {
                actions.forEach { (label, action) ->
                    TextButton({
                        onClose()
                        action()
                    }) { Text(label.text()) }
                }
            }
        },
        confirmButton = { TextButton(onClose) { Text(EditorLabel.Cancel.text()) } },
    )
}
