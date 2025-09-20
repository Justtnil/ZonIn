package com.zonein.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zonein.ZoneinApplication
import com.zonein.ui.theme.ZoneinTheme
import com.zonein.viewmodel.SessionType
import com.zonein.viewmodel.TimerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.zonein.viewmodel.TimerViewModel
import com.zonein.viewmodel.ViewModelFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@Composable
fun TimerScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as ZoneinApplication
    val viewModel: TimerViewModel = viewModel(
        factory = ViewModelFactory(application.repository, application.settingsManager, application)
    )
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val currentPrefix by application.settingsManager.voicePrefixFlow.collectAsState(initial = "onii-chan")

    if (showSettings) {
        SettingsScreen(
            settingsManager = application.settingsManager,
            currentPrefix = currentPrefix,
            onBack = { showSettings = false }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZeninRating(score = uiState.zeninScore)
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }

            TimerDisplay(
                minutes = uiState.minutes,
                seconds = uiState.seconds,
                sessionType = uiState.sessionType,
                timerState = uiState.timerState,
                configuredFocusDuration = uiState.configuredFocusDuration,
                onFocusDurationChange = { newMinutes -> viewModel.onFocusDurationChanged(newMinutes) }
            )

            StartButton(
                onClick = { viewModel.onStartClicked() },
                timerState = uiState.timerState
            )
        }
    }
}

@Composable
fun ZeninRating(score: Int) {
    Text(
        text = "ZENIN: $score",
        style = MaterialTheme.typography.headlineMedium
    )
}

import androidx.compose.ui.text.style.TextAlign

@Composable
fun DotMatrixText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.displayLarge,
        textAlign = TextAlign.Center
    )
}

import com.zonein.viewmodel.TimerState

@Composable
fun TimerDisplay(
    minutes: Int,
    seconds: Int,
    sessionType: SessionType,
    timerState: TimerState,
    configuredFocusDuration: Int,
    onFocusDurationChange: (Int) -> Unit
) {
    val sessionText = when (sessionType) {
        SessionType.FOCUS -> "Focus Time"
        SessionType.SHORT_BREAK -> "Short Break"
        SessionType.LONG_BREAK -> "Long Break"
    }
    val isEnabled = timerState == TimerState.STOPPED

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DotMatrixText(text = String.format("%02d:%02d", minutes, seconds))
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onFocusDurationChange(configuredFocusDuration - 1) },
                enabled = isEnabled
            ) { Text("-") }
            Spacer(modifier = Modifier.width(48.dp))
            Text(text = sessionText, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(48.dp))
            Button(
                onClick = { onFocusDurationChange(configuredFocusDuration + 1) },
                enabled = isEnabled
            ) { Text("+") }
        }
    }
}

@Composable
fun StartButton(onClick: () -> Unit, timerState: TimerState) {
    val buttonText = when (timerState) {
        TimerState.RUNNING -> "P A U S E"
        TimerState.PAUSED -> "R E S U M E"
        TimerState.STOPPED -> "S T A R T"
    }
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = buttonText,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 32.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun TimerScreenPreview() {
    ZoneinTheme {
        TimerScreen()
    }
}
