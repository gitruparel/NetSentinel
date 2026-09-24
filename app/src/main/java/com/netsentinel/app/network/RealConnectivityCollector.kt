package com.netsentinel.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import android.text.format.Formatter

/**
 * Real Android collector resolving active Gateway IP and DNS servers from DhcpInfo & LinkProperties.
 */
class RealConnectivityCollector(
    private val context: Context
) : ConnectivityCollector {

    override fun getGatewayAndDns(): Pair<String, List<String>> {
        var gateway = "192.168.1.1"
        val dnsList = mutableListOf<String>()

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcpInfo: DhcpInfo? = wifiManager?.dhcpInfo

            if (dhcpInfo != null && dhcpInfo.gateway != 0) {
                gateway = Formatter.formatIpAddress(dhcpInfo.gateway)
                if (dhcpInfo.dns1 != 0) dnsList.add(Formatter.formatIpAddress(dhcpInfo.dns1))
                if (dhcpInfo.dns2 != 0) dnsList.add(Formatter.formatIpAddress(dhcpInfo.dns2))
            }

            val connManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = connManager?.activeNetwork
            val linkProperties = connManager?.getLinkProperties(activeNetwork)

            if (linkProperties != null) {
                val routes = linkProperties.routes
                for (route in routes) {
                    if (route.isDefaultRoute && route.gateway != null) {
                        gateway = route.gateway?.hostAddress ?: gateway
                    }
                }
                val dnsServers = linkProperties.dnsServers
                for (dns in dnsServers) {
                    val ip = dns.hostAddress
                    if (ip != null && !dnsList.contains(ip)) {
                        dnsList.add(ip)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (dnsList.isEmpty()) {
            dnsList.add("1.1.1.1")
            dnsList.add("8.8.8.8")
        }

        return Pair(gateway, dnsList)
    }
}
