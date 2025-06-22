package com.contextable.agui4k.sample.client

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AG-UI Agent Chat"
    ) {
        App()
    }
}