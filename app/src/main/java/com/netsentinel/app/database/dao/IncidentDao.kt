package com.netsentinel.app.database.dao

import androidx.room.*
import com.netsentinel.app.database.entity.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    // =========================================================================
    // 1. CREATE (SQL INSERT)
    // =========================================================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity)

    // =========================================================================
    // 2. READ (SQL SELECT QUERIES)
    // =========================================================================
    @Query("SELECT * FROM incidents ORDER BY id DESC")
    fun getAllIncidentsFlow(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents ORDER BY id DESC")
    suspend fun getAllIncidents(): List<IncidentEntity>

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    suspend fun getIncidentById(id: String): IncidentEntity?

    @Query("SELECT * FROM incidents WHERE severity = :severity ORDER BY threatScore DESC")
    suspend fun getIncidentsBySeverity(severity: String): List<IncidentEntity>

    @Query("SELECT COUNT(*) FROM incidents")
    fun getIncidentCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM incidents")
    suspend fun getIncidentCount(): Int

    // =========================================================================
    // 3. UPDATE (SQL UPDATE QUERIES)
    // =========================================================================
    @Update
    suspend fun updateIncident(incident: IncidentEntity)

    @Query("UPDATE incidents SET photoUri = :photoUri WHERE id = :id")
    suspend fun updatePhotoUri(id: String, photoUri: String)

    @Query("UPDATE incidents SET status = :newStatus WHERE id = :id")
    suspend fun updateIncidentStatus(id: String, newStatus: String)

    // =========================================================================
    // 4. DELETE (SQL DELETE QUERIES)
    // =========================================================================
    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncidentById(id: String)

    @Query("DELETE FROM incidents")
    suspend fun deleteAllIncidents()
}
