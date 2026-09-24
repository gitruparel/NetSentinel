package com.netsentinel.app.network

import com.netsentinel.app.data.model.NetworkSnapshot

interface WifiCollector {
    fun getCurrentWifiState(): NetworkSnapshot
}

class FakeWifiCollector : WifiCollector {
    override fun getCurrentWifiState(): NetworkSnapshot {
        return NetworkSnapshot(
            ssid = "CyberCorp_5G_Secured",
            bssid = "00:1A:2B:3C:4D:5E",
            rssi = -42,
            gatewayIp = "192.168.1.1",
            dnsServers = listOf("1.1.1.1", "8.8.8.8"),
            frequencyMhz = 5180,
            securityType = "WPA3-Enterprise",
            rttMs = 12L,
            packetLossPercent = 0.0,
            timestampMs = System.currentTimeMillis()
        )
    }
}
