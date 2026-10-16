package com.netsentinel.app.engine

import com.netsentinel.app.data.model.NetworkSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BaselineAnalyzerTest {

    private lateinit var analyzer: BaselineAnalyzer

    @Before
    fun setup() {
        analyzer = BaselineAnalyzer()
    }

    private fun sampleSnapshot(
        gateway: String = "192.168.1.1",
        dns: List<String> = listOf("1.1.1.1"),
        security: String = "WPA3-Enterprise",
        rttMs: Long = 14L,
        packetLoss: Double = 0.0,
        isConnected: Boolean = true
    ) = NetworkSnapshot(
        ssid = "Corporate_Secure_AP",
        bssid = "00:1A:2B:3C:4D:5E",
        rssi = -50,
        gatewayIp = gateway,
        dnsServers = dns,
        frequencyMhz = 5180,
        securityType = security,
        rttMs = rttMs,
        packetLossPercent = packetLoss,
        isConnected = isConnected
    )

    @Test
    fun analyzeBaselineDeviation_matchingBaseline_yieldsZeroAnomalies() {
        val snapshot = sampleSnapshot(gateway = "10.0.0.1", dns = listOf("10.0.0.1"))
        val anomalies = analyzer.analyzeBaselineDeviation(
            snapshot = snapshot,
            expectedGateway = "10.0.0.1",
            expectedDns = "10.0.0.1"
        )
        assertTrue(anomalies.isEmpty())
    }

    @Test
    fun analyzeBaselineDeviation_gatewayDrift_flagsArpSpoofing() {
        val snapshot = sampleSnapshot(gateway = "10.0.4.99")
        val anomalies = analyzer.analyzeBaselineDeviation(
            snapshot = snapshot,
            expectedGateway = "192.168.1.1"
        )
        assertTrue(anomalies.any { it.type == ThreatType.ARP_SPOOFING })
        assertEquals(30, anomalies.first { it.type == ThreatType.ARP_SPOOFING }.deductionPoints)
    }

    @Test
    fun analyzeBaselineDeviation_rogueDns_flagsRogueDnsResolver() {
        val snapshot = sampleSnapshot(dns = listOf("198.51.100.42"))
        val anomalies = analyzer.analyzeBaselineDeviation(
            snapshot = snapshot,
            expectedDns = "1.1.1.1"
        )
        assertTrue(anomalies.any { it.type == ThreatType.ROGUE_DNS })
        assertEquals(40, anomalies.first { it.type == ThreatType.ROGUE_DNS }.deductionPoints)
    }

    @Test
    fun analyzeBaselineDeviation_latencySpike_flagsLatencyAnomaly() {
        val snapshot = sampleSnapshot(rttMs = 380L)
        val anomalies = analyzer.analyzeBaselineDeviation(snapshot)
        assertTrue(anomalies.any { it.type == ThreatType.LATENCY_HIJACK })
        assertEquals(15, anomalies.first { it.type == ThreatType.LATENCY_HIJACK }.deductionPoints)
    }

    @Test
    fun analyzeBaselineDeviation_packetLoss_flagsJammingAnomaly() {
        val snapshot = sampleSnapshot(packetLoss = 32.0)
        val anomalies = analyzer.analyzeBaselineDeviation(snapshot)
        assertTrue(anomalies.any { it.type == ThreatType.PACKET_JAMMING })
        assertEquals(25, anomalies.first { it.type == ThreatType.PACKET_JAMMING }.deductionPoints)
    }

    @Test
    fun analyzeBaselineDeviation_openSecurity_flagsCriticalDowngrade() {
        val snapshot = sampleSnapshot(security = "Open")
        val anomalies = analyzer.analyzeBaselineDeviation(snapshot)
        assertTrue(anomalies.any { it.type == ThreatType.SECURITY_DOWNGRADE && it.severity == ThreatSeverity.CRITICAL })
        assertEquals(45, anomalies.first { it.type == ThreatType.SECURITY_DOWNGRADE }.deductionPoints)
    }

    @Test
    fun analyzeBaselineDeviation_disconnected_yieldsNoAnomalies() {
        val snapshot = sampleSnapshot(isConnected = false)
        val anomalies = analyzer.analyzeBaselineDeviation(snapshot)
        assertTrue(anomalies.isEmpty())
    }
}
