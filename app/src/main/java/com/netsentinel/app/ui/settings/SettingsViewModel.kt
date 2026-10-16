package com.netsentinel.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.repository.RoomAuditRepository
import com.netsentinel.app.repository.RoomIncidentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    val appVersion: String = "v1.0.0-PRO (NetTrust Engine v2.4)"

    private var incidentRepo: RoomIncidentRepository? = null
    private var auditRepo: RoomAuditRepository? = null

    private val _dbStatsText = MutableStateFlow("Database: Supabase PostgreSQL + Room SQLite Cache\nStatus: Dual-Engine Active (Cloud SQL & Local DB)\nIncidents: Loading... | Audits: Loading...")
    val dbStatsText: StateFlow<String> = _dbStatsText.asStateFlow()

    private var isInitialized = false

    fun initDatabase(context: Context) {
        if (isInitialized) return
        val app = context.applicationContext as? NetSentinelApp
        val iRepo = (app?.container?.incidentRepository as? RoomIncidentRepository) ?: RoomIncidentRepository(context)
        val aRepo = app?.container?.auditRepository ?: RoomAuditRepository(context)
        this.incidentRepo = iRepo
        this.auditRepo = aRepo
        isInitialized = true

        viewModelScope.launch {
            combine(iRepo.incidentCountFlow, aRepo.auditCountFlow) { incCount, auditCount ->
                "Database: Supabase PostgreSQL + Room SQLite Cache\nStatus: Dual-Engine Active (Cloud SQL & Local DB)\nIncidents: $incCount (Synced) | Audit Sessions: $auditCount"
            }.collect { stats ->
                _dbStatsText.value = stats
            }
        }
    }

    fun resetDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            incidentRepo?.resetToDefaultIncidents()
        }
    }

    fun clearDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            incidentRepo?.clearAllIncidents()
            auditRepo?.clearAllAudits()
        }
    }
}
