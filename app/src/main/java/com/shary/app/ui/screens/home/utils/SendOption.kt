package com.shary.app.ui.screens.home.utils

sealed class SendOption(val label: String) {
    data object Email : SendOption("Email")
    data object Cloud : SendOption("Cloud")

    companion object {
        val all: List<SendOption> = listOf(Email, Cloud)

        fun fromLabel(label: String): SendOption? =
            all.find { it.label == label } }
}

