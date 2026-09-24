package com.netsentinel.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_sessions")
data class AuditSessionEntity(
    @PrimaryKey val id: String,
    val startTimeMs: Long,
    val endTimeMs: Long? = null,
    val trustScore: Int,
    val ssid: String,
    val bssid: String,
    val status: String = "ACTIVE"
)
