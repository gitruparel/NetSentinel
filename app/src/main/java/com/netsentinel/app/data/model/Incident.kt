package com.netsentinel.app.data.model

import com.netsentinel.app.engine.ThreatSeverity

/**
 * Detailed Incident record documenting security audit events and evidence.
 */
data class Incident(
    val id: String,
    val auditSessionId: String,
    val title: String,
    val timestampFormatted: String,
    val threatScore: Int,
    val severity: ThreatSeverity,
    val status: String, // e.g. "QUARANTINED", "ACTIVE INVESTIGATION", "RESOLVED"
    val aiAnalysis: String,
    val networkDetails: NetworkSnapshot,
    val fingerprint: Fingerprint,
    val timeline: List<TimelineEvent>,
    val evidencePhotoPlaceholder: String = "SPECTRUM_CAPTURE_042.PNG",
    val gpsCoordinatesFormatted: String = "37.7749° N, 122.4194° W"
)

data class TimelineEvent(
    val time: String,
    val event: String,
    val status: String
)
