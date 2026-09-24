package com.netsentinel.app.network

interface ConnectivityCollector {
    fun getGatewayAndDns(): Pair<String, List<String>>
}

class FakeConnectivityCollector : ConnectivityCollector {
    override fun getGatewayAndDns(): Pair<String, List<String>> {
        return Pair("192.168.1.1", listOf("1.1.1.1", "8.8.8.8"))
    }
}
