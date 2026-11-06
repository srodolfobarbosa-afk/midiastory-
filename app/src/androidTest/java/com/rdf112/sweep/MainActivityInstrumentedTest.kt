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
        var deleteVerified = false

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
            deleteVerified = true
        } finally {
            bitmap.recycle()
            // A successful permanent delete revokes this app's access to the URI.
            // Do not issue a second delete from cleanup after the operation was verified.
            if (!deleteVerified) {
                runCatching { resolver.delete(uri, null, null) }
            }
        }
    }

    @Test
    fun android10RequiresSystemConsentForMediaOwnedByAnotherPackage() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) return

        val targetContext = ApplicationProvider.getApplicationContext<Context>()
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(targetContext.packageName, Manifest.permission.READ_EXTERNAL_STORAGE)

        val ownerResolver = instrumentationContext.contentResolver
        val targetResolver = targetContext.contentResolver
        val displayName = "midiastore-api29-other-owner-${System.nanoTime()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MidiastoreTest/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = ownerResolver.insert(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            values,
        ) ?: throw AssertionError("Instrumentation package could not create the API29 fixture")
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)

        try {
            ownerResolver.openOutputStream(uri, "w").use { output ->
                requireNotNull(output)
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
            }
            val publish = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            assertTrue(ownerResolver.update(uri, publish, null, null) >= 1)

            // Prove the fixture is actually owned by the instrumentation package before
            // using it to test Android 10's cross-package recoverable-consent behavior.
            val ownerPackage = ownerResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.OWNER_PACKAGE_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                assertTrue("MediaStore must return the fixture owner", cursor.moveToFirst())
                val ownerIndex = cursor.getColumnIndex(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)
                assertTrue("OWNER_PACKAGE_NAME must be available on Android 10", ownerIndex >= 0)
                cursor.getString(ownerIndex)
            } ?: throw AssertionError("MediaStore returned a null cursor while checking fixture owner")
            assertEquals(
                "Fixture must be owned by the instrumentation package, not the target app",
                instrumentationContext.packageName,
                ownerPackage,
            )
            assertTrue(
                "Instrumentation and target app must have distinct package identities",
                ownerPackage != targetContext.packageName,
            )

            val record = targetResolver.query(
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
                assertTrue("Target app must be able to read the other package's media", cursor.moveToFirst())
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
            } ?: throw AssertionError("Target app could not query the other package's media")

            val coordinator = AndroidTrashOperationCoordinator(targetContext)
            val attempt = runBlocking { coordinator.deleteApi29Item(record) }
            assertTrue(
                "Another app's media must require Android's recoverable consent flow, got $attempt",
                attempt is Api29DeleteAttempt.ConsentRequired,
            )
            val stillPresent = targetResolver.query(uri, arrayOf(BaseColumns._ID), null, null, null)
                ?.use { it.moveToFirst() } == true
            assertTrue("A consent request must not delete the file before approval", stillPresent)
        } finally {
            bitmap.recycle()
            runCatching { ownerResolver.delete(uri, null, null) }
        }    @Test
    fun android10RequiresSystemConsentForMediaOwnedByAnotherPackage() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) return

        val targetContext = ApplicationProvider.getApplicationContext<Context>()
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(targetContext.packageName, Manifest.permission.READ_EXTERNAL_STORAGE)

        val ownerResolver = instrumentationContext.contentResolver
        val targetResolver = targetContext.contentResolver
        val provider = android.net.Uri.parse(
            "content://com.rdf112.midiastore.testmediaowner.fixtures",
        )
        val displayName = "midiastore-api29-other-owner-${System.nanoTime()}.jpg"
        val fixture = requireNotNull(
            ownerResolver.call(
                provider,
                "create_image",
                displayName,
                null,
            ),
        ) { "Independent MediaStore owner app did not create the fixture" }
        val uri = android.net.Uri.parse(
            requireNotNull(fixture.getString("fixture_uri")) {
                "Fixture provider did not return a MediaStore URI"
            },
        )

        try {
            val ownerPackage = targetResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.OWNER_PACKAGE_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                assertTrue("MediaStore must return the fixture owner", cursor.moveToFirst())
                val ownerIndex = cursor.getColumnIndex(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)
                assertTrue("OWNER_PACKAGE_NAME must be available on Android 10", ownerIndex >= 0)
                cursor.getString(ownerIndex)
            } ?: throw AssertionError("MediaStore returned a null owner cursor")

            assertEquals(
                "The fixture must belong to the separately installed owner app",
                "com.rdf112.midiastore.testmediaowner",
                ownerPackage,
            )
            assertTrue(
                "The fixture owner must differ from the target app",
                ownerPackage != targetContext.packageName,
            )

            val record = targetResolver.query(
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
                assertTrue("Target app must be able to read the other package's media", cursor.moveToFirst())
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
            } ?: throw AssertionError("Target app could not query the other package's media")

            val coordinator = AndroidTrashOperationCoordinator(targetContext)
            val attempt = runBlocking { coordinator.deleteApi29Item(record) }
            assertTrue(
                "Another app's media must require Android's recoverable consent flow, got $attempt",
                attempt is Api29DeleteAttempt.ConsentRequired,
            )
            val stillPresent = targetResolver.query(uri, arrayOf(BaseColumns._ID), null, null, null)
                ?.use { it.moveToFirst() } == true
            assertTrue("A consent request must not delete the file before approval", stillPresent)
        } finally {
            runCatching {
                ownerResolver.call(provider, "delete_fixture", uri.toString(), null)
            }
        }
    }

}
