package com.spliteasy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.spliteasy.app.data.TokenManager
import com.spliteasy.app.data.api.ApiClient
import com.spliteasy.app.navigation.NavGraph
import com.spliteasy.app.navigation.Screen
import com.spliteasy.app.ui.theme.SplitEasyTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplitEasyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val tokenManager = remember { TokenManager(applicationContext) }
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    // Check for saved token on startup
                    LaunchedEffect(Unit) {
                        launch {
                            val token = tokenManager.authToken.first()
                            if (token != null) {
                                ApiClient.setAuthToken(token)
                                startDestination = Screen.Dashboard.route
                            } else {
                                startDestination = Screen.Login.route
                            }
                        }
                    }

                    // Show NavGraph only after determining start destination
                    if (startDestination != null) {
                        NavGraph(
                            navController = navController,
                            startDestination = startDestination!!
                        )
                    }
                }
            }
        }
    }
}
