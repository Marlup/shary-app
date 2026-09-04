package com.shary.app

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.shary.app.core.domain.interfaces.repositories.SettingsRepository
import com.shary.app.core.domain.types.valueobjects.AppSettings
import com.shary.app.infrastructure.persistance.repositories.ThemeRepositoryImpl
import com.shary.app.infrastructure.connector.remote.RemoteMcpCoordinator
import com.shary.app.ui.connector.RemoteMcpDialogs
import com.shary.app.ui.screens.home.utils.AppNavigator
import com.shary.app.ui.theme.AppOptionalTheme
import com.shary.app.utils.AppConfig
import com.shary.app.utils.loadAppConfig
import com.shary.app.viewmodels.configuration.ThemeViewModel
import com.shary.app.viewmodels.configuration.ThemeViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var config: AppConfig
    @Inject lateinit var remoteMcpCoordinator: RemoteMcpCoordinator
    @Inject lateinit var settingsRepository: SettingsRepository

    private val themeViewModel by viewModels<ThemeViewModel> {
        ThemeViewModelFactory(ThemeRepositoryImpl(this))
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

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

        handleMcpIntent(intent)

        setContent {
            val selectedTheme by themeViewModel.selectedTheme.collectAsState()
            val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

            AppOptionalTheme(theme = selectedTheme) {
                Surface {
                    AppNavigator(
                        onThemeSelected = { theme ->
                            themeViewModel.updateTheme(theme)
                        }
                    )
                    RemoteMcpDialogs(
                        activity = this@MainActivity,
                        coordinator = remoteMcpCoordinator,
                        enabled = settings.mcpEnabled
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMcpIntent(intent)
    }

    private fun handleMcpIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.host != "shary-21b61.web.app" || uri.path != "/mcp/approve") return
        requestNotificationPermissionIfNeeded()
        lifecycleScope.launch { remoteMcpCoordinator.inspectApprovalLink(uri) }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
