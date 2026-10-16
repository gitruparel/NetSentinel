package com.netsentinel.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import java.net.InetAddress

/**
 * Real Android collector resolving active Gateway IP and DNS servers from LinkProperties & DhcpInfo.
 */
class RealConnectivityCollector(
    private val context: Context
) : ConnectivityCollector {

    override fun getGatewayAndDns(): Pair<String, List<String>> {
        var gateway = ""
        val dnsList = mutableListOf<String>()

        try {
            val connManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connManager?.activeNetwork
            val linkProperties = connManager?.getLinkProperties(activeNetwork)

            if (linkProperties != null) {
                // Read default route gateway from LinkProperties
                for (route in linkProperties.routes) {
                    if (route.isDefaultRoute && route.gateway != null) {
                        val gw = route.gateway?.hostAddress
                        if (!gw.isNullOrEmpty() && gw != "0.0.0.0" && gw != "::") {
                            gateway = gw
                            break
                        }
                    }
                }

                // Read DNS servers
                for (dns in linkProperties.dnsServers) {
                    val ip = dns.hostAddress
                    if (!ip.isNullOrEmpty() && !dnsList.contains(ip)) {
                        dnsList.add(ip)
                    }
                }
            }

            // Fallback to DhcpInfo if LinkProperties did not expose the gateway
            if (gateway.isEmpty()) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val dhcpInfo: DhcpInfo? = wifiManager?.dhcpInfo
                if (dhcpInfo != null && dhcpInfo.gateway != 0) {
                    val ipInt = dhcpInfo.gateway
                    val bytes = byteArrayOf(
                        (ipInt and 0xFF).toByte(),
                        (ipInt shr 8 and 0xFF).toByte(),
                        (ipInt shr 16 and 0xFF).toByte(),
                        (ipInt shr 24 and 0xFF).toByte()
                    )
                    val dhcpGw = InetAddress.getByAddress(bytes).hostAddress
                    if (!dhcpGw.isNullOrEmpty()) {
                        gateway = dhcpGw
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (gateway.isEmpty()) {
            gateway = "N/A"
        }

        return Pair(gateway, dnsList)
    }
}
