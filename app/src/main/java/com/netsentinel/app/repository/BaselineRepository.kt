package com.netsentinel.app.repository

import com.netsentinel.app.database.dao.BaselineDao
import com.netsentinel.app.database.entity.BaselineNetworkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface BaselineRepository {
    suspend fun getBaselineForSsid(ssid: String): BaselineNetworkEntity?
    suspend fun saveBaseline(baseline: BaselineNetworkEntity)
    suspend fun getAllBaselines(): List<BaselineNetworkEntity>
}

class RoomBaselineRepository(
    private val baselineDao: BaselineDao
) : BaselineRepository {

    override suspend fun getBaselineForSsid(ssid: String): BaselineNetworkEntity? = withContext(Dispatchers.IO) {
        baselineDao.getBaselineBySsid(ssid)
    }

    override suspend fun saveBaseline(baseline: BaselineNetworkEntity) = withContext(Dispatchers.IO) {
        baselineDao.insertBaseline(baseline)
    }

    override suspend fun getAllBaselines(): List<BaselineNetworkEntity> = withContext(Dispatchers.IO) {
        baselineDao.getAllBaselines()
    }
}
