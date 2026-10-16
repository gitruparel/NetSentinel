package com.netsentinel.app.engine

import com.netsentinel.app.data.model.NetworkSnapshot

/**
 * Analyzes current NetworkSnapshot against stored network baseline vectors using defensible security reasoning.
 */
class BaselineAnalyzer {

    fun analyzeBaselineDeviation(
        snapshot: NetworkSnapshot,
        expectedGateway: String = "",
        expectedDns: String = ""
    ): List<ThreatReason> {
        val anomalies = mutableListOf<ThreatReason>()

        // If network is disconnected, no baseline deviation to analyze
        if (!snapshot.isConnected) {
            return anomalies
        }

        // 1. Gateway Drift Check
        // Only trigger if an expected baseline gateway was provided and active gateway has drifted
        if (expectedGateway.isNotEmpty() && expectedGateway != "N/A" &&
            snapshot.gatewayIp.isNotEmpty() && snapshot.gatewayIp != "N/A" &&
            snapshot.gatewayIp != expectedGateway
        ) {
            anomalies.add(
                ThreatReason(
                    type = ThreatType.ARP_SPOOFING,
                    severity = ThreatSeverity.HIGH,
                    message = "Gateway IP address changed from $expectedGateway to ${snapshot.gatewayIp}. Potential ARP spoofing or unannounced subnet reconfiguration.",
                    deductionPoints = 30
                )
            )
        }

        // 2. DNS Resolver Check
        // Trigger if:
        // a) An expected baseline DNS was provided and active resolvers do not contain it
        // OR b) The active resolver belongs to a known test/interception address (e.g. 198.51.100.42)
        val hasKnownRogueDns = snapshot.dnsServers.any { it.startsWith("198.51.100.") || it == "10.0.4.99" }
        val hasBaselineDnsDrift = expectedDns.isNotEmpty() && snapshot.dnsServers.isNotEmpty() && !snapshot.dnsServers.contains(expectedDns)

        if (hasKnownRogueDns || hasBaselineDnsDrift) {
            val reportedDns = snapshot.dnsServers.firstOrNull() ?: "Unknown"
            val detail = if (hasKnownRogueDns) {
                "Untrusted test/interception resolver detected: $reportedDns"
            } else {
                "DNS Resolver changed to $reportedDns (Expected: $expectedDns)"
            }
            anomalies.add(
                ThreatReason(
                    type = ThreatType.ROGUE_DNS,
                    severity = ThreatSeverity.CRITICAL,
                    message = "$detail. High risk of DNS redirection.",
                    deductionPoints = 40
                )
            )
        }

        // 3. Defensible RTT Latency Path Anomaly
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

        // 4. Defensible Packet Loss Connectivity Anomaly (only when connected with partial loss)
        if (snapshot.packetLossPercent > 20.0 && snapshot.packetLossPercent < 100.0) {
            anomalies.add(
                ThreatReason(
                    type = ThreatType.PACKET_JAMMING,
                    severity = ThreatSeverity.HIGH,
                    message = "Elevated packet loss (${snapshot.packetLossPercent}%) relative to baseline. Reported as a connectivity anomaly (causes: channel congestion, RF noise, or deauth/jamming frames).",
                    deductionPoints = 25
                )
            )
        }

        // 5. Security Downgrade Check (Open unencrypted network)
        val sec = snapshot.securityType.lowercase()
        if (sec.contains("open") || (sec.contains("none") && !sec.contains("disconnected"))) {
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
