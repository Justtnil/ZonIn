package com.zonein.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FocusSession::class, UserStats::class], version = 1, exportSchema = false)
abstract class ZoneinDatabase : RoomDatabase() {

    abstract fun zoneinDao(): ZoneinDao

    companion object {
        @Volatile
        private var INSTANCE: ZoneinDatabase? = null

        fun getDatabase(context: Context): ZoneinDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZoneinDatabase::class.java,
                    "zonein_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
