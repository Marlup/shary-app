package com.shary.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.firebase.FirebaseApp
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.ui.screens.home.utils.AppNavigator
import com.shary.app.ui.theme.AppOptionalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            // Suppose you load from DataStore or ViewModel later
            var selectedTheme by rememberSaveable { mutableStateOf(AppTheme.Pastel) }

            // MaterialTheme
            AppOptionalTheme(theme = selectedTheme) {
                Surface {
                    AppNavigator(
                        onThemeSelected = { theme -> selectedTheme = theme }
                    )
                }
            }
        }
    }
}
