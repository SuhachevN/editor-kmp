package com.comixon.editor.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.comixon.editor.compose.encodeImage
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class EditorPreviewTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun demoRendersAtPhoneAndDesktopSizes() {
        var phone by mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.size(if (phone) 400.dp else 1000.dp, 700.dp)) { EditorApp() }
            }
        }
        rule.onNodeWithTag("editor_open_demo").performClick()
        rule.waitForIdle()
        saveScreenshot("desktop")
        rule.runOnIdle { phone = true }
        rule.waitForIdle()
        saveScreenshot("phone")
    }

    private fun saveScreenshot(name: String) {
        val image = rule.onRoot().captureToImage()
        assertTrue(image.width > 300 && image.height > 400)
        File("build/reports/editor/" + name + ".png").apply {
            parentFile.mkdirs()
            writeBytes(encodeImage(image))
        }
    }
}
