package com.rdf112.sweep

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rdf112.sweep.data.media.AndroidTrashOperationCoordinator
import com.rdf112.sweep.data.media.Api29DeleteAttempt
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
                val primaryAction = activity.window.decorView
                    .findViewWithTag<android.view.View>("primary_phone_cleaner_action")
                assertNotNull("Home must expose the global cleaner action", primaryAction)
                assertTrue("Primary cleaner action must be clickable", primaryAction.isClickable)
            }
        }
    }
    @Test
    fun android10CanDeleteOwnedMediaAndVerifyTheResult() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) return

        val context = ApplicationProvider.getApplicationContext<Context>()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.READ_EXTERNAL_STORAGE)

        val resolver = context.contentResolver
        val displayName = "midiastore-api29-${System.nanoTime()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MidiastoreTest")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            values,
        ) ?: throw AssertionError("MediaStore did not create the API29 test image")
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)

        try {
            resolver.openOutputStream(uri, "w").use { output ->
                requireNotNull(output)
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
            }
            val publish = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            assertTrue(resolver.update(uri, publish, null, null) >= 1)

            val record = resolver.query(
                uri,
                arrayOf(
                    BaseColumns._ID,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                assertTrue("Inserted JPEG must be readable before delete", cursor.moveToFirst())
                val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                MediaRecord(
                    uri = uri.toString(),
                    volumeName = MediaStore.VOLUME_EXTERNAL_PRIMARY,
                    kind = MediaKind.IMAGE,
                    displayName = displayName,
                    mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)),
                    sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                    dateModifiedEpochSeconds = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) {
                        cursor.getLong(modifiedIndex)
                    } else null,
                )
            } ?: throw AssertionError("MediaStore returned a null cursor for the inserted JPEG")

            val coordinator = AndroidTrashOperationCoordinator(context)
            assertTrue(coordinator.supportsPermanentDelete())
            val deleteAttempt = runBlocking { coordinator.deleteApi29Item(record) }
            assertEquals("API29 delete result: $deleteAttempt", Api29DeleteAttempt.Deleted, deleteAttempt)

            val verified = runBlocking {
                coordinator.verifyPostDeleteUris(listOf(uri.toString()))
            }
            assertTrue("Deleted URI must be absent from MediaStore", uri.toString() in verified)
        } finally {
            bitmap.recycle()
            resolver.delete(uri, null, null)
        }
    }
}
