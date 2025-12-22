package com.targetdiscriminator.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.targetdiscriminator.domain.model.SessionStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SessionStatsRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    suspend fun saveSessionStats(stats: SessionStats): Unit = withContext(Dispatchers.IO) {
        val allStats = getAllSessionStats().toMutableList()
        val newId = if (allStats.isEmpty()) {
            1L
        } else {
            (allStats.maxOfOrNull { it.id } ?: 0L) + 1L
        }
        val statsWithId = stats.copy(id = newId)
        allStats.add(statsWithId)
        saveAllStats(allStats)
    }
    suspend fun getAllSessionStats(): List<SessionStats> = withContext(Dispatchers.IO) {
        val jsonString = prefs.getString(KEY_STATS, null) ?: return@withContext emptyList()
        try {
            val jsonArray = JSONArray(jsonString)
            val statsList = mutableListOf<SessionStats>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val stats = parseStatsFromJson(jsonObject)
                statsList.add(stats)
            }
            statsList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun getRecentSessionStats(limit: Int = 10): List<SessionStats> = withContext(Dispatchers.IO) {
        getAllSessionStats().take(limit)
    }
    suspend fun getAverageReactionTimeOverTime(): List<Pair<Long, Long?>> = withContext(Dispatchers.IO) {
        getAllSessionStats()
            .filter { it.averageReactionTimeMs != null }
            .map { Pair(it.timestamp, it.averageReactionTimeMs) }
            .sortedBy { it.first }
    }
    private fun saveAllStats(statsList: List<SessionStats>) {
        val jsonArray = JSONArray()
        statsList.forEach { stats ->
            jsonArray.put(convertStatsToJson(stats))
        }
        prefs.edit().putString(KEY_STATS, jsonArray.toString()).apply()
    }
    private fun convertStatsToJson(stats: SessionStats): JSONObject {
        return JSONObject().apply {
            put(KEY_ID, stats.id)
            put(KEY_TIMESTAMP, stats.timestamp)
            put(KEY_TOTAL_RESPONSES, stats.totalResponses)
            put(KEY_CORRECT_RESPONSES, stats.correctResponses)
            if (stats.averageReactionTimeMs != null) {
                put(KEY_AVG_REACTION_TIME, stats.averageReactionTimeMs)
            }
        }
    }
    private fun parseStatsFromJson(jsonObject: JSONObject): SessionStats {
        return SessionStats(
            id = jsonObject.getLong(KEY_ID),
            timestamp = jsonObject.getLong(KEY_TIMESTAMP),
            totalResponses = jsonObject.getInt(KEY_TOTAL_RESPONSES),
            correctResponses = jsonObject.getInt(KEY_CORRECT_RESPONSES),
            averageReactionTimeMs = if (jsonObject.has(KEY_AVG_REACTION_TIME)) {
                jsonObject.getLong(KEY_AVG_REACTION_TIME)
            } else {
                null
            }
        )
    }
    companion object {
        private const val PREFS_NAME = "session_stats_prefs"
        private const val KEY_STATS = "session_stats"
        private const val KEY_ID = "id"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_TOTAL_RESPONSES = "total_responses"
        private const val KEY_CORRECT_RESPONSES = "correct_responses"
        private const val KEY_AVG_REACTION_TIME = "avg_reaction_time"
    }
}

