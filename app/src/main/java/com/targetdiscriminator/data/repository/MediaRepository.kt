package com.targetdiscriminator.data.repository

import android.content.Context
import com.targetdiscriminator.domain.model.MediaItem
import com.targetdiscriminator.domain.model.MediaType
import com.targetdiscriminator.domain.model.ThreatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val context: Context) {
    private val userMediaRepository = UserMediaRepository(context)
    private val overrideRepository = MediaOverrideRepository(context)
    
    suspend fun getMediaItems(
        includeVideos: Boolean,
        includePhotos: Boolean,
        includeUserMedia: Boolean = true
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaItems = mutableListOf<MediaItem>()
        if (includeVideos) {
            mediaItems.addAll(getMediaItemsFromAssets("videos/threat", MediaType.VIDEO, ThreatType.THREAT))
            mediaItems.addAll(getMediaItemsFromAssets("videos/non_threat", MediaType.VIDEO, ThreatType.NON_THREAT))
            if (includeUserMedia) {
                mediaItems.addAll(userMediaRepository.getUserMediaItems(MediaType.VIDEO, ThreatType.THREAT))
                mediaItems.addAll(userMediaRepository.getUserMediaItems(MediaType.VIDEO, ThreatType.NON_THREAT))
            }
        }
        if (includePhotos) {
            mediaItems.addAll(getMediaItemsFromAssets("photos/threat", MediaType.PHOTO, ThreatType.THREAT))
            mediaItems.addAll(getMediaItemsFromAssets("photos/non_threat", MediaType.PHOTO, ThreatType.NON_THREAT))
            if (includeUserMedia) {
                mediaItems.addAll(userMediaRepository.getUserMediaItems(MediaType.PHOTO, ThreatType.THREAT))
                mediaItems.addAll(userMediaRepository.getUserMediaItems(MediaType.PHOTO, ThreatType.NON_THREAT))
            }
        }
        // Apply overrides: filter excluded media and apply threat type overrides
        applyOverrides(mediaItems)
    }
    
    suspend fun getAllMediaItemsForGallery(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaItems = mutableListOf<MediaItem>()
        
        // Add all built-in media
        mediaItems.addAll(getMediaItemsFromAssets("videos/threat", MediaType.VIDEO, ThreatType.THREAT))
        mediaItems.addAll(getMediaItemsFromAssets("videos/non_threat", MediaType.VIDEO, ThreatType.NON_THREAT))
        mediaItems.addAll(getMediaItemsFromAssets("photos/threat", MediaType.PHOTO, ThreatType.THREAT))
        mediaItems.addAll(getMediaItemsFromAssets("photos/non_threat", MediaType.PHOTO, ThreatType.NON_THREAT))
        
        // Add all user media
        mediaItems.addAll(userMediaRepository.getUserMediaItems(MediaType.VIDEO, ThreatType.THREAT))
        mediaItems.addAll(userMediaRepository.getUserMediaItems(MediaType.VIDEO, ThreatType.NON_THREAT))
        mediaItems.addAll(userMediaRepository.getUserMediaItems(MediaType.PHOTO, ThreatType.THREAT))
        mediaItems.addAll(userMediaRepository.getUserMediaItems(MediaType.PHOTO, ThreatType.NON_THREAT))
        
        mediaItems
    }
    
    private suspend fun applyOverrides(mediaItems: List<MediaItem>): List<MediaItem> {
        val overrides = overrideRepository.getOverridesMap()
        val excludedPaths = overrideRepository.getExcludedPaths()
        
        // Filter out excluded media
        val filtered = mediaItems.filter { !excludedPaths.contains(it.path) }
        
        // Apply threat type overrides
        return filtered.map { item ->
            val override = overrides[item.path]
            if (override?.threatTypeOverride != null) {
                item.copy(threatType = override.threatTypeOverride)
            } else {
                item
            }
        }
    }
    private fun getMediaItemsFromAssets(
        assetPath: String,
        mediaType: MediaType,
        threatType: ThreatType
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        try {
            val files = context.assets.list(assetPath) ?: return emptyList()
            files.forEach { fileName ->
                items.add(MediaItem("assets://$assetPath/$fileName", mediaType, threatType))
            }
        } catch (e: Exception) {
        }
        return items
    }
}

