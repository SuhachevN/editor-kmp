package com.comixon.editor.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.sample.resources.Res
import com.comixon.editor.sample.resources.editor_nav_bubbles
import com.comixon.editor.sample.resources.editor_nav_crop
import com.comixon.editor.sample.resources.editor_nav_filters
import com.comixon.editor.sample.resources.editor_nav_frames
import com.comixon.editor.sample.resources.editor_nav_lettering
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal enum class EditorTool(
    val label: EditorLabel,
    val icon: DrawableResource,
) {
    FRAME(EditorLabel.Frames, Res.drawable.editor_nav_frames),
    CROP(EditorLabel.Crop, Res.drawable.editor_nav_crop),
    BUBBLE(EditorLabel.Bubbles, Res.drawable.editor_nav_bubbles),
    LETTERING(EditorLabel.Lettering, Res.drawable.editor_nav_lettering),
    FILTER(EditorLabel.Filters, Res.drawable.editor_nav_filters),
}

@Composable
internal fun EditorTopBar(
    workspace: EditorWorkspace,
    contextual: Boolean,
    labeled: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
    fileActions: List<Pair<EditorLabel, () -> Unit>>,
) {
    val state = workspace.editor
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 20.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (contextual && labeled) {
            HeaderPill(EditorLabel.Back, onBack, Modifier.testTag("editor_back"), leading = true)
        } else {
            RoundEditorButton(
                if (contextual || workspace.isGenerating) Icons.Outlined.Close else Icons.AutoMirrored.Filled.ArrowBack,
                if (contextual || workspace.isGenerating) EditorLabel.Cancel else EditorLabel.Back,
                onBack,
                modifier = Modifier.testTag("editor_back"),
            )
        }
        if (!contextual) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(state::undo, enabled = state.canUndo && !workspace.isGenerating) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, EditorLabel.Undo.text(), Modifier.size(20.dp))
                }
                IconButton(state::redo, enabled = state.canRedo && !workspace.isGenerating) {
                    Icon(Icons.AutoMirrored.Outlined.Redo, EditorLabel.Redo.text(), Modifier.size(20.dp))
                }
                Box {
                    IconButton({ menu = true }) { Icon(Icons.Outlined.MoreHoriz, EditorLabel.Settings.text()) }
                    DropdownMenu(menu, { menu = false }) {
                        fileActions.forEach { (label, action) ->
                            DropdownMenuItem(text = { Text(label.text()) }, onClick = {
                                menu = false
                                action()
                            })
                        }
                    }
                }
            }
        }
        if (contextual && !labeled) {
            RoundEditorButton(Icons.Outlined.Check, EditorLabel.Done, onDone, Modifier.testTag("editor_done"))
        } else {
            HeaderPill(EditorLabel.Done, onDone, Modifier.testTag("editor_done"), enabled = !workspace.isGenerating)
        }
    }
}

@Composable
private fun HeaderPill(
    label: EditorLabel,
    onClick: () -> Unit,
    modifier: Modifier,
    leading: Boolean = false,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(EditorColors.Control)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (leading) Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(20.dp))
        Text(label.text(), fontSize = 17.sp)
        if (!leading) Icon(Icons.Outlined.Check, null, Modifier.size(22.dp))
    }
}

@Composable
internal fun RoundEditorButton(
    icon: ImageVector,
    label: EditorLabel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    IconButton(
        onClick,
        modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (accent) EditorColors.Accent else EditorColors.Control),
    ) {
        Icon(
            icon,
            label.text(),
            Modifier.size(21.dp),
            tint = if (accent) EditorColors.Background else EditorColors.Content,
        )
    }
}

@Composable
internal fun EditorToolTabs(
    selected: EditorTool,
    onSelected: (EditorTool) -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(76.dp), verticalAlignment = Alignment.CenterVertically) {
        EditorTool.entries.forEach { tool ->
            val color = if (tool == selected) EditorColors.Accent else EditorColors.Content
            Column(
                Modifier
                    .weight(1f)
                    .height(64.dp)
                    .selectable(tool == selected, role = Role.Tab, onClick = { onSelected(tool) })
                    .testTag("editor_tool_" + tool.name)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painterResource(tool.icon), null, Modifier.size(26.dp), tint = color)
                Text(tool.label.text(), color = color, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun EditorFrameStrip(workspace: EditorWorkspace) {
    val state = workspace.editor
    LazyRow(
        Modifier.fillMaxWidth().height(66.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding =
            PaddingValues(horizontal = 20.dp),
    ) {
        itemsIndexed(state.project.frames, key = { _, frame -> frame.id }) { index, frame ->
            val selected = frame.id == state.project.selectedFrameId
            Box(
                Modifier
                    .size(44.dp, 54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EditorColors.Control)
                    .border(
                        if (selected) 2.dp else 1.dp,
                        if (selected) EditorColors.Accent else EditorColors.Divider,
                        RoundedCornerShape(8.dp),
                    ).selectable(
                        selected,
                        role = Role.Tab,
                        onClick = { state.select { EditorReducer.selectFrame(it, frame.id) } },
                    ).testTag("editor_frame_" + index),
                contentAlignment = Alignment.Center,
            ) {
                val image = workspace.images[frame.originalImageUrl]
                if (image != null) {
                    Image(
                        image,
                        toolFrameDescription(index),
                        Modifier.fillMaxSize().padding(2.dp),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text((index + 1).toString(), color = EditorColors.Muted)
                }
            }
        }
        item { RoundEditorButton(Icons.Outlined.AddPhotoAlternate, EditorLabel.AddFrame, { workspace.importImage() }) }
    }
}

@Composable
private fun toolFrameDescription(index: Int): String = EditorLabel.Frames.text() + " " + (index + 1)

@Composable
internal fun EditorSectionTitle(label: EditorLabel) {
    Text(
        label.text(),
        Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
    )
}
