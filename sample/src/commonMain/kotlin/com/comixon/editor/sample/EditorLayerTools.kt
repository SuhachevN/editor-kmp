package com.comixon.editor.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorFont
import com.comixon.editor.core.EditorLayer
import com.comixon.editor.core.EditorLetterLayer
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.FontSize
import com.comixon.editor.sample.resources.Res
import com.comixon.editor.sample.resources.editor_action_delete
import com.comixon.editor.sample.resources.editor_action_picture
import com.comixon.editor.sample.resources.editor_action_shape
import com.comixon.editor.sample.resources.editor_action_tail
import com.comixon.editor.sample.resources.editor_nav_lettering
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal fun EditorLayer.editorText(): String =
    when (this) {
        is EditorBubbleLayer -> text
        is EditorLetterLayer -> text
    }

internal val EditorLayer.editorFont: EditorFont
    get() =
        when (this) {
            is EditorBubbleLayer -> font
            is EditorLetterLayer -> font
        }

internal val EditorLayer.editorSize: FontSize
    get() =
        when (this) {
            is EditorBubbleLayer -> fontSizeName
            is EditorLetterLayer -> fontSizeName
        }

@Composable
internal fun LayerEditingBar(
    workspace: EditorWorkspace,
    layer: EditorLayer,
    onText: () -> Unit,
    onShape: () -> Unit,
) {
    val state = workspace.editor
    Row(Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (layer is EditorBubbleLayer) {
            LayerAction(Res.drawable.editor_action_tail, EditorLabel.Tail, Modifier.weight(1f)) {
                state.edit { EditorReducer.toggleBubbleTail(it, layer.id) }
            }
            LayerAction(
                Res.drawable.editor_action_picture,
                EditorLabel.Picture,
                Modifier.weight(1f),
            ) { workspace.importImage(asBubble = true) }
        }
        LayerAction(Res.drawable.editor_nav_lettering, EditorLabel.Text, Modifier.weight(1f), onText)
        LayerAction(Res.drawable.editor_action_shape, EditorLabel.Shape, Modifier.weight(1f), onShape)
        LayerAction(Res.drawable.editor_action_delete, EditorLabel.Delete, Modifier.weight(1f)) {
            state.edit { EditorReducer.deleteLayer(it, layer.id) }
        }
    }
}

@Composable
private fun LayerAction(
    icon: DrawableResource,
    label: EditorLabel,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .testTag(
                "editor_action_" + label.name,
            ).clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            painterResource(icon),
            label.text(),
            Modifier.size(26.dp),
            tint =
                if (label ==
                    EditorLabel.Delete
                ) {
                    EditorColors.Delete
                } else {
                    EditorColors.Content
                },
        )
        Text(label.text(), fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
internal fun LayerTextEditor(
    layer: EditorLayer,
    onDraft: (String, EditorFont, FontSize) -> Unit,
) {
    var text by remember(layer.id) { mutableStateOf(TextFieldValue(layer.editorText())) }
    var dictationBase by remember(layer.id) { mutableStateOf(text) }
    var speechError by remember(layer.id) { mutableStateOf<SpeechInputError?>(null) }
    var font by remember(layer.id) { mutableStateOf(layer.editorFont) }
    var size by remember(layer.id) { mutableStateOf(layer.editorSize) }
    val speech =
        rememberSpeechInput(onText = { transcript ->
            text = insertDictation(dictationBase, transcript)
            onDraft(text.text, font, size)
        }, onError = { speechError = it })

    fun startDictation() {
        speechError = null
        dictationBase = text.copy(composition = null)
        speech.start()
    }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            text,
            {
                speech.cancel()
                text = it
                onDraft(text.text, font, size)
            },
            Modifier.fillMaxWidth(),
            label = { Text(EditorLabel.Text.text()) },
            trailingIcon = {
                DictationButton(speech, onStart = ::startDictation)
            },
            supportingText = { Text((speechError?.label ?: EditorLabel.DictationHint).text(), fontSize = 11.sp) },
            minLines = 2,
            maxLines = 4,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(EditorFont.entries) { item ->
                FilterChip(
                    selected = font == item,
                    onClick = {
                        font = item
                        onDraft(text.text, font, size)
                    },
                    label = { Text(item.name.replace('_', ' ')) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FontSize.entries.forEach { item ->
                FilterChip(selected = size == item, onClick = {
                    size = item
                    onDraft(text.text, font, size)
                }, label = { Text(item.name) })
            }
        }
    }
}

private val SpeechInputError.label: EditorLabel
    get() =
        when (this) {
            SpeechInputError.UNAVAILABLE -> EditorLabel.SpeechUnavailable
            SpeechInputError.PERMISSION -> EditorLabel.SpeechPermission
            SpeechInputError.LANGUAGE -> EditorLabel.SpeechLanguage
            SpeechInputError.NO_MATCH -> EditorLabel.SpeechNoMatch
            SpeechInputError.NETWORK -> EditorLabel.SpeechNetwork
            SpeechInputError.FAILED -> EditorLabel.SpeechFailed
        }

@Composable
private fun DictationButton(
    speech: SpeechInput,
    onStart: () -> Unit,
) {
    val active = speech.state != SpeechInputState.IDLE
    val description = (if (active) EditorLabel.StopDictation else EditorLabel.Dictate).text()
    Icon(
        if (active) Icons.Default.Stop else Icons.Default.Mic,
        contentDescription = description,
        tint = if (active) EditorColors.Delete else EditorColors.Accent,
        modifier =
            Modifier
                .size(48.dp)
                .testTag("editor_dictation")
                .semantics {
                    onClick(description) {
                        if (speech.state == SpeechInputState.IDLE) onStart() else speech.stop()
                        true
                    }
                }.pointerInput(speech) {
                    detectTapGestures(onPress = {
                        onStart()
                        if (tryAwaitRelease()) speech.stop() else speech.cancel()
                    })
                }.padding(12.dp),
    )
}
