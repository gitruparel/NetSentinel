package com.netsentinel.app.database.dao

import androidx.room.*
import com.netsentinel.app.database.entity.BaselineNetworkEntity

@Dao
interface BaselineDao {
    @Query("SELECT * FROM baseline_networks WHERE ssid = :ssid LIMIT 1")
    suspend fun getBaselineBySsid(ssid: String): BaselineNetworkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaseline(baseline: BaselineNetworkEntity)

    @Query("SELECT * FROM baseline_networks")
    suspend fun getAllBaselines(): List<BaselineNetworkEntity>

    @Query("DELETE FROM baseline_networks")
    suspend fun deleteAllBaselines()
}
