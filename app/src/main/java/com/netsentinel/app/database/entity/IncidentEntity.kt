package com.netsentinel.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val auditSessionId: String,
    val title: String,
    val timestampFormatted: String,
    val threatScore: Int,
    val severity: String,
    val status: String,
    val aiAnalysis: String,
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val gatewayIp: String,
    val dnsServersJson: String,
    val securityType: String,
    val ouiVendor: String,
    val photoUri: String? = null,
    val gpsCoordinatesFormatted: String = "37.7749° N, 122.4194° W"
)
