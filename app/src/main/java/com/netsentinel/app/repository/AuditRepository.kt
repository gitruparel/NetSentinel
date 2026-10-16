package com.netsentinel.app.repository

import com.netsentinel.app.data.model.AuditSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface AuditRepository {
    fun observeAuditSession(): Flow<AuditSession>
    suspend fun getCurrentAuditSession(): AuditSession
    suspend fun updateAuditSession(session: AuditSession)
}

class FakeAuditRepository(
    private val networkRepository: NetworkRepository = FakeNetworkRepository()
) : AuditRepository {

    private val currentSnapshot = networkRepository.getNetworkSnapshot()
    private val sessionState = MutableStateFlow(
        AuditSession(
            id = "AUDIT-042",
            startTimeMs = System.currentTimeMillis() - 3600000,
            initialSnapshot = currentSnapshot,
            trustScore = 92,
            detectedThreats = emptyList()
        )
    )

    override suspend fun getCurrentAuditSession(): AuditSession = sessionState.value

    override fun observeAuditSession(): Flow<AuditSession> = sessionState

    override suspend fun updateAuditSession(session: AuditSession) {
        sessionState.value = session
    }
}
