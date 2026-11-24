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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comixon.editor.compose.rememberEditorTextMeasurer
import com.comixon.editor.compose.renderFrame
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.ImageFilter
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.network.AiFilters
import com.comixon.editor.rendering.FrameScene
import com.comixon.editor.rendering.prepareFrameScene

@Composable
internal fun EditorGenerationRow(
    workspace: EditorWorkspace,
    onGenerate: () -> Unit,
    enabled: Boolean = true,
) {
    val state = workspace.editor
    val frames = state.project.frames
    val index = frames.indexOfFirst { it.id == state.project.selectedFrameId }
    val history = workspace.selectedSpeechState
    val busy = workspace.isGenerating
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = {
                if (history != null) {
                    workspace.selectSpeechGeneration(-1)
                } else {
                    frames.getOrNull(index - 1)?.let { frame -> state.select { EditorReducer.selectFrame(it, frame.id) } }
                }
            },
            enabled = !busy && (history?.canShowPreviousGeneration ?: (index > 0)),
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, EditorLabel.Previous.text()) }
        Row(
            Modifier
                .weight(1f)
                .testTag("editor_generate")
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF2F8DFF), Color(0xFF8E3CEB), Color(0xFF4A14B8))))
                .clickable(enabled = !busy && enabled, onClick = onGenerate)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.AutoFixHigh, null, Modifier.size(18.dp))
            }
            Text(EditorLabel.Generate.text(), Modifier.padding(start = 7.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        IconButton(
            onClick = {
                if (history != null) {
                    workspace.selectSpeechGeneration(1)
                } else {
                    frames.getOrNull(index + 1)?.let { frame -> state.select { EditorReducer.selectFrame(it, frame.id) } }
                }
            },
            enabled = !busy && (history?.canShowNextGeneration ?: (index in 0 until frames.lastIndex)),
        ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, EditorLabel.Next.text()) }
    }
}

@Composable
internal fun FilterCatalog(
    workspace: EditorWorkspace,
    scene: FrameScene?,
    settings: EditorServerSettings,
    modifier: Modifier = Modifier,
) {
    val frame = workspace.editor.project.selectedFrame ?: return
    val source = workspace.images[frame.originalImageUrl]
    val measurer = rememberEditorTextMeasurer()
    val enabled = scene != null && !workspace.isGenerating && settings.isValid
    LazyColumn(modifier, contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (frame.generatedImageUrls.isNotEmpty()) {
            item { EditorSectionTitle(EditorLabel.Versions) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(frame.generatedImageUrls, key = { it }) { url ->
                        FilterTile(EditorLabel.Versions.text(), workspace.images[url], enabled = enabled) {
                            workspace.editor.edit { EditorReducer.applyGeneratedImage(it, url) }
                        }
                    }
                }
            }
        }
        item { EditorSectionTitle(EditorLabel.Instant) }
        item {
            LazyRow(
                Modifier.background(EditorColors.Control).padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ImageFilter.entries, key = { it.name }) { filter ->
                    val preview =
                        remember(frame.originalImageUrl, frame.crop, source, filter, measurer) {
                            renderFrame(
                                prepareFrameScene(
                                    frame.copy(layers = emptyList(), instantFilterName = filter),
                                    workspace.images,
                                    emptyMap(),
                                    measurer,
                                ),
                                96,
                            )
                        }
                    FilterTile(filter.label.text(), preview, selected = frame.instantFilterName == filter, enabled = enabled) {
                        workspace.editor.edit { EditorReducer.applyInstantFilter(it, filter) }
                    }
                }
            }
        }
        item { EditorSectionTitle(EditorLabel.AiFilters) }
        item {
            TextButton(
                onClick = {
                    workspace.pickBlendImage { image ->
                        scene?.let { workspace.generate(it, settings.snapshot(), "blending", image) }
                    }
                },
                enabled = enabled,
                modifier = Modifier.padding(horizontal = 12.dp),
            ) { Text(EditorLabel.ChooseImage.text()) }
        }
        items(AiFilters.names.filter { it != "blending" }.chunked(4)) { names ->
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                names.forEach { name ->
                    FilterTile(name.replace('_', ' '), source, enabled = enabled) {
                        scene?.let { workspace.generate(it, settings.snapshot(), name) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTile(
    label: String,
    image: ImageBitmap?,
    selected: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(Modifier.width(68.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EditorColors.Control)
                .border(if (selected) 2.dp else 0.dp, if (selected) EditorColors.Accent else Color.Transparent, RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, onClick = onClick),
        ) {
            if (image !=
                null
            ) {
                Image(image, label, Modifier.fillMaxSize().padding(if (selected) 2.dp else 0.dp), contentScale = ContentScale.Crop)
            }
        }
        Text(label, fontSize = 10.sp, maxLines = 1, color = EditorColors.Content)
    }
}

private val ImageFilter.label: EditorLabel
    get() =
        when (this) {
            ImageFilter.NONE -> EditorLabel.Original
            ImageFilter.OLDFILM -> EditorLabel.OldFilm
            ImageFilter.OLDTV -> EditorLabel.OldTv
            ImageFilter.WARM -> EditorLabel.Warm
            ImageFilter.COOL -> EditorLabel.Cool
            ImageFilter.MONO -> EditorLabel.Mono
            ImageFilter.FADED -> EditorLabel.Faded
        }
