package com.rdf112.sweep

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rdf112.sweep.data.media.AndroidTrashOperationCoordinator
import com.rdf112.sweep.data.media.Api29DeleteAttempt
import com.rdf112.sweep.domain.operations.MediaOperationMode
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {
    @Test
    fun android11OrNewerPermanentDeleteRequiresConsentAndVerifiesPostState() {
        assumeTrue(
            "MediaStore batch delete consent requires Android 11 (API 30) or newer",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        assumeNoExternalAutoClicker(context)
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, permission)

        val volume = awaitAvailableExternalVolume(context)
        val resolver = context.contentResolver
        val displayName = "midiastore-api35-delete-${System.nanoTime()}.png"
        val fixture = requireNotNull(
            resolver.insert(
                MediaStore.Images.Media.getContentUri(volume),
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MidiastoreCodexTest/")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                },
            ),
        )
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        var verifiedDeleted = false
        try {
            resolver.openOutputStream(fixture, "w").use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, requireNotNull(output)))
            }
            assertTrue(
                resolver.update(
                    fixture,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null,
                ) > 0,
            )
            val fixtureSha256 = MessageDigest.getInstance("SHA-256")
                .digest(requireNotNull(resolver.openInputStream(fixture)).use { it.readBytes() })
                .joinToString("") { byte -> "%02x".format(byte) }
            println(
                "MIDIASTORE_FIXTURE id=api30plus-delete-consent uri=$fixture " +
                    "path=Pictures/MidiastoreCodexTest/$displayName sha256=$fixtureSha256",
            )

            val record = resolver.query(
                fixture,
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
                assertTrue("The published synthetic image must be queryable", cursor.moveToFirst())
                val modifiedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                MediaRecord(
                    uri = fixture.toString(),
                    volumeName = volume,
                    kind = MediaKind.IMAGE,
                    displayName = displayName,
                    mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)),
                    sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                    dateModifiedEpochSeconds = if (modifiedColumn >= 0 && !cursor.isNull(modifiedColumn)) {
                        cursor.getLong(modifiedColumn)
                    } else null,
                )
            } ?: throw AssertionError("MediaStore returned a null cursor for the synthetic deletion fixture")
            assertTrue("The synthetic fixture must have non-zero content", record.sizeBytes > 0)

            val coordinator = AndroidTrashOperationCoordinator(context)
            val firstPlan = runBlocking {
                coordinator.prepareAndRevalidatePlan(listOf(record), MediaOperationMode.PERMANENT_DELETE)
            }
            assertEquals("The fixture must pass immediate pre-delete validation", listOf(fixture.toString()),
                firstPlan.uriBatches.flatten())
            val firstSender = requireNotNull(coordinator.createPermanentDeleteIntentSender(firstPlan.uriBatches.single()))

            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val host = requireNotNull(
                instrumentation.startActivitySync(
                    Intent(context, MediaOperationConsentHostActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ),
            ) as MediaOperationConsentHostActivity
            val device = UiDevice.getInstance(instrumentation)

            MediaOperationConsentHostActivity.resetConsentResult()
            instrumentation.runOnMainSync { host.requestConsent(firstSender) }
            val cancelButton = device.wait(Until.findObject(By.res("android", "button2")), 30_000)
            assertNotNull("Android must display a cancel action for permanent deletion consent", cancelButton)
            requireNotNull(cancelButton).click()
            assertTrue(
                "Cancelling system deletion must return a result",
                MediaOperationConsentHostActivity.awaitConsentResult(30, TimeUnit.SECONDS),
            )
            assertEquals(Activity.RESULT_CANCELED, MediaOperationConsentHostActivity.consentResultCode)
            assertTrue("Cancellation must preserve the synthetic file", mediaUriExists(resolver, fixture))
            assertFalse(
                "A cancelled request must not be counted as a confirmed deletion",
                fixture.toString() in runBlocking { coordinator.verifyPostDeleteUris(listOf(fixture.toString())) },
            )

            val retryPlan = runBlocking {
                coordinator.prepareAndRevalidatePlan(listOf(record), MediaOperationMode.PERMANENT_DELETE)
            }
            assertEquals("The fixture must be revalidated after consent cancellation", listOf(fixture.toString()),
                retryPlan.uriBatches.flatten())
            val retrySender = requireNotNull(coordinator.createPermanentDeleteIntentSender(retryPlan.uriBatches.single()))
            MediaOperationConsentHostActivity.resetConsentResult()
            instrumentation.runOnMainSync { host.requestConsent(retrySender) }
            val allowButton = device.wait(Until.findObject(By.res("android", "button1")), 30_000)
            assertNotNull("Android must display the confirmation action for permanent deletion", allowButton)
            requireNotNull(allowButton).click()
            assertTrue(
                "Approved system deletion must return a result before the 120-second provider timeout",
                MediaOperationConsentHostActivity.awaitConsentResult(120, TimeUnit.SECONDS),
            )
            assertEquals(Activity.RESULT_OK, MediaOperationConsentHostActivity.consentResultCode)

            val confirmedMissing = runBlocking {
                coordinator.verifyPostDeleteUris(listOf(fixture.toString()))
            }
            assertTrue("MediaStore must confirm the approved URI is absent", fixture.toString() in confirmedMissing)
            verifiedDeleted = true
        } finally {
            bitmap.recycle()
            if (!verifiedDeleted) runCatching { resolver.delete(fixture, null, null) }
        }
    }

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
        assumeTrue(
            "This system-consent test requires Android 10 (API 29); current API=${Build.VERSION.SDK_INT}",
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q,
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.READ_EXTERNAL_STORAGE)

        val resolver = context.contentResolver
        val displayName = "midiastore-api29-${System.nanoTime()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MidiastoreCodexTest/")
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
        assumeTrue(
            "This foreign-owner consent test requires Android 10 (API 29); current API=${Build.VERSION.SDK_INT}",
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q,
        )

        val targetContext = ApplicationProvider.getApplicationContext<Context>()
        assumeNoExternalAutoClicker(targetContext)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(targetContext.packageName, Manifest.permission.READ_EXTERNAL_STORAGE)

        val helperContext = instrumentation.context
        val helperPackage = HELPER_PACKAGE
        val helperInfo = helperContext.packageManager.getApplicationInfo(helperPackage, 0)
        val targetInfo = targetContext.packageManager.getApplicationInfo(targetContext.packageName, 0)
        assertNotEquals("The helper app must run under a distinct Android UID", targetInfo.uid, helperInfo.uid)

        val helperUri = Uri.parse("content://$HELPER_AUTHORITY")
        val fixture = requireNotNull(
            helperContext.contentResolver.call(
                helperUri,
                HELPER_METHOD_CREATE_IMAGE,
                null,
                null,
            ),
        ) { "The independent media-owner app did not create the API29 fixture" }
        val uri = Uri.parse(requireNotNull(fixture.getString(HELPER_KEY_URI)))
        val ownerPackage = requireNotNull(fixture.getString(HELPER_KEY_OWNER_PACKAGE))
        val ownerUid = fixture.getInt(HELPER_KEY_OWNER_UID, -1)
        assertEquals(helperPackage, ownerPackage)
        assertEquals("Fixture creation must run inside the helper app process", helperInfo.uid, ownerUid)
        assertNotEquals("The fixture owner UID must differ from Midiastore", targetInfo.uid, ownerUid)

        val targetResolver = targetContext.contentResolver
        try {
            // Verify MediaStore's own ownership metadata, not just the helper's self-report.
            val storedOwnerPackage = targetResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.OWNER_PACKAGE_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                assertTrue("MediaStore must return the helper-owned fixture", cursor.moveToFirst())
                val ownerIndex = cursor.getColumnIndex(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)
                assertTrue("OWNER_PACKAGE_NAME must be available on Android 10", ownerIndex >= 0)
                cursor.getString(ownerIndex)
            } ?: throw AssertionError("MediaStore returned a null cursor while checking helper ownership")
            assertEquals(
                "MediaStore must attribute this fixture to the independent helper package",
                helperPackage,
                storedOwnerPackage,
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
                    displayName = uri.lastPathSegment.orEmpty(),
                    mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)),
                    sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                    dateModifiedEpochSeconds = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) {
                        cursor.getLong(modifiedIndex)
                    } else null,
                )
            } ?: throw AssertionError("Target app could not query the other package's media")

            val coordinator = AndroidTrashOperationCoordinator(targetContext)
            val testActivity = requireNotNull(
                instrumentation.startActivitySync(
                    Intent(targetContext, MediaOperationConsentHostActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ),
            ) as MediaOperationConsentHostActivity
            val attempt = runBlocking { coordinator.deleteApi29Item(record) }
            assertTrue(
                "Another app's media must require Android's recoverable consent flow, got $attempt",
                attempt is Api29DeleteAttempt.ConsentRequired,
            )
            val stillPresent = targetResolver.query(uri, arrayOf(BaseColumns._ID), null, null, null)
                ?.use { it.moveToFirst() } == true
            assertTrue("A consent request must not delete the file before approval", stillPresent)

            MediaOperationConsentHostActivity.resetConsentResult()
            instrumentation.runOnMainSync {
                testActivity.requestConsent((attempt as Api29DeleteAttempt.ConsentRequired).intentSender)
            }

            val device = UiDevice.getInstance(instrumentation)
            val cancelButton = device.wait(Until.findObject(By.res("android", "button2")), 30_000)
            assertNotNull("Android 10 must show a cancel option for delete consent", cancelButton)
            requireNotNull(cancelButton).click()
            assertTrue(
                "Cancelling system consent must return a result",
                MediaOperationConsentHostActivity.awaitConsentResult(30, TimeUnit.SECONDS),
            )
            assertEquals(
                "Cancelling system consent must return RESULT_CANCELED",
                Activity.RESULT_CANCELED,
                MediaOperationConsentHostActivity.consentResultCode,
            )
            val presentAfterCancel = targetResolver.query(uri, arrayOf(BaseColumns._ID), null, null, null)
                ?.use { it.moveToFirst() } == true
            assertTrue("Cancelling consent must preserve the helper-owned fixture", presentAfterCancel)

            val consentRequired = runBlocking { coordinator.deleteApi29Item(record) } as? Api29DeleteAttempt.ConsentRequired
                ?: throw AssertionError("Retry after cancellation must request fresh system consent")
            MediaOperationConsentHostActivity.resetConsentResult()
            instrumentation.runOnMainSync { testActivity.requestConsent(consentRequired.intentSender) }
            val allowButton = device.wait(Until.findObject(By.res("android", "button1")), 30_000)
            assertNotNull("Android 10 must show the system delete-consent confirmation", allowButton)
            requireNotNull(allowButton).click()
            assertTrue(
                "Android's consent activity must return a result",
                MediaOperationConsentHostActivity.awaitConsentResult(30, TimeUnit.SECONDS),
            )
            assertEquals("The user-approved system flow must return RESULT_OK", Activity.RESULT_OK,
                MediaOperationConsentHostActivity.consentResultCode)

            val retry = runBlocking { coordinator.deleteApi29Item(record) }
            assertTrue("The same URI must delete after system authorization, got $retry", retry is Api29DeleteAttempt.Deleted)
            val verifiedMissing = runBlocking { coordinator.verifyPostDeleteUris(listOf(uri.toString())) }
            assertTrue("The post-authorization provider query must confirm the URI is gone", uri.toString() in verifiedMissing)
        } finally {
            helperContext.contentResolver.call(
                helperUri,
                HELPER_METHOD_DELETE_MEDIA,
                uri.toString(),
                null,
            )
        }
    }

    companion object {
        private const val HELPER_PACKAGE = "com.rdf112.midiastore.testmediaowner"
        private const val HELPER_AUTHORITY = "$HELPER_PACKAGE.fixtures"
        private const val HELPER_METHOD_CREATE_IMAGE = "create_image"
        private const val HELPER_METHOD_DELETE_MEDIA = "delete_media"
        private const val HELPER_KEY_URI = "media_uri"
        private const val HELPER_KEY_OWNER_PACKAGE = "owner_package"
        private const val HELPER_KEY_OWNER_UID = "owner_uid"
    }

    private fun mediaUriExists(resolver: android.content.ContentResolver, uri: Uri): Boolean =
        resolver.query(uri, arrayOf(BaseColumns._ID), null, null, null)
            ?.use { it.moveToFirst() } == true

    private fun awaitAvailableExternalVolume(context: Context, timeoutMs: Long = 120_000): String {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        var availableVolumes: Set<String> = emptySet()
        while (SystemClock.elapsedRealtime() < deadline) {
            availableVolumes = MediaStore.getExternalVolumeNames(context)
            if (availableVolumes.isNotEmpty()) {
                return if (MediaStore.VOLUME_EXTERNAL_PRIMARY in availableVolumes) {
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                } else {
                    availableVolumes.sorted().first()
                }
            }
            SystemClock.sleep(250)
        }
        throw AssertionError("No external MediaStore volume mounted within ${timeoutMs}ms; available=$availableVolumes")
    }

    /** External auto-tap accessibility services can approve or cancel consent before UiAutomator observes it. */
    private fun assumeNoExternalAutoClicker(context: Context) {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val autoClickerEnabled = enabledServices.split(':').any { component ->
            component.contains("autoclick", ignoreCase = true) ||
                component.contains("auto_tap", ignoreCase = true) ||
                component.contains("autotap", ignoreCase = true)
        }
        assumeTrue(
            "System consent test requires an environment without an independent auto-click accessibility service",
            !autoClickerEnabled,
        )
    }

}
