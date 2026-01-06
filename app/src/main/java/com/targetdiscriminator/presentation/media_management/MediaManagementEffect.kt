package com.targetdiscriminator.presentation.media_management

import com.targetdiscriminator.presentation.mvi.ViewEffect

sealed class MediaManagementEffect : ViewEffect {
    data class ShowError(val message: String) : MediaManagementEffect()
    data class RequestMediaImport(val mediaType: com.targetdiscriminator.domain.model.MediaType) : MediaManagementEffect()
    data class RequestThreatTypeSelection(val mediaType: com.targetdiscriminator.domain.model.MediaType, val uri: android.net.Uri) : MediaManagementEffect()
}

