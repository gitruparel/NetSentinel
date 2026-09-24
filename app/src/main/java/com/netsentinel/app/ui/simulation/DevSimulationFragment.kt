package com.netsentinel.app.ui.simulation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.netsentinel.app.databinding.FragmentDevSimulationBinding
import kotlinx.coroutines.launch

class DevSimulationFragment : Fragment() {

    private var _binding: FragmentDevSimulationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DevSimulationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevSimulationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.initContext(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.liveTrustScore.collect { score ->
                binding.simTrustScoreView.setScore(score, animate = true)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.injectEvent.collect { message ->
                android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
            }
        }

        binding.btnInjectThreatDb.setOnClickListener {
            viewModel.injectSimulatedThreatToDb()
        }

        binding.btnViewLogsNow.setOnClickListener {
            (activity as? com.netsentinel.app.ui.main.MainActivity)?.selectBottomNavItem(com.netsentinel.app.R.id.nav_logs)
        }

        binding.switchDuplicateSsid.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateToggle(isDuplicateSsid = isChecked)
        }

        binding.switchGatewayChange.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateToggle(isGatewayChange = isChecked)
        }

        binding.switchDnsChange.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateToggle(isDnsChange = isChecked)
        }

        binding.switchLatencySpike.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateToggle(isLatencySpike = isChecked)
        }

        binding.switchPacketLoss.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateToggle(isPacketLoss = isChecked)
        }

        binding.switchSecurityDowngrade.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateToggle(isSecurityDowngrade = isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
