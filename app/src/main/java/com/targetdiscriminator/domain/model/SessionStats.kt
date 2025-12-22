package com.targetdiscriminator.domain.model

import java.util.Date

data class SessionStats(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalResponses: Int,
    val correctResponses: Int,
    val averageReactionTimeMs: Long?
) {
    val score: Double
        get() = if (totalResponses > 0) {
            correctResponses.toDouble() / totalResponses
        } else {
            0.0
        }
    val date: Date
        get() = Date(timestamp)
}

