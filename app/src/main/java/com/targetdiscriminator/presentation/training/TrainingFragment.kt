package com.targetdiscriminator.presentation.training

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import java.io.File
import java.io.FileOutputStream
import androidx.navigation.fragment.findNavController
import com.targetdiscriminator.R
import com.targetdiscriminator.databinding.FragmentTrainingBinding
import com.targetdiscriminator.domain.model.MediaType
import kotlinx.coroutines.launch
import android.util.Log

class TrainingFragment : Fragment() {
    private var _binding: FragmentTrainingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrainingViewModel by viewModels {
        TrainingViewModelFactory(requireContext())
    }
    private var exoPlayer: ExoPlayer? = null
    private lateinit var gestureDetector: GestureDetector
    private var isDisplayingMedia = false
    private var feedbackCallbackScheduled = false
    private var feedbackCallbackRunnable: Runnable? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("TrainingFragment", "onCreate: creating back pressed callback")
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("TrainingFragment", "onBackPressedCallback: handleOnBackPressed called")
                handleBackPressed()
            }
        }
        Log.d("TrainingFragment", "onCreate: callback created, enabled=${onBackPressedCallback.isEnabled}")
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainingBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("TrainingFragment", "onViewCreated: called")
        val navController = findNavController()
        val previousEntry = navController.previousBackStackEntry
        Log.d("TrainingFragment", "onViewCreated: currentDestination=${navController.currentDestination?.id}, previousDestination=${previousEntry?.destination?.id}")
        val config = arguments?.getParcelable<com.targetdiscriminator.domain.model.SessionConfig>("config")
        if (config == null) {
            Log.w("TrainingFragment", "onViewCreated: config is null, popping back stack")
            navController.popBackStack()
            return
        }
        setupGestureDetector()
        setupObservers()
        setupBackButtonHandler()
        viewModel.initializeSession(config)
    }
    override fun onStart() {
        super.onStart()
        val navController = findNavController()
        val previousEntry = navController.previousBackStackEntry
        Log.d("TrainingFragment", "onStart: callback enabled=${onBackPressedCallback.isEnabled}, currentDestination=${navController.currentDestination?.id}, previousDestination=${previousEntry?.destination?.id}")
    }
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                showTapIndicator(e.x, e.y)
                viewModel.handleEvent(TrainingEvent.OnTap(e.x, e.y))
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null) {
                    val deltaX = e2.x - e1.x
                    if (kotlin.math.abs(deltaX) > 100) {
                        viewModel.handleEvent(TrainingEvent.OnSwipe)
                        return true
                    }
                    return false
                }
                return false
            }
        })
    }
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                renderState(state)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.effect.collect { effect ->
                handleEffect(effect)
            }
        }
    }
    private fun setupBackButtonHandler() {
        Log.d("TrainingFragment", "setupBackButtonHandler: registering callback")
        val dispatcher = requireActivity().onBackPressedDispatcher
        dispatcher.addCallback(
            this,
            onBackPressedCallback
        )
        Log.d("TrainingFragment", "setupBackButtonHandler: callback registered, enabled=${onBackPressedCallback.isEnabled}")
    }
    private fun handleBackPressed() {
        Log.d("TrainingFragment", "handleBackPressed: called")
        val navController = findNavController()
        val previousEntry = navController.previousBackStackEntry
        Log.d("TrainingFragment", "handleBackPressed: currentDestination=${navController.currentDestination?.id}, previousDestination=${previousEntry?.destination?.id}")
        val currentState = viewModel.state.value
        Log.d("TrainingFragment", "handleBackPressed: isSessionComplete=${currentState.isSessionComplete}")
        try {
            if (!currentState.isSessionComplete) {
                Log.d("TrainingFragment", "handleBackPressed: session in progress, stopping session")
                viewModel.stopSession()
            }
            Log.d("TrainingFragment", "handleBackPressed: navigating back")
            val navigated = navController.navigateUp()
            Log.d("TrainingFragment", "handleBackPressed: navigateUp returned $navigated")
            if (!navigated) {
                Log.w("TrainingFragment", "handleBackPressed: navigateUp failed, finishing activity")
                requireActivity().finish()
            }
        } catch (e: Exception) {
            Log.e("TrainingFragment", "handleBackPressed: error navigating", e)
            requireActivity().finish()
        }
    }
    private fun renderState(state: TrainingState) {
        Log.d("TrainingFragment", "renderState: currentMedia=${state.currentMedia?.path}, isDisplayingMedia=$isDisplayingMedia, showFeedback=${state.showFeedback}")
        if (state.isSessionComplete) {
            hideFeedback()
            binding.sessionCompleteOverlay.visibility = View.VISIBLE
            binding.sessionCompleteText.visibility = View.VISIBLE
            binding.statsText.visibility = View.VISIBLE
            val scoreText = "Score: ${state.correctResponses}/${state.totalResponses}"
            val reactionTimeText = state.averageReactionTimeMs?.let { avgTime ->
                val seconds = avgTime / 1000.0
                String.format("\nAvg Reaction Time: %.2fs", seconds)
            } ?: ""
            binding.statsText.text = scoreText + reactionTimeText
        } else {
            binding.sessionCompleteOverlay.visibility = View.GONE
            binding.sessionCompleteText.visibility = View.GONE
            binding.statsText.visibility = View.GONE
            if (state.showFeedback) {
                showFeedback(state.lastResult)
            } else {
                hideFeedback()
            }
        }
        if (state.currentMedia != null && !isDisplayingMedia) {
            val currentTag = binding.mediaContainer.tag as? com.targetdiscriminator.domain.model.MediaItem
            val currentPath = currentTag?.path
            val newPath = state.currentMedia.path
            Log.d("TrainingFragment", "renderState: comparing paths - currentTag=$currentPath, newPath=$newPath")
            if (currentPath != newPath) {
                Log.d("TrainingFragment", "renderState: paths differ, calling displayMedia")
                displayMedia(state.currentMedia)
                binding.mediaContainer.tag = state.currentMedia
            } else {
                Log.d("TrainingFragment", "renderState: paths match, skipping displayMedia")
            }
        } else {
            if (state.currentMedia == null) {
                Log.d("TrainingFragment", "renderState: currentMedia is null, skipping")
            }
            if (isDisplayingMedia) {
                Log.d("TrainingFragment", "renderState: isDisplayingMedia=true, skipping")
            }
        }
        binding.timerText.text = formatTime(state.timeRemainingSeconds)
    }
    private fun displayMedia(mediaItem: com.targetdiscriminator.domain.model.MediaItem) {
        Log.d("TrainingFragment", "displayMedia: called for ${mediaItem.path}, isDisplayingMedia=$isDisplayingMedia")
        if (isDisplayingMedia) {
            Log.w("TrainingFragment", "displayMedia: already displaying, returning early")
            return
        }
        hideFeedback()
        isDisplayingMedia = true
        Log.d("TrainingFragment", "displayMedia: setting isDisplayingMedia=true, releasing player and removing views")
        releasePlayer()
        binding.mediaContainer.removeAllViews()
        when (mediaItem.type) {
            MediaType.VIDEO -> {
                Log.d("TrainingFragment", "displayMedia: displaying video ${mediaItem.path}")
                displayVideo(mediaItem)
            }
            MediaType.PHOTO -> {
                Log.d("TrainingFragment", "displayMedia: displaying photo ${mediaItem.path}")
                displayPhoto(mediaItem)
            }
        }
        binding.mediaContainer.post {
            Log.d("TrainingFragment", "displayMedia: post callback - resetting isDisplayingMedia=false")
            isDisplayingMedia = false
        }
    }
    @OptIn(UnstableApi::class) private fun displayVideo(mediaItem: com.targetdiscriminator.domain.model.MediaItem) {
        releasePlayer()
        val currentMediaPath = mediaItem.path
        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        exoPlayer?.volume = 0.0f
        val playerRef = exoPlayer
        var hasCompletedOneCycle = false
        playerRef?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val currentPlayer = exoPlayer
                if (playbackState == Player.STATE_ENDED && !hasCompletedOneCycle && currentPlayer == playerRef) {
                    hasCompletedOneCycle = true
                    val currentState = viewModel.state.value
                    if (currentState.currentMedia?.path == currentMediaPath && !currentState.hasResponded) {
                        viewModel.handleEvent(TrainingEvent.OnVideoCompleted)
                        val updatedState = viewModel.state.value
                        if (updatedState.currentMedia?.path == currentMediaPath && 
                            !updatedState.hasResponded && 
                            currentPlayer == playerRef &&
                            currentPlayer != null) {
                            currentPlayer.seekTo(0)
                            currentPlayer.volume = 0.0f
                            currentPlayer.play()
                        }
                    }
                }
            }
        })
        val playerView = PlayerView(requireContext())
        playerView.player = exoPlayer
        playerView.useController = false
        playerView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        binding.mediaContainer.addView(playerView)
        val overlayView = View(requireContext())
        overlayView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        overlayView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        overlayView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        binding.mediaContainer.addView(overlayView)
        try {
            val cacheFile = File(requireContext().cacheDir, File(mediaItem.path).name)
            if (!cacheFile.exists()) {
                requireContext().assets.open(mediaItem.path).use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            val fileDataSourceFactory = DataSource.Factory { FileDataSource() }
            val dataSourceFactory = DefaultDataSource.Factory(requireContext(), fileDataSourceFactory)
            val uri = android.net.Uri.fromFile(cacheFile)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(ExoMediaItem.fromUri(uri))
            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
            exoPlayer?.play()
            exoPlayer?.repeatMode = Player.REPEAT_MODE_OFF
        } catch (e: Exception) {
        }
    }
    private fun displayPhoto(mediaItem: com.targetdiscriminator.domain.model.MediaItem) {
        releasePlayer()
        val imageView = ImageView(requireContext())
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        binding.mediaContainer.addView(imageView)
        val overlayView = View(requireContext())
        overlayView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        overlayView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        overlayView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        binding.mediaContainer.addView(overlayView)
        try {
            val inputStream = requireContext().assets.open(mediaItem.path)
            val drawable = android.graphics.drawable.BitmapDrawable(resources, inputStream)
            imageView.setImageDrawable(drawable)
        } catch (e: Exception) {
        }
    }
    private fun showFeedback(result: com.targetdiscriminator.domain.model.ResponseResult?) {
        if (result == null) {
            Log.d("TrainingFragment", "showFeedback: result is null, returning")
            return
        }
        if (feedbackCallbackScheduled) {
            Log.w("TrainingFragment", "showFeedback: callback already scheduled, skipping")
            return
        }
        Log.d("TrainingFragment", "showFeedback: showing feedback for result isCorrect=${result.isCorrect}")
        feedbackCallbackScheduled = true
        binding.feedbackOverlay.visibility = View.VISIBLE
        binding.feedbackText.visibility = View.VISIBLE
        val feedbackColor = if (result.isCorrect) {
            binding.feedbackText.text = getString(R.string.feedback_correct)
            resources.getColor(R.color.green_correct, null)
        } else {
            binding.feedbackText.text = getString(R.string.feedback_incorrect)
            resources.getColor(R.color.red_incorrect, null)
        }
        binding.feedbackOverlay.background = createVignetteDrawable(feedbackColor)
        Log.d("TrainingFragment", "showFeedback: scheduling OnFeedbackShown event in 1500ms")
        val runnable = Runnable {
            Log.d("TrainingFragment", "showFeedback: delayed callback fired, calling OnFeedbackShown")
            feedbackCallbackScheduled = false
            feedbackCallbackRunnable = null
            viewModel.handleEvent(TrainingEvent.OnFeedbackShown)
        }
        feedbackCallbackRunnable = runnable
        binding.feedbackOverlay.postDelayed(runnable, 1500)
    }
    private fun createVignetteDrawable(color: Int): android.graphics.drawable.Drawable {
        return VignetteDrawable(color)
    }
    private class VignetteDrawable(private val color: Int) : android.graphics.drawable.Drawable() {
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        private var gradient: android.graphics.RadialGradient? = null
        override fun onBoundsChange(bounds: android.graphics.Rect) {
            super.onBoundsChange(bounds)
            val centerX = bounds.centerX().toFloat()
            val centerY = bounds.centerY().toFloat()
            val radius = kotlin.math.max(bounds.width(), bounds.height()) * 0.7f
            val colors = intArrayOf(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.argb(
                    (255 * 0.2).toInt(),
                    android.graphics.Color.red(color),
                    android.graphics.Color.green(color),
                    android.graphics.Color.blue(color)
                ),
                android.graphics.Color.argb(
                    (255 * 0.5).toInt(),
                    android.graphics.Color.red(color),
                    android.graphics.Color.green(color),
                    android.graphics.Color.blue(color)
                ),
                android.graphics.Color.argb(
                    (255 * 0.85).toInt(),
                    android.graphics.Color.red(color),
                    android.graphics.Color.green(color),
                    android.graphics.Color.blue(color)
                ),
                color
            )
            val positions = floatArrayOf(0.0f, 0.3f, 0.5f, 0.7f, 1.0f)
            gradient = android.graphics.RadialGradient(
                centerX,
                centerY,
                radius,
                colors,
                positions,
                android.graphics.Shader.TileMode.CLAMP
            )
            paint.shader = gradient
        }
        override fun draw(canvas: android.graphics.Canvas) {
            canvas.drawRect(bounds, paint)
        }
        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
            paint.colorFilter = colorFilter
        }
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int {
            return android.graphics.PixelFormat.TRANSLUCENT
        }
    }
    private fun hideFeedback() {
        feedbackCallbackRunnable?.let { runnable ->
            binding.feedbackOverlay.removeCallbacks(runnable)
            feedbackCallbackRunnable = null
        }
        binding.feedbackOverlay.visibility = View.GONE
        binding.feedbackText.visibility = View.GONE
        feedbackCallbackScheduled = false
    }
    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }
    private fun handleEffect(effect: TrainingEffect) {
        when (effect) {
            is TrainingEffect.NavigateToConfig -> {
                findNavController().popBackStack()
            }
        }
    }
    private fun showTapIndicator(x: Float, y: Float) {
        val indicatorSize = (48 * resources.displayMetrics.density).toInt()
        val indicatorView = View(requireContext())
        indicatorView.layoutParams = ViewGroup.LayoutParams(
            indicatorSize,
            indicatorSize
        )
        indicatorView.x = x - indicatorSize / 2f
        indicatorView.y = y - indicatorSize / 2f
        indicatorView.background = createTapIndicatorDrawable()
        indicatorView.alpha = 1.0f
        binding.mediaContainer.addView(indicatorView)
        indicatorView.animate()
            .alpha(0f)
            .scaleX(2.0f)
            .scaleY(2.0f)
            .setDuration(1000)
            .withEndAction {
                binding.mediaContainer.removeView(indicatorView)
            }
            .start()
    }
    private fun createTapIndicatorDrawable(): android.graphics.drawable.Drawable {
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
        drawable.setColor(android.graphics.Color.TRANSPARENT)
        val strokeWidth = (4 * resources.displayMetrics.density).toInt()
        drawable.setStroke(
            strokeWidth,
            resources.getColor(R.color.white, null)
        )
        return drawable
    }
    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("TrainingFragment", "onDestroyView: called")
        releasePlayer()
        if (::onBackPressedCallback.isInitialized) {
            Log.d("TrainingFragment", "onDestroyView: removing callback, enabled=${onBackPressedCallback.isEnabled}")
            onBackPressedCallback.remove()
        }
        _binding = null
    }
}

