package com.netsentinel.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is DashboardViewModel.DashboardUiState.Success -> {
                                val snap = state.snapshot
                                binding.trustScoreView.setScore(state.trustScore, animate = true)

                                if (!snap.isConnected) {
                                    binding.tvLastScan.text = "Wi-Fi: DISCONNECTED"
                                    binding.tvLastScan.setTextColor(ContextCompat.getColor(requireContext(), R.color.alert_red))
                                    binding.tvSsid.text = "No Wi-Fi Connection"
                                    binding.tvBssid.text = "—"
                                    binding.tvRssi.text = "Offline"
                                    binding.tvRssi.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                                    binding.tvGateway.text = "N/A"
                                    binding.tvDns.text = "N/A"
                                    binding.tvSecurity.text = "None"
                                    binding.tvSecurity.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                                } else {
                                    binding.tvLastScan.text = if (snap.requiresPermission) "Wi-Fi: PERMISSION NEEDED" else "Hardware Scan: LIVE"
                                    val scanColor = if (snap.requiresPermission) R.color.warning_yellow else R.color.neon_green
                                    binding.tvLastScan.setTextColor(ContextCompat.getColor(requireContext(), scanColor))

                                    binding.tvSsid.text = snap.ssid
                                    binding.tvBssid.text = snap.bssid
                                    binding.tvRssi.text = "${snap.rssi} dBm (${snap.signalQualityPercentage}% Quality)"
                                    val rssiColor = if (snap.signalQualityPercentage >= 70) R.color.neon_green else R.color.warning_yellow
                                    binding.tvRssi.setTextColor(ContextCompat.getColor(requireContext(), rssiColor))

                                    binding.tvGateway.text = snap.gatewayIp
                                    binding.tvDns.text = if (snap.dnsServers.isEmpty()) "DHCP Default" else snap.dnsServers.joinToString(", ")
                                    binding.tvSecurity.text = snap.securityType
                                    val secColor = if (snap.securityType.contains("Open", ignoreCase = true)) R.color.alert_red else R.color.neon_green
                                    binding.tvSecurity.setTextColor(ContextCompat.getColor(requireContext(), secColor))
                                }
                            }
                            DashboardViewModel.DashboardUiState.Loading -> {
                                binding.tvLastScan.text = "Scanning..."
                            }
                        }
                    }
                }

                launch {
                    viewModel.auditEvent.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        binding.btnStartAudit.setOnClickListener {
            viewModel.runHardwareAudit()
        }

        // Acoustic Threat Siren (Multimedia Intrusion Alarm System)
        val mainAct = activity as? MainActivity
        updateSirenUi(mainAct?.isThreatAlarmPlaying() == true)

        binding.btnSoundSiren.setOnClickListener {
            val started = mainAct?.playThreatAlarm {
                activity?.runOnUiThread {
                    updateSirenUi(false)
                }
            } ?: false
            if (started) {
                updateSirenUi(true)
            }
        }

        binding.btnSilenceSiren.setOnClickListener {
            mainAct?.stopThreatAlarm()
            updateSirenUi(false)
        }

        binding.btnHuntMode.setOnClickListener {
            (activity as? MainActivity)?.selectBottomNavItem(R.id.nav_hunt)
        }

        binding.btnViewMap.setOnClickListener {
            (activity as? MainActivity)?.selectBottomNavItem(R.id.nav_map)
        }
    }

    private fun updateSirenUi(isPlaying: Boolean) {
        if (_binding == null) return
        val ctx = context ?: return
        if (isPlaying) {
            binding.tvSirenStatusBadge.text = "🚨 SIREN SOUNDING"
            binding.tvSirenStatusBadge.setBackgroundResource(R.drawable.bg_badge_critical)
            binding.tvSirenStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.alert_red))
            binding.ivSirenIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.alert_red))
            binding.cardAcousticSiren.strokeColor = ContextCompat.getColor(ctx, R.color.alert_red)
        } else {
            binding.tvSirenStatusBadge.text = "SIREN ARMED"
            binding.tvSirenStatusBadge.setBackgroundResource(R.drawable.bg_badge_secure)
            binding.tvSirenStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.neon_green))
            binding.ivSirenIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.neon_green))
            binding.cardAcousticSiren.strokeColor = ContextCompat.getColor(ctx, R.color.cyber_cyan)
        }
    }

    override fun onResume() {
        super.onResume()
        val mainAct = activity as? MainActivity
        updateSirenUi(mainAct?.isThreatAlarmPlaying() == true)
        viewModel.refreshDashboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

