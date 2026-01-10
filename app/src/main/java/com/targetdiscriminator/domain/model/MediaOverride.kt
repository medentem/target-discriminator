package com.targetdiscriminator.domain.model

data class MediaOverride(
    val mediaPath: String,
    val isExcluded: Boolean,
    val threatTypeOverride: ThreatType? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
