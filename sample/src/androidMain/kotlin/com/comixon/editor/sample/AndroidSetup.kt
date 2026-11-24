package com.comixon.editor.sample

import androidx.activity.ComponentActivity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

fun initializeEditorFiles(activity: ComponentActivity) {
    FileKit.init(activity)
}
