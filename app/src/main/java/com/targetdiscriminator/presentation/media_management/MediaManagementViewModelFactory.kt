package com.targetdiscriminator.presentation.media_management

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MediaManagementViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaManagementViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

