package com.rdf112.sweep.data.media

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rdf112.sweep.domain.model.AccessLevel
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaPageOutcome
import com.rdf112.sweep.domain.model.MediaPageRequest
import com.rdf112.sweep.domain.model.PageIssueCode
import com.rdf112.sweep.domain.model.SessionFreshness
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.UUID

/** Executes one real, temporary MediaStore image through the Android paging adapter. */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
        val volume = availableMediaStoreVolume(context)
        val collection = MediaStore.Images.Media.getContentUri(volume)
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
            var discovered = false
            var providerRejectedPaging = false
            var restartCount = 0
            var lastOutcome: MediaPageOutcome? = null

            for (sessionAttempt in 0 until MAX_SESSION_RESTARTS) {
                awaitStableMediaStoreRevision(context, volume)
                val session = repository.createSession()
                assertTrue("Runtime volume $volume must belong to the captured session", volume in session.volumeNames)

                var offset = 0
                var restartSession = false
                for (pageIndex in 0 until MAX_TEST_PAGES) {
                    val page = repository.loadPage(
                        session,
                        MediaPageRequest(
                            sessionId = session.id,
                            kind = MediaKind.IMAGE,
                            volumeName = volume,
                            offset = offset,
                            pageSize = 100,
                        ),
                    )
                    lastOutcome = page.outcome

                    if (page.outcome == MediaPageOutcome.SESSION_STALE) {
                        assertEquals(
                            "A stale paging session must be backed by a proven source revision change",
                            SessionFreshness.SOURCE_CHANGED,
                            repository.assessSession(session),
                        )
                        restartSession = true
                        restartCount++
                        break
                    }

                    assertTrue(
                        "Unexpected MediaStore outcome at offset=$offset: ${page.outcome}; issues=${page.issues}",
                        page.outcome == MediaPageOutcome.MORE_AVAILABLE ||
                            page.outcome == MediaPageOutcome.END_OF_SOURCE ||
                            page.outcome == MediaPageOutcome.PAGING_UNSUPPORTED,
                    )
                    assertTrue("A bounded page must not exceed 100 provider rows", page.rowsRead <= 100)

                    if (page.outcome == MediaPageOutcome.PAGING_UNSUPPORTED) {
                        assertTrue("Unsupported paging must not expose ambiguous records", page.records.isEmpty())
                        providerRejectedPaging = true
                        break
                    }
                    if (page.records.any { it.uri == insertedUri.toString() }) {
                        discovered = true
                        break
                    }
                    if (page.outcome == MediaPageOutcome.END_OF_SOURCE) break

                    val nextOffset = page.nextOffset
                    assertTrue(
                        "A successful non-terminal page must advance monotonically",
                        nextOffset != null && nextOffset > offset,
                    )
                    offset = requireNotNull(nextOffset)
                }

                if (discovered || providerRejectedPaging || !restartSession) break
            }

            assertTrue(
                "Inserted image was not discovered in bounded pages or explicitly rejected; " +
                    "last outcome=$lastOutcome; source-change restarts=$restartCount",
                discovered || providerRejectedPaging,
            )
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
        val volume = availableMediaStoreVolume(context)
        val collection = MediaStore.Audio.Media.getContentUri(volume)
        val fileName = "sweep_audio_paging_test_${UUID.randomUUID()}.wav"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/SweepInstrumentation/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val insertedUri = requireNotNull(resolver.insert(collection, values))

        try {
            resolver.openOutputStream(insertedUri, "w").use { output ->
                requireNotNull(output).write(createValidPcmWaveFixture())
            }
            val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            assertTrue(resolver.update(insertedUri, publish, null, null) >= 1)

            val repository = AndroidMediaPageRepository(context, resolver)
            awaitStableMediaStoreRevision(context, volume)
            var session = repository.createSession()
            assertTrue(volume in session.volumeNames)
            assertTrue(session.accessByKind[MediaKind.AUDIO] == AccessLevel.FULL)

            var page = repository.loadPage(
                session,
                MediaPageRequest(
                    sessionId = session.id,
                    kind = MediaKind.AUDIO,
                    volumeName = volume,
                    offset = 0,
                    pageSize = 100,
                ),
            )
            if (page.outcome == MediaPageOutcome.SESSION_STALE) {
                assertEquals(SessionFreshness.SOURCE_CHANGED, repository.assessSession(session))
                awaitStableMediaStoreRevision(context, volume)
                session = repository.createSession()
                page = repository.loadPage(
                    session,
                    MediaPageRequest(
                        sessionId = session.id,
                        kind = MediaKind.AUDIO,
                        volumeName = volume,
                        offset = 0,
                        pageSize = 100,
                    ),
                )
            }

            assertTrue(page.rowsRead <= 100)
            assertTrue(
                page.outcome == MediaPageOutcome.MORE_AVAILABLE ||
                    page.outcome == MediaPageOutcome.END_OF_SOURCE ||
                    page.outcome == MediaPageOutcome.PAGING_UNSUPPORTED,
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
    fun mediaStoreSha256MatchesIdenticalBytesAndSeparatesDifferentBytes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, permission)

        fun pngBytes(color: Int): ByteArray {
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            return try {
                bitmap.eraseColor(color)
                ByteArrayOutputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                    output.toByteArray()
                }
            } finally {
                bitmap.recycle()
            }
        }

        val identicalBytes = pngBytes(Color.RED)
        val differentBytes = pngBytes(Color.BLUE)
        val resolver = context.contentResolver
        val volume = availableMediaStoreVolume(context)
        val collection = MediaStore.Images.Media.getContentUri(volume)
        val createdUris = mutableListOf<Uri>()

        fun insertFixture(name: String, bytes: ByteArray): Uri {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SweepHashInstrumentation/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = requireNotNull(resolver.insert(collection, values))
            createdUris += uri
            resolver.openOutputStream(uri, "w").use { output ->
                requireNotNull(output).write(bytes)
            }
            val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            assertTrue("Fixture must publish successfully", resolver.update(uri, publish, null, null) >= 1)
            return uri
        }

        try {
            val first = insertFixture("sweep_hash_a_${UUID.randomUUID()}.png", identicalBytes)
            val second = insertFixture("sweep_hash_b_${UUID.randomUUID()}.png", identicalBytes)
            val third = insertFixture("sweep_hash_c_${UUID.randomUUID()}.png", differentBytes)
            val repository = AndroidMediaPageRepository(context, resolver)

            val firstHash = repository.computeSha256(
                first.toString(),
                maxBytes = identicalBytes.size.toLong(),
                expectedBytes = identicalBytes.size.toLong(),
            )
            val secondHash = repository.computeSha256(
                second.toString(),
                maxBytes = identicalBytes.size.toLong(),
                expectedBytes = identicalBytes.size.toLong(),
            )
            val thirdHash = repository.computeSha256(
                third.toString(),
                maxBytes = differentBytes.size.toLong(),
                expectedBytes = differentBytes.size.toLong(),
            )

            assertNotNull("A readable, complete file must produce a SHA-256 digest", firstHash)
            assertNotNull("An identical readable file must produce a digest", secondHash)
            assertNotNull("A different readable file must produce a digest", thirdHash)
            assertTrue("SHA-256 must be 64 lowercase hexadecimal characters", firstHash!!.matches(Regex("[0-9a-f]{64}")))
            assertEquals("Identical file bytes must have identical SHA-256", firstHash, secondHash)
            assertTrue("Different file bytes must not share a SHA-256 digest", firstHash != thirdHash)
        } finally {
            createdUris.forEach { uri -> runCatching { resolver.delete(uri, null, null) } }
        }
    }

    private fun availableMediaStoreVolume(context: android.content.Context): String {
        val volumes = MediaStore.getExternalVolumeNames(context)
        return volumes.firstOrNull { it == MediaStore.VOLUME_EXTERNAL_PRIMARY }
            ?: volumes.sorted().firstOrNull()
            ?: throw AssertionError("MediaStore exposed no external volume to the test app")
    }

    private fun awaitStableMediaStoreRevision(context: android.content.Context, volume: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        var previous = MediaStore.getGeneration(context, volume)
        var unchangedReads = 0
        repeat(24) {
            SystemClock.sleep(150L)
            val current = MediaStore.getGeneration(context, volume)
            if (current == previous) {
                unchangedReads++
                if (unchangedReads >= 2) return
            } else {
                unchangedReads = 0
            }
            previous = current
        }
        throw AssertionError("MediaStore generation did not stabilize for $volume after fixture publication")
    }

    companion object {
        private const val MAX_SESSION_RESTARTS = 3
        private const val MAX_TEST_PAGES = 20
    }

    private fun createValidPcmWaveFixture(): ByteArray {
        val sampleRate = 8_000
        val channels = 1
        val bitsPerSample = 16
        val pcmBytes = ByteArray(1_600) // 100 ms of valid silent PCM audio
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        return java.nio.ByteBuffer.allocate(44 + pcmBytes.size)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt(36 + pcmBytes.size)
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16)
                putShort(1) // PCM
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort(blockAlign.toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(pcmBytes.size)
                put(pcmBytes)
            }
            .array()
    }

    @Test
    fun aSelectedVisualPermissionIsReportedAsPartialCoverage() = runBlocking {
        assumeTrue("Partial visual access exists from Android 14 onward", Build.VERSION.SDK_INT >= 34)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = AndroidMediaPageRepository(context)
        val session = repository.createSession()

        assertTrue(session.accessByKind[MediaKind.IMAGE] == AccessLevel.PARTIAL)
        assertTrue(session.accessByKind[MediaKind.VIDEO] == AccessLevel.PARTIAL)
        assertTrue(session.accessByKind[MediaKind.AUDIO] == AccessLevel.DENIED)

        val volume = availableMediaStoreVolume(context)
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
        assertTrue(page.issues.any { it.code == PageIssueCode.PARTIAL_ACCESS })
    }


}
