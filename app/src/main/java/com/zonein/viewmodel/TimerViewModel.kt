package com.zonein.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.zonein.data.SettingsManager
import com.zonein.data.ZoneinRepository
import com.zonein.service.DndManager
import com.zonein.service.TimerService
import com.zonein.service.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ... (TimerUiState, TimerState, SessionType enums remain the same)

class TimerViewModel(
    private val repository: ZoneinRepository,
    private val settingsManager: SettingsManager,
    private val application: Application
) : ViewModel() {

    private val ttsManager = TtsManager(application)
    private val dndManager = DndManager(application)

    private var voicePrefix = SettingsManager.DEFAULT_PREFIX
    private val _timerState = MutableStateFlow(TimerUiState())

    val uiState: StateFlow<TimerUiState> = repository.zeninRating
        .combine(_timerState) { rating, timerState ->
            timerState.copy(
                zeninScore = rating,
                isDndPermissionGranted = dndManager.isDndPermissionGranted()
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TimerUiState()
        )

    private val timerUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TimerService.BROADCAST_ACTION_TIME_UPDATE -> {
                    val timeMs = intent.getLongExtra(TimerService.BROADCAST_EXTRA_TIME_MS, 0)
                    _timerState.update {
                        it.copy(
                            minutes = (timeMs / 1000 / 60).toInt(),
                            seconds = (timeMs / 1000 % 60).toInt()
                        )
                    }
                }
                TimerService.BROADCAST_ACTION_TIMER_FINISHED -> {
                    onSessionFinished()
                }
            }
        }
    }

    init {
        _timerState.update { it.copy(minutes = it.configuredFocusDuration) }
        viewModelScope.launch {
            settingsManager.voicePrefixFlow.collectLatest { prefix ->
                voicePrefix = prefix
            }
        }
        val filter = IntentFilter().apply {
            addAction(TimerService.BROADCAST_ACTION_TIME_UPDATE)
            addAction(TimerService.BROADCAST_ACTION_TIMER_FINISHED)
        }
        LocalBroadcastManager.getInstance(application).registerReceiver(timerUpdateReceiver, filter)
    }

    fun onStartClicked() {
        when (uiState.value.timerState) {
            TimerState.STOPPED -> startTimer()
            TimerState.PAUSED -> startTimer() // Resume is same as start from service perspective
            TimerState.RUNNING -> pauseTimer()
        }
    }

    private fun startTimer() {
        if (uiState.value.sessionType == SessionType.FOCUS) {
            dndManager.enableDnd()
        }
        _timerState.update { it.copy(timerState = TimerState.RUNNING) }
        val intent = Intent(application, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TIME_MS, uiState.value.minutes * 60 * 1000L + uiState.value.seconds * 1000L)
        }
        application.startService(intent)
    }

    private fun pauseTimer() {
        dndManager.disableDnd()
        _timerState.update { it.copy(timerState = TimerState.PAUSED) }
        val intent = Intent(application, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        application.startService(intent)
    }

    fun onResetClicked() {
        dndManager.disableDnd()
        _timerState.update {
            it.copy(
                minutes = it.configuredFocusDuration,
                seconds = 0,
                timerState = TimerState.STOPPED,
                sessionType = SessionType.FOCUS,
                completedCycles = 0
            )
        }
        val intent = Intent(application, TimerService::class.java).apply {
            action = TimerService.ACTION_RESET
        }
        application.startService(intent)
    }

    private fun onSessionFinished() {
        dndManager.disableDnd()
        val currentSession = _timerState.value.sessionType
        if (currentSession == SessionType.FOCUS) {
            viewModelScope.launch {
                repository.logFocusSession(_timerState.value.configuredFocusDuration)
            }
            val newCycles = _timerState.value.completedCycles + 1
            val nextSession = if (newCycles % 4 == 0) SessionType.LONG_BREAK else SessionType.SHORT_BREAK
            _timerState.update { it.copy(sessionType = nextSession, completedCycles = newCycles) }
        } else {
            _timerState.update { it.copy(sessionType = SessionType.FOCUS) }
        }
        resetTimerForNextSession()
        startTimer() // Start the next session
    }

    private fun resetTimerForNextSession() {
        val duration = when (_timerState.value.sessionType) {
            SessionType.FOCUS -> _timerState.value.configuredFocusDuration
            SessionType.SHORT_BREAK -> _timerState.value.configuredShortBreakDuration
            SessionType.LONG_BREAK -> _timerState.value.configuredLongBreakDuration
        }
        _timerState.update { it.copy(minutes = duration, seconds = 0, timerState = TimerState.STOPPED) }
    }

    fun onFocusDurationChanged(newMinutes: Int) {
        if (uiState.value.timerState == TimerState.STOPPED) {
            val newDuration = newMinutes.coerceIn(1, 90)
            _timerState.update { it.copy(configuredFocusDuration = newDuration, minutes = newDuration) }
        }
    }

    fun onBreakDurationChanged(newMinutes: Int) {
        if (uiState.value.timerState == TimerState.STOPPED) {
            val newDuration = newMinutes.coerceIn(1, 90)
            _timerState.update { it.copy(configuredShortBreakDuration = newDuration) }
        }
    }

    fun requestDndPermission() {
        dndManager.requestDndPermission()
    }

    override fun onCleared() {
        super.onCleared()
        LocalBroadcastManager.getInstance(application).unregisterReceiver(timerUpdateReceiver)
        dndManager.disableDnd()
    }
}
