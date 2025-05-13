package com.shary.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.shary.app.core.dependencyContainer.DependencyContainer
import com.shary.app.ui.screens.home.utils.AppNavigator

//class MainActivity : ComponentActivity() {
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DependencyContainer.initAll(this)

        // CONFIGURE UI
        setContent {
            MaterialTheme { // You can use your own AppTheme here
                Surface {
                    AppNavigator()
                }
            }
        }
    }
}
