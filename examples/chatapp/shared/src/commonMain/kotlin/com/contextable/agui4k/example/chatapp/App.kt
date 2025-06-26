package com.contextable.agui4k.example.chatapp

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.contextable.agui4k.example.chatapp.ui.screens.chat.ChatScreen
import com.contextable.agui4k.example.chatapp.ui.theme.AgentChatTheme

@Composable
fun App() {
    AgentChatTheme {
        Navigator(ChatScreen()) { navigator ->
            SlideTransition(navigator)
        }
    }
}