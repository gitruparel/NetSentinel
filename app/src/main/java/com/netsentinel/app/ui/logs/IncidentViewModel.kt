package com.netsentinel.app.ui.logs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.data.model.Incident
import com.netsentinel.app.engine.ThreatSeverity
import com.netsentinel.app.repository.FakeIncidentRepository
import com.netsentinel.app.repository.IncidentRepository
import com.netsentinel.app.repository.RoomIncidentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IncidentViewModel(
    private var incidentRepository: IncidentRepository = FakeIncidentRepository()
) : ViewModel() {

    private var allIncidents: List<Incident> = emptyList()

    private val _filteredIncidents = MutableStateFlow<List<Incident>>(emptyList())
    val filteredIncidents: StateFlow<List<Incident>> = _filteredIncidents.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedIncident = MutableStateFlow<Incident?>(null)
    val selectedIncident: StateFlow<Incident?> = _selectedIncident.asStateFlow()

    private var currentQuery: String = ""
    private var selectedSeverityFilter: ThreatSeverity? = null
    private var isInitialized = false

    fun initRoomDatabase(context: Context) {
        if (isInitialized) return
        val app = context.applicationContext as? NetSentinelApp
        this.incidentRepository = app?.container?.incidentRepository ?: RoomIncidentRepository(context)
        isInitialized = true

        viewModelScope.launch(Dispatchers.IO) {
            incidentRepository.observeIncidents().collect { incidents ->
                allIncidents = incidents
                _isLoading.value = false
                filterLogs()
            }
        }
    }

    fun loadIncidentDetails(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val incident = incidentRepository.getIncidentById(id)
                ?: allIncidents.find { it.id == id }
                ?: allIncidents.firstOrNull()
            _selectedIncident.value = incident
        }
    }

    fun filterLogs(query: String? = null, severity: ThreatSeverity? = null) {
        if (query != null) currentQuery = query
        if (severity != null || query != null) selectedSeverityFilter = severity

        _filteredIncidents.value = allIncidents.filter { incident ->
            val matchesQuery = currentQuery.isEmpty() ||
                    incident.title.contains(currentQuery, ignoreCase = true) ||
                    incident.id.contains(currentQuery, ignoreCase = true) ||
                    incident.networkDetails.ssid.contains(currentQuery, ignoreCase = true) ||
                    incident.status.contains(currentQuery, ignoreCase = true)

            val matchesSeverity = selectedSeverityFilter == null || incident.severity == selectedSeverityFilter

            matchesQuery && matchesSeverity
        }
    }

    suspend fun getIncidentById(id: String): Incident? {
        return incidentRepository.getIncidentById(id) ?: allIncidents.find { it.id == id }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            (incidentRepository as? RoomIncidentRepository)?.clearAllIncidents()
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch(Dispatchers.IO) {
            (incidentRepository as? RoomIncidentRepository)?.resetToDefaultIncidents()
        }
    }
}
