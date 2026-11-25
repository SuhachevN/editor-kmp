package com.comixon.editor.sample

/** Implemented by the Swift host. All callbacks must run on the main thread. */
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
