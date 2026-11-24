package com.comixon.editor.sample

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class DictationTextTest {
    @Test fun partialResultsReplaceTheSelectionWithoutDuplicatingWords() {
        val original = TextFieldValue("Hello, world!", TextRange(7, 12))
        assertEquals("Hello, my!", insertDictation(original, "my").text)
        val final = insertDictation(original, "my friends")
        assertEquals("Hello, my friends!", final.text)
        assertEquals(TextRange(17), final.selection)
    }

    @Test fun reversedSelectionAndUnicodeArePreserved() {
        val original = TextFieldValue("Привет, мир!", TextRange(11, 8))
        val result = insertDictation(original, "друзья 👋")
        assertEquals("Привет, друзья 👋!", result.text)
        assertEquals(TextRange(result.text.length - 1), result.selection)
    }

    @Test fun dictationInsertsAtTheCursorAndLeavesTheRestOfTheText() {
        val result = insertDictation(TextFieldValue("One three", TextRange(4)), "two ")
        assertEquals("One two three", result.text)
        assertEquals(TextRange(8), result.selection)
    }
}
