package com.targetdiscriminator.presentation.media_management

import com.targetdiscriminator.domain.model.MediaItem
import com.targetdiscriminator.presentation.mvi.ViewState

data class MediaManagementState(
    val isLoading: Boolean = false,
    val userMediaItems: List<MediaItem> = emptyList(),
    val selectedMediaType: MediaTypeFilter = MediaTypeFilter.ALL,
    val selectedThreatType: ThreatTypeFilter = ThreatTypeFilter.ALL,
    val errorMessage: String? = null
) : ViewState {
    val filteredMediaItems: List<MediaItem>
        get() = userMediaItems.filter { item ->
            val matchesType = when (selectedMediaType) {
                MediaTypeFilter.ALL -> true
                MediaTypeFilter.PHOTOS -> item.type == com.targetdiscriminator.domain.model.MediaType.PHOTO
                MediaTypeFilter.VIDEOS -> item.type == com.targetdiscriminator.domain.model.MediaType.VIDEO
            }
            val matchesThreat = when (selectedThreatType) {
                ThreatTypeFilter.ALL -> true
                ThreatTypeFilter.THREAT -> item.threatType == com.targetdiscriminator.domain.model.ThreatType.THREAT
                ThreatTypeFilter.NON_THREAT -> item.threatType == com.targetdiscriminator.domain.model.ThreatType.NON_THREAT
            }
            matchesType && matchesThreat
        }
}

enum class MediaTypeFilter {
    ALL, PHOTOS, VIDEOS
}

enum class ThreatTypeFilter {
    ALL, THREAT, NON_THREAT
}

