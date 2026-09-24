package com.netsentinel.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.repository.RoomAuditRepository
import com.netsentinel.app.repository.RoomIncidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    val appVersion: String = "v1.0.0-PRO (NetTrust Engine v2.4)"

    private var incidentRepo: RoomIncidentRepository? = null
    private var auditRepo: RoomAuditRepository? = null

    private val _dbStatsText = MutableStateFlow("Database: Firebase Firestore + Room Offline Cache\nStatus: Persistence Enabled & Sync Ready\nIncidents: Loading... | Audits: Loading...")
    val dbStatsText: StateFlow<String> = _dbStatsText.asStateFlow()

    fun initDatabase(context: Context) {
        val iRepo = RoomIncidentRepository(context)
        val aRepo = RoomAuditRepository(context)
        this.incidentRepo = iRepo
        this.auditRepo = aRepo

        viewModelScope.launch {
            combine(iRepo.incidentCountFlow, aRepo.auditCountFlow) { incCount, auditCount ->
                "Database: Firebase Firestore + Room Offline Cache\nStatus: Persistence Enabled & Sync Ready\nIncidents: $incCount (Synced) | Audit Sessions: $auditCount"
            }.collect { stats ->
                _dbStatsText.value = stats
            }
        }
    }

    fun resetDatabase() {
        incidentRepo?.resetToDefaultIncidents()
    }

    fun clearDatabase() {
        incidentRepo?.clearAllIncidents()
        auditRepo?.clearAllAudits()
    }
}
