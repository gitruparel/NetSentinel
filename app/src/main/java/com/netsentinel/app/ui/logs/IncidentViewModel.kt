package com.netsentinel.app.ui.logs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private var allIncidents: List<Incident> = incidentRepository.getAllIncidents()

    private val _filteredIncidents = MutableStateFlow<List<Incident>>(allIncidents)
    val filteredIncidents: StateFlow<List<Incident>> = _filteredIncidents.asStateFlow()

    private var currentQuery: String = ""
    private var selectedSeverityFilter: ThreatSeverity? = null

    fun initRoomDatabase(context: Context) {
        val roomRepo = RoomIncidentRepository(context)
        this.incidentRepository = roomRepo

        viewModelScope.launch(Dispatchers.IO) {
            roomRepo.incidentsFlow.collect { incidents ->
                allIncidents = incidents
                filterLogs()
            }
        }
    }

    fun filterLogs(query: String? = null, severity: ThreatSeverity? = null) {
        if (query != null) currentQuery = query
        if (severity != null || query != null) selectedSeverityFilter = severity

        _filteredIncidents.value = allIncidents.filter { incident ->
            val matchesQuery = currentQuery.isEmpty() ||
                    incident.title.contains(currentQuery, ignoreCase = true) ||
                    incident.id.contains(currentQuery, ignoreCase = true) ||
                    incident.networkDetails.ssid.contains(currentQuery, ignoreCase = true)

            val matchesSeverity = selectedSeverityFilter == null || incident.severity == selectedSeverityFilter

            matchesQuery && matchesSeverity
        }
    }

    fun getIncidentById(id: String): Incident? {
        return incidentRepository.getIncidentById(id) ?: allIncidents.find { it.id == id } ?: allIncidents.firstOrNull()
    }

    fun clearAllLogs() {
        (incidentRepository as? RoomIncidentRepository)?.clearAllIncidents()
    }

    fun resetToDefaults() {
        (incidentRepository as? RoomIncidentRepository)?.resetToDefaultIncidents()
    }
}
