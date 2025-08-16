package com.shary.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.ui.screens.home.AppNavigator
import com.shary.app.viewmodels.authentication.AuthenticationViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val entryPoint = EntryPoints.get(applicationContext, MainActivityEntryPoint::class.java)
        val authModel = entryPoint.authModel()
        val credentialsStore = entryPoint.credentialsStore()

        setContent {
            MaterialTheme {
                Surface {
                    AppNavigator(
                        authModel = authModel,
                        credentialsStore = credentialsStore
                    )
                }
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MainActivityEntryPoint {
        fun authModel(): AuthenticationViewModel
        fun credentialsStore(): CredentialsStore
    }
}
