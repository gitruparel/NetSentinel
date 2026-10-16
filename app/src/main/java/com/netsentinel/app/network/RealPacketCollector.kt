package com.netsentinel.app.network

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Real packet & latency collector measuring gateway round-trip time (RTT) and packet drop rate.
 * Uses bounded TCP socket probes with ICMP ping fallback and truthful timeout reporting.
 */
class RealPacketCollector(
    private val defaultTargetHost: String = ""
) : PacketCollector {

    override fun measureLatencyAndLoss(): Pair<Long, Double> {
        return measureLatencyAndLoss(defaultTargetHost)
    }

    fun measureLatencyAndLoss(target: String): Pair<Long, Double> {
        val hostToPing = when {
            target.isNotEmpty() && target != "N/A" -> target
            defaultTargetHost.isNotEmpty() -> defaultTargetHost
            else -> "1.1.1.1" // Fallback public DNS target for internet path measurement
        }

        var rttMs = -1L
        var packetLoss = 100.0

        // 1. Fast TCP socket latency measurement (bounded non-blocking probe)
        val socket = Socket()
        val startTime = System.currentTimeMillis()
        try {
            // Probe port 53 (DNS) with a bounded 800ms timeout
            socket.connect(InetSocketAddress(hostToPing, 53), 800)
            val endTime = System.currentTimeMillis()
            rttMs = (endTime - startTime).coerceAtLeast(1)
            packetLoss = 0.0
            return Pair(rttMs, packetLoss)
        } catch (e: ConnectException) {
            // Port 53 refused with TCP RST, meaning host is online and responded!
            val endTime = System.currentTimeMillis()
            rttMs = (endTime - startTime).coerceAtLeast(1)
            packetLoss = 0.0
            return Pair(rttMs, packetLoss)
        } catch (e: SocketTimeoutException) {
            // Socket timed out
            rttMs = -1L
            packetLoss = 100.0
        } catch (e: Exception) {
            // Other network error
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }

        // 2. Process ICMP ping fallback if socket probe timed out
        try {
            val process = Runtime.getRuntime().exec("ping -c 2 -w 1 $hostToPing")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var totalRtt = 0L
            var count = 0

            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("time=") == true) {
                    val timeStr = line!!.substringAfter("time=").substringBefore(" ms").trim()
                    val ms = timeStr.toDoubleOrNull()?.toLong() ?: 15L
                    totalRtt += ms
                    count++
                }
            }
            process.waitFor()

            if (count > 0) {
                rttMs = totalRtt / count
                packetLoss = ((2 - count) / 2.0) * 100.0
            }
        } catch (e: Exception) {
            // Ping process failed or unavailable
        }

        // Truthful result: if unreachable, report 0ms RTT and 100% loss
        if (rttMs < 0) {
            return Pair(0L, 100.0)
        }

        return Pair(rttMs, packetLoss)
    }
}
