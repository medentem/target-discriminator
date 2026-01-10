package com.targetdiscriminator.presentation.media_gallery

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.targetdiscriminator.data.provider.ThreatLabelProvider
import com.targetdiscriminator.databinding.FragmentMediaGalleryBinding
import com.targetdiscriminator.domain.model.MediaItem
import com.targetdiscriminator.domain.model.ThreatType
import kotlinx.coroutines.launch

class MediaGalleryFragment : Fragment() {
    private var _binding: FragmentMediaGalleryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MediaGalleryViewModel by viewModels {
        MediaGalleryViewModelFactory(requireContext())
    }
    private lateinit var mediaAdapter: MediaGalleryAdapter
    private val threatLabelProvider by lazy { ThreatLabelProvider(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        setupObservers()
        observeThreatLabels()
    }
    
    private fun observeThreatLabels() {
        viewLifecycleOwner.lifecycleScope.launch {
            threatLabelProvider.labels.collect { labels ->
                mediaAdapter.updateThreatLabels(labels)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh media when returning to ensure overrides are up to date
        viewModel.handleEvent(MediaGalleryEvent.LoadMedia)
        // Reload threat labels in case they changed
        viewLifecycleOwner.lifecycleScope.launch {
            threatLabelProvider.loadConfig()
        }
    }

    private fun setupRecyclerView() {
        val spanCount = if (resources.configuration.screenWidthDp >= 600) 3 else 2
        binding.mediaRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        
        mediaAdapter = MediaGalleryAdapter(
            onItemClick = { item, position ->
                val filteredItems = viewModel.state.value.filteredMediaItems.map { it.mediaItem }
                viewModel.handleEvent(MediaGalleryEvent.ViewMedia(item.mediaItem, position))
            },
            onItemLongClick = { item ->
                showItemContextMenu(item)
            },
            onSelectionToggle = { path ->
                viewModel.handleEvent(MediaGalleryEvent.ToggleSelection(path))
            },
            threatLabels = threatLabelProvider.getCurrentLabels()
        )
        binding.mediaRecyclerView.adapter = mediaAdapter
    }

    private fun setupListeners() {
        // Clear all overrides button
        binding.clearAllOverridesButton.setOnClickListener {
            showClearAllOverridesConfirmation()
        }
        
        // Bulk actions
        binding.selectAllButton.setOnClickListener {
            viewModel.handleEvent(MediaGalleryEvent.SelectAll)
        }
        binding.excludeSelectedButton.setOnClickListener {
            val selected = viewModel.state.value.selectedPaths
            if (selected.isNotEmpty()) {
                showBulkExcludeConfirmation(selected)
            }
        }
        binding.includeSelectedButton.setOnClickListener {
            val selected = viewModel.state.value.selectedPaths
            if (selected.isNotEmpty()) {
                viewModel.handleEvent(MediaGalleryEvent.BulkInclude(selected))
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

    private fun renderState(state: MediaGalleryState) {
        binding.loadingProgressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        if (state.errorMessage != null) {
            showErrorDialog(state.errorMessage)
            viewModel.handleEvent(MediaGalleryEvent.DismissError)
        }

        val filteredItems = state.filteredMediaItems
        mediaAdapter.submitList(filteredItems)
        mediaAdapter.selectedPaths = state.selectedPaths
        mediaAdapter.isSelectionMode = state.selectedPaths.isNotEmpty()

        // Show/hide bulk actions
        binding.bulkActionsLayout.visibility = if (state.selectedPaths.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // Show/hide empty state
        if (filteredItems.isEmpty() && !state.isLoading) {
            binding.emptyText.visibility = View.VISIBLE
            binding.mediaRecyclerView.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.mediaRecyclerView.visibility = View.VISIBLE
        }
    }


    private fun handleEffect(effect: MediaGalleryEffect) {
        when (effect) {
            is MediaGalleryEffect.ShowError -> {
                showErrorDialog(effect.message)
            }
            is MediaGalleryEffect.NavigateToPreview -> {
                navigateToPreview(effect.mediaItems, effect.currentIndex)
            }
        }
    }

    private fun showItemContextMenu(item: MediaItemWithOverride) {
        val options = arrayOf(
            if (item.isExcluded) "Include" else "Exclude",
            "Reclassify",
            if (item.override != null) "Clear Override" else null
        ).filterNotNull()

        AlertDialog.Builder(requireContext())
            .setTitle("Media Actions")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        if (item.isExcluded) {
                            viewModel.handleEvent(MediaGalleryEvent.IncludeMedia(item.mediaItem.path))
                        } else {
                            viewModel.handleEvent(MediaGalleryEvent.ExcludeMedia(item.mediaItem.path))
                        }
                    }
                    1 -> {
                        showReclassifyDialog(item.mediaItem)
                    }
                    2 -> {
                        viewModel.handleEvent(MediaGalleryEvent.ClearOverrides(setOf(item.mediaItem.path)))
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReclassifyDialog(mediaItem: MediaItem) {
        val currentThreatType = viewModel.state.value.overrides[mediaItem.path]?.threatTypeOverride
            ?: mediaItem.threatType
        val newThreatType = if (currentThreatType == ThreatType.THREAT) {
            ThreatType.NON_THREAT
        } else {
            ThreatType.THREAT
        }
        val labels = threatLabelProvider.getCurrentLabels()
        val newLabel = if (newThreatType == ThreatType.THREAT) labels.threat else labels.nonThreat

        AlertDialog.Builder(requireContext())
            .setTitle("Reclassify Media")
            .setMessage("Change classification to $newLabel?")
            .setPositiveButton("Reclassify") { _, _ ->
                viewModel.handleEvent(MediaGalleryEvent.ReclassifyMedia(mediaItem.path, newThreatType))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBulkExcludeConfirmation(paths: Set<String>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Exclude Media")
            .setMessage("Exclude ${paths.size} selected items?")
            .setPositiveButton("Exclude") { _, _ ->
                viewModel.handleEvent(MediaGalleryEvent.BulkExclude(paths))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearAllOverridesConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear All Overrides")
            .setMessage("This will remove all exclusions and threat type overrides. This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                viewModel.handleEvent(MediaGalleryEvent.ClearAllOverrides)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun navigateToPreview(mediaItems: List<MediaItem>, currentIndex: Int) {
        if (currentIndex < 0 || currentIndex >= mediaItems.size) return
        val mediaItem = mediaItems[currentIndex]
        val action = MediaGalleryFragmentDirections.actionMediaGalleryFragmentToMediaPreviewFragment(
            mediaPath = mediaItem.path,
            currentIndex = currentIndex
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
