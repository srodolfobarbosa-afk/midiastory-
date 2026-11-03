package com.rdf112.sweep.data.media

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rdf112.sweep.domain.model.AccessLevel
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaPageOutcome
import com.rdf112.sweep.domain.model.MediaPageRequest
import com.rdf112.sweep.domain.model.PageIssueCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.util.UUID

/** Executes one real, temporary MediaStore image through the Android paging adapter. */
@RunWith(AndroidJUnit4::class)
class MediaStorePagingInstrumentedTest {
    @Test
    fun insertedImageAppearsInBoundedPageOrProviderIsExplicitlyRejected() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val readPermission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, readPermission)

        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val fileName = "sweep_paging_test_${UUID.randomUUID()}.png"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SweepInstrumentation/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val insertedUri = requireNotNull(resolver.insert(collection, values))
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

        try {
            resolver.openOutputStream(insertedUri, "w").use { output ->
                requireNotNull(output)
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            val publish = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            assertTrue(resolver.update(insertedUri, publish, null, null) >= 1)

            val repository = AndroidMediaPageRepository(context, resolver)
            val session = repository.createSession()
            val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY
            assertTrue(volume in session.volumeNames)
            val page = repository.loadPage(
                session,
                MediaPageRequest(
                    sessionId = session.id,
                    kind = MediaKind.IMAGE,
                    volumeName = volume,
                    offset = 0,
                    pageSize = 100,
                ),
            )

            assertTrue(page.rowsRead <= 100)
            assertTrue(
                page.outcome == MediaPageOutcome.MORE_AVAILABLE ||
                    page.outcome == MediaPageOutcome.END_OF_SOURCE ||
                    page.outcome == MediaPageOutcome.PAGING_UNSUPPORTED
            )
            if (page.outcome == MediaPageOutcome.MORE_AVAILABLE ||
                page.outcome == MediaPageOutcome.END_OF_SOURCE
            ) {
                assertTrue("Inserted test image must be visible in the first sorted page", page.records.any { it.uri == insertedUri.toString() })
            } else {
                // A provider which does not prove the structured paging contract must fail closed.
                assertTrue(page.records.isEmpty())
            }
        } finally {
            bitmap.recycle()
            resolver.delete(insertedUri, null, null)
        }
    }
    @Test
    fun audioOnlyPermissionCanCreateSessionAndReadAudioPage() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val readPermission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, readPermission)

        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val fileName = "sweep_audio_paging_test_${UUID.randomUUID()}.mp3"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/SweepInstrumentation/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val insertedUri = requireNotNull(resolver.insert(collection, values))

        try {
            resolver.openOutputStream(insertedUri, "w").use { output ->
                requireNotNull(output).write(byteArrayOf(0x49, 0x44, 0x33, 0x04))
            }
            val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            assertTrue(resolver.update(insertedUri, publish, null, null) >= 1)

            val repository = AndroidMediaPageRepository(context, resolver)
            val session = repository.createSession()
            assertTrue(session.accessByKind[MediaKind.AUDIO] == com.rdf112.sweep.domain.model.AccessLevel.FULL)
            val page = repository.loadPage(
                session,
                MediaPageRequest(
                    sessionId = session.id,
                    kind = MediaKind.AUDIO,
                    volumeName = MediaStore.VOLUME_EXTERNAL_PRIMARY,
                    offset = 0,
                    pageSize = 100,
                ),
            )
            assertTrue(page.rowsRead <= 100)
            assertTrue(
                page.outcome == MediaPageOutcome.MORE_AVAILABLE ||
                    page.outcome == MediaPageOutcome.END_OF_SOURCE ||
                    page.outcome == MediaPageOutcome.PAGING_UNSUPPORTED
            )
            if (page.outcome == MediaPageOutcome.MORE_AVAILABLE ||
                page.outcome == MediaPageOutcome.END_OF_SOURCE
            ) {
                assertTrue(page.records.any { it.uri == insertedUri.toString() })
            } else {
                assertTrue(page.records.isEmpty())
            }
        } finally {
            resolver.delete(insertedUri, null, null)
        }
    }

    @Test
    fun selectedVisualPermissionIsReportedAsPartialCoverage() = runBlocking {
        assumeTrue("Partial visual access exists from Android 14 onward", Build.VERSION.SDK_INT >= 34)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        runCatching { automation.revokeRuntimePermission(context.packageName, Manifest.permission.READ_MEDIA_IMAGES) }
        runCatching { automation.revokeRuntimePermission(context.packageName, Manifest.permission.READ_MEDIA_VIDEO) }
        runCatching { automation.revokeRuntimePermission(context.packageName, Manifest.permission.READ_MEDIA_AUDIO) }
        automation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        val repository = AndroidMediaPageRepository(context)
        val session = repository.createSession()
        assertTrue(session.accessByKind[MediaKind.IMAGE] == AccessLevel.PARTIAL)
        assertTrue(session.accessByKind[MediaKind.VIDEO] == AccessLevel.PARTIAL)
        assertTrue(session.accessByKind[MediaKind.AUDIO] == AccessLevel.DENIED)

        val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY
        val page = repository.loadPage(
            session,
            MediaPageRequest(
                sessionId = session.id,
                kind = MediaKind.IMAGE,
                volumeName = volume,
                offset = 0,
                pageSize = 100,
            ),
        )
        assertTrue(page.issues.any { it.code == PageIssueCode.PARTIAL_ACCESS })
    }

}
