package com.targetdiscriminator.domain.model

data class ResponseResult(
    val isCorrect: Boolean,
    val userResponse: UserResponse,
    val actualThreatType: ThreatType,
    val reactionTimeMs: Long? = null
)

