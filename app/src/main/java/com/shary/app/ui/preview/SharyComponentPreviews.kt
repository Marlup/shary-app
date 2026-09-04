@file:Suppress("unused")

package com.shary.app.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.ui.components.CloudStatusStrip
import com.shary.app.ui.components.FieldCard
import com.shary.app.ui.components.RequestCard
import com.shary.app.ui.components.RequestStatusTone
import com.shary.app.ui.components.SectionTab
import com.shary.app.ui.components.SharyPrimaryButton
import com.shary.app.ui.components.SharySearchBar
import com.shary.app.ui.components.SharySectionNavigationBar
import com.shary.app.ui.components.SharyTopBar
import com.shary.app.ui.components.UserCard
import com.shary.app.ui.theme.SharyTheme
import com.shary.app.ui.theme.Violet50
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.utils.CloudUiStatus

@Preview(name = "Fields first fold", showBackground = true, widthDp = 412, heightDp = 720)
@Composable
private fun FieldsFirstFoldPreview() {
    SharyPreviewSurface {
        SharyTopBar(title = "Fields", subtitle = "12 saved fields")
        SharySearchBar(
            query = "",
            onQueryChange = {},
            filters = listOf("All", "Personal", "Work"),
            activeFilter = "All",
            onFilterChange = {}
        )
        Spacer(Modifier.size(12.dp))
        CloudStatusStrip(
            status = CloudUiStatus(
                ready = true,
                reason = "",
                label = "Ready",
                detail = "Cloud inbox and secure relay are available.",
                safetyLabel = "cloud-ready"
            ),
            pendingCount = 2
        )
        Spacer(Modifier.size(12.dp))
        FieldCard(
            fieldKey = "Passport number",
            fieldValue = "P12345678",
            tag = Tag.Entry("Personal", Color(0xFFDCD0FF)),
            typedValueLabel = "Text",
            alias = "Travel document",
            isSelected = true,
            onCardTap = {},
            onToggleSelection = {}
        )
    }
}

@Preview(name = "Contacts selected card", showBackground = true, widthDp = 412)
@Composable
private fun UserCardPreview() {
    SharyPreviewSurface {
        UserCard(
            name = "Alex Morgan",
            email = "alex@example.com",
            isSelected = true,
            onCardTap = {},
            onToggleSelection = {}
        )
    }
}

@Preview(name = "Received request", showBackground = true, widthDp = 412)
@Composable
private fun RequestCardPreview() {
    SharyPreviewSurface {
        RequestCard(
            senderName = "Alex Morgan",
            timestamp = "Today, 10:42",
            keys = listOf("Email", "Phone", "Address"),
            fieldCount = 3,
            statusLabel = "Received",
            statusTone = RequestStatusTone.RECEIVED,
            isActive = true,
            onCardTap = {},
            onToggleSelection = {},
            onMatchClick = {}
        )
    }
}

@Preview(name = "Interactive section navigation", showBackground = true, widthDp = 412)
@Composable
private fun SectionNavigationPreview() {
    var selectedTab by remember { mutableStateOf(SectionTab.FIELDS) }

    SharyTheme {
        SharySectionNavigationBar(
            currentTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }
}

@Preview(name = "Interactive search and filters", showBackground = true, widthDp = 412)
@Composable
private fun SearchAndFiltersPreview() {
    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") }

    SharyPreviewSurface {
        SharySearchBar(
            query = query,
            onQueryChange = { query = it },
            filters = listOf("All", "Personal", "Work"),
            activeFilter = activeFilter,
            onFilterChange = { activeFilter = it }
        )
        Spacer(Modifier.size(16.dp))
        SharyPrimaryButton(
            text = "Add field",
            icon = Icons.Default.Add,
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "Cloud unavailable", showBackground = true, widthDp = 412)
@Composable
private fun CloudUnavailablePreview() {
    SharyPreviewSurface {
        CloudStatusStrip(
            status = CloudUiStatus(
                ready = false,
                reason = "Email not verified for cloud",
                label = "Verify email",
                detail = "Verify your email before syncing or reviewing cloud inbox.",
                safetyLabel = "cloud-blocked"
            )
        )
    }
}

@Composable
private fun SharyPreviewSurface(content: @Composable () -> Unit) {
    SharyTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(Violet50)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                content = { content() }
            )
        }
    }
}
