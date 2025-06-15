package com.contextable.agui4k.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.contextable.agui4k.client.util.getPlatformSettings
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class AndroidIntegrationTest {
    
    @Test
    fun testAndroidSettingsInitialization() {
        val settings = getPlatformSettings()
        assertNotNull(settings)
    }
}
