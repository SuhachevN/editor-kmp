package com.comixon.editor.sample

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NativeSpeechProtocolTest {
    @Test fun transcriptsCrossTheProcessBoundaryWithoutLosingUnicodeOrNewlines() {
        val text = "Привет!\nHello 👋"
        val encoded = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
        assertEquals(NativeSpeechEvent.Text(text), parseSpeechEvent("text\t" + encoded))
        assertNull(parseSpeechEvent("text\t%%%"))
        assertNull(parseSpeechEvent("unrecognized"))
    }

    @Test fun nativeErrorsRemainTypedAndUnknownErrorsHaveASafeFallback() {
        assertEquals(NativeSpeechEvent.Error(SpeechInputError.PERMISSION), parseSpeechEvent("error\tPERMISSION"))
        assertEquals(NativeSpeechEvent.Error(SpeechInputError.FAILED), parseSpeechEvent("error\tUNKNOWN"))
        assertEquals(NativeSpeechEvent.Done, parseSpeechEvent("done"))
    }
}
