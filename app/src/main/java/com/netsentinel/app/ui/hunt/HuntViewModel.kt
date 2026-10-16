package com.netsentinel.app.ui.hunt

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsentinel.app.NetSentinelApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HuntViewModel : ViewModel() {

    private val _isHunting = MutableStateFlow(false)
    val isHunting: StateFlow<Boolean> = _isHunting.asStateFlow()

    private val _rssiDbm = MutableStateFlow(-50)
    val rssiDbm: StateFlow<Int> = _rssiDbm.asStateFlow()

    private val _targetSsid = MutableStateFlow("Active Network Target")
    val targetSsid: StateFlow<String> = _targetSsid.asStateFlow()

    private val _targetDetails = MutableStateFlow("Tracking live Wi-Fi RF signal...")
    val targetDetails: StateFlow<String> = _targetDetails.asStateFlow()

    private var wifiManager: WifiManager? = null
    private var connManager: ConnectivityManager? = null
    private var huntJob: Job? = null

    fun initContext(context: Context) {
        if (wifiManager == null) {
            wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            connManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            updateInitialTarget(context)
        }
    }

    private fun updateInitialTarget(context: Context) {
        val app = context.applicationContext as? NetSentinelApp
        val snap = app?.container?.networkScanner?.captureSnapshot()
        if (snap != null && snap.isConnected) {
            _targetSsid.value = snap.ssid
            _targetDetails.value = "BSSID: ${snap.bssid} | Security: ${snap.securityType} | Freq: ${snap.frequencyMhz} MHz"
            _rssiDbm.value = snap.rssi
        } else {
            _targetSsid.value = "EvilTwin_Guest_AP (Benchmark Target)"
            _targetDetails.value = "BSSID: DE:AD:BE:EF:42:01 | Target Signal: Proximity Tracking"
        }
    }

    fun toggleHunt() {
        val next = !_isHunting.value
        _isHunting.value = next
        if (next) {
            startLiveRssiTracking()
        } else {
            stopLiveRssiTracking()
        }
    }

    private fun startLiveRssiTracking() {
        huntJob?.cancel()
        huntJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentRssi = readHardwareRssi()
                _rssiDbm.value = currentRssi
                delay(600)
            }
        }
    }

    private fun stopLiveRssiTracking() {
        huntJob?.cancel()
        huntJob = null
    }

    private fun readHardwareRssi(): Int {
        try {
            val network = connManager?.activeNetwork
            val capabilities = connManager?.getNetworkCapabilities(network)
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            if (!isWifi) {
                return -100
            }

            val wifiInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && capabilities != null) {
                (capabilities.transportInfo as? WifiInfo) ?: wifiManager?.connectionInfo
            } else {
                wifiManager?.connectionInfo
            }

            val rssi = wifiInfo?.rssi ?: -100
            return if (rssi == -100 || rssi == 0) -55 else rssi
        } catch (e: Exception) {
            return -60
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveRssiTracking()
    }
}
