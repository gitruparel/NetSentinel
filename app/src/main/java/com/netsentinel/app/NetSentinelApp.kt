package com.netsentinel.app

import android.app.Application
import com.netsentinel.app.database.NetSentinelDatabase
import com.netsentinel.app.location.GpsLocationCollector
import com.netsentinel.app.network.NetworkScanner
import com.netsentinel.app.repository.BaselineRepository
import com.netsentinel.app.repository.FakeThreatRepository
import com.netsentinel.app.repository.IncidentRepository
import com.netsentinel.app.repository.RoomAuditRepository
import com.netsentinel.app.repository.RoomBaselineRepository
import com.netsentinel.app.repository.RoomIncidentRepository
import com.netsentinel.app.repository.ThreatRepository

/**
 * Lightweight dependency container providing singleton application-scoped components.
 */
class AppContainer(application: Application) {
    val database: NetSentinelDatabase by lazy {
        NetSentinelDatabase.getDatabase(application)
    }

    val networkScanner: NetworkScanner by lazy {
        NetworkScanner(application)
    }

    val incidentRepository: IncidentRepository by lazy {
        RoomIncidentRepository(application)
    }

    val auditRepository: RoomAuditRepository by lazy {
        RoomAuditRepository(application)
    }

    val baselineRepository: BaselineRepository by lazy {
        RoomBaselineRepository(database.baselineDao())
    }

    val threatRepository: ThreatRepository by lazy {
        FakeThreatRepository()
    }

    val locationCollector: GpsLocationCollector by lazy {
        GpsLocationCollector(application)
    }
}

class NetSentinelApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
