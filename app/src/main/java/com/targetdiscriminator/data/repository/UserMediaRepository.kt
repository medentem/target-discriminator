package com.targetdiscriminator.data.repository

import android.content.Context
import com.targetdiscriminator.domain.model.MediaItem
import com.targetdiscriminator.domain.model.MediaType
import com.targetdiscriminator.domain.model.ThreatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class UserMediaRepository(private val context: Context) {
    private val userMediaDir: File by lazy {
        File(context.getExternalFilesDir(null), "user_media").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    suspend fun getUserMediaItems(
        mediaType: MediaType,
        threatType: ThreatType
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        try {
            val typeFolder = when (mediaType) {
                MediaType.PHOTO -> "photos"
                MediaType.VIDEO -> "videos"
            }
            val threatFolder = when (threatType) {
                ThreatType.THREAT -> "threat"
                ThreatType.NON_THREAT -> "non_threat"
            }
            val folder = File(userMediaDir, "$typeFolder/$threatFolder")
            if (folder.exists() && folder.isDirectory) {
                folder.listFiles()?.forEach { file ->
                    if (file.isFile && isValidMediaFile(file, mediaType)) {
                        items.add(MediaItem(file.absolutePath, mediaType, threatType))
                    }
                }
            }
        } catch (e: Exception) {
        }
        items
    }
    suspend fun saveUserMedia(
        sourceUri: android.net.Uri,
        mediaType: MediaType,
        threatType: ThreatType
    ): Result<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val typeFolder = when (mediaType) {
                MediaType.PHOTO -> "photos"
                MediaType.VIDEO -> "videos"
            }
            val threatFolder = when (threatType) {
                ThreatType.THREAT -> "threat"
                ThreatType.NON_THREAT -> "non_threat"
            }
            val folder = File(userMediaDir, "$typeFolder/$threatFolder")
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val extension = getFileExtension(sourceUri, context)
            val fileName = "${UUID.randomUUID()}.$extension"
            val destinationFile = File(folder, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Failed to open input stream"))
            val mediaItem = MediaItem(destinationFile.absolutePath, mediaType, threatType)
            Result.success(mediaItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun deleteUserMedia(mediaItem: MediaItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(mediaItem.path)
            if (file.exists() && file.delete()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getAllUserMediaItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        items.addAll(getUserMediaItems(MediaType.PHOTO, ThreatType.THREAT))
        items.addAll(getUserMediaItems(MediaType.PHOTO, ThreatType.NON_THREAT))
        items.addAll(getUserMediaItems(MediaType.VIDEO, ThreatType.THREAT))
        items.addAll(getUserMediaItems(MediaType.VIDEO, ThreatType.NON_THREAT))
        items
    }
    private fun isValidMediaFile(file: File, mediaType: MediaType): Boolean {
        val extension = file.extension.lowercase()
        return when (mediaType) {
            MediaType.PHOTO -> extension in listOf("jpg", "jpeg", "png", "webp")
            MediaType.VIDEO -> extension in listOf("mp4", "webm", "mkv")
        }
    }
    private fun getFileExtension(uri: android.net.Uri, context: Context): String {
        val mimeType = context.contentResolver.getType(uri)
        return when {
            mimeType?.startsWith("image/") == true -> {
                when (mimeType) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
            }
            mimeType?.startsWith("video/") == true -> {
                when (mimeType) {
                    "video/mp4" -> "mp4"
                    "video/webm" -> "webm"
                    "video/x-matroska" -> "mkv"
                    else -> "mp4"
                }
            }
            else -> {
                val path = uri.path ?: ""
                val lastDot = path.lastIndexOf('.')
                if (lastDot >= 0 && lastDot < path.length - 1) {
                    path.substring(lastDot + 1).lowercase()
                } else {
                    "jpg"
                }
            }
        }
    }
}

