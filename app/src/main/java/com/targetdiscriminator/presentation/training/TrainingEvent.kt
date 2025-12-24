package com.targetdiscriminator.presentation.training

import com.targetdiscriminator.presentation.mvi.ViewEvent

sealed class TrainingEvent : ViewEvent {
    data class OnTap(val x: Float, val y: Float) : TrainingEvent()
    object OnSwipe : TrainingEvent()
    object OnFeedbackShown : TrainingEvent()
    object OnSessionComplete : TrainingEvent()
    object OnVideoCompleted : TrainingEvent()
    object OnBackPressed : TrainingEvent()
}

