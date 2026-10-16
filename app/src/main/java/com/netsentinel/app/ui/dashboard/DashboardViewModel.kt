package com.netsentinel.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.NetSentinelApp
import com.netsentinel.app.data.model.AuditSession
import com.netsentinel.app.data.model.Incident
import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.data.model.SimulationState
import com.netsentinel.app.data.model.TimelineEvent
import com.netsentinel.app.database.entity.BaselineNetworkEntity
import com.netsentinel.app.domain.usecases.CalculateTrustScoreUseCase
import com.netsentinel.app.network.NetworkScanner
import com.netsentinel.app.repository.AuditRepository
import com.netsentinel.app.repository.BaselineRepository
import com.netsentinel.app.repository.FakeNetworkRepository
import com.netsentinel.app.repository.IncidentRepository
import com.netsentinel.app.repository.NetworkRepository
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

class DashboardViewModel(
    private var networkRepository: NetworkRepository = FakeNetworkRepository(),
    private val calculateTrustScoreUseCase: CalculateTrustScoreUseCase = CalculateTrustScoreUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _auditEvent = MutableSharedFlow<String>()
    val auditEvent: SharedFlow<String> = _auditEvent.asSharedFlow()

    private var auditRepository: AuditRepository? = null
    private var incidentRepository: IncidentRepository? = null
    private var baselineRepository: BaselineRepository? = null
    private var currentSimulation = SimulationState()
    private var isRealTelemetryInitialized = false

    init {
        refreshDashboard()
    }

    fun initRealTelemetry(context: Context) {
        if (isRealTelemetryInitialized) return
        val app = context.applicationContext as? NetSentinelApp
        if (app != null) {
            val scanner = app.container.networkScanner
            this.networkRepository = object : NetworkRepository {
                override fun getNetworkSnapshot(): NetworkSnapshot = scanner.captureSnapshot()
                override fun observeNetworkState() = MutableStateFlow(scanner.captureSnapshot())
            }
            this.auditRepository = app.container.auditRepository
            this.incidentRepository = app.container.incidentRepository
            this.baselineRepository = app.container.baselineRepository
        } else {
            val scanner = NetworkScanner(context)
            this.networkRepository = object : NetworkRepository {
                override fun getNetworkSnapshot(): NetworkSnapshot = scanner.captureSnapshot()
                override fun observeNetworkState() = MutableStateFlow(scanner.captureSnapshot())
            }
        }
        isRealTelemetryInitialized = true
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = networkRepository.getNetworkSnapshot()

            val baseline = if (snapshot.isConnected && snapshot.ssid.isNotEmpty()) {
                baselineRepository?.getBaselineForSsid(snapshot.ssid)
            } else null

            val expectedGateway = baseline?.expectedGateway ?: ""
            val expectedDns = baseline?.expectedDnsJson ?: ""

            val eval = calculateTrustScoreUseCase(
                snapshot = snapshot,
                simulationState = currentSimulation,
                expectedGateway = expectedGateway,
                expectedDns = expectedDns
            )

            _uiState.value = DashboardUiState.Success(
                snapshot = eval.evaluatedSnapshot,
                trustScore = eval.trustScore,
                statusSummary = eval.statusSummary
            )
        }
    }

    fun runHardwareAudit() {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = networkRepository.getNetworkSnapshot()

            val baseline = if (snapshot.isConnected && snapshot.ssid.isNotEmpty()) {
                baselineRepository?.getBaselineForSsid(snapshot.ssid)
            } else null

            val expectedGateway = baseline?.expectedGateway ?: ""
            val expectedDns = baseline?.expectedDnsJson ?: ""

            val eval = calculateTrustScoreUseCase(
                snapshot = snapshot,
                simulationState = currentSimulation,
                expectedGateway = expectedGateway,
                expectedDns = expectedDns
            )

            val auditId = "AUDIT-${System.currentTimeMillis().toString().takeLast(4)}"
            val nowMs = System.currentTimeMillis()
            val auditSession = AuditSession(
                id = auditId,
                startTimeMs = nowMs,
                endTimeMs = nowMs,
                initialSnapshot = eval.evaluatedSnapshot,
                trustScore = eval.trustScore,
                detectedThreats = emptyList(),
                status = if (eval.trustScore >= 80) "SECURE" else "ANOMALIES_FLAGGED"
            )

            auditRepository?.updateAuditSession(auditSession)

            // Auto-learn baseline for clean connected network if not yet saved
            if (baseline == null && snapshot.isConnected && eval.reasons.isEmpty() && snapshot.bssid != "00:00:00:00:00:00") {
                baselineRepository?.saveBaseline(
                    BaselineNetworkEntity(
                        bssid = snapshot.bssid,
                        ssid = snapshot.ssid,
                        expectedGateway = snapshot.gatewayIp,
                        expectedDnsJson = snapshot.dnsServers.firstOrNull() ?: "",
                        expectedSecurity = snapshot.securityType,
                        lastSeenMs = nowMs
                    )
                )
            }

            // If anomalies are detected, record an incident in Room DB
            if (eval.reasons.isNotEmpty()) {
                val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val topReason = eval.reasons.first()
                val incidentId = "INC-${(1000..9999).random()}"
                val incident = Incident(
                    id = incidentId,
                    auditSessionId = auditId,
                    title = "Hardware Audit Anomaly: ${topReason.type.name}",
                    timestampFormatted = timeFmt,
                    threatScore = eval.trustScore,
                    severity = topReason.severity,
                    status = "FLAGGED",
                    aiAnalysis = topReason.message,
                    networkDetails = eval.evaluatedSnapshot,
                    fingerprint = eval.fingerprint,
                    timeline = listOf(
                        TimelineEvent(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()), "Hardware Audit Initiated", "NORMAL"),
                        TimelineEvent(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()), topReason.message, "ALERT")
                    ),
                    evidencePhotoPlaceholder = "HARDWARE_AUDIT_${incidentId}.PNG",
                    gpsCoordinatesFormatted = "37.7749° N, 122.4194° W"
                )
                incidentRepository?.addIncident(incident)
            }

            _uiState.value = DashboardUiState.Success(
                snapshot = eval.evaluatedSnapshot,
                trustScore = eval.trustScore,
                statusSummary = eval.statusSummary
            )

            val msg = if (!eval.evaluatedSnapshot.isConnected) {
                "Audit Completed: Device is currently disconnected from Wi-Fi"
            } else if (eval.reasons.isNotEmpty()) {
                "Audit Saved ($auditId) — ${eval.reasons.size} Threat(s) Logged to DB!"
            } else {
                "Audit Session $auditId Saved to Database (Score: ${eval.trustScore}/100)"
            }
            _auditEvent.emit(msg)
        }
    }

    fun applySimulationState(simulationState: SimulationState) {
        this.currentSimulation = simulationState
        refreshDashboard()
    }

    sealed class DashboardUiState {
        object Loading : DashboardUiState()
        data class Success(
            val snapshot: NetworkSnapshot,
            val trustScore: Int,
            val statusSummary: String
        ) : DashboardUiState()
    }
}
