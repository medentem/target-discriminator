package com.targetdiscriminator.presentation.media_gallery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.targetdiscriminator.data.repository.MediaOverrideRepository
import com.targetdiscriminator.data.repository.MediaRepository
import com.targetdiscriminator.domain.model.ThreatType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaGalleryViewModel(private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(MediaGalleryState())
    val state: StateFlow<MediaGalleryState> = _state.asStateFlow()
    private val _effect = MutableSharedFlow<MediaGalleryEffect>()
    val effect: SharedFlow<MediaGalleryEffect> = _effect.asSharedFlow()
    
    private val mediaRepository = MediaRepository(context)
    private val overrideRepository = MediaOverrideRepository(context)
    
    init {
        loadMedia()
    }
    
    fun handleEvent(event: MediaGalleryEvent) {
        when (event) {
            is MediaGalleryEvent.LoadMedia -> {
                loadMedia()
            }
            is MediaGalleryEvent.SelectMediaTypeFilter -> {
                _state.value = _state.value.copy(
                    filters = _state.value.filters.copy(mediaType = event.filter)
                )
            }
            is MediaGalleryEvent.SelectThreatTypeFilter -> {
                _state.value = _state.value.copy(
                    filters = _state.value.filters.copy(threatType = event.filter)
                )
            }
            is MediaGalleryEvent.SelectStatusFilter -> {
                _state.value = _state.value.copy(
                    filters = _state.value.filters.copy(status = event.filter)
                )
            }
            is MediaGalleryEvent.SelectSourceFilter -> {
                _state.value = _state.value.copy(
                    filters = _state.value.filters.copy(source = event.filter)
                )
            }
            is MediaGalleryEvent.ToggleSelection -> {
                val currentSelected = _state.value.selectedPaths.toMutableSet()
                if (currentSelected.contains(event.mediaPath)) {
                    currentSelected.remove(event.mediaPath)
                } else {
                    currentSelected.add(event.mediaPath)
                }
                _state.value = _state.value.copy(selectedPaths = currentSelected)
            }
            is MediaGalleryEvent.SelectAll -> {
                val allPaths = _state.value.filteredMediaItems.map { it.mediaItem.path }.toSet()
                _state.value = _state.value.copy(selectedPaths = allPaths)
            }
            is MediaGalleryEvent.DeselectAll -> {
                _state.value = _state.value.copy(selectedPaths = emptySet())
            }
            is MediaGalleryEvent.ExcludeMedia -> {
                excludeMedia(event.mediaPath)
            }
            is MediaGalleryEvent.IncludeMedia -> {
                includeMedia(event.mediaPath)
            }
            is MediaGalleryEvent.ReclassifyMedia -> {
                reclassifyMedia(event.mediaPath, event.threatType)
            }
            is MediaGalleryEvent.BulkExclude -> {
                bulkExclude(event.paths)
            }
            is MediaGalleryEvent.BulkInclude -> {
                bulkInclude(event.paths)
            }
            is MediaGalleryEvent.BulkReclassify -> {
                bulkReclassify(event.paths, event.threatType)
            }
            is MediaGalleryEvent.ClearOverrides -> {
                clearOverrides(event.paths)
            }
            is MediaGalleryEvent.ClearAllOverrides -> {
                clearAllOverrides()
            }
            is MediaGalleryEvent.ViewMedia -> {
                val filteredItems = _state.value.filteredMediaItems.map { it.mediaItem }
                viewModelScope.launch {
                    _effect.emit(MediaGalleryEffect.NavigateToPreview(filteredItems, event.currentIndex))
                }
            }
            is MediaGalleryEvent.DismissError -> {
                _state.value = _state.value.copy(errorMessage = null)
            }
        }
    }
    
    private fun loadMedia() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                val mediaItems = mediaRepository.getAllMediaItemsForGallery()
                val overrides = overrideRepository.getOverridesMap()
                _state.value = _state.value.copy(
                    isLoading = false,
                    allMediaItems = mediaItems,
                    overrides = overrides
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load media: ${e.message}"
                )
            }
        }
    }
    
    private fun excludeMedia(mediaPath: String) {
        viewModelScope.launch {
            try {
                overrideRepository.setExcluded(mediaPath, true)
                loadMedia()
            } catch (e: Exception) {
                _effect.emit(MediaGalleryEffect.ShowError("Failed to exclude media: ${e.message}"))
            }
        }
    }
    
    private fun includeMedia(mediaPath: String) {
        viewModelScope.launch {
            try {
                overrideRepository.setExcluded(mediaPath, false)
                loadMedia()
            } catch (e: Exception) {
                _effect.emit(MediaGalleryEffect.ShowError("Failed to include media: ${e.message}"))
            }
        }
    }
    
    private fun reclassifyMedia(mediaPath: String, threatType: ThreatType) {
        viewModelScope.launch {
            try {
                overrideRepository.setThreatTypeOverride(mediaPath, threatType)
                loadMedia()
            } catch (e: Exception) {
                _effect.emit(MediaGalleryEffect.ShowError("Failed to reclassify media: ${e.message}"))
            }
        }
    }
    
    private fun bulkExclude(paths: Set<String>) {
        viewModelScope.launch {
            try {
                paths.forEach { path ->
                    overrideRepository.setExcluded(path, true)
                }
                _state.value = _state.value.copy(selectedPaths = emptySet())
                loadMedia()
            } catch (e: Exception) {
                _effect.emit(MediaGalleryEffect.ShowError("Failed to exclude media: ${e.message}"))
            }
        }
    }
    
    private fun bulkInclude(paths: Set<String>) {
        viewModelScope.launch {
            try {
                paths.forEach { path ->
                    overrideRepository.setExcluded(path, false)
                }
                _state.value = _state.value.copy(selectedPaths = emptySet())
                loadMedia()
            } catch (e: Exception) {
                _effect.emit(MediaGalleryEffect.ShowError("Failed to include media: ${e.message}"))
            }
        }
    }
    
    private fun bulkReclassify(paths: Set<String>, threatType: ThreatType) {
        viewModelScope.launch {
            try {
                paths.forEach { path ->
                    overrideRepository.setThreatTypeOverride(path, threatType)
                }
                _state.value = _state.value.copy(selectedPaths = emptySet())
                loadMedia()
            } catch (e: Exception) {
                _effect.emit(MediaGalleryEffect.ShowError("Failed to reclassify media: ${e.message}"))
            }
        }
    }
    
    private fun clearOverrides(paths: Set<String>) {
        viewModelScope.launch {
            try {
                paths.forEach { path ->
                    overrideRepository.clearOverride(path)
                }
                _state.value = _state.value.copy(selectedPaths = emptySet())
                loadMedia()
            } catch (e: Exception) {
                _effect.emit(MediaGalleryEffect.ShowError("Failed to clear overrides: ${e.message}"))
            }
        }
    }
    
    private fun clearAllOverrides() {
        viewModelScope.launch {
            try {
                overrideRepository.clearAllOverrides()
                _state.value = _state.value.copy(selectedPaths = emptySet())
                loadMedia()
            } catch (e: Exception) {
                _effect.emit(MediaGalleryEffect.ShowError("Failed to clear all overrides: ${e.message}"))
            }
        }
    }
}
