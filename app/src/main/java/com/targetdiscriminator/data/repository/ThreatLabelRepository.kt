package com.targetdiscriminator.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.targetdiscriminator.domain.model.ThreatLabelConfig
import com.targetdiscriminator.domain.model.ThreatLabelPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ThreatLabelRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    suspend fun getThreatLabelConfig(): ThreatLabelConfig = withContext(Dispatchers.IO) {
        val jsonString = prefs.getString(KEY_THREAT_LABELS, null)
        if (jsonString == null) {
            return@withContext ThreatLabelConfig()
        }
        try {
            val jsonObject = JSONObject(jsonString)
            ThreatLabelConfig(
                preset = ThreatLabelPreset.valueOf(
                    jsonObject.getString(KEY_PRESET)
                ),
                customThreatLabel = if (jsonObject.has(KEY_CUSTOM_THREAT_LABEL)) {
                    jsonObject.getString(KEY_CUSTOM_THREAT_LABEL)
                } else {
                    null
                },
                customNonThreatLabel = if (jsonObject.has(KEY_CUSTOM_NON_THREAT_LABEL)) {
                    jsonObject.getString(KEY_CUSTOM_NON_THREAT_LABEL)
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            ThreatLabelConfig()
        }
    }

    suspend fun saveThreatLabelConfig(config: ThreatLabelConfig): Unit = withContext(Dispatchers.IO) {
        try {
            val jsonObject = JSONObject().apply {
                put(KEY_PRESET, config.preset.name)
                if (config.customThreatLabel != null) {
                    put(KEY_CUSTOM_THREAT_LABEL, config.customThreatLabel)
                }
                if (config.customNonThreatLabel != null) {
                    put(KEY_CUSTOM_NON_THREAT_LABEL, config.customNonThreatLabel)
                }
            }
            prefs.edit().putString(KEY_THREAT_LABELS, jsonObject.toString()).apply()
        } catch (e: Exception) {
            // Handle error silently or log it
        }
    }

    companion object {
        private const val PREFS_NAME = "threat_label_prefs"
        private const val KEY_THREAT_LABELS = "threat_labels"
        private const val KEY_PRESET = "preset"
        private const val KEY_CUSTOM_THREAT_LABEL = "custom_threat_label"
        private const val KEY_CUSTOM_NON_THREAT_LABEL = "custom_non_threat_label"
    }
}
