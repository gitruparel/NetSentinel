package com.netsentinel.app.ui.simulation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.data.model.Fingerprint
import com.netsentinel.app.data.model.Incident
import com.netsentinel.app.data.model.SimulationState
import com.netsentinel.app.data.model.TimelineEvent
import com.netsentinel.app.domain.usecases.CalculateTrustScoreUseCase
import com.netsentinel.app.engine.ThreatSeverity
import com.netsentinel.app.location.GpsLocationCollector
import com.netsentinel.app.repository.FakeNetworkRepository
import com.netsentinel.app.repository.IncidentRepository
import com.netsentinel.app.repository.NetworkRepository
import com.netsentinel.app.repository.RoomIncidentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DevSimulationViewModel(
    private val networkRepository: NetworkRepository = FakeNetworkRepository(),
    private val calculateTrustScoreUseCase: CalculateTrustScoreUseCase = CalculateTrustScoreUseCase()
) : ViewModel() {

    private val _simulationState = MutableStateFlow(SimulationState())
    val simulationState: StateFlow<SimulationState> = _simulationState.asStateFlow()

    private val _liveTrustScore = MutableStateFlow(92)
    val liveTrustScore: StateFlow<Int> = _liveTrustScore.asStateFlow()

    private val _injectEvent = MutableSharedFlow<String>()
    val injectEvent: SharedFlow<String> = _injectEvent.asSharedFlow()

    private var incidentRepository: IncidentRepository? = null
    private var locationCollector: GpsLocationCollector? = null

    init {
        reevaluateScore()
    }

    fun initContext(context: Context) {
        this.incidentRepository = RoomIncidentRepository(context)
        this.locationCollector = GpsLocationCollector(context)
    }

    fun updateToggle(
        isDuplicateSsid: Boolean = _simulationState.value.isDuplicateSsidActive,
        isGatewayChange: Boolean = _simulationState.value.isGatewayChangeActive,
        isDnsChange: Boolean = _simulationState.value.isDnsChangeActive,
        isLatencySpike: Boolean = _simulationState.value.isLatencySpikeActive,
        isPacketLoss: Boolean = _simulationState.value.isPacketLossActive,
        isSecurityDowngrade: Boolean = _simulationState.value.isSecurityDowngradeActive
    ) {
        val newState = SimulationState(
            isDuplicateSsidActive = isDuplicateSsid,
            isGatewayChangeActive = isGatewayChange,
            isDnsChangeActive = isDnsChange,
            isLatencySpikeActive = isLatencySpike,
            isPacketLossActive = isPacketLoss,
            isSecurityDowngradeActive = isSecurityDowngrade
        )
        _simulationState.value = newState
        reevaluateScore()
    }

    private fun reevaluateScore() {
        val snapshot = networkRepository.getNetworkSnapshot()
        val eval = calculateTrustScoreUseCase(snapshot, _simulationState.value)
        _liveTrustScore.value = eval.trustScore
    }

    fun injectSimulatedThreatToDb() {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = networkRepository.getNetworkSnapshot()
            val eval = calculateTrustScoreUseCase(snapshot, _simulationState.value)

            val simState = _simulationState.value
            val title = when {
                simState.isDuplicateSsidActive -> "Evil Twin AP Rogue Clone (Simulated)"
                simState.isGatewayChangeActive -> "Gateway ARP Spoofing Attack (Simulated)"
                simState.isDnsChangeActive -> "Rogue DNS Redirection Injection (Simulated)"
                simState.isSecurityDowngradeActive -> "WPA Security Downgrade Exploit (Simulated)"
                simState.isLatencySpikeActive -> "Latency Path Hijack Spike (Simulated)"
                simState.isPacketLossActive -> "RF Jamming Packet Drop Anomaly (Simulated)"
                else -> "Baseline Security Deviation Audit (Simulated)"
            }

            val severity = when {
                eval.trustScore < 40 -> ThreatSeverity.CRITICAL
                eval.trustScore < 70 -> ThreatSeverity.HIGH
                else -> ThreatSeverity.MEDIUM
            }

            val (lat, lng) = locationCollector?.getCurrentLocation() ?: Pair(37.7749, -122.4194)
            val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val incidentId = "INC-SIM-${(1000..9999).random()}"

            val incident = Incident(
                id = incidentId,
                auditSessionId = "AUDIT-SIM-${System.currentTimeMillis().toString().takeLast(4)}",
                title = title,
                timestampFormatted = timeFmt,
                threatScore = eval.trustScore,
                severity = severity,
                status = "ACTIVE_THREAT",
                aiAnalysis = "NetTrust Engine verified security anomaly during active attack simulation. Attack signature: $title. Network Trust Score dropped to ${eval.trustScore}/100. Forensic evidence preserved in Room SQLite DB.",
                networkDetails = eval.evaluatedSnapshot,
                fingerprint = Fingerprint(
                    bssid = eval.evaluatedSnapshot.bssid,
                    ssid = eval.evaluatedSnapshot.ssid,
                    ouiVendor = if (simState.isDuplicateSsidActive) "Hak5 Tactical Rogue AP" else "Unknown Vendor",
                    hashSignature = "SHA256-" + Integer.toHexString((eval.evaluatedSnapshot.ssid + eval.evaluatedSnapshot.bssid).hashCode()).uppercase(),
                    capabilityMask = 0x05,
                    isKnownBaseline = false
                ),
                timeline = listOf(
                    TimelineEvent(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()), "Attack vector triggered in Dev Sandbox", "NORMAL"),
                    TimelineEvent(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()), "NetTrust Engine flagged security anomaly", "ALERT"),
                    TimelineEvent(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()), "Forensic incident registered in Local SQLite Room DB", "QUARANTINED")
                ),
                evidencePhotoPlaceholder = "SIMULATION_EVIDENCE_${incidentId}.PNG",
                gpsCoordinatesFormatted = "${String.format("%.4f", lat)}° N, ${String.format("%.4f", lng)}° W"
            )

            incidentRepository?.addIncident(incident)
            _injectEvent.emit("Simulated Incident $incidentId recorded to Room DB!")
        }
    }
}
