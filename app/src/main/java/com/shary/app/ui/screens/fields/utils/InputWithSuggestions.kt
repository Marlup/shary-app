package com.shary.app.ui.screens.fields.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
fun InputWithSuggestions(
    key: String,
    onKeyChange: (String) -> Unit,
    predefinedKeys: List<String>
) {
    var showSuggestions by remember { mutableStateOf(false) }
    var filteredSuggestions by remember { mutableStateOf(listOf<String>()) }
    val focusRequester = remember { FocusRequester() }

    val textFieldBounds = remember { mutableStateOf(Rect.Zero) }

    Box {
        OutlinedTextField(
            value = key,
            isError = key.isBlank(),
            onValueChange = {
                onKeyChange(it)

                showSuggestions = it.length >= 2
                filteredSuggestions = predefinedKeys.filter { suggestion ->
                    suggestion.contains(it, ignoreCase = true)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onGloballyPositioned { layoutCoordinates ->
                    val position = layoutCoordinates.boundsInRoot()
                    textFieldBounds.value = position
                },

            label = { Text("Enter text") },
            singleLine = true
        )

        if (showSuggestions && filteredSuggestions.isNotEmpty()) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(
                    x = textFieldBounds.value.left.toInt(),
                    y = textFieldBounds.value.bottom.toInt() / 2
                ),
            ) {
                Card(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { textFieldBounds.value.width.toDp() })
                        .wrapContentHeight()
                ) {
                    Column {
                        filteredSuggestions.forEach { suggestion ->
                            Text(
                                text = suggestion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onKeyChange(suggestion)
                                        showSuggestions = false
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}