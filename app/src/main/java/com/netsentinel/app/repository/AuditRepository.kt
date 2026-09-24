package com.netsentinel.app.repository

import com.netsentinel.app.data.model.AuditSession
import com.netsentinel.app.data.model.NetworkSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface AuditRepository {
    fun getCurrentAuditSession(): AuditSession
    fun observeAuditSession(): Flow<AuditSession>
    fun updateAuditSession(session: AuditSession)
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

    override fun getCurrentAuditSession(): AuditSession = sessionState.value

    override fun observeAuditSession(): Flow<AuditSession> = sessionState

    override fun updateAuditSession(session: AuditSession) {
        sessionState.value = session
    }
}
