package com.zonein.viewmodel

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
    // Permissions
    val isDndPermissionGranted: Boolean = false,
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
