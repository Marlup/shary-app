package com.shary.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.shary.app.infrastructure.repositories.ThemeRepository
import com.shary.app.ui.screens.home.utils.AppNavigator
import com.shary.app.ui.theme.AppOptionalTheme
import com.shary.app.utils.AppConfig
import com.shary.app.utils.loadAppConfig
import com.shary.app.viewmodels.configuration.ThemeViewModel
import com.shary.app.viewmodels.configuration.ThemeViewModelFactory
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var config: AppConfig

    private val themeViewModel by viewModels<ThemeViewModel> {
        ThemeViewModelFactory(ThemeRepository(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load config inside onCreate() where 'applicationContext' is guaranteed to be valid
        // Replace loadAppConfig with your actual loading function
        // Note: You must handle the case where loadAppConfig returns null
        config = loadAppConfig(applicationContext) ?: run {
            // Handle fatal configuration error here, e.g., throw an exception or log and exit
            throw IllegalStateException("Failed to load application configuration.")
        }

        FirebaseApp.initializeApp(this)

        if (config.environment == "dev")
            setupFirebaseEmulators()

        setContent {
            val selectedTheme by themeViewModel.selectedTheme.collectAsState()

            AppOptionalTheme(theme = selectedTheme) {
                Surface {
                    AppNavigator(
                        onThemeSelected = { theme ->
                            themeViewModel.updateTheme(theme)
                        }
                    )
                }
            }
        }
    }
}

fun setupFirebaseEmulators() {
    val auth = FirebaseAuth.getInstance()
    val firestore = Firebase.firestore
    //val functions = Firebase.functions("us-central1")

    // Host machine IP for Android emulator
    val host = "10.0.2.2"

    auth.useEmulator(host, 9099)
    firestore.useEmulator(host, 5002)
    //functions.useEmulator(host, 5003)
}

