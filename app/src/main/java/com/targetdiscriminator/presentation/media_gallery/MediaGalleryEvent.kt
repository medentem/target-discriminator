package com.targetdiscriminator.presentation.media_gallery

import com.targetdiscriminator.domain.model.MediaItem
import com.targetdiscriminator.domain.model.ThreatType
import com.targetdiscriminator.presentation.mvi.ViewEvent

sealed class MediaGalleryEvent : ViewEvent {
    object LoadMedia : MediaGalleryEvent()
    data class SelectMediaTypeFilter(val filter: MediaTypeFilter) : MediaGalleryEvent()
    data class SelectThreatTypeFilter(val filter: ThreatTypeFilter) : MediaGalleryEvent()
    data class SelectStatusFilter(val filter: StatusFilter) : MediaGalleryEvent()
    data class SelectSourceFilter(val filter: SourceFilter) : MediaGalleryEvent()
    data class ToggleSelection(val mediaPath: String) : MediaGalleryEvent()
    object SelectAll : MediaGalleryEvent()
    object DeselectAll : MediaGalleryEvent()
    data class ExcludeMedia(val mediaPath: String) : MediaGalleryEvent()
    data class IncludeMedia(val mediaPath: String) : MediaGalleryEvent()
    data class ReclassifyMedia(val mediaPath: String, val threatType: ThreatType) : MediaGalleryEvent()
    data class BulkExclude(val paths: Set<String>) : MediaGalleryEvent()
    data class BulkInclude(val paths: Set<String>) : MediaGalleryEvent()
    data class BulkReclassify(val paths: Set<String>, val threatType: ThreatType) : MediaGalleryEvent()
    data class ClearOverrides(val paths: Set<String>) : MediaGalleryEvent()
    object ClearAllOverrides : MediaGalleryEvent()
    data class ViewMedia(val mediaItem: MediaItem, val currentIndex: Int) : MediaGalleryEvent()
    object DismissError : MediaGalleryEvent()
}
