package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SecurityLog::class, SecuritySetting::class], version = 1, exportSchema = false)
abstract class SecurityDatabase : RoomDatabase() {
    abstract fun securityDao(): SecurityDao

    companion object {
        @Volatile
        private var INSTANCE: SecurityDatabase? = null

        fun getDatabase(context: Context): SecurityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SecurityDatabase::class.java,
                    "security_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
