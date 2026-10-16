package com.netsentinel.app.network

import android.content.Context
import com.netsentinel.app.data.model.NetworkSnapshot

/**
 * Orchestrates collection of NetworkSnapshot vectors across Wifi, Packet, and Connectivity collectors.
 */
class NetworkScanner(
    private val wifiCollector: WifiCollector,
    private val packetCollector: PacketCollector,
    private val connectivityCollector: ConnectivityCollector
) {
    constructor(context: Context) : this(
        wifiCollector = RealWifiCollector(context),
        connectivityCollector = RealConnectivityCollector(context),
        packetCollector = RealPacketCollector()
    )

    constructor() : this(
        wifiCollector = FakeWifiCollector(),
        packetCollector = FakePacketCollector(),
        connectivityCollector = FakeConnectivityCollector()
    )

    fun captureSnapshot(): NetworkSnapshot {
        val baseState = wifiCollector.getCurrentWifiState()
        val (gateway, dns) = connectivityCollector.getGatewayAndDns()

        val (rtt, packetLoss) = if (!baseState.isConnected) {
            Pair(0L, 100.0)
        } else if (packetCollector is RealPacketCollector) {
            packetCollector.measureLatencyAndLoss(gateway)
        } else {
            packetCollector.measureLatencyAndLoss()
        }

        return baseState.copy(
            gatewayIp = if (baseState.isConnected) gateway else "N/A",
            dnsServers = if (baseState.isConnected) dns else emptyList(),
            rttMs = rtt,
            packetLossPercent = packetLoss,
            timestampMs = System.currentTimeMillis()
        )
    }
}
