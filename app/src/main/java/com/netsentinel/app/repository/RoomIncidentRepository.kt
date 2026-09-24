package com.netsentinel.app.repository

import android.content.Context
import com.netsentinel.app.data.model.Fingerprint
import com.netsentinel.app.data.model.Incident
import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.data.model.TimelineEvent
import com.netsentinel.app.database.FirestoreManager
import com.netsentinel.app.database.NetSentinelDatabase
import com.netsentinel.app.database.entity.IncidentEntity
import com.netsentinel.app.engine.ThreatSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RoomIncidentRepository(context: Context) : IncidentRepository {

    private val db = NetSentinelDatabase.getDatabase(context)
    private val incidentDao = db.incidentDao()
    private val firestoreManager = FirestoreManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Pre-populate initial forensic incidents if database is empty
        scope.launch {
            if (incidentDao.getAllIncidents().isEmpty()) {
                val initialList = FakeIncidentRepository().getAllIncidents()
                for (item in initialList) {
                    addIncident(item)
                }
            }
        }
    }

    val incidentsFlow: Flow<List<Incident>> = incidentDao.getAllIncidentsFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    override fun getAllIncidents(): List<Incident> = runBlocking(Dispatchers.IO) {
        incidentDao.getAllIncidents().map { it.toDomainModel() }
    }

    override fun getIncidentById(id: String): Incident? = runBlocking(Dispatchers.IO) {
        incidentDao.getIncidentById(id)?.toDomainModel()
    }

    override fun addIncident(incident: Incident) {
        scope.launch {
            incidentDao.insertIncident(incident.toEntity())
            firestoreManager.saveIncident(incident)
        }
    }

    fun updateIncidentPhoto(id: String, photoUri: String) {
        scope.launch {
            incidentDao.updatePhotoUri(id, photoUri)
            incidentDao.getIncidentById(id)?.toDomainModel()?.let {
                firestoreManager.saveIncident(it)
            }
        }
    }

    val incidentCountFlow: Flow<Int> = incidentDao.getIncidentCountFlow()

    fun clearAllIncidents() {
        scope.launch {
            incidentDao.deleteAllIncidents()
        }
    }

    fun resetToDefaultIncidents() {
        scope.launch {
            incidentDao.deleteAllIncidents()
            val initialList = FakeIncidentRepository().getAllIncidents()
            for (item in initialList) {
                incidentDao.insertIncident(item.toEntity())
            }
        }
    }

    private fun IncidentEntity.toDomainModel(): Incident {
        val severityEnum = try {
            ThreatSeverity.valueOf(this.severity)
        } catch (e: Exception) {
            ThreatSeverity.HIGH
        }

        val snapshot = NetworkSnapshot(
            ssid = this.ssid,
            bssid = this.bssid,
            rssi = this.rssi,
            gatewayIp = this.gatewayIp,
            dnsServers = listOf(this.dnsServersJson),
            frequencyMhz = 5240,
            securityType = this.securityType,
            rttMs = 14L,
            packetLossPercent = 0.0
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
                TimelineEvent("10:41:50 AM", "Initial Wi-Fi baseline scan executed", "NORMAL"),
                TimelineEvent("10:42:01 AM", "Multi-vector telemetry anomaly flagged", "ALERT"),
                TimelineEvent("10:42:05 AM", "Incident logged & evidence archived", "QUARANTINED")
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
            gpsCoordinatesFormatted = this.gpsCoordinatesFormatted
        )
    }
}
