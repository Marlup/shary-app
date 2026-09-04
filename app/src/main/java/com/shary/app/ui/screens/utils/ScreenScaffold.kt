package com.shary.app.ui.screens.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    snackbarHostState: SnackbarHostState? = null,
    topActions: (@Composable RowScope.() -> Unit)? = null,
    searchContent: (@Composable () -> Unit)? = null,
    floatingActionContent: (@Composable () -> Unit)? = null,
    bottomBarContent: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = typography.headlineMedium,
                            color = colorScheme.secondary
                        )
                    },
                    actions = { topActions?.invoke(this) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.inverseSurface,
                        titleContentColor = colorScheme.inverseOnSurface,
                    )
                )
                if (searchContent != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        searchContent()
                    }
                }
            }
        },
        floatingActionButton = {
            floatingActionContent?.invoke()
        },
        bottomBar = {
            bottomBarContent?.invoke()
        },
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(snackbarHostState)
            }
        },
        content = content
    )
}
