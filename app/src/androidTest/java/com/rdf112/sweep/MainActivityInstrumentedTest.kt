package com.rdf112.sweep

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rdf112.sweep.data.media.AndroidTrashOperationCoordinator
import com.rdf112.sweep.data.media.Api29DeleteAttempt
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {
    @Test
    fun activityStartsAndUsesExpectedApplicationLabel() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Midiastore", activity.getString(R.string.app_name))
                assertEquals("com.rdf112.midiastore.debug", activity.packageName)
            }
        }
    }
    @Test
    fun android10CanDeleteOwnedMediaAndVerifyTheResult() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) return

        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolver = context.contentResolver
        val displayName = "midiastore-api29-${System.nanoTime()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MidiastoreTest")
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw AssertionError("MediaStore did not create the API29 test image")

        try {
            resolver.openOutputStream(uri)?.use { it.write(byteArrayOf(0x01, 0x02, 0x03, 0x04)) }
                ?: throw AssertionError("MediaStore did not open the test image")
            val coordinator = AndroidTrashOperationCoordinator(context)
            val record = MediaRecord(
                uri = uri.toString(),
                volumeName = "external_primary",
                kind = MediaKind.IMAGE,
                displayName = displayName,
                mimeType = "image/jpeg",
                sizeBytes = 4L,
                dateModifiedEpochSeconds = null,
            )
            assertTrue(coordinator.supportsPermanentDelete())
            assertEquals(Api29DeleteAttempt.Deleted, runBlocking {
                coordinator.deleteApi29Item(record)
            })
            val verified = runBlocking {
                coordinator.verifyPostDeleteUris(listOf(uri.toString()))
            }
            assertTrue("Deleted URI must be absent from MediaStore", uri.toString() in verified)
        } finally {
            resolver.delete(uri, null, null)
        }
    }

}
