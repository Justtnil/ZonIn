package com.zonein.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val VOICE_PREFIX_KEY = stringPreferencesKey("voice_prefix")
        const val DEFAULT_PREFIX = "onii-chan"
    }

    val voicePrefixFlow: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[VOICE_PREFIX_KEY] ?: DEFAULT_PREFIX
        }

    suspend fun setVoicePrefix(prefix: String) {
        dataStore.edit { settings ->
            settings[VOICE_PREFIX_KEY] = prefix
        }
    }
}
