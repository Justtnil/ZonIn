package com.zonein.ui.screen

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.zonein.ZoneinApplication
import com.zonein.ui.theme.ZoneinTheme
import com.zonein.viewmodel.SessionType
import com.zonein.viewmodel.TimerState
import com.zonein.viewmodel.TimerViewModel
import com.zonein.viewmodel.ViewModelFactory

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TimerScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as ZoneinApplication
    val viewModel: TimerViewModel = viewModel(
        factory = ViewModelFactory(application.repository, application.settingsManager, application)
    )
    val uiState by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showDndDialog by remember { mutableStateOf(false) }
    val currentPrefix by application.settingsManager.voicePrefixFlow.collectAsState(initial = "onii-chan")

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    if (showDndDialog) {
        DndPermissionDialog(
            onDismiss = { showDndDialog = false },
            onConfirm = {
                viewModel.requestDndPermission()
                showDndDialog = false
            }
        )
    }

    if (showSettings) {
        // ... (SettingsScreen call) ...
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ... (Top row) ...

            TimerDisplay(
                minutes = uiState.minutes,
                seconds = uiState.seconds,
                sessionType = uiState.sessionType,
                timerState = uiState.timerState,
                configuredFocusDuration = uiState.configuredFocusDuration,
                configuredBreakDuration = uiState.configuredShortBreakDuration, // Assuming short break for now
                onFocusDurationChange = { newMinutes -> viewModel.onFocusDurationChanged(newMinutes) },
                onBreakDurationChange = { newMinutes -> viewModel.onBreakDurationChanged(newMinutes) }
            )

            Row(
                // ...
            ) {
                StartButton(
                    onClick = {
                        if (notificationPermissionState == null || notificationPermissionState.status.isGranted) {
                            if (uiState.sessionType == SessionType.FOCUS && !uiState.isDndPermissionGranted) {
                                showDndDialog = true
                            } else {
                                viewModel.onStartClicked()
                            }
                        } else {
                            notificationPermissionState.launchPermissionRequest()
                        }
                    },
                    timerState = uiState.timerState
                )
                // ... (Reset button) ...
            }
        }
    }
}

@Composable
fun DndPermissionDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Do Not Disturb?") },
        text = { Text("To help you focus, Zonein can automatically enable Do Not Disturb mode during focus sessions. Would you like to grant permission?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Grant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

// ... (Other Composables)
@Composable
fun TimerDisplay(
    minutes: Int,
    seconds: Int,
    sessionType: SessionType,
    timerState: TimerState,
    configuredFocusDuration: Int,
    configuredBreakDuration: Int,
    onFocusDurationChange: (Int) -> Unit,
    onBreakDurationChange: (Int) -> Unit
) {
    // ...
}
@Composable
fun StartButton(onClick: () -> Unit, timerState: TimerState) {
    // ...
}
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun TimerScreenPreview() {
    ZoneinTheme {
        TimerScreen()
    }
}
