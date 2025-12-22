package com.targetdiscriminator.presentation.training

import com.targetdiscriminator.domain.model.MediaItem
import com.targetdiscriminator.domain.model.ResponseResult
import com.targetdiscriminator.presentation.mvi.ViewState

data class TrainingState(
    val currentMedia: MediaItem? = null,
    val isPlaying: Boolean = false,
    val timeRemainingSeconds: Long = 0,
    val showFeedback: Boolean = false,
    val lastResult: ResponseResult? = null,
    val isSessionComplete: Boolean = false,
    val totalResponses: Int = 0,
    val correctResponses: Int = 0,
    val hasResponded: Boolean = false,
    val reactionTimesMs: List<Long> = emptyList()
) : ViewState {
    val averageReactionTimeMs: Long?
        get() = if (reactionTimesMs.isNotEmpty()) {
            reactionTimesMs.average().toLong()
        } else {
            null
        }
}

