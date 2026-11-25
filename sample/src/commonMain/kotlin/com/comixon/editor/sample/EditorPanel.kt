package com.comixon.editor.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorLetterLayer
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.selectedLayer
import com.comixon.editor.rendering.FrameScene

@Composable
internal fun EditorPanel(
    session: EditorSession,
    workspace: EditorWorkspace,
    scene: FrameScene?,
    settings: EditorServerSettings,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    val selected = session.editor.project.selectedLayer
    val defaultText = EditorLabel.NewBubble.text()
    val tool = session.tool
    val draft = session.textDraft
    val shape =
        if (tool == EditorTool.FRAME && draft == null) {
            RoundedCornerShape(0.dp)
        } else {
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        }
    Column(modifier.clip(shape).background(EditorColors.Background)) {
        if (session.isPicker) PickerHandle(session.expanded) { session.expanded = it }
        if ((draft != null || session.isPicker || session.cropDraft != null) && tool != EditorTool.BUBBLE) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundEditorButton(Icons.Outlined.Close, EditorLabel.Cancel, { session.finishEditing(false) })
                Text(if (draft != null) EditorLabel.Text.text() else tool.label.text())
                RoundEditorButton(
                    Icons.Outlined.Check,
                    EditorLabel.Done,
                    { session.finishEditing(true) },
                    accent = true,
                )
            }
        }
        when {
            draft != null -> LayerTextEditor(draft.layer, session::updateTextDraft)
            tool == EditorTool.BUBBLE ->
                BubbleCatalog(
                    onSelected = { style ->
                        session.editor.edit { project ->
                            if (selected is EditorBubbleLayer) {
                                EditorReducer.updateBubbleStyle(project, selected.id, style)
                            } else {
                                EditorReducer.addBubble(project, style, defaultText)
                            }
                        }
                        session.showFrameTools()
                    },
                    modifier = Modifier.weight(1f),
                )
            tool == EditorTool.LETTERING ->
                LetteringCatalog(
                    onSelected = { style ->
                        session.editor.edit { project ->
                            if (selected is EditorLetterLayer) {
                                EditorReducer.updateLetteringStyle(project, selected.id, style)
                            } else {
                                EditorReducer.addLettering(project, style, "WOW!")
                            }
                        }
                        session.showFrameTools()
                    },
                    modifier = Modifier.weight(1f),
                )
            tool == EditorTool.FILTER -> FilterCatalog(workspace, scene, settings, Modifier.weight(1f))
            session.cropDraft != null -> {
                CropTools(requireNotNull(session.cropDraft))
                Spacer(Modifier.weight(1f))
            }
            selected != null ->
                LayerEditingBar(
                    workspace,
                    selected,
                    onText = { session.editText(selected) },
                    onShape = {
                        session.selectTool(
                            if (selected is EditorBubbleLayer) EditorTool.BUBBLE else EditorTool.LETTERING,
                        )
                    },
                )
            else -> {
                if (wide) Spacer(Modifier.weight(1f))
                EditorGenerationRow(
                    workspace,
                    onGenerate = { scene?.let { workspace.generate(it, settings.snapshot()) } },
                    enabled = settings.isValid,
                )
                EditorFrameStrip(workspace)
            }
        }
        if ((draft == null && selected == null) || session.isPicker || session.cropDraft != null) {
            EditorToolTabs(tool, session::selectTool)
        }
    }
}

@Composable
private fun PickerHandle(
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit,
) {
    var drag by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxWidth().height(40.dp).pointerInput(expanded) {
            detectVerticalDragGestures(
                onDragStart = { drag = 0f },
                onVerticalDrag = { change, delta ->
                    change.consume()
                    drag += delta
                },
                onDragEnd = { if (kotlin.math.abs(drag) > 24.dp.toPx()) onExpanded(drag < 0f) },
            )
        },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(42.dp, 5.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(EditorColors.Muted.copy(alpha = .35f)),
        )
    }
}
