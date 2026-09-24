package com.netsentinel.app.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.netsentinel.app.R
import com.netsentinel.app.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("netsentinel_settings", Context.MODE_PRIVATE)
        val isDarkModeEnabled = prefs.getBoolean("dark_mode", true)

        binding.tvAppVersion.text = viewModel.appVersion
        binding.switchDarkMode.isChecked = isDarkModeEnabled

        viewModel.initDatabase(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dbStatsText.collect { stats ->
                binding.tvDbStats.text = stats
            }
        }

        binding.btnResetDb.setOnClickListener {
            viewModel.resetDatabase()
            Toast.makeText(requireContext(), "Local Room DB Reset to Benchmark Security Incidents", Toast.LENGTH_SHORT).show()
        }

        binding.btnClearDb.setOnClickListener {
            viewModel.clearDatabase()
            Toast.makeText(requireContext(), "Local Room DB Cache Cleared", Toast.LENGTH_SHORT).show()
        }

        binding.btnOpenDevSim.setOnClickListener {
            findNavController().navigate(R.id.nav_simulation)
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            Toast.makeText(requireContext(), "Theme Mode Updated", Toast.LENGTH_SHORT).show()
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Push Notifications: $isChecked", Toast.LENGTH_SHORT).show()
        }

        binding.switchCommunityFeed.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Community Threat Feed Sync: $isChecked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
