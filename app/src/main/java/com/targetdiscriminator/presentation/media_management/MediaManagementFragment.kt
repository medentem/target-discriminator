package com.targetdiscriminator.presentation.media_management

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.targetdiscriminator.R
import com.targetdiscriminator.databinding.FragmentMediaManagementBinding
import com.targetdiscriminator.domain.model.MediaType
import com.targetdiscriminator.domain.model.ThreatType
import kotlinx.coroutines.launch

class MediaManagementFragment : Fragment() {
    private var _binding: FragmentMediaManagementBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MediaManagementViewModel by viewModels {
        MediaManagementViewModelFactory(requireContext())
    }
    private lateinit var mediaAdapter: MediaAdapter
    private var pendingImportUri: android.net.Uri? = null
    private var pendingImportMediaType: MediaType? = null
    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingImportUri = it
            pendingImportMediaType = MediaType.PHOTO
            showThreatTypeSelectionDialog(MediaType.PHOTO, it)
        }
    }
    private val videoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            pendingImportUri = it
            pendingImportMediaType = MediaType.VIDEO
            showThreatTypeSelectionDialog(MediaType.VIDEO, it)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaManagementBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        setupObservers()
    }
    private fun setupRecyclerView() {
        mediaAdapter = MediaAdapter { mediaItem ->
            showDeleteConfirmationDialog(mediaItem)
        }
        binding.mediaRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.mediaRecyclerView.adapter = mediaAdapter
    }
    private fun setupListeners() {
        binding.importPhotoButton.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }
        binding.importVideoButton.setOnClickListener {
            videoPickerLauncher.launch("video/*")
        }
        binding.mediaTypeAllChip.setOnClickListener {
            if (!binding.mediaTypeAllChip.isChecked) {
                binding.mediaTypeAllChip.isChecked = true
                viewModel.handleEvent(MediaManagementEvent.SelectMediaTypeFilter(MediaTypeFilter.ALL))
            }
        }
        binding.mediaTypePhotosChip.setOnClickListener {
            if (!binding.mediaTypePhotosChip.isChecked) {
                binding.mediaTypePhotosChip.isChecked = true
                viewModel.handleEvent(MediaManagementEvent.SelectMediaTypeFilter(MediaTypeFilter.PHOTOS))
            }
        }
        binding.mediaTypeVideosChip.setOnClickListener {
            if (!binding.mediaTypeVideosChip.isChecked) {
                binding.mediaTypeVideosChip.isChecked = true
                viewModel.handleEvent(MediaManagementEvent.SelectMediaTypeFilter(MediaTypeFilter.VIDEOS))
            }
        }
        binding.threatTypeAllChip.setOnClickListener {
            if (!binding.threatTypeAllChip.isChecked) {
                binding.threatTypeAllChip.isChecked = true
                viewModel.handleEvent(MediaManagementEvent.SelectThreatTypeFilter(ThreatTypeFilter.ALL))
            }
        }
        binding.threatTypeThreatChip.setOnClickListener {
            if (!binding.threatTypeThreatChip.isChecked) {
                binding.threatTypeThreatChip.isChecked = true
                viewModel.handleEvent(MediaManagementEvent.SelectThreatTypeFilter(ThreatTypeFilter.THREAT))
            }
        }
        binding.threatTypeNonThreatChip.setOnClickListener {
            if (!binding.threatTypeNonThreatChip.isChecked) {
                binding.threatTypeNonThreatChip.isChecked = true
                viewModel.handleEvent(MediaManagementEvent.SelectThreatTypeFilter(ThreatTypeFilter.NON_THREAT))
            }
        }
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
    private fun renderState(state: MediaManagementState) {
        binding.loadingProgressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        if (state.errorMessage != null) {
            showErrorDialog(state.errorMessage)
            viewModel.handleEvent(MediaManagementEvent.DismissError)
        }
        updateChipSelection(state)
        val filteredItems = state.filteredMediaItems
        mediaAdapter.submitList(filteredItems)
        if (filteredItems.isEmpty() && !state.isLoading) {
            binding.emptyText.visibility = View.VISIBLE
            binding.mediaRecyclerView.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.mediaRecyclerView.visibility = View.VISIBLE
        }
    }
    private fun updateChipSelection(state: MediaManagementState) {
        binding.mediaTypeAllChip.isChecked = state.selectedMediaType == MediaTypeFilter.ALL
        binding.mediaTypePhotosChip.isChecked = state.selectedMediaType == MediaTypeFilter.PHOTOS
        binding.mediaTypeVideosChip.isChecked = state.selectedMediaType == MediaTypeFilter.VIDEOS
        binding.threatTypeAllChip.isChecked = state.selectedThreatType == ThreatTypeFilter.ALL
        binding.threatTypeThreatChip.isChecked = state.selectedThreatType == ThreatTypeFilter.THREAT
        binding.threatTypeNonThreatChip.isChecked = state.selectedThreatType == ThreatTypeFilter.NON_THREAT
    }
    private fun handleEffect(effect: MediaManagementEffect) {
        when (effect) {
            is MediaManagementEffect.ShowError -> {
                showErrorDialog(effect.message)
            }
            is MediaManagementEffect.RequestMediaImport -> {
                when (effect.mediaType) {
                    MediaType.PHOTO -> photoPickerLauncher.launch("image/*")
                    MediaType.VIDEO -> videoPickerLauncher.launch("video/*")
                }
            }
            is MediaManagementEffect.RequestThreatTypeSelection -> {
                showThreatTypeSelectionDialog(effect.mediaType, effect.uri)
            }
        }
    }
    private fun showThreatTypeSelectionDialog(mediaType: MediaType, uri: android.net.Uri) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.media_management_select_threat_type)
            .setPositiveButton(R.string.media_management_threat) { _, _ ->
                viewModel.handleEvent(MediaManagementEvent.ImportMedia(uri, mediaType, ThreatType.THREAT))
            }
            .setNegativeButton(R.string.media_management_non_threat) { _, _ ->
                viewModel.handleEvent(MediaManagementEvent.ImportMedia(uri, mediaType, ThreatType.NON_THREAT))
            }
            .setNeutralButton(R.string.media_management_cancel) { _, _ ->
                pendingImportUri = null
                pendingImportMediaType = null
            }
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(R.color.white, null))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(resources.getColor(R.color.white, null))
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(resources.getColor(R.color.white, null))
    }
    private fun showDeleteConfirmationDialog(mediaItem: com.targetdiscriminator.domain.model.MediaItem) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete Media")
            .setMessage("Are you sure you want to delete this media item?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.handleEvent(MediaManagementEvent.DeleteMedia(mediaItem))
            }
            .setNegativeButton("Cancel", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(R.color.white, null))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(resources.getColor(R.color.white, null))
    }
    private fun showErrorDialog(message: String) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(R.color.white, null))
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

