package com.comixon.editor.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal object EditorColors {
    val Background = Color(0xFF1C1C1C)
    val Control = Color(0xFF2C2C2E)
    val Divider = Color(0xFF3A3A3C)
    val Accent = Color(0xFFFFCC00)
    val Content = Color.White
    val Muted = Color.White.copy(alpha = .55f)
    val Delete = Color(0xFFFF3B30)
}

@Composable
internal fun EditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary = EditorColors.Accent,
                onPrimary = EditorColors.Background,
                background = EditorColors.Background,
                surface = EditorColors.Background,
                surfaceVariant = EditorColors.Control,
                outline = EditorColors.Divider,
                error = EditorColors.Delete,
            ),
        content = content,
    )
}
