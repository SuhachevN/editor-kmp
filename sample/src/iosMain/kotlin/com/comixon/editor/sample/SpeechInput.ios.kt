package com.comixon.editor.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalIosSpeechInput = staticCompositionLocalOf<IosSpeechInput?> { null }

@Composable
internal actual fun rememberSpeechInput(
    onText: (String) -> Unit,
    onError: (SpeechInputError) -> Unit,
): SpeechInput {
    val host = LocalIosSpeechInput.current
    val textCallback by rememberUpdatedState(onText)
    val errorCallback by rememberUpdatedState(onError)
    val input = remember(host) { IosSpeechAdapter(host, { textCallback(it) }, { errorCallback(it) }) }
    DisposableEffect(input) { onDispose { input.cancel() } }
    return input
}

internal class IosSpeechAdapter(
    private val host: IosSpeechInput?,
    private val onText: (String) -> Unit,
    private val onError: (SpeechInputError) -> Unit,
) : SpeechInput {
    override var state by mutableStateOf(SpeechInputState.IDLE)
        private set
    private var session = 0

    override fun start() {
        if (state != SpeechInputState.IDLE) return
        val host = host ?: return onError(SpeechInputError.UNAVAILABLE)
        val id = ++session
        state = SpeechInputState.LISTENING
        host.start(
            object : IosSpeechListener {
                override fun onText(text: String) {
                    if (id == session) this@IosSpeechAdapter.onText(text)
                }

                override fun onError(code: String) {
                    if (id != session) return
                    val error = SpeechInputError.entries.firstOrNull { it.name == code } ?: SpeechInputError.FAILED
                    this@IosSpeechAdapter.onError(error)
                }

                override fun onFinished() {
                    if (id == session) state = SpeechInputState.IDLE
                }
            },
        )
    }

    override fun stop() {
        if (state != SpeechInputState.LISTENING) return
        state = SpeechInputState.FINISHING
        host?.stop()
    }

    override fun cancel() {
        session++
        host?.cancel()
        state = SpeechInputState.IDLE
    }
}
