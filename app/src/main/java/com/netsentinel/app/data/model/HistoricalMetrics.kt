package com.netsentinel.app.data.model

data class HistoricalMetrics(
    val lastScanTimeFormatted: String = "2 mins ago",
    val totalAuditsCompleted: Int = 142,
    val totalThreatsNeutralized: Int = 19,
    val averageTrustScore: Int = 89
)
