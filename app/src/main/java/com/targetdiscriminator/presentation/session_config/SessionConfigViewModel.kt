package com.targetdiscriminator.presentation.session_config

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.targetdiscriminator.data.repository.ThreatLabelRepository
import com.targetdiscriminator.domain.model.SessionConfig
import com.targetdiscriminator.domain.model.ThreatLabelConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionConfigViewModel(private val context: Context) : ViewModel() {
    private val threatLabelRepository = ThreatLabelRepository(context)
    private val _state = MutableStateFlow(SessionConfigState())
    val state: StateFlow<SessionConfigState> = _state.asStateFlow()
    private val _effect = MutableSharedFlow<SessionConfigEffect>()
    val effect: SharedFlow<SessionConfigEffect> = _effect.asSharedFlow()

    init {
        loadThreatLabelConfig()
    }

    private fun loadThreatLabelConfig() {
        viewModelScope.launch {
            val config = threatLabelRepository.getThreatLabelConfig()
            _state.value = _state.value.copy(
                threatLabelPreset = config.preset,
                customThreatLabel = config.customThreatLabel ?: "",
                customNonThreatLabel = config.customNonThreatLabel ?: ""
            )
        }
    }

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
            is SessionConfigEvent.SetThreatLabelPreset -> {
                _state.value = _state.value.copy(threatLabelPreset = event.preset)
                saveThreatLabelConfig()
            }
            is SessionConfigEvent.SetCustomThreatLabel -> {
                _state.value = _state.value.copy(customThreatLabel = event.label)
                saveThreatLabelConfig()
            }
            is SessionConfigEvent.SetCustomNonThreatLabel -> {
                _state.value = _state.value.copy(customNonThreatLabel = event.label)
                saveThreatLabelConfig()
            }
            is SessionConfigEvent.StartSession -> {
                startSession()
            }
        }
    }

    private fun saveThreatLabelConfig() {
        viewModelScope.launch {
            val currentState = _state.value
            val config = ThreatLabelConfig(
                preset = currentState.threatLabelPreset,
                customThreatLabel = if (currentState.threatLabelPreset == com.targetdiscriminator.domain.model.ThreatLabelPreset.CUSTOM) {
                    currentState.customThreatLabel.takeIf { it.isNotBlank() }
                } else {
                    null
                },
                customNonThreatLabel = if (currentState.threatLabelPreset == com.targetdiscriminator.domain.model.ThreatLabelPreset.CUSTOM) {
                    currentState.customNonThreatLabel.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            )
            threatLabelRepository.saveThreatLabelConfig(config)
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

