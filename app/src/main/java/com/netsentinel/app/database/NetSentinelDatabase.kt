package com.netsentinel.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.netsentinel.app.database.dao.AuditSessionDao
import com.netsentinel.app.database.dao.BaselineDao
import com.netsentinel.app.database.dao.IncidentDao
import com.netsentinel.app.database.entity.AuditSessionEntity
import com.netsentinel.app.database.entity.BaselineNetworkEntity
import com.netsentinel.app.database.entity.IncidentEntity

@Database(
    entities = [IncidentEntity::class, AuditSessionEntity::class, BaselineNetworkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NetSentinelDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao
    abstract fun auditSessionDao(): AuditSessionDao
    abstract fun baselineDao(): BaselineDao

    companion object {
        @Volatile
        private var INSTANCE: NetSentinelDatabase? = null

        fun getDatabase(context: Context): NetSentinelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NetSentinelDatabase::class.java,
                    "netsentinel_security_db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
