package com.netsentinel.app.database.dao

import androidx.room.*
import com.netsentinel.app.database.entity.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY id DESC")
    fun getAllIncidentsFlow(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents ORDER BY id DESC")
    suspend fun getAllIncidents(): List<IncidentEntity>

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    suspend fun getIncidentById(id: String): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity)

    @Update
    suspend fun updateIncident(incident: IncidentEntity)

    @Query("UPDATE incidents SET photoUri = :photoUri WHERE id = :id")
    suspend fun updatePhotoUri(id: String, photoUri: String)

    @Query("DELETE FROM incidents")
    suspend fun deleteAllIncidents()

    @Query("SELECT COUNT(*) FROM incidents")
    fun getIncidentCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM incidents")
    suspend fun getIncidentCount(): Int
}
