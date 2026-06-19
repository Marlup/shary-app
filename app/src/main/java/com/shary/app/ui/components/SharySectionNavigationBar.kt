package com.shary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet400
import com.shary.app.ui.theme.Violet600

enum class SectionTab {
    FIELDS,
    USERS,
    REQUESTS
}

@Composable
fun SharySectionNavigationBar(
    currentTab: SectionTab,
    onTabSelected: (SectionTab) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceLight)
    ) {
        HorizontalDivider(color = Violet200, thickness = 1.dp)
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .wrapContentHeight(),
            containerColor = SurfaceLight,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = currentTab == SectionTab.FIELDS,
                onClick = { onTabSelected(SectionTab.FIELDS) },
                icon = { Icon(imageVector = Icons.Default.TextFields, contentDescription = "Fields") },
                label = { Text(text = "Fields") },
                colors = itemColors()
            )
            NavigationBarItem(
                selected = currentTab == SectionTab.USERS,
                onClick = { onTabSelected(SectionTab.USERS) },
                icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Contacts") },
                label = { Text(text = "Contacts") },
                colors = itemColors()
            )
            NavigationBarItem(
                selected = currentTab == SectionTab.REQUESTS,
                onClick = { onTabSelected(SectionTab.REQUESTS) },
                icon = { Icon(imageVector = Icons.Default.Description, contentDescription = "Requests") },
                label = { Text(text = "Requests") },
                colors = itemColors()
            )
        }
    }
}

@Composable
private fun itemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Violet600,
    selectedTextColor = Violet600,
    unselectedIconColor = Violet400,
    unselectedTextColor = Violet400,
    indicatorColor = Color.Transparent
)
