package com.netsentinel.app.database.dao

import androidx.room.*
import com.netsentinel.app.database.entity.AuditSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditSessionDao {
    @Query("SELECT * FROM audit_sessions ORDER BY startTimeMs DESC LIMIT 1")
    fun getLatestAuditSessionFlow(): Flow<AuditSessionEntity?>

    @Query("SELECT * FROM audit_sessions ORDER BY startTimeMs DESC LIMIT 1")
    suspend fun getLatestAuditSession(): AuditSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditSession(session: AuditSessionEntity)

    @Query("SELECT COUNT(*) FROM audit_sessions")
    fun getAuditCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM audit_sessions")
    suspend fun getAuditCount(): Int

    @Query("DELETE FROM audit_sessions")
    suspend fun deleteAllAuditSessions()
}
