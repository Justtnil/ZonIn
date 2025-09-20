package com.zonein

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.zonein.ui.screen.TimerScreen
import com.zonein.ui.theme.ZoneinTheme

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZoneinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Notification permission is required for Android 13 (API 33) and above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val notificationPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
                        if (notificationPermission.status.isGranted) {
                            TimerScreen()
                        } else {
                            PermissionRationale(
                                text = "Zonein needs the notification permission to show the timer when the app is in the background. Please grant the permission to continue.",
                                onPermissionRequested = {
                                    notificationPermission.launchPermissionRequest()
                                }
                            )
                        }
                    } else {
                        // No permission needed for older versions
                        TimerScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRationale(text: String, onPermissionRequested: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onPermissionRequested) {
            Text("Grant Permission")
        }
    }
}
