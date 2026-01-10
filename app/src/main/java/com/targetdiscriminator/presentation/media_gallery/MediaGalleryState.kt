package com.targetdiscriminator.presentation.media_gallery

import com.targetdiscriminator.domain.model.MediaItem
import com.targetdiscriminator.domain.model.MediaOverride
import com.targetdiscriminator.domain.model.MediaType
import com.targetdiscriminator.domain.model.ThreatType
import com.targetdiscriminator.presentation.mvi.ViewState

data class MediaGalleryState(
    val isLoading: Boolean = false,
    val allMediaItems: List<MediaItem> = emptyList(),
    val overrides: Map<String, MediaOverride> = emptyMap(),
    val selectedPaths: Set<String> = emptySet(),
    val filters: MediaGalleryFilters = MediaGalleryFilters(),
    val errorMessage: String? = null
) : ViewState {
    val filteredMediaItems: List<MediaItemWithOverride>
        get() {
            return allMediaItems
                .map { item ->
                    val override = overrides[item.path]
                    val displayThreatType = override?.threatTypeOverride ?: item.threatType
                    val isExcluded = override?.isExcluded ?: false
                    MediaItemWithOverride(item, override, displayThreatType, isExcluded)
                }
                .filter { item ->
                    // Apply media type filter
                    val matchesType = when (filters.mediaType) {
                        MediaTypeFilter.ALL -> true
                        MediaTypeFilter.PHOTOS -> item.mediaItem.type == MediaType.PHOTO
                        MediaTypeFilter.VIDEOS -> item.mediaItem.type == MediaType.VIDEO
                    }
                    
                    // Apply threat type filter
                    val matchesThreat = when (filters.threatType) {
                        ThreatTypeFilter.ALL -> true
                        ThreatTypeFilter.THREAT -> item.displayThreatType == ThreatType.THREAT
                        ThreatTypeFilter.NON_THREAT -> item.displayThreatType == ThreatType.NON_THREAT
                    }
                    
                    // Apply status filter
                    val matchesStatus = when (filters.status) {
                        StatusFilter.ALL -> true
                        StatusFilter.INCLUDED -> !item.isExcluded
                        StatusFilter.EXCLUDED -> item.isExcluded
                    }
                    
                    // Apply source filter
                    val matchesSource = when (filters.source) {
                        SourceFilter.ALL -> true
                        SourceFilter.BUILT_IN -> item.mediaItem.path.startsWith("assets://")
                        SourceFilter.USER -> !item.mediaItem.path.startsWith("assets://")
                    }
                    
                    matchesType && matchesThreat && matchesStatus && matchesSource
                }
        }
}

data class MediaItemWithOverride(
    val mediaItem: MediaItem,
    val override: MediaOverride?,
    val displayThreatType: ThreatType,
    val isExcluded: Boolean
)

data class MediaGalleryFilters(
    val mediaType: MediaTypeFilter = MediaTypeFilter.ALL,
    val threatType: ThreatTypeFilter = ThreatTypeFilter.ALL,
    val status: StatusFilter = StatusFilter.ALL,
    val source: SourceFilter = SourceFilter.ALL
)

enum class MediaTypeFilter {
    ALL, PHOTOS, VIDEOS
}

enum class ThreatTypeFilter {
    ALL, THREAT, NON_THREAT
}

enum class StatusFilter {
    ALL, INCLUDED, EXCLUDED
}

enum class SourceFilter {
    ALL, BUILT_IN, USER
}
