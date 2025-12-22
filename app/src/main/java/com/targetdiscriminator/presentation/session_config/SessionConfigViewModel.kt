package com.targetdiscriminator.presentation.session_config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.targetdiscriminator.domain.model.SessionConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionConfigViewModel : ViewModel() {
    private val _state = MutableStateFlow(SessionConfigState())
    val state: StateFlow<SessionConfigState> = _state.asStateFlow()
    private val _effect = MutableSharedFlow<SessionConfigEffect>()
    val effect: SharedFlow<SessionConfigEffect> = _effect.asSharedFlow()
    fun handleEvent(event: SessionConfigEvent) {
        when (event) {
            is SessionConfigEvent.ToggleVideos -> {
                _state.value = _state.value.copy(includeVideos = event.enabled)
                updateCanStart()
            }
            is SessionConfigEvent.TogglePhotos -> {
                _state.value = _state.value.copy(includePhotos = event.enabled)
                updateCanStart()
            }
            is SessionConfigEvent.SetDuration -> {
                _state.value = _state.value.copy(durationMinutes = event.minutes)
                updateCanStart()
            }
            is SessionConfigEvent.StartSession -> {
                startSession()
            }
        }
    }
    private fun updateCanStart() {
        val currentState = _state.value
        val config = SessionConfig(
            includeVideos = currentState.includeVideos,
            includePhotos = currentState.includePhotos,
            durationMinutes = currentState.durationMinutes
        )
        _state.value = currentState.copy(canStart = config.isValid())
    }
    private fun startSession() {
        val currentState = _state.value
        val config = SessionConfig(
            includeVideos = currentState.includeVideos,
            includePhotos = currentState.includePhotos,
            durationMinutes = currentState.durationMinutes
        )
        if (config.isValid()) {
            viewModelScope.launch {
                _effect.emit(SessionConfigEffect.NavigateToTraining(config))
            }
        } else {
            viewModelScope.launch {
                _effect.emit(SessionConfigEffect.ShowError("Invalid session configuration"))
            }
        }
    }
}

