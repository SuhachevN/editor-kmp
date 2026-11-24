package com.comixon.editor.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// The iOS host is retained for later work; its speech adapter is deliberately unavailable.
@Composable
internal actual fun rememberSpeechInput(
    onText: (String) -> Unit,
    onError: (SpeechInputError) -> Unit,
): SpeechInput =
    remember(onError) {
        object : SpeechInput {
            override val state = SpeechInputState.IDLE

            override fun start() = onError(SpeechInputError.UNAVAILABLE)

            override fun stop() = Unit

            override fun cancel() = Unit
        }
    }
