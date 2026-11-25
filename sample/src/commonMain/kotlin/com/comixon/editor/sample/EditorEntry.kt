package com.comixon.editor.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorRoot(workspace: EditorWorkspace) {
    if (workspace.isEditorOpen) EditorScreen(workspace) else EditorEntry(workspace)
}

@Composable
private fun EditorEntry(workspace: EditorWorkspace) {
    EditorTheme {
        Surface(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.widthIn(max = 420.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.size(64.dp), tint = EditorColors.Accent)
                    Text("Comixon Editor", style = MaterialTheme.typography.headlineLarge)
                    Text(EditorLabel.PickPhotosHint.text(), textAlign = TextAlign.Center, color = EditorColors.Muted)
                    Button(
                        onClick = { workspace.importImage(newProject = true) },
                        modifier = Modifier.fillMaxWidth().testTag("editor_choose_photos"),
                        enabled = !workspace.isImporting,
                    ) { Text(EditorLabel.PickPhotos.text()) }
                    TextButton(
                        onClick = workspace::openDemo,
                        modifier = Modifier.testTag("editor_open_demo"),
                        enabled = !workspace.isImporting,
                    ) { Text(EditorLabel.OpenDemo.text()) }
                    TextButton(
                        onClick = { workspace.openProject() },
                        enabled = !workspace.isImporting,
                    ) { Text(EditorLabel.Open.text()) }
                    if (workspace.isImporting) CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
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
