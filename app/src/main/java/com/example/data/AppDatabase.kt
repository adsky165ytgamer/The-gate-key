package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "audit_logs")
data class AuditLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val operation: String, // ENCRYPT, DECRYPT, RECOVERY, MIGRATION, KEY_VIEW
    val filename: String,
    val algorithm: String,
    val envelopeUsed: String,
    val status: String, // SUCCESS, FAILURE, CANCELLED
    val details: String
)

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: AuditLogEntry): Long

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogEntry>>

    @Query("DELETE FROM audit_logs")
    suspend fun clearAllLogs()
}

@Database(entities = [AuditLogEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adsky_vault.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
