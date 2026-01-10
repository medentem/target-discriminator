package com.targetdiscriminator.data.provider

import android.content.Context
import com.targetdiscriminator.data.repository.ThreatLabelRepository
import com.targetdiscriminator.domain.model.ThreatLabelConfig
import com.targetdiscriminator.domain.model.ThreatLabelPreset
import com.targetdiscriminator.domain.model.ThreatLabels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThreatLabelProvider(private val context: Context) {
    private val repository = ThreatLabelRepository(context)
    
    private val _labels = MutableStateFlow<ThreatLabels>(getDefaultLabels())
    val labels: StateFlow<ThreatLabels> = _labels.asStateFlow()

    private val _config = MutableStateFlow<ThreatLabelConfig>(ThreatLabelConfig())
    val config: StateFlow<ThreatLabelConfig> = _config.asStateFlow()

    init {
        // Load config asynchronously on initialization
        CoroutineScope(Dispatchers.Main).launch {
            loadConfig()
        }
    }

    suspend fun loadConfig() {
        val config = withContext(Dispatchers.IO) {
            repository.getThreatLabelConfig()
        }
        _config.value = config
        _labels.value = getLabelsForConfig(config)
    }

    suspend fun saveConfig(config: ThreatLabelConfig) {
        withContext(Dispatchers.IO) {
            repository.saveThreatLabelConfig(config)
        }
        _config.value = config
        _labels.value = getLabelsForConfig(config)
    }

    fun getLabelsForConfig(config: ThreatLabelConfig): ThreatLabels {
        return when (config.preset) {
            ThreatLabelPreset.THREAT_NON_THREAT -> ThreatLabels(
                threat = "Threat",
                nonThreat = "Non-Threat"
            )
            ThreatLabelPreset.SHOOT_NO_SHOOT -> ThreatLabels(
                threat = "Shoot",
                nonThreat = "No-Shoot"
            )
            ThreatLabelPreset.CUSTOM -> ThreatLabels(
                threat = config.customThreatLabel?.takeIf { it.isNotBlank() } ?: "Threat",
                nonThreat = config.customNonThreatLabel?.takeIf { it.isNotBlank() } ?: "Non-Threat"
            )
        }
    }

    private fun getDefaultLabels(): ThreatLabels {
        return ThreatLabels(
            threat = "Threat",
            nonThreat = "Non-Threat"
        )
    }

    fun getCurrentLabels(): ThreatLabels {
        return _labels.value
    }
}
