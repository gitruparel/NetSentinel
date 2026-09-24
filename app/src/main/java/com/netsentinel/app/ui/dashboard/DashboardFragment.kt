package com.netsentinel.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.netsentinel.app.R
import com.netsentinel.app.databinding.FragmentDashboardBinding
import com.netsentinel.app.ui.main.MainActivity
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initRealTelemetry(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is DashboardViewModel.DashboardUiState.Success -> {
                        binding.trustScoreView.setScore(state.trustScore, animate = true)
                        binding.tvSsid.text = state.snapshot.ssid
                        binding.tvBssid.text = state.snapshot.bssid
                        binding.tvRssi.text = "${state.snapshot.rssi} dBm (${state.snapshot.signalQualityPercentage}% Quality)"
                        binding.tvGateway.text = state.snapshot.gatewayIp
                        binding.tvDns.text = state.snapshot.dnsServers.joinToString(", ")
                        binding.tvSecurity.text = state.snapshot.securityType
                    }
                    DashboardViewModel.DashboardUiState.Loading -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.auditEvent.collect { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnStartAudit.setOnClickListener {
            viewModel.runHardwareAudit()
        }

        binding.btnHuntMode.setOnClickListener {
            (activity as? MainActivity)?.selectBottomNavItem(R.id.nav_hunt)
        }

        binding.btnViewMap.setOnClickListener {
            (activity as? MainActivity)?.selectBottomNavItem(R.id.nav_map)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
