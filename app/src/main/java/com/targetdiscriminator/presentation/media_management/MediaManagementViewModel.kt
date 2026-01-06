package com.targetdiscriminator.presentation.media_management

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.targetdiscriminator.data.repository.UserMediaRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaManagementViewModel(private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(MediaManagementState())
    val state: StateFlow<MediaManagementState> = _state.asStateFlow()
    private val _effect = MutableSharedFlow<MediaManagementEffect>()
    val effect: SharedFlow<MediaManagementEffect> = _effect.asSharedFlow()
    private val userMediaRepository = UserMediaRepository(context)
    init {
        loadMedia()
    }
    fun handleEvent(event: MediaManagementEvent) {
        when (event) {
            is MediaManagementEvent.LoadMedia -> {
                loadMedia()
            }
            is MediaManagementEvent.SelectMediaTypeFilter -> {
                _state.value = _state.value.copy(selectedMediaType = event.filter)
            }
            is MediaManagementEvent.SelectThreatTypeFilter -> {
                _state.value = _state.value.copy(selectedThreatType = event.filter)
            }
            is MediaManagementEvent.ImportMedia -> {
                importMedia(event.uri, event.mediaType, event.threatType)
            }
            is MediaManagementEvent.DeleteMedia -> {
                deleteMedia(event.mediaItem)
            }
            is MediaManagementEvent.DismissError -> {
                _state.value = _state.value.copy(errorMessage = null)
            }
        }
    }
    private fun loadMedia() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val mediaItems = userMediaRepository.getAllUserMediaItems()
                _state.value = _state.value.copy(
                    isLoading = false,
                    userMediaItems = mediaItems
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load media: ${e.message}"
                )
            }
        }
    }
    private fun importMedia(
        uri: android.net.Uri,
        mediaType: com.targetdiscriminator.domain.model.MediaType,
        threatType: com.targetdiscriminator.domain.model.ThreatType
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = userMediaRepository.saveUserMedia(uri, mediaType, threatType)
                result.onSuccess {
                    loadMedia()
                }.onFailure { exception ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to import media: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to import media: ${e.message}"
                )
            }
        }
    }
    private fun deleteMedia(mediaItem: com.targetdiscriminator.domain.model.MediaItem) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = userMediaRepository.deleteUserMedia(mediaItem)
                result.onSuccess {
                    loadMedia()
                }.onFailure { exception ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete media: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete media: ${e.message}"
                )
            }
        }
    }
}

