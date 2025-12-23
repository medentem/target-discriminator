package com.targetdiscriminator.presentation.age_verification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.targetdiscriminator.R
import com.targetdiscriminator.databinding.FragmentAgeVerificationBinding

class AgeVerificationFragment : Fragment() {
    private var _binding: FragmentAgeVerificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitApp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgeVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideBackButton()
        setupListeners()
    }
    private fun hideBackButton() {
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
    private fun setupListeners() {
        binding.confirmButton.setOnClickListener {
            navigateToSessionConfig()
        }
        binding.exitButton.setOnClickListener {
            exitApp()
        }
    }
    private fun navigateToSessionConfig() {
        findNavController().navigate(R.id.action_ageVerificationFragment_to_sessionConfigFragment)
    }
    private fun exitApp() {
        requireActivity().finish()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        if (::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.remove()
        }
        _binding = null
    }
}

