package com.contextable.agui4k.client.navigation

import cafe.adriel.voyager.core.screen.Screen

sealed class AppScreen : Screen {
    object Chat : AppScreen()
    object Settings : AppScreen()
}