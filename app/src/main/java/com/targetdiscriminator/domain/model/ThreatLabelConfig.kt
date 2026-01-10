package com.targetdiscriminator.domain.model

data class ThreatLabelConfig(
    val preset: ThreatLabelPreset = ThreatLabelPreset.THREAT_NON_THREAT,
    val customThreatLabel: String? = null,
    val customNonThreatLabel: String? = null
)
