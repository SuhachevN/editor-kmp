package com.comixon.editor.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SpeechRequestDto(
    val image: String? = null,
    @SerialName("first_gen") val firstGen: Boolean? = null,
    @SerialName("lang_zone") val langZone: String? = null,
    @SerialName("image_desc") val imageDesc: String? = null,
)

@Serializable
internal data class SpeechResponseDto(
    @SerialName("created_at") val createdAt: String? = null,
    val id: Int? = null,
    @SerialName("seen_image") val seenImage: Int? = null,
    val speech: List<List<SpeechItemDto>> = emptyList(),
    @SerialName("first_generation") val firstGeneration: SpeechGenerationDto? = null,
    @SerialName("next_generation") val nextGeneration: SpeechGenerationDto? = null,
    val user: Int? = null,
    @SerialName("image_desc") val imageDesc: String? = null,
)

@Serializable
internal data class SpeechGenerationDto(
    val speech: Map<String, SpeechItemDto> = emptyMap(),
    @SerialName("image_desc") val imageDesc: String? = null,
)

@Serializable
internal data class SpeechItemDto(
    val bubble: List<Double>? = null,
    val personage: String? = null,
    val text: String? = null,
    val tone: String? = null,
)
