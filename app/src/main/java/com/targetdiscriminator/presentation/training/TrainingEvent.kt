package com.targetdiscriminator.presentation.training

import com.targetdiscriminator.presentation.mvi.ViewEvent

sealed class TrainingEvent : ViewEvent {
    object OnTap : TrainingEvent()
    object OnSwipe : TrainingEvent()
    object OnFeedbackShown : TrainingEvent()
    object OnSessionComplete : TrainingEvent()
    object OnVideoCompleted : TrainingEvent()
    object OnBackPressed : TrainingEvent()
}

