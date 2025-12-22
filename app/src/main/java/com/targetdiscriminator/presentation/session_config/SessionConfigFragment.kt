package com.targetdiscriminator.presentation.session_config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.targetdiscriminator.R
import com.targetdiscriminator.databinding.FragmentSessionConfigBinding
import kotlinx.coroutines.launch

class SessionConfigFragment : Fragment() {
    private var _binding: FragmentSessionConfigBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionConfigViewModel by viewModels()
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

