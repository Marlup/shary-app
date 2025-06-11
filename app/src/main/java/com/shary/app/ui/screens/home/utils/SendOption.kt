package com.shary.app.ui.screens.home.utils

sealed class SendOption(val label: String) {
    object Email : SendOption("Email")
    object Cloud : SendOption("Cloud")
    object Whatsapp : SendOption("Whatsapp")
    object Telegram : SendOption("Telegram")
    object Bluetooth : SendOption("Bluetooth")

    companion object {
        val all: List<SendOption> = listOf(Email, Cloud, Whatsapp, Telegram, Bluetooth)

        fun fromLabel(label: String): SendOption? =
            all.find { it.label == label } }
}

