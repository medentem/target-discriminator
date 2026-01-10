package com.targetdiscriminator.presentation.media_gallery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MediaGalleryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaGalleryViewModel::class.java)) {
            return MediaGalleryViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
