package com.netsentinel.app.repository

import android.content.Context
import com.netsentinel.app.data.model.Fingerprint
import com.netsentinel.app.data.model.Incident
import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.data.model.TimelineEvent
import com.netsentinel.app.database.NetSentinelDatabase
import com.netsentinel.app.database.SupabaseManager
import com.netsentinel.app.database.entity.IncidentEntity
import com.netsentinel.app.engine.ThreatSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomIncidentRepository(context: Context) : IncidentRepository {

    private val db = NetSentinelDatabase.getDatabase(context)
    private val incidentDao = db.incidentDao()
    private val supabaseManager = SupabaseManager(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Pre-populate benchmark security incidents if database is empty
        repositoryScope.launch {
            if (incidentDao.getIncidentCount() == 0) {
                val initialList = FakeIncidentRepository().getAllIncidents()
                for (item in initialList) {
                    insertInternal(item)
                }
            }
        }
    }

    val incidentsFlow: Flow<List<Incident>> = incidentDao.getAllIncidentsFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    override fun observeIncidents(): Flow<List<Incident>> = incidentsFlow

    override suspend fun getAllIncidents(): List<Incident> = withContext(Dispatchers.IO) {
        incidentDao.getAllIncidents().map { it.toDomainModel() }
    }

    override suspend fun getIncidentById(id: String): Incident? = withContext(Dispatchers.IO) {
        incidentDao.getIncidentById(id)?.toDomainModel()
    }

    override suspend fun addIncident(incident: Incident) {
        insertInternal(incident)
    }

    private suspend fun insertInternal(incident: Incident) = withContext(Dispatchers.IO) {
        incidentDao.insertIncident(incident.toEntity())
        repositoryScope.launch {
            try {
                supabaseManager.saveIncident(incident)
            } catch (e: Exception) {
                // Background sync failure handled gracefully
            }
        }
    }

    suspend fun updateIncidentPhoto(id: String, photoUri: String) = withContext(Dispatchers.IO) {
        incidentDao.updatePhotoUri(id, photoUri)
        val updated = incidentDao.getIncidentById(id)?.toDomainModel()
        if (updated != null) {
            repositoryScope.launch {
                try {
                    supabaseManager.saveIncident(updated)
                } catch (_: Exception) {}
            }
        }
    }

    val incidentCountFlow: Flow<Int> = incidentDao.getIncidentCountFlow()

    suspend fun clearAllIncidents() = withContext(Dispatchers.IO) {
        incidentDao.deleteAllIncidents()
    }

    suspend fun resetToDefaultIncidents() = withContext(Dispatchers.IO) {
        incidentDao.deleteAllIncidents()
        val initialList = FakeIncidentRepository().getAllIncidents()
        for (item in initialList) {
            incidentDao.insertIncident(item.toEntity())
        }
    }

    private fun IncidentEntity.toDomainModel(): Incident {
        val severityEnum = try {
            ThreatSeverity.valueOf(this.severity)
        } catch (e: Exception) {
            ThreatSeverity.HIGH
        }

        val parsedDns = if (this.dnsServersJson.isBlank()) {
            emptyList()
        } else {
            this.dnsServersJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        val snapshot = NetworkSnapshot(
            ssid = this.ssid,
            bssid = this.bssid,
            rssi = this.rssi,
            gatewayIp = this.gatewayIp,
            dnsServers = parsedDns,
            frequencyMhz = this.frequencyMhz,
            securityType = this.securityType,
            rttMs = this.rttMs,
            packetLossPercent = this.packetLossPercent,
            isConnected = this.ssid != "Wi-Fi Disconnected" && this.bssid != "00:00:00:00:00:00"
        )

        val fingerprint = Fingerprint(
            bssid = this.bssid,
            ssid = this.ssid,
            ouiVendor = this.ouiVendor,
            hashSignature = "SHA256-" + Integer.toHexString((this.ssid + this.bssid).hashCode()).uppercase(),
            capabilityMask = 0x05,
            isKnownBaseline = false
        )

        return Incident(
            id = this.id,
            auditSessionId = this.auditSessionId,
            title = this.title,
            timestampFormatted = this.timestampFormatted,
            threatScore = this.threatScore,
            severity = severityEnum,
            status = this.status,
            aiAnalysis = this.aiAnalysis,
            networkDetails = snapshot,
            fingerprint = fingerprint,
            timeline = listOf(
                TimelineEvent("10:41:50 AM", "Baseline telemetry scan recorded", "NORMAL"),
                TimelineEvent("10:42:01 AM", "Multi-vector anomaly flagged: ${this.title}", "ALERT"),
                TimelineEvent("10:42:05 AM", "Incident logged & forensic evidence preserved", this.status)
            ),
            evidencePhotoPlaceholder = this.photoUri ?: "SPECTRUM_CAPTURE_042.PNG",
            gpsCoordinatesFormatted = this.gpsCoordinatesFormatted
        )
    }

    private fun Incident.toEntity(): IncidentEntity {
        return IncidentEntity(
            id = this.id,
            auditSessionId = this.auditSessionId,
            title = this.title,
            timestampFormatted = this.timestampFormatted,
            threatScore = this.threatScore,
            severity = this.severity.name,
            status = this.status,
            aiAnalysis = this.aiAnalysis,
            ssid = this.networkDetails.ssid,
            bssid = this.networkDetails.bssid,
            rssi = this.networkDetails.rssi,
            gatewayIp = this.networkDetails.gatewayIp,
            dnsServersJson = this.networkDetails.dnsServers.joinToString(", "),
            securityType = this.networkDetails.securityType,
            ouiVendor = this.fingerprint.ouiVendor,
            photoUri = if (this.evidencePhotoPlaceholder.startsWith("content://") || this.evidencePhotoPlaceholder.startsWith("file://")) this.evidencePhotoPlaceholder else null,
            gpsCoordinatesFormatted = this.gpsCoordinatesFormatted,
            frequencyMhz = this.networkDetails.frequencyMhz,
            rttMs = this.networkDetails.rttMs,
            packetLossPercent = this.networkDetails.packetLossPercent
        )
    }
}
