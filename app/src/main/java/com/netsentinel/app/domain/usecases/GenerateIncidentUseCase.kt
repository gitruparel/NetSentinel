package com.netsentinel.app.domain.usecases

import com.netsentinel.app.data.model.Incident
import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.data.model.TimelineEvent
import com.netsentinel.app.engine.FingerprintEngine
import com.netsentinel.app.engine.ThreatSeverity

class GenerateIncidentUseCase(
    private val fingerprintEngine: FingerprintEngine = FingerprintEngine()
) {
    operator fun invoke(
        title: String,
        threatScore: Int,
        severity: ThreatSeverity,
        snapshot: NetworkSnapshot,
        aiAnalysis: String
    ): Incident {
        val fingerprint = fingerprintEngine.generateFingerprint(snapshot)
        val id = "INC-" + (1000..9999).random()

        return Incident(
            id = id,
            auditSessionId = "AUDIT-042",
            title = title,
            timestampFormatted = "Just now",
            threatScore = threatScore,
            severity = severity,
            status = "QUARANTINED",
            aiAnalysis = aiAnalysis,
            networkDetails = snapshot,
            fingerprint = fingerprint,
            timeline = listOf(
                TimelineEvent("10:42:01 AM", "Collector captured NetworkSnapshot", "COMPLETED"),
                TimelineEvent("10:42:02 AM", "NetTrust Engine flagged duplicate SSID / MAC anomaly", "ALERT"),
                TimelineEvent("10:42:03 AM", "Incident report generated & target isolated", "QUARANTINED")
            )
        )
    }
}
