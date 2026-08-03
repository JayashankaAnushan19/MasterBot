package com.masterbot.app.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterbot.app.data.sync.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onOpenAbout: () -> Unit, viewModel: ProfileViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val context = LocalContext.current
    var nameField by remember(state.name) { mutableStateOf(state.name) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.setNotificationsEnabled(enabled = true, granted = granted) }

    var confirmResetAll by remember { mutableStateOf(false) }
    if (confirmResetAll) {
        AlertDialog(
            onDismissRequest = { confirmResetAll = false },
            title = { Text("Reset all progress?") },
            text = { Text("Every topic and quiz stage goes back to Not started, and coins/streak reset to 0. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAllProgress()
                    confirmResetAll = false
                }) { Text("Reset everything") }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetAll = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text("Name", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nameField,
                    onValueChange = { nameField = it },
                    modifier = Modifier.fillMaxWidth()
                        .onFocusLost { viewModel.updateName(nameField) },
                    singleLine = true,
                    placeholder = { Text("What should we call you?") },
                )

                Spacer(Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Daily reminders", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Nudge me if I haven't hit today's goal by evening",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { checked ->
                            if (!checked) {
                                viewModel.setNotificationsEnabled(enabled = false, granted = true)
                                return@Switch
                            }
                            val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            if (!needsRuntimePermission) {
                                viewModel.setNotificationsEnabled(enabled = true, granted = true)
                                return@Switch
                            }
                            val alreadyGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (alreadyGranted) {
                                viewModel.setNotificationsEnabled(enabled = true, granted = true)
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }

                Spacer(Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(Modifier.height(24.dp))

                Text("Content sync", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(syncStatusLabel(syncState), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))

                when (syncState) {
                    is SyncState.UpdateAvailable -> Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    ) {
                        Button(onClick = viewModel::pullUpdate) { Text("Pull now") }
                        Button(onClick = viewModel::dismissUpdate) { Text("Later") }
                    }
                    is SyncState.Syncing -> {}
                    else -> Button(onClick = viewModel::checkForUpdates) { Text("Check for updates") }
                }

                Spacer(Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onOpenAbout,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("About MasterBot")
                }

                Spacer(Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(Modifier.height(24.dp))
                Text(
                    "Danger zone",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Reset every topic, quiz stage, coin and streak back to zero.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { confirmResetAll = true },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Reset all progress")
                }
            }
        }
    }
}

private fun syncStatusLabel(state: SyncState): String = when (state) {
    is SyncState.Syncing -> "Checking…"
    is SyncState.Ready -> "Up to date"
    is SyncState.UpdateAvailable -> "New content is available"
    is SyncState.Error -> "Sync error: ${state.message}"
}

private fun Modifier.onFocusLost(onLost: () -> Unit): Modifier = this.then(
    Modifier.onFocusChanged { if (!it.isFocused) onLost() },
)
