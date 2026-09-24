package com.netsentinel.app.data.model

/**
 * Represents a complete security audit session grouping snapshots, threats, & overall trust score.
 */
data class AuditSession(
    val id: String,
    val startTimeMs: Long,
    var endTimeMs: Long? = null,
    val initialSnapshot: NetworkSnapshot,
    val trustScore: Int,
    val detectedThreats: List<Threat>,
    val status: String = "ACTIVE" // ACTIVE, COMPLETED, THREAT_DETECTED
)
