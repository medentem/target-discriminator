package com.targetdiscriminator.presentation.media_management

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.targetdiscriminator.databinding.ItemMediaBinding
import com.targetdiscriminator.domain.model.MediaItem
import com.targetdiscriminator.domain.model.MediaType
import com.targetdiscriminator.domain.model.ThreatType
import java.io.File

class MediaAdapter(
    private val onDeleteClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, MediaAdapter.MediaViewHolder>(MediaDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }
    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    inner class MediaViewHolder(private val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mediaItem: MediaItem) {
            binding.mediaTypeText.text = when (mediaItem.type) {
                MediaType.PHOTO -> "Photo"
                MediaType.VIDEO -> "Video"
            }
            binding.threatTypeText.text = when (mediaItem.threatType) {
                ThreatType.THREAT -> "Threat"
                ThreatType.NON_THREAT -> "Non-Threat"
            }
            loadThumbnail(mediaItem)
            binding.deleteButton.setOnClickListener {
                onDeleteClick(mediaItem)
            }
        }
        private fun loadThumbnail(mediaItem: MediaItem) {
            try {
                Log.d("MediaAdapter", "loadThumbnail: binding thumbnail for ${mediaItem.path}, adapterPosition=$adapterPosition")
                clearPreviousBitmap()
                when (mediaItem.type) {
                    MediaType.PHOTO -> {
                        val file = File(mediaItem.path)
                        if (file.exists()) {
                            val bitmap = decodeBitmapWithOrientation(file.absolutePath)
                            if (bitmap != null && !bitmap.isRecycled) {
                                Log.d("MediaAdapter", "loadThumbnail: setting photo bitmap, isRecycled=${bitmap.isRecycled}")
                                binding.mediaThumbnail.setImageBitmap(bitmap)
                            } else {
                                Log.w("MediaAdapter", "loadThumbnail: bitmap is null or recycled for ${mediaItem.path}")
                                binding.mediaThumbnail.setImageBitmap(null)
                            }
                        } else {
                            binding.mediaThumbnail.setImageBitmap(null)
                        }
                    }
                    MediaType.VIDEO -> {
                        val file = File(mediaItem.path)
                        if (file.exists()) {
                            val thumbnail = android.media.ThumbnailUtils.createVideoThumbnail(
                                file.absolutePath,
                                MediaStore.Video.Thumbnails.MINI_KIND
                            )
                            if (thumbnail != null && !thumbnail.isRecycled) {
                                Log.d("MediaAdapter", "loadThumbnail: setting video thumbnail, isRecycled=${thumbnail.isRecycled}")
                                binding.mediaThumbnail.setImageBitmap(thumbnail)
                            } else {
                                Log.w("MediaAdapter", "loadThumbnail: video thumbnail is null or recycled for ${mediaItem.path}")
                                binding.mediaThumbnail.setImageBitmap(null)
                            }
                        } else {
                            binding.mediaThumbnail.setImageBitmap(null)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaAdapter", "loadThumbnail: error loading thumbnail for ${mediaItem.path}", e)
                binding.mediaThumbnail.setImageBitmap(null)
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
            Log.d("MediaAdapter", "decodeBitmapWithOrientation: decoded bitmap for $filePath, isRecycled=${bitmap.isRecycled}")
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
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.width,
                        bitmap.height,
                        matrix,
                        true
                    )
                    Log.d("MediaAdapter", "decodeBitmapWithOrientation: created rotated bitmap")
                    bitmap = rotatedBitmap
                    Log.d("MediaAdapter", "decodeBitmapWithOrientation: rotated bitmap isRecycled=${bitmap.isRecycled}")
                }
            } catch (e: Exception) {
                Log.e("MediaAdapter", "decodeBitmapWithOrientation: error processing orientation for $filePath", e)
            }
            return bitmap
        }
        private fun calculateInSampleSize(filePath: String): Int {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            val reqWidth = 160
            val reqHeight = 160
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
    private class MediaDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.path == newItem.path
        }
        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }
}

