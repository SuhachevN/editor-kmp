package com.comixon.editor.sample

/** The iOS host owns microphone permissions and the Apple Speech session. Callbacks use the main thread. */
interface IosSpeechInput {
    fun start(listener: IosSpeechListener)

    fun stop()

    fun cancel()
}

interface IosSpeechListener {
    fun onText(text: String)

    fun onError(code: String)

    fun onFinished()
}
