package com.zonein.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zonein.data.ZoneinRepository

import com.zonein.data.SettingsManager

class ViewModelFactory(
    private val repository: ZoneinRepository,
    private val settingsManager: SettingsManager,
    private val application: Application
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimerViewModel(repository, settingsManager, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
