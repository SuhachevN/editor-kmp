package com.comixon.editor.rendering

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.comixon.editor.core.ImageFilter

internal fun ImageFilter.colorFilter(): ColorFilter? =
    when (this) {
        ImageFilter.NONE -> null
        ImageFilter.OLDFILM ->
            rgba(
                floatArrayOf(.9f, .18f, .08f, 0f, 12f),
                floatArrayOf(.08f, .84f, .08f, 0f, 8f),
                floatArrayOf(.03f, .12f, .72f, 0f, -8f),
                floatArrayOf(0f, 0f, 0f, 1f, 0f),
            )
        ImageFilter.OLDTV ->
            rgba(
                floatArrayOf(.48f, .48f, .48f, 0f, 4f),
                floatArrayOf(.42f, .42f, .42f, 0f, 4f),
                floatArrayOf(.34f, .34f, .34f, 0f, 4f),
                floatArrayOf(0f, 0f, 0f, 1f, 0f),
            )
        ImageFilter.WARM ->
            rgba(
                floatArrayOf(1.08f, .04f, 0f, 0f, 6f),
                floatArrayOf(.02f, 1f, 0f, 0f, 2f),
                floatArrayOf(0f, .02f, .9f, 0f, -4f),
                floatArrayOf(0f, 0f, 0f, 1f, 0f),
            )
        ImageFilter.COOL ->
            rgba(
                floatArrayOf(.9f, .02f, 0f, 0f, -4f),
                floatArrayOf(0f, 1f, .04f, 0f, 2f),
                floatArrayOf(0f, .04f, 1.12f, 0f, 8f),
                floatArrayOf(0f, 0f, 0f, 1f, 0f),
            )
        ImageFilter.MONO ->
            rgba(
                floatArrayOf(.299f, .587f, .114f, 0f, 0f),
                floatArrayOf(.299f, .587f, .114f, 0f, 0f),
                floatArrayOf(.299f, .587f, .114f, 0f, 0f),
                floatArrayOf(0f, 0f, 0f, 1f, 0f),
            )
        ImageFilter.FADED ->
            rgba(
                floatArrayOf(.86f, .08f, .04f, 0f, 18f),
                floatArrayOf(.04f, .86f, .06f, 0f, 18f),
                floatArrayOf(.04f, .08f, .82f, 0f, 18f),
                floatArrayOf(0f, 0f, 0f, 1f, 0f),
            )
    }

private fun rgba(vararg rows: FloatArray): ColorFilter =
    ColorFilter.colorMatrix(
        ColorMatrix(
            rows
                .flatMap {
                    it.toList()
                }.toFloatArray(),
        ),
    )
