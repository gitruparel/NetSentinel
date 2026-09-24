package com.netsentinel.app.data.model

/**
 * Represents a canonical physical and network snapshot captured by collectors.
 */
data class NetworkSnapshot(
    val ssid: String,
    val bssid: String,
    val rssi: Int,               // dBm
    val gatewayIp: String,
    val dnsServers: List<String>,
    val frequencyMhz: Int,
    val securityType: String,    // e.g. WPA3-Enterprise, WPA2-Personal, Open
    val rttMs: Long,             // Round Trip Time latency
    val packetLossPercent: Double,
    val timestampMs: Long = System.currentTimeMillis(),
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194
) {
    val signalQualityPercentage: Int
        get() = when {
            rssi >= -50 -> 100
            rssi <= -100 -> 0
            else -> 2 * (rssi + 100)
        }
}
