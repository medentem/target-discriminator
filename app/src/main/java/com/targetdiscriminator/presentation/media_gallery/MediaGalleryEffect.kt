package com.targetdiscriminator.presentation.media_gallery

import com.targetdiscriminator.domain.model.MediaItem
import com.targetdiscriminator.presentation.mvi.ViewEffect

sealed class MediaGalleryEffect : ViewEffect {
    data class ShowError(val message: String) : MediaGalleryEffect()
    data class NavigateToPreview(val mediaItems: List<MediaItem>, val currentIndex: Int) : MediaGalleryEffect()
}
