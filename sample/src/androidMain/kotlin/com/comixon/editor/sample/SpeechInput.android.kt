package com.comixon.editor.sample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

@Composable
internal actual fun rememberSpeechInput(
    onText: (String) -> Unit,
    onError: (SpeechInputError) -> Unit,
): SpeechInput {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val textCallback by rememberUpdatedState(onText)
    val errorCallback by rememberUpdatedState(onError)
    val input = remember(context) { AndroidSpeechInput(context, { textCallback(it) }, { errorCallback(it) }) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            input.permissionResult(granted)
        }
    input.requestPermission = { permission.launch(Manifest.permission.RECORD_AUDIO) }
    DisposableEffect(input, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) input.cancel() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            input.close()
        }
    }
    return input
}

private class AndroidSpeechInput(
    private val context: android.content.Context,
    private val onText: (String) -> Unit,
    private val onError: (SpeechInputError) -> Unit,
) : SpeechInput,
    RecognitionListener {
    override var state by mutableStateOf(SpeechInputState.IDLE)
        private set
    var requestPermission: () -> Unit = {}
    private var recognizer: SpeechRecognizer? = null
    private var permissionPending = false

    override fun start() {
        if (state != SpeechInputState.IDLE) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError(SpeechInputError.UNAVAILABLE)
            return
        }
        state = SpeechInputState.LISTENING
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionPending = true
            requestPermission()
        } else {
            listen()
        }
    }

    fun permissionResult(granted: Boolean) {
        val shouldStart = permissionPending && state == SpeechInputState.LISTENING
        permissionPending = false
        if (granted && shouldStart) {
            listen()
        } else {
            state = SpeechInputState.IDLE
            if (!granted) onError(SpeechInputError.PERMISSION)
        }
    }

    private fun listen() {
        try {
            val speech =
                recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
                    it.setRecognitionListener(this)
                    recognizer = it
                }
            speech.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                },
            )
        } catch (_: Exception) {
            cancel()
            onError(SpeechInputError.FAILED)
        }
    }

    override fun stop() {
        if (state != SpeechInputState.LISTENING) return
        if (permissionPending) {
            permissionPending = false
            state = SpeechInputState.IDLE
        } else {
            state = SpeechInputState.FINISHING
            recognizer?.stopListening()
        }
    }

    override fun cancel() {
        permissionPending = false
        state = SpeechInputState.IDLE
        recognizer?.cancel()
    }

    fun close() {
        cancel()
        recognizer?.destroy()
        recognizer = null
    }

    override fun onPartialResults(results: Bundle?) = deliver(results)

    override fun onResults(results: Bundle?) {
        deliver(results)
        state = SpeechInputState.IDLE
    }

    private fun deliver(results: Bundle?) {
        if (state == SpeechInputState.IDLE) return
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onText)
    }

    override fun onError(error: Int) {
        if (state == SpeechInputState.IDLE) return
        state = SpeechInputState.IDLE
        onError(
            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechInputError.PERMISSION
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechInputError.NETWORK
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechInputError.NO_MATCH
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                -> SpeechInputError.LANGUAGE
                else -> SpeechInputError.FAILED
            },
        )
    }

    override fun onEndOfSpeech() {
        if (state == SpeechInputState.LISTENING) state = SpeechInputState.FINISHING
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEvent(
        eventType: Int,
        params: Bundle?,
    ) = Unit
}
