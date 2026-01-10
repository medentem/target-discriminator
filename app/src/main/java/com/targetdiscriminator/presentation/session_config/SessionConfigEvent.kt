package com.targetdiscriminator.presentation.session_config

import com.targetdiscriminator.domain.model.ThreatLabelPreset
import com.targetdiscriminator.presentation.mvi.ViewEvent

sealed class SessionConfigEvent : ViewEvent {
    data class ToggleVideos(val enabled: Boolean) : SessionConfigEvent()
    data class TogglePhotos(val enabled: Boolean) : SessionConfigEvent()
    data class SetDuration(val minutes: Int) : SessionConfigEvent()
    data class SetThreatLabelPreset(val preset: ThreatLabelPreset) : SessionConfigEvent()
    data class SetCustomThreatLabel(val label: String) : SessionConfigEvent()
    data class SetCustomNonThreatLabel(val label: String) : SessionConfigEvent()
    object StartSession : SessionConfigEvent()
}

