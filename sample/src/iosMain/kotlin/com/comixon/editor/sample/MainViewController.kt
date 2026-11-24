package com.comixon.editor.sample

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController

fun mainViewController(
    speechInput: IosSpeechInput,
    startWithDemo: Boolean = false,
) = ComposeUIViewController {
    CompositionLocalProvider(LocalIosSpeechInput provides speechInput) {
        EditorApp(startWithDemo)
    }
}
