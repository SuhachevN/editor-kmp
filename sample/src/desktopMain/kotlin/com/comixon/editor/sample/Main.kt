package com.comixon.editor.sample

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.delay
import java.io.File

fun main(args: Array<String>) {
    FileKit.init("com.comixon.editor.sample")
    val smokeReport = args.firstOrNull { it.startsWith("--smoke-test=") }?.substringAfter('=')?.let(::File)
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Comixon Editor",
            state = rememberWindowState(width = 1200.dp, height = 900.dp),
        ) {
            EditorApp(startWithDemo = smokeReport != null)
            if (smokeReport != null) {
                LaunchedEffect(Unit) {
                    // Resolve the shared UI and font resources before reporting a successful packaged launch.
                    delay(3_000)
                    check(window.isShowing)
                    smokeReport.parentFile?.mkdirs()
                    smokeReport.writeText("Comixon Editor opened successfully on " + System.getProperty("os.name"))
                    exitApplication()
                }
            }
        }
    }
}
