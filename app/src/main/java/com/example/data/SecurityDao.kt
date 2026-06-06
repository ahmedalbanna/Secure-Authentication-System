package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityDao {
    // Security Logs Queries
    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<SecurityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLog)

    @Query("DELETE FROM security_logs")
    suspend fun clearAllLogs()

    // Security Settings Queries
    @Query("SELECT * FROM security_settings")
    fun getAllSettingsFlow(): Flow<List<SecuritySetting>>

    @Query("SELECT value FROM security_settings WHERE `key` = :settingKey LIMIT 1")
    suspend fun getSettingValue(settingKey: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SecuritySetting)

    @Query("DELETE FROM security_settings WHERE `key` = :settingKey")
    suspend fun deleteSetting(settingKey: String)
}
