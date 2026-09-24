package com.netsentinel.app.network

interface PacketCollector {
    fun measureLatencyAndLoss(): Pair<Long, Double>
}

class FakePacketCollector : PacketCollector {
    override fun measureLatencyAndLoss(): Pair<Long, Double> {
        return Pair(14L, 0.2)
    }
}
