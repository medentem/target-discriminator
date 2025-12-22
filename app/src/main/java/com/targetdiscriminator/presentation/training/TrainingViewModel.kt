package com.targetdiscriminator.presentation.training

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.targetdiscriminator.data.repository.MediaRepository
import com.targetdiscriminator.data.repository.SessionStatsRepository
import com.targetdiscriminator.domain.model.SessionStats
import com.targetdiscriminator.domain.model.MediaItem
import com.targetdiscriminator.domain.model.ResponseResult
import com.targetdiscriminator.domain.model.SessionConfig
import com.targetdiscriminator.domain.model.ThreatType
import com.targetdiscriminator.domain.model.UserResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Random
import android.util.Log

class TrainingViewModel(private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(TrainingState())
    val state: StateFlow<TrainingState> = _state.asStateFlow()
    private val _effect = MutableSharedFlow<TrainingEffect>()
    val effect: SharedFlow<TrainingEffect> = _effect.asSharedFlow()
    private val mediaRepository = MediaRepository(context)
    private val sessionStatsRepository = SessionStatsRepository(context)
    private var availableMedia: List<MediaItem> = emptyList()
    private var shownMediaPaths: MutableSet<String> = mutableSetOf()
    private var timerJob: Job? = null
    private val random = Random()
    private var isShowingNextMedia = false
    private var currentMediaStartTimeMs: Long? = null
    fun initializeSession(config: SessionConfig) {
        viewModelScope.launch {
            _state.value = TrainingState(
                isPlaying = true,
                timeRemainingSeconds = config.durationMinutes * 60L
            )
            availableMedia = mediaRepository.getMediaItems(
                includeVideos = config.includeVideos,
                includePhotos = config.includePhotos
            )
            if (availableMedia.isEmpty()) {
                _effect.emit(TrainingEffect.NavigateToConfig)
                return@launch
            }
            shownMediaPaths.clear()
            startTimer(config.durationMinutes * 60L)
            showNextMedia()
        }
    }
    fun handleEvent(event: TrainingEvent) {
        Log.d("TrainingViewModel", "handleEvent: ${event::class.simpleName}")
        when (event) {
            is TrainingEvent.OnTap -> {
                Log.d("TrainingViewModel", "handleEvent: OnTap")
                handleUserResponse(UserResponse.TAP)
            }
            is TrainingEvent.OnSwipe -> {
                Log.d("TrainingViewModel", "handleEvent: OnSwipe")
                handleUserResponse(UserResponse.SWIPE)
            }
            is TrainingEvent.OnFeedbackShown -> {
                Log.d("TrainingViewModel", "OnFeedbackShown: isShowingNextMedia=$isShowingNextMedia, currentMedia=${_state.value.currentMedia?.path}")
                if (isShowingNextMedia) {
                    Log.w("TrainingViewModel", "OnFeedbackShown: already showing next media, skipping")
                    return
                }
                _state.value = _state.value.copy(showFeedback = false)
                if (_state.value.isSessionComplete) {
                    Log.d("TrainingViewModel", "OnFeedbackShown: session complete, navigating")
                    viewModelScope.launch {
                        _effect.emit(TrainingEffect.NavigateToConfig)
                    }
                } else {
                    Log.d("TrainingViewModel", "OnFeedbackShown: calling showNextMedia")
                    showNextMedia()
                }
            }
            is TrainingEvent.OnSessionComplete -> {
                Log.d("TrainingViewModel", "handleEvent: OnSessionComplete")
                timerJob?.cancel()
                val currentState = _state.value
                _state.value = currentState.copy(
                    isPlaying = false,
                    isSessionComplete = true
                )
                saveSessionStats(currentState)
            }
            is TrainingEvent.OnVideoCompleted -> {
                Log.d("TrainingViewModel", "handleEvent: OnVideoCompleted")
                handleVideoCompleted()
            }
            is TrainingEvent.OnBackPressed -> {
                Log.d("TrainingViewModel", "handleEvent: OnBackPressed")
                handleBackPressed()
            }
        }
    }
    private fun handleUserResponse(userResponse: UserResponse) {
        val currentState = _state.value
        if (currentState.hasResponded || currentState.currentMedia == null) return
        val currentMedia = currentState.currentMedia
        val expectedThreatType = when (userResponse) {
            UserResponse.TAP -> ThreatType.THREAT
            UserResponse.SWIPE -> ThreatType.NON_THREAT
        }
        val isCorrect = currentMedia.threatType == expectedThreatType
        val reactionTimeMs = calculateReactionTime(isCorrect, userResponse, currentMedia.threatType, currentMedia.type)
        val result = ResponseResult(
            isCorrect = isCorrect,
            userResponse = userResponse,
            actualThreatType = currentMedia.threatType,
            reactionTimeMs = reactionTimeMs
        )
        val updatedReactionTimes = if (reactionTimeMs != null) {
            currentState.reactionTimesMs + reactionTimeMs
        } else {
            currentState.reactionTimesMs
        }
        _state.value = currentState.copy(
            showFeedback = true,
            lastResult = result,
            totalResponses = currentState.totalResponses + 1,
            correctResponses = if (isCorrect) currentState.correctResponses + 1 else currentState.correctResponses,
            hasResponded = true,
            reactionTimesMs = updatedReactionTimes
        )
        currentMediaStartTimeMs = null
    }
    private fun calculateReactionTime(
        isCorrect: Boolean,
        userResponse: UserResponse,
        actualThreatType: ThreatType,
        mediaType: com.targetdiscriminator.domain.model.MediaType
    ): Long? {
        if (!isCorrect) return null
        val isThreatTap = userResponse == UserResponse.TAP && actualThreatType == ThreatType.THREAT
        val isNonThreatPhotoSwipe = userResponse == UserResponse.SWIPE && 
            actualThreatType == ThreatType.NON_THREAT && 
            mediaType == com.targetdiscriminator.domain.model.MediaType.PHOTO
        if (!isThreatTap && !isNonThreatPhotoSwipe) return null
        val startTime = currentMediaStartTimeMs ?: return null
        val reactionTime = System.currentTimeMillis() - startTime
        return if (reactionTime > 0) reactionTime else null
    }
    private fun handleVideoCompleted() {
        val currentState = _state.value
        Log.d("TrainingViewModel", "handleVideoCompleted: hasResponded=${currentState.hasResponded}, currentMedia=${currentState.currentMedia?.path}")
        if (currentState.hasResponded || currentState.currentMedia == null) {
            Log.d("TrainingViewModel", "handleVideoCompleted: returning early - hasResponded=${currentState.hasResponded}, currentMedia=${currentState.currentMedia?.path}")
            return
        }
        val currentMedia = currentState.currentMedia
        if (currentMedia.type == com.targetdiscriminator.domain.model.MediaType.VIDEO && 
            currentMedia.threatType == ThreatType.NON_THREAT) {
            Log.d("TrainingViewModel", "handleVideoCompleted: non-threat video completed, marking as correct")
            val result = ResponseResult(
                isCorrect = true,
                userResponse = UserResponse.SWIPE,
                actualThreatType = ThreatType.NON_THREAT,
                reactionTimeMs = null
            )
            _state.value = currentState.copy(
                showFeedback = true,
                lastResult = result,
                totalResponses = currentState.totalResponses + 1,
                correctResponses = currentState.correctResponses + 1,
                hasResponded = true
            )
            currentMediaStartTimeMs = null
            Log.d("TrainingViewModel", "handleVideoCompleted: state updated with feedback")
        } else {
            Log.d("TrainingViewModel", "handleVideoCompleted: not a non-threat video, doing nothing")
        }
    }
    private fun showNextMedia() {
        Log.d("TrainingViewModel", "showNextMedia: called, availableMedia.size=${availableMedia.size}, shownMediaPaths.size=${shownMediaPaths.size}, isShowingNextMedia=$isShowingNextMedia")
        if (isShowingNextMedia) {
            Log.w("TrainingViewModel", "showNextMedia: already showing next media, returning early")
            return
        }
        if (availableMedia.isEmpty()) {
            Log.w("TrainingViewModel", "showNextMedia: availableMedia is empty, returning")
            return
        }
        isShowingNextMedia = true
        val currentState = _state.value
        Log.d("TrainingViewModel", "showNextMedia: currentState.currentMedia=${currentState.currentMedia?.path}")
        val unshownMedia = availableMedia.filter { it.path !in shownMediaPaths }
        val mediaToChooseFrom = if (unshownMedia.isEmpty()) {
            Log.d("TrainingViewModel", "showNextMedia: all media shown, resetting and using all available media")
            shownMediaPaths.clear()
            availableMedia
        } else {
            unshownMedia
        }
        val randomIndex = random.nextInt(mediaToChooseFrom.size)
        val nextMedia = mediaToChooseFrom[randomIndex]
        shownMediaPaths.add(nextMedia.path)
        Log.d("TrainingViewModel", "showNextMedia: selected nextMedia=${nextMedia.path}, shownMediaPaths.size=${shownMediaPaths.size}")
        currentMediaStartTimeMs = System.currentTimeMillis()
        _state.value = currentState.copy(
            currentMedia = nextMedia,
            hasResponded = false
        )
        Log.d("TrainingViewModel", "showNextMedia: state updated with new media=${nextMedia.path}")
        isShowingNextMedia = false
    }
    private fun startTimer(durationSeconds: Long) {
        timerJob?.cancel()
        _state.value = _state.value.copy(timeRemainingSeconds = durationSeconds)
        timerJob = viewModelScope.launch {
            var remaining = durationSeconds
            while (remaining > 0 && _state.value.isPlaying) {
                delay(1000)
                remaining--
                _state.value = _state.value.copy(timeRemainingSeconds = remaining)
            }
            if (remaining == 0L) {
                handleEvent(TrainingEvent.OnSessionComplete)
            }
        }
    }
    fun stopSession() {
        timerJob?.cancel()
        val currentState = _state.value
        if (currentState.totalResponses > 0) {
            saveSessionStats(currentState)
        }
    }
    private fun handleBackPressed() {
        timerJob?.cancel()
        val currentState = _state.value
        if (currentState.totalResponses > 0) {
            saveSessionStats(currentState)
        }
        viewModelScope.launch {
            _effect.emit(TrainingEffect.NavigateToConfig)
        }
    }
    private fun saveSessionStats(state: TrainingState) {
        viewModelScope.launch {
            try {
                val sessionStats = SessionStats(
                    totalResponses = state.totalResponses,
                    correctResponses = state.correctResponses,
                    averageReactionTimeMs = state.averageReactionTimeMs
                )
                sessionStatsRepository.saveSessionStats(sessionStats)
                Log.d("TrainingViewModel", "Session stats saved successfully")
            } catch (e: Exception) {
                Log.e("TrainingViewModel", "Error saving session stats", e)
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

