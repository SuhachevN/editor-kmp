package com.comixon.editor.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.comixon.editor.core.BubbleFamily
import com.comixon.editor.core.EditorBubbleLayer
import com.comixon.editor.core.EditorFont
import com.comixon.editor.core.EditorLetterLayer
import com.comixon.editor.core.EditorReducer
import com.comixon.editor.core.FontSize
import com.comixon.editor.core.ImageFilter
import com.comixon.editor.core.StyleCatalog
import com.comixon.editor.core.selectedFrame
import com.comixon.editor.core.selectedLayer
import com.comixon.editor.network.AiFilters
import com.comixon.editor.network.ServerConfig
import com.comixon.editor.rendering.FrameScene

private enum class ToolTab { BUBBLES, TEXT, IMAGE, AI, LAYERS }

@Composable
internal fun EditorTools(
    workspace: EditorWorkspace,
    scene: FrameScene?,
    cropMode: Boolean,
    onCropMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(ToolTab.BUBBLES) }
    val state = workspace.editor
    val selected = state.project.selectedLayer
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolTab.entries.forEach { item ->
                FilterChip(
                    selected = tab == item,
                    onClick = { tab = item },
                    label = { Text(item.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
        when (tab) {
            ToolTab.BUBBLES -> {
                Button(
                    onClick = { state.edit { EditorReducer.addDefaultBubble(it, "Your story") } },
                    enabled = state.project.selectedFrame != null,
                ) { Text("Add bubble") }
                BubbleFamily.entries.forEach { family ->
                    Text(
                        family.name
                            .replace('_', ' ')
                            .lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyleCatalog.bubbles.filter { it.type == family }.forEach { style ->
                            FilterChip(
                                selected =
                                    selected is EditorBubbleLayer && selected.type == family && selected.bubbleIndex == style.index,
                                onClick = {
                                    state.edit { project ->
                                        if (selected is EditorBubbleLayer) {
                                            EditorReducer.updateBubbleStyle(project, selected.id, style)
                                        } else {
                                            EditorReducer.addBubble(project, style, "Your story")
                                        }
                                    }
                                },
                                label = { Text((style.index + 1).toString()) },
                            )
                        }
                    }
                }
                if (selected is EditorBubbleLayer) {
                    OutlinedButton(onClick = { state.edit { EditorReducer.toggleBubbleTail(it, selected.id) } }) {
                        Text(if (selected.isTailEnabled) "Hide tail" else "Show tail")
                    }
                    OutlinedButton(onClick = { workspace.importImage(asBubble = true) }) { Text("Image inside bubble") }
                }
            }
            ToolTab.TEXT -> {
                Button(
                    onClick = { state.edit { EditorReducer.addLettering(it, StyleCatalog.defaultLetteringStyle, "WOW!") } },
                    enabled = state.project.selectedFrame != null,
                ) { Text("Add lettering") }
                val text =
                    when (selected) {
                        is EditorBubbleLayer -> selected.text
                        is EditorLetterLayer -> selected.text
                        else -> null
                    }
                if (selected != null && text != null) {
                    OutlinedTextField(value = text, onValueChange = { value ->
                        state.edit {
                            EditorReducer.updateLayerText(it, selected.id, value)
                        }
                    }, modifier = Modifier.fillMaxWidth(), label = { Text("Text") })
                    Text("Size", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FontSize.entries.forEach { size ->
                            FilterChip(
                                selected =
                                    size ==
                                        when (selected) {
                                            is EditorBubbleLayer -> selected.fontSizeName
                                            is EditorLetterLayer -> selected.fontSizeName
                                        },
                                onClick = { state.edit { EditorReducer.updateLayerText(it, selected.id, text, fontSizeName = size) } },
                                label = { Text(size.name) },
                            )
                        }
                    }
                    Text("Font", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EditorFont.entries.forEach { font ->
                            FilterChip(
                                selected =
                                    font ==
                                        when (selected) {
                                            is EditorBubbleLayer -> selected.font
                                            is EditorLetterLayer -> selected.font
                                        },
                                onClick = { state.edit { EditorReducer.updateLayerText(it, selected.id, text, font = font) } },
                                label = { Text(font.name.replace('_', ' ')) },
                            )
                        }
                    }
                    if (selected is EditorLetterLayer) {
                        StyleCatalog.lettering.groupBy { it.type }.forEach { (family, styles) ->
                            Text(family.name, style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                styles.forEach { style ->
                                    FilterChip(
                                        selected = selected.type == family && selected.letterIndex == style.index,
                                        onClick = { state.edit { EditorReducer.updateLetteringStyle(it, selected.id, style) } },
                                        label = { Text((style.index + 1).toString()) },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text("Select a bubble or add lettering to edit its text.")
                }
            }
            ToolTab.IMAGE -> ImageTools(workspace, cropMode, onCropMode)
            ToolTab.AI -> GenerationTools(workspace, scene)
            ToolTab.LAYERS -> LayerTools(workspace)
        }
    }
}

@Composable
private fun ImageTools(
    workspace: EditorWorkspace,
    cropMode: Boolean,
    onCropMode: (Boolean) -> Unit,
) {
    val state = workspace.editor
    val frame = state.project.selectedFrame
    OutlinedButton(onClick = { workspace.importImage() }) { Text("Import image as frame") }
    if (frame != null) {
        FilterChip(selected = cropMode, onClick = { onCropMode(!cropMode) }, label = { Text("Crop / pan") })
        Text("Zoom")
        Slider(value = frame.crop.scale.toFloat(), onValueChange = { zoom ->
            state.edit {
                EditorReducer.transformSelectedFrameCrop(it, zoom / frame.crop.scale, 0.0, 0.0)
            }
        }, valueRange = 1f..4f)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { state.edit { EditorReducer.rotateSelectedFrameCrop(it, 90) } }) { Text("Rotate") }
            OutlinedButton(onClick = { state.edit { EditorReducer.flipSelectedFrameCrop(it) } }) { Text("Flip") }
            TextButton(onClick = { state.edit { EditorReducer.resetSelectedFrameCrop(it) } }) { Text("Reset crop") }
        }
        Text("Instant filters", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ImageFilter.entries.forEach { filter ->
                FilterChip(
                    selected = frame.instantFilterName == filter,
                    onClick = { state.edit { EditorReducer.applyInstantFilter(it, filter) } },
                    label = { Text(filter.name.lowercase()) },
                )
            }
        }
        if (frame.generatedImageUrls.isNotEmpty()) {
            Text("Generated versions", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                frame.generatedImageUrls.forEachIndexed { index, source ->
                    OutlinedButton(onClick = {
                        state.edit { EditorReducer.applyGeneratedImage(it, source) }
                    }) { Text((index + 1).toString()) }
                }
            }
        }
    }
}

@Composable
private fun GenerationTools(
    workspace: EditorWorkspace,
    scene: FrameScene?,
) {
    var baseUrl by remember { mutableStateOf("https://dev.comixon.com") }
    var token by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("ru-RU") }
    var selectedFilter by remember { mutableStateOf("anime") }
    var backImage by remember { mutableStateOf<String?>(null) }
    val enabled = scene != null && !workspace.isGenerating && baseUrl.startsWith("https://")
    Text("Comixon AI", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Server URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(
        token,
        { token = it },
        label = { Text("Comixon token") },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
    )
    OutlinedTextField(language, { language = it }, label = { Text("Language") }, singleLine = true)
    Text("The token stays in memory for this session.", style = MaterialTheme.typography.bodySmall)
    Button(
        onClick = {
            scene?.let {
                workspace.generate(
                    it,
                    ServerConfig(baseUrl.trim(), language.trim(), { token.trim().ifEmpty { null } }),
                )
            }
        },
        enabled = enabled,
    ) { Text("Generate dialogue") }
    Text("AI filter", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AiFilters.names.forEach { name ->
            FilterChip(selected = selectedFilter == name, onClick = { selectedFilter = name }, label = { Text(name.replace('_', ' ')) })
        }
    }
    if (selectedFilter == "blending") {
        OutlinedButton(onClick = { workspace.pickBlendImage { backImage = it } }) {
            Text(if (backImage == null) "Choose second image" else "Change second image")
        }
    }
    Button(
        onClick = {
            scene?.let {
                val requestToken = token.trim().ifEmpty { null }
                workspace.generate(
                    it,
                    ServerConfig(baseUrl.trim(), language.trim(), { requestToken }),
                    selectedFilter,
                    backImage = if (selectedFilter == "blending") backImage else null,
                )
            }
        },
        enabled = enabled && (selectedFilter != "blending" || backImage != null),
    ) { Text("Apply AI filter") }
}

@Composable
private fun LayerTools(workspace: EditorWorkspace) {
    val state = workspace.editor
    val project = state.project
    val frame = project.selectedFrame ?: return
    frame.layers.asReversed().forEach { layer ->
        val label =
            when (layer) {
                is EditorBubbleLayer -> layer.text.ifBlank { "Image bubble" }
                is EditorLetterLayer -> layer.text
            }
        FilterChip(
            selected = project.selectedLayerId == layer.id,
            onClick = { state.select { EditorReducer.selectLayer(it, layer.id) } },
            label = { Text(label.take(32)) },
        )
    }
    project.selectedLayerId?.let { id ->
        OutlinedButton(onClick = { state.edit { EditorReducer.deleteLayer(it, id) } }) { Text("Delete layer") }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { state.edit { EditorReducer.moveFrame(it, frame.id, -1) } }) { Text("Frame left") }
        OutlinedButton(onClick = { state.edit { EditorReducer.moveFrame(it, frame.id, 1) } }) { Text("Frame right") }
        TextButton(onClick = { state.edit { EditorReducer.deleteFrame(it, frame.id) } }) { Text("Delete frame") }
    }
}
