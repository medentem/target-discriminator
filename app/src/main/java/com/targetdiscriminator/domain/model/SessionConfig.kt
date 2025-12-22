package com.targetdiscriminator.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SessionConfig(
    val includeVideos: Boolean,
    val includePhotos: Boolean,
    val durationMinutes: Int
) : Parcelable {
    fun isValid(): Boolean {
        return (includeVideos || includePhotos) && durationMinutes > 0
    }
}

