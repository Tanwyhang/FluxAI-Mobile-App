package com.teamflux.fluxai

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.teamflux.fluxai.navigation.AppNavigation
import com.teamflux.fluxai.ui.components.AnimatedGradientBackground
import com.teamflux.fluxai.ui.theme.FluxAITheme
import com.teamflux.fluxai.viewmodel.AuthViewModel
import com.teamflux.fluxai.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val themeViewModel by viewModels<ThemeViewModel>()
    private val authViewModel by viewModels<AuthViewModel>()

    companion object {
        private const val TAG = "MainActivity"
        private const val OAUTH_CALLBACK_SCHEME = "https"
        private const val OAUTH_CALLBACK_HOST = "https://flux-731fd.firebaseapp.com/__/auth/handler"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle OAuth callback if this activity was launched from an intent
        handleOAuthCallback(intent)

        setContent {
            val isDarkTheme by themeViewModel.isDarkTheme.collectAsStateWithLifecycle()
                val authState by authViewModel.uiState.collectAsState()

            FluxAITheme(darkTheme = isDarkTheme) {
                // Only show animated background on login screen
                if (!authState.isLoggedIn) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Add the animated gradient background only for login screen
                        AnimatedGradientBackground(
                            modifier = Modifier.fillMaxSize()
                        )

                        // App content on top of animated background
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent // Make surface transparent to show animated gradient
                        ) {
                            AppNavigation(
                                themeViewModel = themeViewModel,
                                authViewModel = authViewModel,
                                activity = this@MainActivity
                            )
                        }
                    }
                } else {
                    // Normal surface without animated background for other screens
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            themeViewModel = themeViewModel,
                            authViewModel = authViewModel,
                            activity = this@MainActivity
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthCallback(intent)
    }

    private fun handleOAuthCallback(intent: Intent?) {
        intent?.data?.let { uri ->
            Log.d(TAG, "Handling OAuth callback with URI: $uri")

            // Check if this is our OAuth callback
            if (uri.scheme == OAUTH_CALLBACK_SCHEME && uri.host == OAUTH_CALLBACK_HOST) {
                Log.d(TAG, "OAuth callback detected")

                // Extract any error parameters
                val error = uri.getQueryParameter("error")
                val errorDescription = uri.getQueryParameter("error_description")

                if (error != null) {
                    Log.e(TAG, "OAuth error: $error - $errorDescription")
                    lifecycleScope.launch {
                        authViewModel.handleOAuthError(error, errorDescription)
                    }
                } else {
                    Log.d(TAG, "OAuth callback successful, Firebase will handle authentication")
                    // Firebase Auth will automatically handle the successful callback
                }
            }
        }
    }
}
