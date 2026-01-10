package com.targetdiscriminator.presentation.session_config

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SessionConfigViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionConfigViewModel::class.java)) {
            return SessionConfigViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
