package com.netsentinel.app.repository

import android.content.Context
import com.netsentinel.app.data.model.AuditSession
import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.database.FirestoreManager
import com.netsentinel.app.database.NetSentinelDatabase
import com.netsentinel.app.database.entity.AuditSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RoomAuditRepository(
    context: Context,
    private val networkRepository: NetworkRepository = FakeNetworkRepository()
) : AuditRepository {

    private val db = NetSentinelDatabase.getDatabase(context)
    private val auditDao = db.auditSessionDao()
    private val firestoreManager = FirestoreManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val fallbackState = MutableStateFlow(
        AuditSession(
            id = "AUDIT-042",
            startTimeMs = System.currentTimeMillis() - 3600000,
            initialSnapshot = networkRepository.getNetworkSnapshot(),
            trustScore = 92,
            detectedThreats = emptyList()
        )
    )

    override fun getCurrentAuditSession(): AuditSession = runBlocking(Dispatchers.IO) {
        val entity = auditDao.getLatestAuditSession()
        entity?.toDomainModel(networkRepository.getNetworkSnapshot()) ?: fallbackState.value
    }

    override fun observeAuditSession(): Flow<AuditSession> {
        return auditDao.getLatestAuditSessionFlow().map { entity ->
            entity?.toDomainModel(networkRepository.getNetworkSnapshot()) ?: fallbackState.value
        }
    }

    override fun updateAuditSession(session: AuditSession) {
        fallbackState.value = session
        scope.launch {
            auditDao.insertAuditSession(
                AuditSessionEntity(
                    id = session.id,
                    startTimeMs = session.startTimeMs,
                    endTimeMs = session.endTimeMs,
                    trustScore = session.trustScore,
                    ssid = session.initialSnapshot.ssid,
                    bssid = session.initialSnapshot.bssid,
                    status = session.status
                )
            )
            firestoreManager.saveAuditSession(session)
        }
    }

    val auditCountFlow: Flow<Int> = auditDao.getAuditCountFlow()

    fun clearAllAudits() {
        scope.launch {
            auditDao.deleteAllAuditSessions()
        }
    }

    private fun AuditSessionEntity.toDomainModel(snapshot: NetworkSnapshot): AuditSession {
        return AuditSession(
            id = this.id,
            startTimeMs = this.startTimeMs,
            endTimeMs = this.endTimeMs,
            initialSnapshot = snapshot.copy(ssid = this.ssid, bssid = this.bssid),
            trustScore = this.trustScore,
            detectedThreats = emptyList(),
            status = this.status
        )
    }
}
