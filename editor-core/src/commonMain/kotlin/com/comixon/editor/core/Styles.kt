package com.comixon.editor.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BubbleFamily {
    @SerialName("classic")
    CLASSIC,

    @SerialName("classicTwo")
    CLASSIC_TWO,

    @SerialName("rocks")
    ROCKS,

    @SerialName("rectangles")
    RECTANGLES,

    @SerialName("screamo")
    SCREAMO,

    @SerialName("corner")
    CORNER,
}

@Serializable
enum class LetteringFamily { BASIC, STAIRS, BRIDGE }

@Serializable
enum class FontSize(
    val points: Float,
) {
    S(12f),
    M(18f),
    L(24f),
    XL(30f),
    XXL(36f),
}

@Serializable
enum class ImageFilter { NONE, OLDFILM, OLDTV, WARM, COOL, MONO, FADED }

@Serializable
data class BubbleStyle(
    val type: BubbleFamily,
    val index: Int,
) {
    init {
        require(index in 0..3) { "Bubble index must be in 0..3" }
    }
}

@Serializable
data class LetteringStyle(
    val type: LetteringFamily,
    val index: Int,
) {
    init {
        val count = if (type == LetteringFamily.STAIRS) 4 else 5
        require(index in 0 until count) { "Unsupported lettering variant" }
    }
}

object StyleCatalog {
    val bubbles: List<BubbleStyle> =
        BubbleFamily.entries.flatMap { family ->
            List(4) { BubbleStyle(family, it) }
        }
    val lettering: List<LetteringStyle> =
        LetteringFamily.entries.flatMap { family ->
            List(if (family == LetteringFamily.STAIRS) 4 else 5) { LetteringStyle(family, it) }
        }
    val defaultBubbleStyle: BubbleStyle = bubbles.first()
    val defaultLetteringStyle: LetteringStyle = lettering.first()
}
