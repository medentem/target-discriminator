package com.targetdiscriminator.presentation.session_config

import com.targetdiscriminator.presentation.mvi.ViewEffect

sealed class SessionConfigEffect : ViewEffect {
    data class NavigateToTraining(val config: com.targetdiscriminator.domain.model.SessionConfig) : SessionConfigEffect()
    data class ShowError(val message: String) : SessionConfigEffect()
}

