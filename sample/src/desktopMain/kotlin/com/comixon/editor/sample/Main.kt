package com.comixon.editor.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.FileKit

fun main() {
    FileKit.init("com.comixon.editor.sample")
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Comixon Editor",
            state = rememberWindowState(width = 1200.dp, height = 900.dp),
        ) {
            EditorApp()
        }
    }
}
