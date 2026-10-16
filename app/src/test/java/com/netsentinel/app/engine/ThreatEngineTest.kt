package com.netsentinel.app.engine

import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.data.model.SimulationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ThreatEngineTest {

    private lateinit var threatEngine: ThreatEngine

    @Before
    fun setup() {
        threatEngine = ThreatEngine()
    }

    private fun createSafeSnapshot(
        gateway: String = "192.168.0.1",
        dns: String = "8.8.8.8",
        security: String = "WPA3-Enterprise"
    ): NetworkSnapshot {
        return NetworkSnapshot(
            ssid = "Test_Office_Mesh",
            bssid = "00:1A:2B:3C:4D:5E",
            rssi = -45,
            gatewayIp = gateway,
            dnsServers = listOf(dns),
            frequencyMhz = 5180,
            securityType = security,
            rttMs = 12L,
            packetLossPercent = 0.0,
            isConnected = true
        )
    }

    @Test
    fun evaluateNetwork_cleanNetwork_returnsHighTrustScoreWithoutFalsePositives() {
        // Any standard gateway (e.g. 192.168.0.1) should NOT be flagged as ARP spoofing
        val snapshot = createSafeSnapshot(gateway = "192.168.0.1", dns = "192.168.0.1")
        val result = threatEngine.evaluateNetwork(snapshot)

        assertEquals(100, result.trustScore)
        assertEquals("SECURE NETWORK", result.statusSummary)
        assertTrue(result.reasons.isEmpty())
    }

    @Test
    fun evaluateNetwork_openHotspot_flagsSecurityDowngradeCritical() {
        val snapshot = createSafeSnapshot(security = "Open")
        val result = threatEngine.evaluateNetwork(snapshot)

        assertTrue(result.trustScore <= 55)
        assertTrue(result.reasons.any { it.type == ThreatType.SECURITY_DOWNGRADE && it.severity == ThreatSeverity.CRITICAL })
        assertEquals(45, result.reasons.first { it.type == ThreatType.SECURITY_DOWNGRADE }.deductionPoints)
    }

    @Test
    fun evaluateNetwork_simulationEvilTwin_appliesCriticalPenalty() {
        val snapshot = createSafeSnapshot()
        val simState = SimulationState(isDuplicateSsidActive = true)
        val result = threatEngine.evaluateNetwork(snapshot, simState)

        assertTrue(result.reasons.any { it.type == ThreatType.EVIL_TWIN })
        assertEquals("DE:AD:BE:EF:42:01", result.evaluatedSnapshot.bssid)
        assertTrue(result.trustScore <= 50)
    }

    @Test
    fun evaluateNetwork_simulationGatewayDrift_appliesArpSpoofingPenalty() {
        val snapshot = createSafeSnapshot(gateway = "192.168.1.1")
        val simState = SimulationState(isGatewayChangeActive = true)
        val result = threatEngine.evaluateNetwork(snapshot, simState, expectedGateway = "192.168.1.1")

        assertTrue(result.reasons.any { it.type == ThreatType.ARP_SPOOFING })
        assertEquals(30, result.reasons.first { it.type == ThreatType.ARP_SPOOFING }.deductionPoints)
    }

    @Test
    fun evaluateNetwork_simulationRogueDns_appliesRogueDnsPenalty() {
        val snapshot = createSafeSnapshot(dns = "1.1.1.1")
        val simState = SimulationState(isDnsChangeActive = true)
        val result = threatEngine.evaluateNetwork(snapshot, simState, expectedDns = "1.1.1.1")

        assertTrue(result.reasons.any { it.type == ThreatType.ROGUE_DNS })
        assertEquals(40, result.reasons.first { it.type == ThreatType.ROGUE_DNS }.deductionPoints)
    }

    @Test
    fun evaluateNetwork_disconnectedState_reportsNoActiveConnectionTruthfully() {
        val snapshot = NetworkSnapshot(
            ssid = "Wi-Fi Disconnected",
            bssid = "00:00:00:00:00:00",
            rssi = -100,
            gatewayIp = "N/A",
            dnsServers = emptyList(),
            frequencyMhz = 0,
            securityType = "None (Disconnected)",
            rttMs = 0L,
            packetLossPercent = 100.0,
            isConnected = false
        )
        val result = threatEngine.evaluateNetwork(snapshot)

        assertEquals("NO ACTIVE CONNECTION", result.statusSummary)
        assertTrue(result.reasons.isEmpty())
        assertFalse(result.evaluatedSnapshot.isConnected)
    }
}
