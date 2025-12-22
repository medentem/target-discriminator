package com.targetdiscriminator.presentation.session_config

import com.targetdiscriminator.presentation.mvi.ViewState

data class SessionConfigState(
    val includeVideos: Boolean = true,
    val includePhotos: Boolean = true,
    val durationMinutes: Int = 5,
    val isLoading: Boolean = false,
    val canStart: Boolean = true
) : ViewState

