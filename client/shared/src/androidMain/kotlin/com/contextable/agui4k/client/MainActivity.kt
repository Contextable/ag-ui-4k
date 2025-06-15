package com.contextable.agui4k.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.contextable.agui4k.client.util.initializeAndroid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Android-specific utilities
        initializeAndroid(this)

        setContent {
            App()
        }
    }
}