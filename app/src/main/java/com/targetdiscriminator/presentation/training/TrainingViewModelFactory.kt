package com.targetdiscriminator.presentation.training

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TrainingViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrainingViewModel::class.java)) {
            return TrainingViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

