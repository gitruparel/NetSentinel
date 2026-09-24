package com.netsentinel.app.engine

import com.netsentinel.app.data.model.Fingerprint
import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.data.model.SimulationState

/**
 * NetTrust Engine core IP. Evaluates NetworkSnapshot instances through unified fingerprint,
 * baseline deviation, and explainable threat scoring pipelines.
 */
class ThreatEngine(
    private val fingerprintEngine: FingerprintEngine = FingerprintEngine(),
    private val baselineAnalyzer: BaselineAnalyzer = BaselineAnalyzer(),
    private val confidenceCalculator: ConfidenceCalculator = ConfidenceCalculator()
) {

    fun evaluateNetwork(
        snapshot: NetworkSnapshot,
        simulationState: SimulationState = SimulationState(),
        expectedGateway: String = "192.168.1.1",
        expectedDns: String = "1.1.1.1"
    ): EvaluationResult {
        // Build effective snapshot (merging simulation flags if active)
        val effectiveSsid = snapshot.ssid
        val effectiveBssid = if (simulationState.isDuplicateSsidActive) "DE:AD:BE:EF:42:01" else snapshot.bssid
        val effectiveGateway = if (simulationState.isGatewayChangeActive) "10.0.4.99" else snapshot.gatewayIp
        val effectiveDns = if (simulationState.isDnsChangeActive) listOf("198.51.100.42") else snapshot.dnsServers
        val effectiveRtt = if (simulationState.isLatencySpikeActive) 480L else snapshot.rttMs
        val effectiveLoss = if (simulationState.isPacketLossActive) 35.0 else snapshot.packetLossPercent
        val effectiveSecurity = if (simulationState.isSecurityDowngradeActive) "Open" else snapshot.securityType

        val processedSnapshot = snapshot.copy(
            ssid = effectiveSsid,
            bssid = effectiveBssid,
            gatewayIp = effectiveGateway,
            dnsServers = effectiveDns,
            rttMs = effectiveRtt,
            packetLossPercent = effectiveLoss,
            securityType = effectiveSecurity
        )

        val reasons = mutableListOf<ThreatReason>()

        // 1. Multi-Vector AP Fingerprint Analysis
        val fingerprint = fingerprintEngine.generateFingerprint(processedSnapshot)
        if (simulationState.isDuplicateSsidActive || processedSnapshot.bssid.equals("DE:AD:BE:EF:42:01", ignoreCase = true)) {
            val apAssessment = fingerprintEngine.evaluateApVector(
                snapshot = processedSnapshot,
                knownBssidsForSsid = listOf("00:1A:2B:3C:4D:5E"),
                baselineGateway = expectedGateway,
                baselineSecurity = "WPA3-Enterprise"
            )
            if (apAssessment.deductionPoints > 0) {
                reasons.add(
                    ThreatReason(
                        type = ThreatType.EVIL_TWIN,
                        severity = ThreatSeverity.CRITICAL,
                        message = apAssessment.summary,
                        deductionPoints = apAssessment.deductionPoints
                    )
                )
            }
        }

        // 2. Baseline Deviation Analysis (Defensible Security Reasoning)
        val baselineReasons = baselineAnalyzer.analyzeBaselineDeviation(
            snapshot = processedSnapshot,
            expectedGateway = expectedGateway,
            expectedDns = expectedDns
        )
        reasons.addAll(baselineReasons)

        // 3. Compute final NetTrust score (100 - total deductions)
        val score = confidenceCalculator.calculateConfidenceScore(reasons)

        val statusText = when {
            score >= 85 -> "SECURE NETWORK"
            score >= 60 -> "WARNING: ANOMALIES DETECTED"
            score >= 30 -> "HIGH RISK: SUSPECTED MITM / ROGUE AP"
            else -> "CRITICAL THREAT: EVIL TWIN DETECTED"
        }

        return EvaluationResult(
            trustScore = score,
            statusSummary = statusText,
            reasons = reasons,
            evaluatedSnapshot = processedSnapshot,
            fingerprint = fingerprint
        )
    }

    data class EvaluationResult(
        val trustScore: Int,
        val statusSummary: String,
        val reasons: List<ThreatReason>,
        val evaluatedSnapshot: NetworkSnapshot,
        val fingerprint: Fingerprint
    )
}
