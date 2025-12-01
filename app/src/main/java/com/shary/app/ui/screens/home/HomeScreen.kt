package com.shary.app.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.home.utils.AppTopBar

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun HomeScreen(navController: NavHostController) {

    Scaffold(
        topBar = { AppTopBar(navController) },
        modifier = Modifier
            .background(color = Color(0xFFF7F3FF)) // Hardcoded
    ) { padding ->
        Column {

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
                    //.fillMaxWidth()
                    .fillMaxHeight(0.30f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Button(
                        onClick = { navController.navigate(Screen.Fields.route) },
                        modifier = Modifier.size(50.dp, 50.dp)
                    ) {
                        Text("Fields")
                    }
                }
                item {
                    Button(
                        onClick = { navController.navigate(Screen.Users.route) },
                        modifier = Modifier.size(50.dp, 50.dp)
                    ) {
                        Text("Users")
                    }
                }
                item {
                    Button(
                        onClick = { navController.navigate(Screen.Requests.route) },
                        modifier = Modifier.size(50.dp, 50.dp)
                    ) {
                        Text("Requests")
                    }
                }
                item {
                    Button(
                        onClick = { navController.navigate(Screen.FileVisualizer.route) },
                        modifier = Modifier.size(50.dp, 50.dp)
                    ) {
                        Text("File Visualizer")
                    }
                }
            }
        }
    }
}
