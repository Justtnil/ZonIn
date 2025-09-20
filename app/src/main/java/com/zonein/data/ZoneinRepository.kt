package com.zonein.data

import com.zonein.data.local.FocusSession
import com.zonein.data.local.UserStats
import com.zonein.data.local.ZoneinDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

class ZoneinRepository(private val zoneinDao: ZoneinDao) {

    // A default UserStats to be used if the database is empty.
    private val defaultStats = UserStats(id = 1, totalFocusMinutes = 0, currentStreak = 0, longestStreak = 0, lastSessionTimestamp = 0)

    val userStats: Flow<UserStats> = zoneinDao.getUserStats().map { it ?: defaultStats }

    val zeninRating: Flow<Int> = userStats.map { stats ->
        // Zenin Rating = (Total Minutes) + (Streak * 10)
        (stats.totalFocusMinutes + (stats.currentStreak * 10)).toInt()
    }

    suspend fun logFocusSession(durationMinutes: Int) {
        val currentTime = System.currentTimeMillis()
        zoneinDao.insertSession(FocusSession(timestamp = currentTime, durationMinutes = durationMinutes))

        val currentStats = zoneinDao.getUserStats().first() ?: defaultStats

        // Update total focus time
        val newTotalMinutes = currentStats.totalFocusMinutes + durationMinutes

        // Update streak
        val (newStreak, newLongestStreak) = calculateNewStreak(currentStats)

        val newStats = currentStats.copy(
            totalFocusMinutes = newTotalMinutes,
            currentStreak = newStreak,
            longestStreak = newLongestStreak,
            lastSessionTimestamp = currentTime
        )

        zoneinDao.updateUserStats(newStats)
    }

    private fun calculateNewStreak(stats: UserStats): Pair<Int, Int> {
        val lastSession = Calendar.getInstance().apply { timeInMillis = stats.lastSessionTimestamp }
        val now = Calendar.getInstance()

        if (stats.lastSessionTimestamp == 0L) {
            return Pair(1, maxOf(1, stats.longestStreak))
        }

        val isYesterday = now.get(Calendar.DAY_OF_YEAR) == lastSession.get(Calendar.DAY_OF_YEAR) + 1 && now.get(Calendar.YEAR) == lastSession.get(Calendar.YEAR)
        val isSameDay = now.get(Calendar.DAY_OF_YEAR) == lastSession.get(Calendar.DAY_OF_YEAR) && now.get(Calendar.YEAR) == lastSession.get(Calendar.YEAR)

        val newStreak = when {
            isSameDay -> stats.currentStreak // Continue streak if another session on the same day
            isYesterday -> stats.currentStreak + 1 // Increment streak if session was yesterday
            else -> 1 // Reset streak
        }
        val newLongestStreak = maxOf(newStreak, stats.longestStreak)
        return Pair(newStreak, newLongestStreak)
    }
}
