package com.shary.app.ui.screens.summary.utils

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryTopAppBar(navController: NavHostController) {

    TopAppBar(
        title = {
            CenterAlignedTopAppBar(
                title = { Text("Summary") },
                expandedHeight = 64.dp
            )
        },
    )
}