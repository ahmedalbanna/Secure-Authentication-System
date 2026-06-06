package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_logs")
data class SecurityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val methodName: String,       // e.g. "بصمة الإصبع", "التعرف على الوجه", "بصمة العين", "بصمة الصوت", "قفل النمط", "كلمة المرور"
    val status: String,           // "ناجح" | "فشل" | "تنبيه"
    val details: String
)
