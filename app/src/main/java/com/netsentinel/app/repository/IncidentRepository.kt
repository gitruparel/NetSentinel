package com.netsentinel.app.repository

import com.netsentinel.app.data.model.Fingerprint
import com.netsentinel.app.data.model.Incident
import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.data.model.TimelineEvent
import com.netsentinel.app.engine.ThreatSeverity

interface IncidentRepository {
    fun getAllIncidents(): List<Incident>
    fun getIncidentById(id: String): Incident?
    fun addIncident(incident: Incident)
}

class FakeIncidentRepository : IncidentRepository {

    private val sampleSnapshot = NetworkSnapshot(
        ssid = "CyberCorp_5G_Secured",
        bssid = "DE:AD:BE:EF:42:01",
        rssi = -38,
        gatewayIp = "10.0.4.99",
        dnsServers = listOf("198.51.100.42"),
        frequencyMhz = 5240,
        securityType = "WPA2-Personal (Downgraded)",
        rttMs = 380L,
        packetLossPercent = 18.5
    )

    private val sampleFingerprint = Fingerprint(
        bssid = "DE:AD:BE:EF:42:01",
        ssid = "CyberCorp_5G_Secured",
        ouiVendor = "Hak5 Pineapple NANO",
        hashSignature = "SHA256-E99A04B1C28D",
        capabilityMask = 0x09,
        isKnownBaseline = false
    )

    private val incidentsList = mutableListOf(
        Incident(
            id = "INC-8942",
            auditSessionId = "AUDIT-042",
            title = "Evil Twin AP Rogue Clone",
            timestampFormatted = "10:42 AM Today",
            threatScore = 88,
            severity = ThreatSeverity.CRITICAL,
            status = "QUARANTINED",
            aiAnalysis = "NetTrust AI Engine detected duplicate SSID 'CyberCorp_5G_Secured' broadcasting with unauthorized OUI vendor bytes (Hak5). Beacon frames confirm deauth injection sequence.",
            networkDetails = sampleSnapshot,
            fingerprint = sampleFingerprint,
            timeline = listOf(
                TimelineEvent("10:41:50 AM", "Initial Wi-Fi baseline scan executed", "NORMAL"),
                TimelineEvent("10:42:01 AM", "Duplicate SSID detected with mismatched MAC vendor", "ALERT"),
                TimelineEvent("10:42:04 AM", "DNS resolution redirected to 198.51.100.42", "CRITICAL"),
                TimelineEvent("10:42:05 AM", "NetTrust Engine quarantined AP & logged incident evidence", "QUARANTINED")
            )
        ),
        Incident(
            id = "INC-8939",
            auditSessionId = "AUDIT-041",
            title = "ARP Gateway Poisoning Attempt",
            timestampFormatted = "Yesterday 11:15 PM",
            threatScore = 74,
            severity = ThreatSeverity.HIGH,
            status = "ACTIVE INVESTIGATION",
            aiAnalysis = "Gateway MAC mapping changed unexpectedly from 00:1A:2B to 00:11:22 without DHCP lease renegotiation. Potential ARP cache poisoning detected.",
            networkDetails = sampleSnapshot.copy(gatewayIp = "192.168.1.254", rssi = -58),
            fingerprint = sampleFingerprint.copy(ouiVendor = "Ubiquiti Networks"),
            timeline = listOf(
                TimelineEvent("11:14:00 PM", "Gateway ARP sweep started", "NORMAL"),
                TimelineEvent("11:15:12 PM", "Gateway IP reassigned to unverified MAC", "ALERT")
            )
        ),
        Incident(
            id = "INC-8921",
            auditSessionId = "AUDIT-038",
            title = "RF Packet Loss & Jamming Spike",
            timestampFormatted = "Aug 05, 2026",
            threatScore = 45,
            severity = ThreatSeverity.MEDIUM,
            status = "RESOLVED",
            aiAnalysis = "High 2.4GHz RF interference caused 32% frame drops. Deauth broadcast attack suspected, resolved after channel switch.",
            networkDetails = sampleSnapshot.copy(packetLossPercent = 32.0),
            fingerprint = sampleFingerprint,
            timeline = listOf(
                TimelineEvent("02:10:00 PM", "Channel packet loss exceeded threshold", "ALERT"),
                TimelineEvent("02:12:00 PM", "Audit completed, interference cleared", "RESOLVED")
            )
        )
    )

    override fun getAllIncidents(): List<Incident> = incidentsList

    override fun getIncidentById(id: String): Incident? = incidentsList.find { it.id == id }

    override fun addIncident(incident: Incident) {
        incidentsList.add(0, incident)
    }
}
