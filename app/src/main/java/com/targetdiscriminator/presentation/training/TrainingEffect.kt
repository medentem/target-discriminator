package com.targetdiscriminator.presentation.training

import com.targetdiscriminator.presentation.mvi.ViewEffect

sealed class TrainingEffect : ViewEffect {
    object NavigateToConfig : TrainingEffect()
}

