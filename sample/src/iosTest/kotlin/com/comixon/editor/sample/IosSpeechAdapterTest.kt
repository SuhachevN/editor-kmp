package com.comixon.editor.sample

import kotlin.test.Test
import kotlin.test.assertEquals

class IosSpeechAdapterTest {
    @Test
    fun partialResultsAndCompletionReachTheEditor() {
        val host = FakeSpeechInput()
        val transcripts = mutableListOf<String>()
        val input = IosSpeechAdapter(host, transcripts::add, {})
        input.start()
        host.listener.onText("Hello")
        host.listener.onText("Hello world")
        assertEquals(listOf("Hello", "Hello world"), transcripts)
        assertEquals(SpeechInputState.LISTENING, input.state)
        host.listener.onFinished()
        assertEquals(SpeechInputState.IDLE, input.state)
    }

    @Test
    fun cancellationIgnoresLateCallbacks() {
        val host = FakeSpeechInput()
        val transcripts = mutableListOf<String>()
        val input = IosSpeechAdapter(host, transcripts::add, { error("Unexpected error") })
        input.start()
        input.cancel()
        host.listener.onText("Late")
        host.listener.onError("FAILED")
        host.listener.onFinished()
        assertEquals(emptyList(), transcripts)
        assertEquals(SpeechInputState.IDLE, input.state)
        assertEquals(1, host.cancellations)
    }

    @Test
    fun stopWaitsForFinalTranscript() {
        val host = FakeSpeechInput()
        val input = IosSpeechAdapter(host, {}, {})
        input.start()
        input.stop()
        assertEquals(SpeechInputState.FINISHING, input.state)
        assertEquals(1, host.stops)
        host.listener.onFinished()
        assertEquals(SpeechInputState.IDLE, input.state)
    }

    @Test
    fun permissionDenialIsMapped() {
        val host = FakeSpeechInput()
        val errors = mutableListOf<SpeechInputError>()
        val input = IosSpeechAdapter(host, {}, errors::add)
        input.start()
        host.listener.onError("PERMISSION")
        host.listener.onFinished()
        assertEquals(listOf(SpeechInputError.PERMISSION), errors)
        assertEquals(SpeechInputState.IDLE, input.state)
    }

    @Test
    fun missingHostDoesNotStartListening() {
        val errors = mutableListOf<SpeechInputError>()
        val input = IosSpeechAdapter(null, {}, errors::add)
        input.start()
        assertEquals(listOf(SpeechInputError.UNAVAILABLE), errors)
        assertEquals(SpeechInputState.IDLE, input.state)
    }

    private class FakeSpeechInput : IosSpeechInput {
        lateinit var listener: IosSpeechListener
        var stops = 0
        var cancellations = 0

        override fun start(listener: IosSpeechListener) {
            this.listener = listener
        }

        override fun stop() {
            stops++
        }

        override fun cancel() {
            cancellations++
        }
    }
}
