package com.spliteasy.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.spliteasy.app.data.TokenManager
import com.spliteasy.app.data.api.ApiClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf("http://10.0.2.2:8080") }
    var showSavedMessage by remember { mutableStateOf(false) }

    // Load saved server URL on startup
    LaunchedEffect(Unit) {
        serverUrl = tokenManager.serverUrl.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = {
            if (showSavedMessage) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Server URL saved!")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Server Configuration",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("http://10.0.2.2:8080") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Default: http://10.0.2.2:8080 (Android Emulator)\n" +
                "For physical device: http://YOUR_IP:8080",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        // Save to DataStore
                        tokenManager.saveServerUrl(serverUrl)
                        // Update ApiClient
                        ApiClient.setBaseUrl(serverUrl)
                        // Show confirmation
                        showSavedMessage = true
                        kotlinx.coroutines.delay(2000)
                        showSavedMessage = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
