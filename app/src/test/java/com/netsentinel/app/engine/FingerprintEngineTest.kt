package com.netsentinel.app.engine

import com.netsentinel.app.data.model.NetworkSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FingerprintEngineTest {

    private lateinit var fingerprintEngine: FingerprintEngine

    @Before
    fun setup() {
        fingerprintEngine = FingerprintEngine()
    }

    private fun snapshotForMac(bssid: String) = NetworkSnapshot(
        ssid = "TestNet",
        bssid = bssid,
        rssi = -55,
        gatewayIp = "192.168.1.1",
        dnsServers = listOf("1.1.1.1"),
        frequencyMhz = 5240,
        securityType = "WPA3-Enterprise",
        rttMs = 15L,
        packetLossPercent = 0.0
    )

    @Test
    fun parseOuiVendor_ciscoMac_resolvesCisco() {
        val fp = fingerprintEngine.generateFingerprint(snapshotForMac("00:1A:2B:AA:BB:CC"))
        assertEquals("Cisco Systems Enterprise", fp.ouiVendor)
    }

    @Test
    fun parseOuiVendor_ubiquitiMac_resolvesUbiquiti() {
        val fp = fingerprintEngine.generateFingerprint(snapshotForMac("74:83:C2:11:22:33"))
        assertEquals("Ubiquiti Networks", fp.ouiVendor)
    }

    @Test
    fun parseOuiVendor_hak5Mac_resolvesRogueHardware() {
        val fp = fingerprintEngine.generateFingerprint(snapshotForMac("DE:AD:BE:EF:42:01"))
        assertEquals("Rogue Hardware (Hak5 Pineapple)", fp.ouiVendor)
    }

    @Test
    fun parseOuiVendor_appleMac_resolvesApple() {
        val fp = fingerprintEngine.generateFingerprint(snapshotForMac("F4:92:BF:33:44:55"))
        assertEquals("Apple Inc.", fp.ouiVendor)
    }

    @Test
    fun evaluateApVector_knownBaseline_returnsClean() {
        val snapshot = snapshotForMac("00:1A:2B:11:22:33")
        val assessment = fingerprintEngine.evaluateApVector(
            snapshot = snapshot,
            knownBssidsForSsid = listOf("00:1A:2B:11:22:33"),
            baselineGateway = "192.168.1.1",
            baselineSecurity = "WPA3-Enterprise"
        )
        assertFalse(assessment.isSuspicious)
        assertEquals("CLEAN", assessment.confidenceLevel)
        assertEquals(0, assessment.deductionPoints)
    }

    @Test
    fun evaluateApVector_newBssidWithSecurityDowngrade_flagsSuspiciousRogue() {
        val snapshot = snapshotForMac("DE:AD:BE:EF:42:01").copy(securityType = "WPA2-Personal")
        val assessment = fingerprintEngine.evaluateApVector(
            snapshot = snapshot,
            knownBssidsForSsid = listOf("00:1A:2B:11:22:33"),
            baselineGateway = "192.168.1.1",
            baselineSecurity = "WPA3-Enterprise"
        )
        assertTrue(assessment.isSuspicious)
        assertEquals("HIGH", assessment.confidenceLevel)
        assertEquals(50, assessment.deductionPoints)
    }

    @Test
    fun evaluateApVector_legitimateEnterpriseExpansion_returnsLowDeduction() {
        val snapshot = snapshotForMac("00:1A:2B:99:88:77")
        val assessment = fingerprintEngine.evaluateApVector(
            snapshot = snapshot,
            knownBssidsForSsid = listOf("00:1A:2B:11:22:33"),
            baselineGateway = "192.168.1.1",
            baselineSecurity = "WPA3-Enterprise"
        )
        assertFalse(assessment.isSuspicious)
        assertEquals("LOW", assessment.confidenceLevel)
        assertEquals(5, assessment.deductionPoints)
    }
}
