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
import org.junit.runners.MethodSorters
import org.junit.runner.RunWith
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

        val volume = awaitAvailableExternalVolume(context)
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(volume)
        val fileName = "sweep_paging_test_${UUID.randomUUID()}.png"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MidiastoreCodexTest/")
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
            awaitStableMediaStoreRevision(context, volume)

            val repository = AndroidMediaPageRepository(context, resolver)
            // Do not assume a newly inserted row must be in page zero: OEM providers can
            // return a different stable ordering. Walk bounded pages and require either
            // that the row is discovered or that paging is explicitly rejected fail-closed.
            var discovered = false
            var providerRejectedPaging = false
            var lastOutcome: MediaPageOutcome? = null
            var completedWithStableSession = false
            for (sessionAttempt in 0 until MAX_SESSION_RESTARTS) {
                val session = repository.createSession()
                assertTrue(volume in session.volumeNames)
                val freshnessBeforeFirstPage = repository.assessSession(session)
                var offset = 0
                var sourceChanged = false

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
                        assertSessionWasInvalidatedBySourceChange(repository, session, page.records, page.issues)
                        sourceChanged = true
                        break
                    }
                    assertTrue(
                        "Unexpected MediaStore outcome at offset=$offset: ${page.outcome}; " +
                            "issues=${page.issues}; sessionRevisions=${session.revisionsByVolume}; " +
                            "freshnessBeforePage=$freshnessBeforeFirstPage; " +
                            "freshnessAfterPage=${repository.assessSession(session)}",
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

                if (sourceChanged) {
                    assertTrue(
                        "MediaStore source kept changing across $MAX_SESSION_RESTARTS page attempts; " +
                            "last outcome=$lastOutcome",
                        sessionAttempt < MAX_SESSION_RESTARTS - 1,
                    )
                    awaitStableMediaStoreRevision(context, volume)
                    continue
                }

                completedWithStableSession = true
                break
            }

            assertTrue("Paging must complete against one stable MediaStore session", completedWithStableSession)
            assertTrue(
                "Inserted image was not discovered in $MAX_TEST_PAGES bounded pages; last outcome=$lastOutcome",
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

        val volume = awaitAvailableExternalVolume(context)
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(volume)
        val fileName = "sweep_audio_paging_test_${UUID.randomUUID()}.wav"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/MidiastoreCodexTest/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val insertedUri = requireNotNull(resolver.insert(collection, values))

        try {
            resolver.openOutputStream(insertedUri, "w").use { output ->
                requireNotNull(output).write(createValidPcmWaveFixture())
            }
            val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            assertTrue(resolver.update(insertedUri, publish, null, null) >= 1)
            awaitStableMediaStoreRevision(context, volume)

            val repository = AndroidMediaPageRepository(context, resolver)
            var stablePage: com.rdf112.sweep.domain.model.MediaPage? = null
            var lastFreshness: com.rdf112.sweep.domain.model.SessionFreshness? = null
            for (attempt in 0 until MAX_SESSION_RESTARTS) {
                val session = repository.createSession()
                assertTrue(session.accessByKind[MediaKind.AUDIO] == AccessLevel.FULL)
                assertTrue(volume in session.volumeNames)
                val page = repository.loadPage(
                    session,
                    MediaPageRequest(
                        sessionId = session.id,
                        kind = MediaKind.AUDIO,
                        volumeName = volume,
                        offset = 0,
                        pageSize = 100,
                    ),
                )
                if (page.outcome != MediaPageOutcome.SESSION_STALE) {
                    stablePage = page
                    break
                }
                assertSessionWasInvalidatedBySourceChange(repository, session, page.records, page.issues)
                lastFreshness = repository.assessSession(session)
                assertTrue(
                    "Audio MediaStore source kept changing across $MAX_SESSION_RESTARTS attempts; " +
                        "lastFreshness=$lastFreshness",
                    attempt < MAX_SESSION_RESTARTS - 1,
                )
                awaitStableMediaStoreRevision(context, volume)
            }
            val page = requireNotNull(stablePage) {
                "Could not read an audio page from a stable MediaStore session; lastFreshness=$lastFreshness"
            }
            assertTrue(
                "Audio page outcome=${page.outcome}; issues=${page.issues}; rowsRead=${page.rowsRead}",
                page.rowsRead <= 100,
            )
            assertTrue(
                "Audio page outcome=${page.outcome}; issues=${page.issues}; rowsRead=${page.rowsRead}",
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
        val volume = awaitAvailableExternalVolume(context)

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
        val collection = MediaStore.Images.Media.getContentUri(volume)
        val createdUris = mutableListOf<Uri>()

        fun insertFixture(name: String, bytes: ByteArray): Uri {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MidiastoreCodexTest/")
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

    @Test
    fun zSyntheticMediaFixturesAreRemovedAfterTheirScenarios() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val resolver = context.contentResolver
        val volume = awaitAvailableExternalVolume(context)
        val fixturePrefixesByCollection = listOf(
            MediaStore.Images.Media.getContentUri(volume) to listOf(
                "sweep_paging_test_",
                "sweep_hash_",
                "midiastore-api35-delete-",
            ),
            MediaStore.Audio.Media.getContentUri(volume) to listOf("sweep_audio_paging_test_"),
        )
        fixturePrefixesByCollection.forEach { (collection, prefixes) ->
            resolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                arrayOf(
                    if (collection == MediaStore.Images.Media.getContentUri(volume)) {
                        "Pictures/MidiastoreCodexTest/"
                    } else {
                        "Music/MidiastoreCodexTest/"
                    },
                ),
                null,
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val displayName = cursor.getString(nameIndex).orEmpty()
                    assertTrue(
                        "Synthetic test fixture $displayName was not removed by its scenario",
                        prefixes.none(displayName::startsWith),
                    )
                }
            } ?: throw AssertionError("MediaStore returned a null cursor while auditing test fixture cleanup")
        }
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

    private suspend fun assertSessionWasInvalidatedBySourceChange(
        repository: AndroidMediaPageRepository,
        session: com.rdf112.sweep.domain.model.MediaScanSession,
        records: List<com.rdf112.sweep.domain.model.MediaRecord>,
        issues: List<com.rdf112.sweep.domain.model.PageIssue>,
    ) {
        assertTrue("A stale page must not expose rows", records.isEmpty())
        assertTrue(issues.any { it.code == PageIssueCode.SESSION_STALE })
        assertEquals(
            "SESSION_STALE must correspond to a verified MediaStore source change",
            SessionFreshness.SOURCE_CHANGED,
            repository.assessSession(session),
        )
    }

    @Test
    fun aSelectedVisualPermissionIsReportedAsPartialCoverage() = runBlocking {
        assumeTrue("Partial visual access exists from Android 14 onward", Build.VERSION.SDK_INT >= 34)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        assumeTrue(
            "Configure this isolated test with only visual-selected access before instrumentation; " +
                "revoking permissions inside this process causes Android to kill the target app",
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES,
            ) == android.content.pm.PackageManager.PERMISSION_DENIED,
        )
        assumeTrue(
            "Configure this isolated test with audio denied before instrumentation",
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO,
            ) == android.content.pm.PackageManager.PERMISSION_DENIED,
        )
        assumeTrue(
            "Configure this isolated test with video denied before instrumentation",
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO,
            ) == android.content.pm.PackageManager.PERMISSION_DENIED,
        )
        automation.grantRuntimePermission(
            context.packageName,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        val repository = AndroidMediaPageRepository(context)
        val session = repository.createSession()
        assertTrue(session.accessByKind[MediaKind.IMAGE] == AccessLevel.PARTIAL)
        assertTrue(session.accessByKind[MediaKind.VIDEO] == AccessLevel.PARTIAL)
        assertTrue(session.accessByKind[MediaKind.AUDIO] == AccessLevel.DENIED)

        val volume = awaitAvailableExternalVolume(context)
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
            assertTrue(
                "Expected a partial-access page, outcome=${page.outcome}; issues=${page.issues}; " +
                    "sessionAccess=${session.accessByKind}",
                page.issues.any { it.code == PageIssueCode.PARTIAL_ACCESS },
            )
    }

    private fun awaitAvailableExternalVolume(
        context: android.content.Context,
        timeoutMs: Long = 120_000,
    ): String {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        var availableVolumes: Set<String> = emptySet()
        var lastFailure: String? = null
        while (SystemClock.elapsedRealtime() < deadline) {
            try {
                availableVolumes = MediaStore.getExternalVolumeNames(context)
                if (availableVolumes.isNotEmpty()) {
                    return if (MediaStore.VOLUME_EXTERNAL_PRIMARY in availableVolumes) {
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    } else {
                        availableVolumes.sorted().first()
                    }
                }
                lastFailure = "MediaStore reported no available external volumes"
            } catch (error: RuntimeException) {
                lastFailure = "${error.javaClass.simpleName}: ${error.message}"
            }
            SystemClock.sleep(250)
        }
        throw AssertionError(
            "No external MediaStore volume became available within ${timeoutMs}ms; " +
                "availableVolumes=$availableVolumes; lastFailure=$lastFailure",
        )
    }

    /** MediaStore generation advances asynchronously while its scanner publishes fixture changes. */
    private fun awaitStableMediaStoreRevision(
        context: android.content.Context,
        volumeName: String,
        stableWindowMs: Long = 1_000,
        timeoutMs: Long = 20_000,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        var previousVersion = MediaStore.getVersion(context, volumeName)
        var previousGeneration = MediaStore.getGeneration(context, volumeName)
        var stableSince = SystemClock.elapsedRealtime()

        while (SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(250)
            val currentVersion = MediaStore.getVersion(context, volumeName)
            val currentGeneration = MediaStore.getGeneration(context, volumeName)
            if (currentVersion != previousVersion || currentGeneration != previousGeneration) {
                previousVersion = currentVersion
                previousGeneration = currentGeneration
                stableSince = SystemClock.elapsedRealtime()
            } else if (SystemClock.elapsedRealtime() - stableSince >= stableWindowMs) {
                return
            }
        }

        throw AssertionError(
            "MediaStore revision did not stabilize within ${timeoutMs}ms; " +
                "version=$previousVersion generation=$previousGeneration",
        )
    }

    private companion object {
        const val MAX_SESSION_RESTARTS = 3
        const val MAX_TEST_PAGES = 20
    }

}
