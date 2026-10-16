package com.netsentinel.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.netsentinel.app.database.dao.AuditSessionDao
import com.netsentinel.app.database.dao.BaselineDao
import com.netsentinel.app.database.dao.IncidentDao
import com.netsentinel.app.database.entity.AuditSessionEntity
import com.netsentinel.app.database.entity.BaselineNetworkEntity
import com.netsentinel.app.database.entity.IncidentEntity

@Database(
    entities = [IncidentEntity::class, AuditSessionEntity::class, BaselineNetworkEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NetSentinelDatabase : RoomDatabase() {

    abstract fun incidentDao(): IncidentDao
    abstract fun auditSessionDao(): AuditSessionDao
    abstract fun baselineDao(): BaselineDao

    companion object {
        @Volatile
        private var INSTANCE: NetSentinelDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE incidents ADD COLUMN frequencyMhz INTEGER NOT NULL DEFAULT 2412")
                db.execSQL("ALTER TABLE incidents ADD COLUMN rttMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE incidents ADD COLUMN packetLossPercent REAL NOT NULL DEFAULT 0.0")
            }
        }

        fun getDatabase(context: Context): NetSentinelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NetSentinelDatabase::class.java,
                    "netsentinel_security_db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
