package com.targetdiscriminator.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.targetdiscriminator.domain.model.MediaOverride
import com.targetdiscriminator.domain.model.ThreatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MediaOverrideRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    suspend fun getOverride(mediaPath: String): MediaOverride? = withContext(Dispatchers.IO) {
        getAllOverrides().find { it.mediaPath == mediaPath }
    }

    suspend fun getAllOverrides(): List<MediaOverride> = withContext(Dispatchers.IO) {
        val jsonString = prefs.getString(KEY_OVERRIDES, null) ?: return@withContext emptyList()
        try {
            val jsonArray = JSONArray(jsonString)
            val overridesList = mutableListOf<MediaOverride>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val override = parseOverrideFromJson(jsonObject)
                overridesList.add(override)
            }
            overridesList
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getOverridesMap(): Map<String, MediaOverride> = withContext(Dispatchers.IO) {
        getAllOverrides().associateBy { it.mediaPath }
    }

    suspend fun getExcludedPaths(): Set<String> = withContext(Dispatchers.IO) {
        getAllOverrides()
            .filter { it.isExcluded }
            .map { it.mediaPath }
            .toSet()
    }

    suspend fun saveOverride(override: MediaOverride): Unit = withContext(Dispatchers.IO) {
        val allOverrides = getAllOverrides().toMutableList()
        val existingIndex = allOverrides.indexOfFirst { it.mediaPath == override.mediaPath }
        
        if (existingIndex >= 0) {
            allOverrides[existingIndex] = override
        } else {
            allOverrides.add(override)
        }
        
        saveAllOverrides(allOverrides)
    }

    suspend fun setExcluded(mediaPath: String, excluded: Boolean): Unit = withContext(Dispatchers.IO) {
        val existing = getOverride(mediaPath)
        val now = System.currentTimeMillis()
        
        val override = existing?.copy(
            isExcluded = excluded,
            updatedAt = now
        ) ?: MediaOverride(
            mediaPath = mediaPath,
            isExcluded = excluded,
            createdAt = now,
            updatedAt = now
        )
        
        // If no exclusions and no overrides, delete the override entry
        if (!excluded && override.threatTypeOverride == null) {
            clearOverride(mediaPath)
        } else {
            saveOverride(override)
        }
    }

    suspend fun setThreatTypeOverride(
        mediaPath: String,
        threatType: ThreatType?
    ): Unit = withContext(Dispatchers.IO) {
        val existing = getOverride(mediaPath)
        val now = System.currentTimeMillis()
        
        val override = existing?.copy(
            threatTypeOverride = threatType,
            updatedAt = now
        ) ?: MediaOverride(
            mediaPath = mediaPath,
            isExcluded = false,
            threatTypeOverride = threatType,
            createdAt = now,
            updatedAt = now
        )
        
        // If no exclusions and no overrides, delete the override entry
        if (!override.isExcluded && threatType == null) {
            clearOverride(mediaPath)
        } else {
            saveOverride(override)
        }
    }

    suspend fun clearOverride(mediaPath: String): Unit = withContext(Dispatchers.IO) {
        val allOverrides = getAllOverrides().toMutableList()
        allOverrides.removeAll { it.mediaPath == mediaPath }
        saveAllOverrides(allOverrides)
    }

    suspend fun clearAllOverrides(): Unit = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_OVERRIDES).apply()
    }

    private fun saveAllOverrides(overridesList: List<MediaOverride>) {
        val jsonArray = JSONArray()
        overridesList.forEach { override ->
            jsonArray.put(convertOverrideToJson(override))
        }
        prefs.edit().putString(KEY_OVERRIDES, jsonArray.toString()).apply()
    }

    private fun convertOverrideToJson(override: MediaOverride): JSONObject {
        return JSONObject().apply {
            put(KEY_MEDIA_PATH, override.mediaPath)
            put(KEY_IS_EXCLUDED, override.isExcluded)
            if (override.threatTypeOverride != null) {
                put(KEY_THREAT_TYPE_OVERRIDE, override.threatTypeOverride.name)
            }
            put(KEY_CREATED_AT, override.createdAt)
            put(KEY_UPDATED_AT, override.updatedAt)
        }
    }

    private fun parseOverrideFromJson(jsonObject: JSONObject): MediaOverride {
        return MediaOverride(
            mediaPath = jsonObject.getString(KEY_MEDIA_PATH),
            isExcluded = jsonObject.getBoolean(KEY_IS_EXCLUDED),
            threatTypeOverride = if (jsonObject.has(KEY_THREAT_TYPE_OVERRIDE)) {
                ThreatType.valueOf(jsonObject.getString(KEY_THREAT_TYPE_OVERRIDE))
            } else {
                null
            },
            createdAt = jsonObject.getLong(KEY_CREATED_AT),
            updatedAt = jsonObject.getLong(KEY_UPDATED_AT)
        )
    }

    companion object {
        private const val PREFS_NAME = "media_override_prefs"
        private const val KEY_OVERRIDES = "media_overrides"
        private const val KEY_MEDIA_PATH = "media_path"
        private const val KEY_IS_EXCLUDED = "is_excluded"
        private const val KEY_THREAT_TYPE_OVERRIDE = "threat_type_override"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_UPDATED_AT = "updated_at"
    }
}
