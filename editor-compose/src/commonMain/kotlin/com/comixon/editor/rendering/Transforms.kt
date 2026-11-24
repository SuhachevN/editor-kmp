package com.comixon.editor.rendering

import androidx.compose.ui.graphics.Matrix
import kotlin.math.PI

internal fun degrees(radians: Float): Float = (radians * 180.0 / PI).toFloat()

internal fun scaleMatrix(
    x: Float,
    y: Float,
): Matrix = Matrix().apply { scale(x, y) }

internal fun rotationMatrix(
    degrees: Float,
    x: Float = 0f,
    y: Float = 0f,
): Matrix =
    Matrix().apply {
        translate(x, y)
        rotateZ(degrees)
        translate(-x, -y)
    }
