package com.netsentinel.app.repository

import com.netsentinel.app.data.model.Threat
import com.netsentinel.app.engine.ThreatReason
import com.netsentinel.app.engine.ThreatSeverity
import com.netsentinel.app.engine.ThreatType

interface ThreatRepository {
    fun getActiveThreats(): List<Threat>
    fun getThreatMapPins(): List<ThreatMapPin>
}

data class ThreatMapPin(
    val id: String,
    val title: String,
    val isThreat: Boolean,
    val latitude: Double,
    val longitude: Double,
    val details: String
)

class FakeThreatRepository : ThreatRepository {
    override fun getActiveThreats(): List<Threat> {
        return listOf(
            Threat(
                id = "THR-001",
                title = "Evil Twin AP Detected",
                description = "Unverified access point cloning SSID 'CyberCorp_5G_Secured' with mismatched OUI vendor.",
                threatType = ThreatType.EVIL_TWIN,
                severity = ThreatSeverity.CRITICAL,
                threatScore = 88,
                reasons = listOf(
                    ThreatReason(ThreatType.EVIL_TWIN, ThreatSeverity.CRITICAL, "SSID Duplicate with different BSSID vendor", 50)
                )
            ),
            Threat(
                id = "THR-002",
                title = "Rogue DNS Resolver",
                description = "DNS queries intercepted and routed to untrusted resolver 198.51.100.42.",
                threatType = ThreatType.ROGUE_DNS,
                severity = ThreatSeverity.HIGH,
                threatScore = 72,
                reasons = listOf(
                    ThreatReason(ThreatType.ROGUE_DNS, ThreatSeverity.HIGH, "Rogue DNS Resolver detected", 40)
                )
            )
        )
    }

    override fun getThreatMapPins(): List<ThreatMapPin> {
        return listOf(
            ThreatMapPin("PIN-1", "Safe: Home_Mesh_5G", false, 37.7749, -122.4194, "Trust Score: 98 - WPA3 Enterprise"),
            ThreatMapPin("PIN-2", "Safe: Office_Secure_AP", false, 37.7758, -122.4182, "Trust Score: 95 - Cisco OUI"),
            ThreatMapPin("PIN-3", "THREAT: EvilTwin_Guest_AP", true, 37.7742, -122.4208, "CRITICAL: Hak5 Pineapple Clone"),
            ThreatMapPin("PIN-4", "THREAT: Rogue_ARP_Poisoner", true, 37.7735, -122.4175, "HIGH RISK: ARP Spoofing Active")
        )
    }
}
