package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_settings")
data class SecuritySetting(
    @PrimaryKey val key: String,
    val value: String
)
