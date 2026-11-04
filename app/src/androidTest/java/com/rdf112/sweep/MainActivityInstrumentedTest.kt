package com.rdf112.sweep

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {
    @Test
    fun activityStartsAndUsesExpectedApplicationLabel() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Sweep", activity.getString(R.string.app_name))
                assertEquals("com.rdf112.midiastore.debug", activity.packageName)
            }
        }
    }
}
