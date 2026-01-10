package com.targetdiscriminator.presentation.session_config

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.targetdiscriminator.R
import com.targetdiscriminator.databinding.FragmentSessionConfigBinding
import com.targetdiscriminator.domain.model.ThreatLabelPreset
import kotlinx.coroutines.launch

class SessionConfigFragment : Fragment() {
    private var _binding: FragmentSessionConfigBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionConfigViewModel by viewModels {
        SessionConfigViewModelFactory(requireContext())
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionConfigBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideBackButton()
        setupObservers()
        setupListeners()
    }
    private fun hideBackButton() {
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
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
    private fun setupListeners() {
        binding.includeVideosSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.handleEvent(SessionConfigEvent.ToggleVideos(isChecked))
        }
        binding.includePhotosSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.handleEvent(SessionConfigEvent.TogglePhotos(isChecked))
        }
        binding.durationSlider.addOnChangeListener { _, value, _ ->
            viewModel.handleEvent(SessionConfigEvent.SetDuration(value.toInt()))
        }
        binding.threatNonThreatRadio.setOnClickListener {
            viewModel.handleEvent(SessionConfigEvent.SetThreatLabelPreset(ThreatLabelPreset.THREAT_NON_THREAT))
        }
        binding.shootNoShootRadio.setOnClickListener {
            viewModel.handleEvent(SessionConfigEvent.SetThreatLabelPreset(ThreatLabelPreset.SHOOT_NO_SHOOT))
        }
        binding.customRadio.setOnClickListener {
            viewModel.handleEvent(SessionConfigEvent.SetThreatLabelPreset(ThreatLabelPreset.CUSTOM))
        }
        binding.customThreatLabelInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.handleEvent(SessionConfigEvent.SetCustomThreatLabel(s?.toString() ?: ""))
            }
        })
        binding.customNonThreatLabelInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.handleEvent(SessionConfigEvent.SetCustomNonThreatLabel(s?.toString() ?: ""))
            }
        })
        binding.manageMediaButton.setOnClickListener {
            findNavController().navigate(R.id.action_sessionConfigFragment_to_mediaManagementFragment)
        }
        binding.startButton.setOnClickListener {
            viewModel.handleEvent(SessionConfigEvent.StartSession)
        }
    }
    private fun renderState(state: SessionConfigState) {
        binding.includeVideosSwitch.isChecked = state.includeVideos
        binding.includePhotosSwitch.isChecked = state.includePhotos
        binding.durationSlider.value = state.durationMinutes.toFloat()
        binding.durationValue.text = "${state.durationMinutes} min"
        binding.startButton.isEnabled = state.canStart
        
        // Update threat label preset radio buttons
        when (state.threatLabelPreset) {
            ThreatLabelPreset.THREAT_NON_THREAT -> binding.threatNonThreatRadio.isChecked = true
            ThreatLabelPreset.SHOOT_NO_SHOOT -> binding.shootNoShootRadio.isChecked = true
            ThreatLabelPreset.CUSTOM -> binding.customRadio.isChecked = true
        }
        
        // Show/hide custom labels container
        binding.customLabelsContainer.visibility = if (state.threatLabelPreset == ThreatLabelPreset.CUSTOM) {
            View.VISIBLE
        } else {
            View.GONE
        }
        
        // Update custom label inputs (only if they differ to avoid infinite loops)
        if (binding.customThreatLabelInput.text?.toString() != state.customThreatLabel) {
            binding.customThreatLabelInput.setText(state.customThreatLabel)
        }
        if (binding.customNonThreatLabelInput.text?.toString() != state.customNonThreatLabel) {
            binding.customNonThreatLabelInput.setText(state.customNonThreatLabel)
        }
    }
    private fun handleEffect(effect: SessionConfigEffect) {
        when (effect) {
            is SessionConfigEffect.NavigateToTraining -> {
                val bundle = Bundle().apply {
                    putParcelable("config", effect.config)
                }
                findNavController().navigate(R.id.action_sessionConfigFragment_to_trainingFragment, bundle)
            }
            is SessionConfigEffect.ShowError -> {
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

