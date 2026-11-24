package com.comixon.editor.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal enum class SpeechInputState { IDLE, LISTENING, FINISHING }

internal enum class SpeechInputError { UNAVAILABLE, PERMISSION, LANGUAGE, NO_MATCH, NETWORK, FAILED }

internal interface SpeechInput {
    val state: SpeechInputState

    fun start()

    fun stop()

    fun cancel()
}

@Composable
internal expect fun rememberSpeechInput(
    onText: (String) -> Unit,
    onError: (SpeechInputError) -> Unit,
): SpeechInput

/** Each partial result replaces the original selection, rather than appending itself. */
internal fun insertDictation(
    source: TextFieldValue,
    transcript: String,
): TextFieldValue {
    val start = source.selection.min
    val end = source.selection.max
    val text = source.text.replaceRange(start, end, transcript)
    return TextFieldValue(text, TextRange(start + transcript.length))
}
