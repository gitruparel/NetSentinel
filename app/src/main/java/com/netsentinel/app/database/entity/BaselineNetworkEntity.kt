package com.netsentinel.app.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "baseline_networks")
data class BaselineNetworkEntity(
    @PrimaryKey val bssid: String,
    val ssid: String,
    val expectedGateway: String,
    val expectedDnsJson: String,
    val expectedSecurity: String,
    val lastSeenMs: Long = System.currentTimeMillis()
)
