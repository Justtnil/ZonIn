package com.zonein.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey
    val id: Int = 1, // Singleton table
    val totalFocusMinutes: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastSessionTimestamp: Long
)
