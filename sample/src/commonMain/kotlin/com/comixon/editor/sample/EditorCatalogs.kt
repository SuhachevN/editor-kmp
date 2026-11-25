package com.comixon.editor.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.comixon.editor.compose.BubbleStylePreview
import com.comixon.editor.compose.LetteringStylePreview
import com.comixon.editor.core.BubbleFamily
import com.comixon.editor.core.BubbleStyle
import com.comixon.editor.core.LetteringFamily
import com.comixon.editor.core.LetteringStyle
import com.comixon.editor.core.StyleCatalog

@Composable
internal fun BubbleCatalog(
    onSelected: (BubbleStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.testTag("editor_bubble_catalog"), contentPadding = PaddingValues(vertical = 14.dp)) {
        items(BubbleFamily.entries, key = { it.name }) { family ->
            EditorSectionTitle(family.label)
            LazyRow(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(StyleCatalog.bubbles.filter { it.type == family }, key = { it.index }) { style ->
                    val description = family.label.text() + " " + (style.index + 1)
                    Box(
                        Modifier
                            .size(104.dp, 82.dp)
                            .semantics { contentDescription = description }
                            .testTag("editor_style_" + family.name + "_" + style.index)
                            .clickable { onSelected(style) },
                        contentAlignment = Alignment.Center,
                    ) { BubbleStylePreview(style, Modifier.size(76.dp, 72.dp)) }
                }
            }
            HorizontalDivider(
                Modifier.padding(start = 20.dp),
                color = EditorColors.Divider.copy(alpha = .65f),
                thickness = .5.dp,
            )
        }
    }
}

@Composable
internal fun LetteringCatalog(
    onSelected: (LetteringStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, contentPadding = PaddingValues(vertical = 14.dp)) {
        items(LetteringFamily.entries, key = { it.name }) { family ->
            EditorSectionTitle(family.label)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(StyleCatalog.lettering.filter { it.type == family }, key = { it.index }) { style ->
                    val description = family.label.text() + " " + (style.index + 1)
                    Box(
                        Modifier
                            .size(104.dp, 82.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .semantics { contentDescription = description }
                            .clickable { onSelected(style) },
                        contentAlignment = Alignment.Center,
                    ) { LetteringStylePreview(style, Modifier.size(90.dp, 60.dp)) }
                }
            }
        }
    }
}

internal val BubbleFamily.label: EditorLabel
    get() =
        when (this) {
            BubbleFamily.CLASSIC -> EditorLabel.Classic
            BubbleFamily.CLASSIC_TWO -> EditorLabel.ClassicTwo
            BubbleFamily.ROCKS -> EditorLabel.Rocks
            BubbleFamily.RECTANGLES -> EditorLabel.Rectangles
            BubbleFamily.SCREAMO -> EditorLabel.Screamo
            BubbleFamily.CORNER -> EditorLabel.Corner
        }

internal val LetteringFamily.label: EditorLabel
    get() =
        when (this) {
            LetteringFamily.BASIC -> EditorLabel.Basic
            LetteringFamily.STAIRS -> EditorLabel.Stairs
            LetteringFamily.BRIDGE -> EditorLabel.Bridge
        }
