package com.targetdiscriminator.presentation.session_config

import com.targetdiscriminator.presentation.mvi.ViewEvent

sealed class SessionConfigEvent : ViewEvent {
    data class ToggleVideos(val enabled: Boolean) : SessionConfigEvent()
    data class TogglePhotos(val enabled: Boolean) : SessionConfigEvent()
    data class SetDuration(val minutes: Int) : SessionConfigEvent()
    object StartSession : SessionConfigEvent()
}

