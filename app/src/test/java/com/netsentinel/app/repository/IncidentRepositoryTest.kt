package com.netsentinel.app.repository

import com.netsentinel.app.data.model.Fingerprint
import com.netsentinel.app.data.model.Incident
import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.engine.ThreatSeverity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IncidentRepositoryTest {

    private lateinit var repository: FakeIncidentRepository

    @Before
    fun setup() {
        repository = FakeIncidentRepository()
    }

    @Test
    fun getAllIncidents_returnsPrepopulatedBenchmarkIncidents() = runBlocking {
        val list = repository.getAllIncidents()
        assertTrue(list.isNotEmpty())
        assertEquals(3, list.size)
        assertTrue(list.any { it.id == "INC-8942" })
    }

    @Test
    fun getIncidentById_existingId_returnsIncident() = runBlocking {
        val incident = repository.getIncidentById("INC-8942")
        assertNotNull(incident)
        assertEquals("Evil Twin AP Rogue Clone", incident?.title)
        assertEquals(ThreatSeverity.CRITICAL, incident?.severity)
    }

    @Test
    fun addIncident_prependsToStream() = runBlocking {
        val newIncident = Incident(
            id = "INC-TEST-001",
            auditSessionId = "AUDIT-TEST",
            title = "Test Anomaly Insertion",
            timestampFormatted = "Just now",
            threatScore = 75,
            severity = ThreatSeverity.HIGH,
            status = "FLAGGED",
            aiAnalysis = "Test AI analysis",
            networkDetails = NetworkSnapshot(
                ssid = "TestNet",
                bssid = "00:11:22:33:44:55",
                rssi = -60,
                gatewayIp = "192.168.1.1",
                dnsServers = listOf("1.1.1.1"),
                frequencyMhz = 5200,
                securityType = "WPA3",
                rttMs = 12L,
                packetLossPercent = 0.0
            ),
            fingerprint = Fingerprint(
                bssid = "00:11:22:33:44:55",
                ssid = "TestNet",
                ouiVendor = "Ubiquiti",
                hashSignature = "SHA256-TEST",
                capabilityMask = 0x05
            ),
            timeline = emptyList()
        )

        repository.addIncident(newIncident)

        val latestList = repository.observeIncidents().first()
        assertEquals(4, latestList.size)
        assertEquals("INC-TEST-001", latestList.first().id)
    }
}
