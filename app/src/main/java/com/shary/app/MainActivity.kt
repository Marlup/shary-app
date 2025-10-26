package com.shary.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.FirebaseApp
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.infrastructure.repositories.ThemeRepository
import com.shary.app.ui.screens.home.utils.AppNavigator
import com.shary.app.ui.theme.AppOptionalTheme
import com.shary.app.viewmodels.ViewModelFactory
import com.shary.app.viewmodels.configuration.ThemeViewModel
import com.shary.app.viewmodels.configuration.ThemeViewModelFactory
import com.shary.app.viewmodels.field.FieldViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val themeViewModel by viewModels<ThemeViewModel> {
        ThemeViewModelFactory(ThemeRepository(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

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

