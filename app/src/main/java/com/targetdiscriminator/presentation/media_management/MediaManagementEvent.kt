package com.targetdiscriminator.presentation.media_management

import com.targetdiscriminator.presentation.mvi.ViewEvent

sealed class MediaManagementEvent : ViewEvent {
    object LoadMedia : MediaManagementEvent()
    data class SelectMediaTypeFilter(val filter: MediaTypeFilter) : MediaManagementEvent()
    data class SelectThreatTypeFilter(val filter: ThreatTypeFilter) : MediaManagementEvent()
    data class ImportMedia(val uri: android.net.Uri, val mediaType: com.targetdiscriminator.domain.model.MediaType, val threatType: com.targetdiscriminator.domain.model.ThreatType) : MediaManagementEvent()
    data class DeleteMedia(val mediaItem: com.targetdiscriminator.domain.model.MediaItem) : MediaManagementEvent()
    object DismissError : MediaManagementEvent()
}

