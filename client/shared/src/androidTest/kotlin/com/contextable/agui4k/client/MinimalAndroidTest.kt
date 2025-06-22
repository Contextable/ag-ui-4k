package com.contextable.agui4k.sample.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class MinimalAndroidTest {
    @Test
    fun testContextExists() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(context)
    }
}