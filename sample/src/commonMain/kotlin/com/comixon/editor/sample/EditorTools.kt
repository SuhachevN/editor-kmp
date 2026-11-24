package com.comixon.editor.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.RotateLeft
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.comixon.editor.compose.EditorState
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.network.ServerConfig

@Stable
internal class EditorServerSettings {
    var baseUrl by mutableStateOf("https://dev.comixon.com")
    var token by mutableStateOf("")
    var language by mutableStateOf("ru-RU")

    val isValid: Boolean
        get() = runCatching { ServerConfig(baseUrl.trim()) }.isSuccess

    fun snapshot(): ServerConfig {
        val requestToken = token.trim().ifEmpty { null }
        return ServerConfig(baseUrl.trim(), language.trim(), { requestToken })
    }
}

@Composable
internal fun ServerSettingsDialog(
    settings: EditorServerSettings,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(EditorLabel.Settings.text()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    settings.baseUrl,
                    { settings.baseUrl = it },
                    label = { Text(EditorLabel.Server.text()) },
                    isError = !settings.isValid,
                    singleLine = true,
                )
                OutlinedTextField(
                    settings.token,
                    { settings.token = it },
                    label = { Text(EditorLabel.Token.text()) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                OutlinedTextField(
                    settings.language,
                    { settings.language = it },
                    label = { Text(EditorLabel.Language.text()) },
                    singleLine = true,
                )
                Text(EditorLabel.TokenHint.text())
            }
        },
        confirmButton = { TextButton(onClose, enabled = settings.isValid) { Text(EditorLabel.Done.text()) } },
    )
}

@Composable
internal fun CropTools(state: EditorState) {
    val frame = state.project.selectedFrame ?: return
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(EditorLabel.Zoom.text())
        Slider(
            value = frame.crop.scale.toFloat(),
            onValueChange = { zoom -> state.edit { EditorReducer.transformSelectedFrameCrop(it, zoom / frame.crop.scale, 0.0, 0.0) } },
            valueRange = 1f..4f,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            RoundEditorButton(Icons.AutoMirrored.Outlined.RotateLeft, EditorLabel.RotateLeft, {
                state.edit { EditorReducer.rotateSelectedFrameCrop(it, -90) }
            })
            RoundEditorButton(Icons.AutoMirrored.Outlined.RotateRight, EditorLabel.RotateRight, {
                state.edit { EditorReducer.rotateSelectedFrameCrop(it, 90) }
            })
            RoundEditorButton(Icons.Outlined.Flip, EditorLabel.Flip, { state.edit { EditorReducer.flipSelectedFrameCrop(it) } })
            RoundEditorButton(Icons.Outlined.RestartAlt, EditorLabel.Reset, { state.edit(EditorReducer::resetSelectedFrameCrop) })
        }
    }
}

@Composable
internal fun LayerList(
    workspace: EditorWorkspace,
    onClose: () -> Unit,
) {
    val state = workspace.editor
    val frame = state.project.selectedFrame
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(EditorLabel.Layers.text()) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                frame?.layers?.asReversed()?.forEach { layer ->
                    FilterChip(
                        selected = state.project.selectedLayerId == layer.id,
                        onClick = {
                            state.select { EditorReducer.selectLayer(it, layer.id) }
                            onClose()
                        },
                        label = { Text(layer.editorText().ifBlank { EditorLabel.Picture.text() }.take(32)) },
                    )
                }
                if (frame?.layers.isNullOrEmpty()) Text(EditorLabel.NoLayers.text())
                if (frame != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton({ state.edit { EditorReducer.moveFrame(it, frame.id, -1) } }) { Text(EditorLabel.MoveLeft.text()) }
                        TextButton({ state.edit { EditorReducer.moveFrame(it, frame.id, 1) } }) { Text(EditorLabel.MoveRight.text()) }
                        if (state.project.frames.size > 1) {
                            TextButton({
                                state.edit { EditorReducer.deleteFrame(it, frame.id) }
                                onClose()
                            }) { Text(EditorLabel.Delete.text()) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClose) { Text(EditorLabel.Done.text()) } },
    )
}
