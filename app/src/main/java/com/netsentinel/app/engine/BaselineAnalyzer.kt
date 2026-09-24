package com.netsentinel.app.engine

import com.netsentinel.app.data.model.NetworkSnapshot

/**
 * Analyzes current NetworkSnapshot against stored network baseline vectors using defensible security reasoning.
 */
class BaselineAnalyzer {

    fun analyzeBaselineDeviation(
        snapshot: NetworkSnapshot,
        expectedGateway: String = "192.168.1.1",
        expectedDns: String = "1.1.1.1"
    ): List<ThreatReason> {
        val anomalies = mutableListOf<ThreatReason>()

        // Gateway Drift Check
        if (snapshot.gatewayIp.isNotEmpty() && expectedGateway.isNotEmpty() && snapshot.gatewayIp != expectedGateway) {
            anomalies.add(
                ThreatReason(
                    type = ThreatType.ARP_SPOOFING,
                    severity = ThreatSeverity.HIGH,
                    message = "Gateway IP address changed from $expectedGateway to ${snapshot.gatewayIp}. Potential ARP spoofing or unannounced subnet reconfiguration.",
                    deductionPoints = 30
                )
            )
        }

        // DNS Resolver Check
        if (snapshot.dnsServers.isNotEmpty() && !snapshot.dnsServers.contains(expectedDns)) {
            anomalies.add(
                ThreatReason(
                    type = ThreatType.ROGUE_DNS,
                    severity = ThreatSeverity.CRITICAL,
                    message = "DNS Resolver changed! Active resolver: ${snapshot.dnsServers.firstOrNull()} (Expected: $expectedDns). High risk of DNS redirection.",
                    deductionPoints = 40
                )
            )
        }

        // Defensible RTT Latency Path Anomaly
        if (snapshot.rttMs > 250) {
            anomalies.add(
                ThreatReason(
                    type = ThreatType.LATENCY_HIJACK,
                    severity = ThreatSeverity.MEDIUM,
                    message = "RTT latency spike (${snapshot.rttMs} ms) relative to historical baseline. Reported as a potential network-path anomaly (causes: congestion, RF noise, rerouting, or inspection).",
                    deductionPoints = 15
                )
            )
        }

        // Defensible Packet Loss Connectivity Anomaly
        if (snapshot.packetLossPercent > 20.0) {
            anomalies.add(
                ThreatReason(
                    type = ThreatType.PACKET_JAMMING,
                    severity = ThreatSeverity.HIGH,
                    message = "Elevated packet loss (${snapshot.packetLossPercent}%) relative to baseline. Reported as a connectivity anomaly (causes: channel congestion, RF noise, or deauth/jamming frames).",
                    deductionPoints = 25
                )
            )
        }

        // Security Downgrade Check
        if (snapshot.securityType.equals("Open", ignoreCase = true) || snapshot.securityType.contains("None", ignoreCase = true)) {
            anomalies.add(
                ThreatReason(
                    type = ThreatType.SECURITY_DOWNGRADE,
                    severity = ThreatSeverity.CRITICAL,
                    message = "Network security broadcast is unencrypted OPEN! Data transmitted across this channel is vulnerable to eavesdropping.",
                    deductionPoints = 45
                )
            )
        }

        return anomalies
    }
}
