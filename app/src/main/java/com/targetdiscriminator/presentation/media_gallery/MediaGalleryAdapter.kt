package com.targetdiscriminator.presentation.media_gallery

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.targetdiscriminator.databinding.ItemMediaGalleryBinding
import com.targetdiscriminator.domain.model.MediaType
import com.targetdiscriminator.domain.model.ThreatType
import com.targetdiscriminator.domain.model.ThreatLabels
import java.io.File
import java.io.InputStream

class MediaGalleryAdapter(
    private val onItemClick: (MediaItemWithOverride, Int) -> Unit,
    private val onItemLongClick: (MediaItemWithOverride) -> Unit,
    private val onSelectionToggle: (String) -> Unit,
    private var threatLabels: ThreatLabels = ThreatLabels("Threat", "Non-Threat")
) : ListAdapter<MediaItemWithOverride, MediaGalleryAdapter.MediaViewHolder>(MediaDiffCallback()) {
    
    fun updateThreatLabels(labels: ThreatLabels) {
        threatLabels = labels
        notifyDataSetChanged()
    }

    var isSelectionMode = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedPaths = emptySet<String>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaGalleryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class MediaViewHolder(private val binding: ItemMediaGalleryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItemWithOverride, position: Int) {
            val mediaItem = item.mediaItem
            val isSelected = selectedPaths.contains(mediaItem.path)

            // Set selection checkbox
            binding.selectionCheckbox.visibility = if (isSelectionMode) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
            binding.selectionCheckbox.isChecked = isSelected
            binding.selectionCheckbox.setOnClickListener {
                onSelectionToggle(mediaItem.path)
            }

            // Set excluded badge
            binding.excludedBadge.visibility = if (item.isExcluded) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            // Set threat type badge
            binding.threatTypeBadge.text = when (item.displayThreatType) {
                ThreatType.THREAT -> threatLabels.threat.uppercase()
                ThreatType.NON_THREAT -> threatLabels.nonThreat.uppercase()
            }
            binding.threatTypeBadge.setBackgroundColor(
                if (item.displayThreatType == ThreatType.THREAT) {
                    binding.root.context.getColor(android.R.color.holo_red_dark)
                } else {
                    binding.root.context.getColor(android.R.color.holo_green_dark)
                }
            )

            // Set overridden badge
            binding.overriddenBadge.visibility = if (item.override?.threatTypeOverride != null) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            // Set media type
            binding.mediaTypeText.text = when (mediaItem.type) {
                MediaType.PHOTO -> "PHOTO"
                MediaType.VIDEO -> "VIDEO"
            }

            // Set source
            binding.sourceText.text = if (mediaItem.path.startsWith("assets://")) {
                "Built-in"
            } else {
                "User"
            }

            // Set opacity for excluded items
            binding.root.alpha = if (item.isExcluded) 0.5f else 1.0f

            // Load thumbnail
            loadThumbnail(mediaItem)

            // Set click listeners
            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    onSelectionToggle(mediaItem.path)
                } else {
                    onItemClick(item, position)
                }
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }

        private fun loadThumbnail(mediaItem: com.targetdiscriminator.domain.model.MediaItem) {
            try {
                clearPreviousBitmap()
                when (mediaItem.type) {
                    MediaType.PHOTO -> {
                        if (mediaItem.path.startsWith("assets://")) {
                            loadAssetThumbnail(mediaItem.path)
                        } else {
                            val file = File(mediaItem.path)
                            if (file.exists()) {
                                val bitmap = decodeBitmapWithOrientation(file.absolutePath)
                                if (bitmap != null && !bitmap.isRecycled) {
                                    // Scale bitmap to thumbnail size while preserving aspect ratio
                                    val scaledBitmap = scaleBitmapToThumbnail(bitmap)
                                    bitmap.recycle()
                                    binding.mediaThumbnail.setImageBitmap(scaledBitmap)
                                }
                            }
                        }
                    }
                    MediaType.VIDEO -> {
                        if (mediaItem.path.startsWith("assets://")) {
                            loadAssetVideoThumbnail(mediaItem.path)
                        } else {
                            val file = File(mediaItem.path)
                            if (file.exists()) {
                                loadVideoThumbnail(file.absolutePath)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaGalleryAdapter", "Error loading thumbnail for ${mediaItem.path}", e)
                binding.mediaThumbnail.setImageBitmap(null)
            }
        }

        private fun loadAssetThumbnail(assetPath: String) {
            try {
                val path = assetPath.removePrefix("assets://")
                val inputStream: InputStream? = binding.root.context.assets.open(path)
                inputStream?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = false
                        inSampleSize = 4 // Reduce size for thumbnails
                    }
                    val bitmap = BitmapFactory.decodeStream(stream, null, options)
                    if (bitmap != null && !bitmap.isRecycled) {
                        // Scale bitmap to thumbnail size while preserving aspect ratio
                        val scaledBitmap = scaleBitmapToThumbnail(bitmap)
                        bitmap.recycle()
                        binding.mediaThumbnail.setImageBitmap(scaledBitmap)
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaGalleryAdapter", "Error loading asset thumbnail: $assetPath", e)
            }
        }

        private fun scaleBitmapToThumbnail(bitmap: Bitmap): Bitmap {
            // Target size for thumbnails - scale to a reasonable size for display
            // The ImageView will use centerCrop to display in the 16:9 container
            val maxWidth = 320
            val maxHeight = 180
            
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height
            
            // Only scale down if the bitmap is larger than max dimensions
            // Maintain aspect ratio when scaling
            return if (originalWidth > maxWidth || originalHeight > maxHeight) {
                val scale = minOf(
                    maxWidth.toFloat() / originalWidth,
                    maxHeight.toFloat() / originalHeight
                )
                val scaledWidth = (originalWidth * scale).toInt().coerceAtLeast(1)
                val scaledHeight = (originalHeight * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            } else {
                // Bitmap is already small enough, create a copy to avoid recycling issues
                // The ImageView will hold a reference to this copy
                Bitmap.createBitmap(bitmap)
            }
        }

        private fun loadVideoThumbnail(filePath: String) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)
                val thumbnail = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (thumbnail != null && !thumbnail.isRecycled) {
                    // Scale thumbnail while maintaining aspect ratio
                    val scaledThumbnail = scaleBitmapToThumbnail(thumbnail)
                    thumbnail.recycle()
                    binding.mediaThumbnail.setImageBitmap(scaledThumbnail)
                } else {
                    // Fallback to ThumbnailUtils if MediaMetadataRetriever fails
                    val fallbackThumbnail = ThumbnailUtils.createVideoThumbnail(
                        filePath,
                        MediaStore.Video.Thumbnails.MINI_KIND
                    )
                    if (fallbackThumbnail != null && !fallbackThumbnail.isRecycled) {
                        val scaledThumbnail = scaleBitmapToThumbnail(fallbackThumbnail)
                        fallbackThumbnail.recycle()
                        binding.mediaThumbnail.setImageBitmap(scaledThumbnail)
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaGalleryAdapter", "Error loading video thumbnail: $filePath", e)
                // Try fallback
                try {
                    val fallbackThumbnail = ThumbnailUtils.createVideoThumbnail(
                        filePath,
                        MediaStore.Video.Thumbnails.MINI_KIND
                    )
                    if (fallbackThumbnail != null && !fallbackThumbnail.isRecycled) {
                        val scaledThumbnail = scaleBitmapToThumbnail(fallbackThumbnail)
                        fallbackThumbnail.recycle()
                        binding.mediaThumbnail.setImageBitmap(scaledThumbnail)
                    }
                } catch (e2: Exception) {
                    Log.e("MediaGalleryAdapter", "Fallback thumbnail also failed: $filePath", e2)
                }
            } finally {
                retriever?.release()
            }
        }

        private fun loadAssetVideoThumbnail(assetPath: String) {
            var retriever: MediaMetadataRetriever? = null
            try {
                val path = assetPath.removePrefix("assets://")
                val assetFileDescriptor = binding.root.context.assets.openFd(path)
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
                )
                val thumbnail = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (thumbnail != null && !thumbnail.isRecycled) {
                    // Scale thumbnail while maintaining aspect ratio
                    val scaledThumbnail = scaleBitmapToThumbnail(thumbnail)
                    thumbnail.recycle()
                    binding.mediaThumbnail.setImageBitmap(scaledThumbnail)
                }
                assetFileDescriptor.close()
            } catch (e: Exception) {
                Log.e("MediaGalleryAdapter", "Error loading asset video thumbnail: $assetPath", e)
            } finally {
                retriever?.release()
            }
        }

        private fun clearPreviousBitmap() {
            binding.mediaThumbnail.setImageDrawable(null)
            binding.mediaThumbnail.setImageBitmap(null)
        }

        private fun decodeBitmapWithOrientation(filePath: String): Bitmap? {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = calculateInSampleSize(filePath)
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
                Log.e("MediaGalleryAdapter", "Error processing orientation for $filePath", e)
            }
            return bitmap
        }

        private fun calculateInSampleSize(filePath: String): Int {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            val reqWidth = 320
            val reqHeight = 180
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }

    private class MediaDiffCallback : DiffUtil.ItemCallback<MediaItemWithOverride>() {
        override fun areItemsTheSame(
            oldItem: MediaItemWithOverride,
            newItem: MediaItemWithOverride
        ): Boolean {
            return oldItem.mediaItem.path == newItem.mediaItem.path
        }

        override fun areContentsTheSame(
            oldItem: MediaItemWithOverride,
            newItem: MediaItemWithOverride
        ): Boolean {
            return oldItem == newItem
        }
    }
}
