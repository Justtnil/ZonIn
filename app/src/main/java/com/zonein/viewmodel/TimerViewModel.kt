package com.zonein.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zonein.data.SettingsManager
import com.zonein.data.ZoneinRepository
import com.zonein.service.TtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimerUiState(
    // Active timer state
    val minutes: Int = 25,
    val seconds: Int = 0,
    val timerState: TimerState = TimerState.STOPPED,
    val sessionType: SessionType = SessionType.FOCUS,
    // User-configured durations
    val configuredFocusDuration: Int = 25,
    val configuredShortBreakDuration: Int = 5,
    val configuredLongBreakDuration: Int = 15,
    // Stats
    val completedCycles: Int = 0,
    val zeninScore: Int = 0
)

enum class TimerState {
    RUNNING, PAUSED, STOPPED
}

enum class SessionType {
    FOCUS, SHORT_BREAK, LONG_BREAK
}

class TimerViewModel(
    private val repository: ZoneinRepository,
    private val settingsManager: SettingsManager,
    application: Application
) : ViewModel() {

    private val ttsManager = TtsManager(application)
    private var isFirstSession = true
    private var voicePrefix = SettingsManager.DEFAULT_PREFIX
    private val _timerState = MutableStateFlow(TimerUiState())

    val uiState: StateFlow<TimerUiState> = repository.zeninRating
        .combine(_timerState) { rating, timerState ->
            timerState.copy(zeninScore = rating)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TimerUiState()
        )

    init {
        // Initialize timer with configured duration
        _timerState.update { it.copy(minutes = it.configuredFocusDuration) }

        viewModelScope.launch {
            settingsManager.voicePrefixFlow.collectLatest { prefix ->
                voicePrefix = prefix
            }
        }
    }

    private var timerJob: Job? = null

    fun onStartClicked() {
        if (isFirstSession && _timerState.value.timerState == TimerState.STOPPED) {
            isFirstSession = false
            startTimer(announce = false)
        } else {
            when (_timerState.value.timerState) {
                TimerState.STOPPED, TimerState.PAUSED -> startTimer(announce = true)
                TimerState.RUNNING -> pauseTimer()
            }
        }
    }

    fun onFocusDurationChanged(newMinutes: Int) {
        if (_timerState.value.timerState == TimerState.STOPPED) {
            val newDuration = newMinutes.coerceIn(1, 90)
            _timerState.update { it.copy(configuredFocusDuration = newDuration, minutes = newDuration) }
        }
    }

    private fun startTimer(announce: Boolean) {
        if (announce) {
            val phrase = if (_timerState.value.sessionType == SessionType.FOCUS) {
                "$voicePrefix, shuchu shuchu, focus time starts now."
            } else {
                "$voicePrefix, it's break time."
            }
            ttsManager.speak(phrase)
        }

        _timerState.update { it.copy(timerState = TimerState.RUNNING) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var totalSeconds = _timerState.value.minutes * 60 + _timerState.value.seconds
            while (totalSeconds > 0 && _timerState.value.timerState == TimerState.RUNNING) {
                totalSeconds--
                _timerState.update { it.copy(minutes = totalSeconds / 60, seconds = totalSeconds % 60) }
                delay(1000L)
            }
            if (totalSeconds == 0) {
                onSessionFinished()
            }
        }
    }

    private fun pauseTimer() {
        _timerState.update { it.copy(timerState = TimerState.PAUSED) }
        timerJob?.cancel()
    }

    private fun onSessionFinished() {
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
        startTimer(announce = true)
    }

    private fun resetTimerForNextSession() {
        val duration = when (_timerState.value.sessionType) {
            SessionType.FOCUS -> _timerState.value.configuredFocusDuration
            SessionType.SHORT_BREAK -> _timerState.value.configuredShortBreakDuration
            SessionType.LONG_BREAK -> _timerState.value.configuredLongBreakDuration
        }
        _timerState.update { it.copy(minutes = duration, seconds = 0, timerState = TimerState.STOPPED) }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
