package com.zonein

import android.app.Application
import com.zonein.data.SettingsManager
import com.zonein.data.ZoneinRepository
import com.zonein.data.local.ZoneinDatabase

class ZoneinApplication : Application() {
    // Using by lazy so the database and repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { ZoneinDatabase.getDatabase(this) }
    val repository by lazy { ZoneinRepository(database.zoneinDao()) }
    val settingsManager by lazy { SettingsManager(this) }
}
