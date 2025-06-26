package com.contextable.agui4k.example.chatapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.contextable.agui4k.example.chatapp.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AG-UI Agent Chat"
    ) {
        App()
    }
}