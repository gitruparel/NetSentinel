package com.netsentinel.app.ui.hunt

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
import com.netsentinel.app.databinding.FragmentHuntBinding
import com.netsentinel.app.multimedia.AudioRadarSynthesizer
import kotlinx.coroutines.launch

class HuntFragment : Fragment() {

    private var _binding: FragmentHuntBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HuntViewModel by viewModels()
    private var audioRadar: AudioRadarSynthesizer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHuntBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initContext(requireContext())
        audioRadar = AudioRadarSynthesizer()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isHunting.collect { isHunting ->
                        if (isHunting) {
                            binding.btnToggleHunt.setText(R.string.btn_stop_hunt)
                            binding.btnToggleHunt.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.alert_red)
                            binding.tvAudioRadar.text = "Audio Radar: ACTIVE (Tracking RF)"
                            binding.tvAudioRadar.setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_green))
                            audioRadar?.startRadar()
                        } else {
                            binding.btnToggleHunt.setText(R.string.btn_start_hunt)
                            binding.btnToggleHunt.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.cyber_cyan)
                            binding.tvAudioRadar.text = "Audio Radar: STANDBY"
                            binding.tvAudioRadar.setTextColor(ContextCompat.getColor(requireContext(), R.color.cyber_cyan))
                            audioRadar?.stopRadar()
                        }
                    }
                }

                launch {
                    viewModel.rssiDbm.collect { rssi ->
                        binding.tvHuntRssi.text = "$rssi dBm"
                        binding.radarMeterView.setRssi(rssi)
                        audioRadar?.updateRelativeRssi(rssi)
                    }
                }

                launch {
                    viewModel.targetSsid.collect { ssid ->
                        binding.tvTargetSsid.text = ssid
                    }
                }

                launch {
                    viewModel.targetDetails.collect { details ->
                        binding.tvTargetDetails.text = details
                    }
                }
            }
        }

        binding.btnToggleHunt.setOnClickListener {
            viewModel.toggleHunt()
            val msg = if (viewModel.isHunting.value) {
                "Hunt Mode Active — Relative RSSI Audio Radar Tracking"
            } else {
                "Hunt Mode Stopped"
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        audioRadar?.stopRadar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioRadar?.release()
        audioRadar = null
        _binding = null
    }
}
