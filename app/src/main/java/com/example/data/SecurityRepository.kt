package com.example.data

import kotlinx.coroutines.flow.Flow

class SecurityRepository(private val securityDao: SecurityDao) {

    val allLogs: Flow<List<SecurityLog>> = securityDao.getAllLogsFlow()
    val allSettings: Flow<List<SecuritySetting>> = securityDao.getAllSettingsFlow()

    suspend fun insertLog(methodName: String, status: String, details: String) {
        val log = SecurityLog(
            methodName = methodName,
            status = status,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        securityDao.insertLog(log)
    }

    suspend fun clearAllLogs() {
        securityDao.clearAllLogs()
    }

    suspend fun getSettingValue(key: String): String? {
        return securityDao.getSettingValue(key)
    }

    suspend fun saveSetting(key: String, value: String) {
        securityDao.insertSetting(SecuritySetting(key, value))
    }

    suspend fun deleteSetting(key: String) {
        securityDao.deleteSetting(key)
    }
}
