package com.targetdiscriminator.domain.model

data class MediaItem(
    val path: String,
    val type: MediaType,
    val threatType: ThreatType
)

