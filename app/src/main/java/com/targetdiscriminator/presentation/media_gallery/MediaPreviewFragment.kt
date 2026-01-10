package com.targetdiscriminator.presentation.media_gallery

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.navArgs
import com.targetdiscriminator.databinding.FragmentMediaPreviewBinding
import com.targetdiscriminator.domain.model.MediaItem
import com.targetdiscriminator.domain.model.MediaType
import com.targetdiscriminator.domain.model.ThreatType
import com.targetdiscriminator.data.provider.ThreatLabelProvider
import com.targetdiscriminator.data.repository.MediaRepository
import com.targetdiscriminator.data.repository.MediaOverrideRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class MediaPreviewFragment : Fragment() {
    private var _binding: FragmentMediaPreviewBinding? = null
    private val binding get() = _binding!!
    private val args: MediaPreviewFragmentArgs by navArgs()
    private var exoPlayer: ExoPlayer? = null
    private lateinit var mediaRepository: MediaRepository
    private lateinit var overrideRepository: MediaOverrideRepository
    private val threatLabelProvider by lazy { ThreatLabelProvider(requireContext()) }
    private var allMediaItems: List<MediaItem> = emptyList()
    private var currentIndex: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mediaRepository = MediaRepository(requireContext())
        overrideRepository = MediaOverrideRepository(requireContext())
        setupPlayer()
        setupListeners()
        loadAllMedia()
        observeThreatLabels()
    }
    
    override fun onResume() {
        super.onResume()
        // Reload threat labels in case they changed
        viewLifecycleOwner.lifecycleScope.launch {
            threatLabelProvider.loadConfig()
        }
    }
    
    private fun observeThreatLabels() {
        viewLifecycleOwner.lifecycleScope.launch {
            threatLabelProvider.labels.collect { labels ->
                updateButtonLabels(labels)
            }
        }
    }
    
    private fun updateButtonLabels(labels: com.targetdiscriminator.domain.model.ThreatLabels) {
        binding.threatButton.text = labels.threat.uppercase()
        binding.nonThreatButton.text = labels.nonThreat.uppercase()
    }

    private fun loadAllMedia() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                allMediaItems = withContext(Dispatchers.IO) {
                    mediaRepository.getAllMediaItemsForGallery()
                }
                // Find index by path if currentIndex is invalid
                currentIndex = if (args.currentIndex >= 0 && args.currentIndex < allMediaItems.size) {
                    args.currentIndex
                } else {
                    allMediaItems.indexOfFirst { it.path == args.mediaPath }.takeIf { it >= 0 } ?: 0
                }
                loadCurrentMedia()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupPlayer() {
        exoPlayer = ExoPlayer.Builder(requireContext()).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
            volume = 0f // Muted
        }
        binding.videoView.player = exoPlayer
    }

    private fun setupListeners() {
        binding.closeButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.previousButton.setOnClickListener {
            navigateToPrevious()
        }
        binding.nextButton.setOnClickListener {
            navigateToNext()
        }
        binding.threatButton.setOnClickListener {
            setThreatType(ThreatType.THREAT)
        }
        binding.nonThreatButton.setOnClickListener {
            setThreatType(ThreatType.NON_THREAT)
        }
        binding.excludeIncludeButton.setOnClickListener {
            toggleExcludeInclude()
        }
    }

    private fun loadCurrentMedia() {
        if (currentIndex < 0 || currentIndex >= allMediaItems.size) return

        val mediaItem = allMediaItems[currentIndex]
        updateMediaInfo(currentIndex, allMediaItems.size)

        when (mediaItem.type) {
            MediaType.PHOTO -> {
                binding.videoView.visibility = View.GONE
                binding.imageView.visibility = View.VISIBLE
                loadImage(mediaItem)
            }
            MediaType.VIDEO -> {
                binding.imageView.visibility = View.GONE
                binding.videoView.visibility = View.VISIBLE
                loadVideo(mediaItem)
            }
        }

        // Update navigation buttons
        binding.previousButton.visibility = if (currentIndex > 0) View.VISIBLE else View.GONE
        binding.nextButton.visibility = if (currentIndex < allMediaItems.size - 1) View.VISIBLE else View.GONE

        // Load and update override info
        loadOverrideInfo(mediaItem)
    }

    private fun loadOverrideInfo(mediaItem: MediaItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val override = withContext(Dispatchers.IO) {
                    overrideRepository.getOverride(mediaItem.path)
                }
                val displayThreatType = override?.threatTypeOverride ?: mediaItem.threatType
                val isExcluded = override?.isExcluded ?: false

                updateActionBar(displayThreatType, isExcluded)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateActionBar(displayThreatType: ThreatType, isExcluded: Boolean) {
        // Update status text
        binding.statusText.text = if (isExcluded) {
            "Status: Excluded"
        } else {
            "Status: Included"
        }

        // Update threat type buttons - use backgroundTint for Material buttons
        if (displayThreatType == ThreatType.THREAT) {
            binding.threatButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(android.R.color.holo_red_dark)
            )
            binding.threatButton.setTextColor(
                requireContext().getColor(android.R.color.white)
            )
            binding.nonThreatButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(android.R.color.transparent)
            )
            binding.nonThreatButton.setTextColor(
                requireContext().getColor(android.R.color.white)
            )
        } else {
            binding.nonThreatButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(android.R.color.holo_green_dark)
            )
            binding.nonThreatButton.setTextColor(
                requireContext().getColor(android.R.color.white)
            )
            binding.threatButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(android.R.color.transparent)
            )
            binding.threatButton.setTextColor(
                requireContext().getColor(android.R.color.white)
            )
        }

        // Update exclude/include button
        if (isExcluded) {
            binding.excludeIncludeButton.text = "Include"
            binding.excludeIncludeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(android.R.color.holo_green_dark)
            )
        } else {
            binding.excludeIncludeButton.text = "Exclude"
            binding.excludeIncludeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(android.R.color.holo_red_dark)
            )
        }
    }

    private fun setThreatType(threatType: ThreatType) {
        if (currentIndex < 0 || currentIndex >= allMediaItems.size) return
        val mediaItem = allMediaItems[currentIndex]

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    overrideRepository.setThreatTypeOverride(mediaItem.path, threatType)
                }
                loadOverrideInfo(mediaItem)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun toggleExcludeInclude() {
        if (currentIndex < 0 || currentIndex >= allMediaItems.size) return
        val mediaItem = allMediaItems[currentIndex]

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val override = withContext(Dispatchers.IO) {
                    overrideRepository.getOverride(mediaItem.path)
                }
                val currentExcluded = override?.isExcluded ?: false
                withContext(Dispatchers.IO) {
                    overrideRepository.setExcluded(mediaItem.path, !currentExcluded)
                }
                loadOverrideInfo(mediaItem)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadImage(mediaItem: MediaItem) {
        binding.loadingProgressBar.visibility = View.VISIBLE
        try {
            val bitmap = if (mediaItem.path.startsWith("assets://")) {
                loadAssetImage(mediaItem.path)
            } else {
                val file = File(mediaItem.path)
                if (file.exists()) {
                    decodeBitmapWithOrientation(file.absolutePath)
                } else {
                    null
                }
            }
            if (bitmap != null) {
                binding.imageView.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            binding.loadingProgressBar.visibility = View.GONE
        }
    }

    private fun loadVideo(mediaItem: MediaItem) {
        binding.loadingProgressBar.visibility = View.VISIBLE
        try {
            val uri = if (mediaItem.path.startsWith("assets://")) {
                // For assets, we need to use AssetDataSource
                // This is a simplified version - in production, use proper asset URI handling
                android.net.Uri.parse("file:///android_asset/${mediaItem.path.removePrefix("assets://")}")
            } else {
                android.net.Uri.fromFile(File(mediaItem.path))
            }
            exoPlayer?.setMediaItem(ExoMediaItem.fromUri(uri))
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            binding.loadingProgressBar.visibility = View.GONE
        }
    }

    private fun loadAssetImage(assetPath: String): Bitmap? {
        return try {
            val path = assetPath.removePrefix("assets://")
            val inputStream: InputStream? = requireContext().assets.open(path)
            inputStream?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeBitmapWithOrientation(filePath: String): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
        }
        var bitmap = BitmapFactory.decodeFile(filePath, options) ?: return null
        try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
            }
            if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                bitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )
            }
        } catch (e: Exception) {
            // Ignore orientation errors
        }
        return bitmap
    }

    private fun navigateToPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            loadCurrentMedia()
        }
    }

    private fun navigateToNext() {
        if (currentIndex < allMediaItems.size - 1) {
            currentIndex++
            loadCurrentMedia()
        }
    }

    private fun updateMediaInfo(currentIndex: Int, totalCount: Int) {
        binding.mediaInfoText.text = "${currentIndex + 1} / $totalCount"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        exoPlayer?.release()
        exoPlayer = null
        _binding = null
    }
}
