package com.netsentinel.app.repository

import android.content.Context
import com.netsentinel.app.data.model.AuditSession
import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.database.NetSentinelDatabase
import com.netsentinel.app.database.SupabaseManager
import com.netsentinel.app.database.entity.AuditSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoomAuditRepository(
    context: Context,
    private val networkRepository: NetworkRepository = FakeNetworkRepository()
) : AuditRepository {

    private val db = NetSentinelDatabase.getDatabase(context)
    private val auditDao = db.auditSessionDao()
    private val supabaseManager = SupabaseManager(context)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val fallbackState = MutableStateFlow(
        AuditSession(
            id = "AUDIT-042",
            startTimeMs = System.currentTimeMillis() - 3600000,
            initialSnapshot = networkRepository.getNetworkSnapshot(),
            trustScore = 92,
            detectedThreats = emptyList()
        )
    )

    override suspend fun getCurrentAuditSession(): AuditSession = withContext(Dispatchers.IO) {
        val entity = auditDao.getLatestAuditSession()
        entity?.toDomainModel(networkRepository.getNetworkSnapshot()) ?: fallbackState.value
    }

    override fun observeAuditSession(): Flow<AuditSession> {
        return auditDao.getLatestAuditSessionFlow().map { entity ->
            entity?.toDomainModel(networkRepository.getNetworkSnapshot()) ?: fallbackState.value
        }
    }

    override suspend fun updateAuditSession(session: AuditSession): Unit = withContext(Dispatchers.IO) {
        fallbackState.value = session
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
        repositoryScope.launch {
            try {
                supabaseManager.saveAuditSession(session)
            } catch (_: Exception) {}
        }
        Unit
    }

    val auditCountFlow: Flow<Int> = auditDao.getAuditCountFlow()

    suspend fun clearAllAudits(): Unit = withContext(Dispatchers.IO) {
        auditDao.deleteAllAuditSessions()
        Unit
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
