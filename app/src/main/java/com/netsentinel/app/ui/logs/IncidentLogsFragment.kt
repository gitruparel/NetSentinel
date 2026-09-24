package com.netsentinel.app.ui.logs

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
import com.netsentinel.app.R
import com.netsentinel.app.databinding.FragmentIncidentLogsBinding
import com.netsentinel.app.engine.ThreatSeverity
import kotlinx.coroutines.launch

class IncidentLogsFragment : Fragment() {

    private var _binding: FragmentIncidentLogsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentViewModel by viewModels()
    private lateinit var adapter: IncidentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncidentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initRoomDatabase(requireContext())

        adapter = IncidentAdapter { incident ->
            val bundle = Bundle().apply {
                putString("incidentId", incident.id)
            }
            findNavController().navigate(R.id.action_logs_to_details, bundle)
        }

        binding.rvIncidentLogs.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredIncidents.collect { incidents ->
                adapter.submitList(incidents)
            }
        }

        binding.etSearchLogs.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filterLogs(query = s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_critical -> viewModel.filterLogs(severity = ThreatSeverity.CRITICAL)
                R.id.chip_warning -> viewModel.filterLogs(severity = ThreatSeverity.HIGH)
                else -> viewModel.filterLogs(severity = null)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
