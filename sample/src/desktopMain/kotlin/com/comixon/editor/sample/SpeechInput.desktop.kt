package com.comixon.editor.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Base64
import java.util.Locale

@Composable
internal actual fun rememberSpeechInput(
    onText: (String) -> Unit,
    onError: (SpeechInputError) -> Unit,
): SpeechInput {
    val scope = rememberCoroutineScope()
    val textCallback by rememberUpdatedState(onText)
    val errorCallback by rememberUpdatedState(onError)
    val input = remember(scope) { DesktopSpeechInput(scope, { textCallback(it) }, { errorCallback(it) }) }
    DisposableEffect(input) { onDispose { input.cancel() } }
    return input
}

// Native helpers return transcripts over stdout; stdin carries the stop command.
private class DesktopSpeechInput(
    private val scope: CoroutineScope,
    private val onText: (String) -> Unit,
    private val onError: (SpeechInputError) -> Unit,
) : SpeechInput {
    override var state by mutableStateOf(SpeechInputState.IDLE)
        private set
    private var process: Process? = null
    private var job: Job? = null
    private var session = 0

    override fun start() {
        if (state != SpeechInputState.IDLE) return
        val command = nativeSpeechCommand()
        if (command == null) {
            onError(SpeechInputError.UNAVAILABLE)
            return
        }
        val id = ++session
        state = SpeechInputState.LISTENING
        job =
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val child = ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start()
                        withContext(Dispatchers.Main) {
                            if (id == session) process = child else child.destroy()
                        }
                        if (state != SpeechInputState.LISTENING) {
                            child.outputStream.bufferedWriter().apply {
                                write("stop\n")
                                flush()
                            }
                        }
                        child.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                            for (line in lines) {
                                val event = parseSpeechEvent(line) ?: continue
                                withContext(Dispatchers.Main) {
                                    if (id == session) {
                                        when (event) {
                                            is NativeSpeechEvent.Text -> onText(event.value)
                                            is NativeSpeechEvent.Error -> onError(event.value)
                                            NativeSpeechEvent.Done -> state = SpeechInputState.IDLE
                                        }
                                    }
                                }
                            }
                        }
                        val exit = child.waitFor()
                        withContext(Dispatchers.Main) {
                            if (id == session && exit != 0) onError(SpeechInputError.FAILED)
                        }
                    }
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (_: Exception) {
                    if (id == session) onError(SpeechInputError.FAILED)
                } finally {
                    if (id == session) {
                        process?.destroy()
                        process = null
                        state = SpeechInputState.IDLE
                    }
                }
            }
    }

    override fun stop() {
        if (state != SpeechInputState.LISTENING) return
        state = SpeechInputState.FINISHING
        runCatching {
            process?.outputStream?.bufferedWriter()?.apply {
                write("stop\n")
                flush()
            }
        }
    }

    override fun cancel() {
        session++
        process?.destroy()
        process = null
        job?.cancel()
        state = SpeechInputState.IDLE
    }
}

internal sealed interface NativeSpeechEvent {
    data class Text(
        val value: String,
    ) : NativeSpeechEvent

    data class Error(
        val value: SpeechInputError,
    ) : NativeSpeechEvent

    data object Done : NativeSpeechEvent
}

internal fun parseSpeechEvent(line: String): NativeSpeechEvent? {
    val value = line.substringAfter('\t', "")
    return when (line.substringBefore('\t')) {
        "text" ->
            runCatching {
                NativeSpeechEvent.Text(
                    String(Base64.getDecoder().decode(value), Charsets.UTF_8),
                )
            }.getOrNull()
        "error" ->
            NativeSpeechEvent.Error(
                SpeechInputError.entries.firstOrNull { it.name == value } ?: SpeechInputError.FAILED,
            )
        "done" -> NativeSpeechEvent.Done
        else -> null
    }
}

private fun nativeSpeechCommand(): List<String>? {
    val os = System.getProperty("os.name").lowercase(Locale.ROOT)
    val language = Locale.getDefault().toLanguageTag()
    return when {
        os.contains("win") -> {
            val script = File.createTempFile("comixon-dictation-", ".ps1").apply { deleteOnExit() }
            val resource = object {}.javaClass.getResourceAsStream("/speech/windows-dictation.ps1") ?: return null
            resource.use { input -> script.outputStream().use(input::copyTo) }
            listOf(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                script.path,
                "-Language",
                language,
            )
        }
        os.contains("mac") -> {
            val resources = System.getProperty("compose.application.resources.dir")
            val configured = System.getProperty("comixon.speech.helper")
            val helper =
                File(
                    configured ?: resources?.let { File(it, "comixon-speech").path }
                        ?: listOf(
                            "build/native-resources/macos/comixon-speech",
                            "sample/build/native-resources/macos/comixon-speech",
                        ).firstOrNull { File(it).canExecute() } ?: return null,
                )
            if (helper.canExecute()) listOf(helper.absolutePath, language) else null
        }
        else -> null
    }
}
