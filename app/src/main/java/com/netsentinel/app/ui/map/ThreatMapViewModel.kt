package com.netsentinel.app.ui.map

import androidx.lifecycle.ViewModel
import com.netsentinel.app.repository.FakeThreatRepository
import com.netsentinel.app.repository.ThreatMapPin
import com.netsentinel.app.repository.ThreatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThreatMapViewModel(
    private val threatRepository: ThreatRepository = FakeThreatRepository()
) : ViewModel() {

    private val _pins = MutableStateFlow<List<ThreatMapPin>>(emptyList())
    val pins: StateFlow<List<ThreatMapPin>> = _pins.asStateFlow()

    init {
        _pins.value = threatRepository.getThreatMapPins()
    }

    fun updatePinsNearLocation(lat: Double, lng: Double) {
        val dynamicPins = listOf(
            ThreatMapPin("PIN-1", "Safe: Home_Mesh_5G", false, lat + 0.0012, lng + 0.0008, "Trust Score: 98 - WPA3 Enterprise"),
            ThreatMapPin("PIN-2", "Safe: Office_Secure_AP", false, lat - 0.0009, lng - 0.0011, "Trust Score: 95 - Cisco OUI"),
            ThreatMapPin("PIN-3", "THREAT: EvilTwin_Guest_AP", true, lat + 0.0021, lng - 0.0014, "CRITICAL: Hak5 Pineapple Clone"),
            ThreatMapPin("PIN-4", "THREAT: Rogue_ARP_Poisoner", true, lat - 0.0018, lng + 0.0019, "HIGH RISK: ARP Spoofing Active")
        )
        _pins.value = dynamicPins
    }
}
