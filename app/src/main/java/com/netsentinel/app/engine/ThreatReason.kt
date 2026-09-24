package com.netsentinel.app.engine

enum class ThreatSeverity {
    SECURE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class ThreatType {
    NONE,
    EVIL_TWIN,
    ARP_SPOOFING,
    ROGUE_DNS,
    LATENCY_HIJACK,
    PACKET_JAMMING,
    SECURITY_DOWNGRADE
}

data class ThreatReason(
    val type: ThreatType,
    val severity: ThreatSeverity,
    val message: String,
    val deductionPoints: Int
)
