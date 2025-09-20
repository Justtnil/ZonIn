package com.zonein.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.zonein.data.SettingsManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(settingsManager: SettingsManager, currentPrefix: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val radioOptions = listOf("onii-chan", "onee-chan")

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(16.dp))
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(Modifier.height(16.dp))
        Text("Voice Prefix", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))

        radioOptions.forEach { text ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (text == currentPrefix),
                        onClick = {
                            scope.launch {
                                settingsManager.setVoicePrefix(text)
                            }
                        },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (text == currentPrefix),
                    onClick = null // null recommended for accessibility with selectable parent
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
