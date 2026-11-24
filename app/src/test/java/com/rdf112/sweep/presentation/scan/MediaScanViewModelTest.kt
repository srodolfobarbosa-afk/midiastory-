package com.rdf112.sweep.presentation.scan

import com.rdf112.sweep.domain.oaas.DesiredOutcomeMission
import com.rdf112.sweep.domain.model.AccessLevel
import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaPage
import com.rdf112.sweep.domain.model.MediaPageOutcome
import com.rdf112.sweep.domain.model.MediaPageRequest
import com.rdf112.sweep.domain.model.MediaRecord
import com.rdf112.sweep.domain.model.MediaScanSession
import com.rdf112.sweep.domain.model.MediaSourceRevision
import com.rdf112.sweep.domain.model.SessionFreshness
import com.rdf112.sweep.domain.repository.MediaPageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MediaScanViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadsBoundedFirstPageAndNavigatesWithoutLosingUriSelection() = runTest {
        val repository = FakePageRepository(overlapSecondPage = false)
        val viewModel = MediaScanViewModel(repository)

        viewModel.startSession()
        advanceUntilIdle()

        assertEquals(ScanUiStatus.READY, viewModel.state.value.status)
        assertEquals(0, viewModel.state.value.currentPage?.request?.offset)
        assertEquals(SWEEP_PAGE_SIZE, viewModel.state.value.currentPage?.request?.pageSize)
        val first = requireNotNull(viewModel.state.value.currentPage).records.single()
        viewModel.setSelected(first, true)
        assertTrue(viewModel.state.value.selection.isSelected(first.uri))

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(ScanUiStatus.READY, viewModel.state.value.status)
        assertEquals(1, viewModel.state.value.currentPage?.request?.offset)
        assertTrue(viewModel.state.value.selection.isSelected(first.uri))
    }

    @Test
    fun ignoresStaleCheckboxEventsWhileTheNextPageIsLoading() = runTest {
        val viewModel = MediaScanViewModel(FakePageRepository(overlapSecondPage = false))
        viewModel.startSession()
        advanceUntilIdle()

        val firstPageRecord = requireNotNull(viewModel.state.value.currentPage).records.single()
        viewModel.loadNextPage()
        assertTrue(viewModel.state.value.isLoading)
        viewModel.setSelected(firstPageRecord, true)
        assertTrue(viewModel.state.value.selection.isEmpty)

        advanceUntilIdle()
        assertEquals(ScanUiStatus.READY, viewModel.state.value.status)
    }

    @Test
    fun invalidatesPartialAccessSessionOnResumeBecauseSelectedSetIsNotSnapshotStable() = runTest {
        val viewModel = MediaScanViewModel(FakePageRepository(overlapSecondPage = false, partialImageAccess = true))
        viewModel.startSession()
        advanceUntilIdle()
        assertEquals(ScanUiStatus.READY, viewModel.state.value.status)

        val visible = requireNotNull(viewModel.state.value.currentPage).records.single()
        viewModel.setSelected(visible, true)
        assertTrue(viewModel.state.value.selection.isSelected(visible.uri))

        viewModel.checkSessionFreshness()
        assertEquals(ScanUiStatus.SESSION_STALE, viewModel.state.value.status)
        assertTrue(viewModel.state.value.session == null)
        assertTrue(viewModel.state.value.selection.isEmpty)
    }

    @Test
    fun marksExistingPageInformationalWhenItsRevisionBecomesUnverifiable() = runTest {
        val repository = FakePageRepository(overlapSecondPage = false)
        val viewModel = MediaScanViewModel(repository)
        viewModel.startSession()
        advanceUntilIdle()
        assertEquals(ScanUiStatus.READY, viewModel.state.value.status)

        repository.freshness = SessionFreshness.UNVERIFIABLE
        viewModel.checkSessionFreshness()
        advanceUntilIdle()

        assertEquals(ScanUiStatus.PARTIAL, viewModel.state.value.status)
        assertTrue(
            viewModel.state.value.currentPage?.issues?.any {
                it.code == com.rdf112.sweep.domain.model.PageIssueCode.REVISION_UNVERIFIABLE
            } == true
        )
    }

    @Test
    fun stopsWhenProviderRepeatsAnUriOnAnotherOffset() = runTest {
        val viewModel = MediaScanViewModel(FakePageRepository(overlapSecondPage = true))
        viewModel.startSession()
        advanceUntilIdle()
        assertEquals(ScanUiStatus.READY, viewModel.state.value.status)

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(ScanUiStatus.PAGING_UNSUPPORTED, viewModel.state.value.status)
        assertTrue(viewModel.state.value.currentPage?.request?.offset == 0)
    }


    @Test
    fun hidesReviewAccessActionWhenPageFailsExclusivelyFromProviderError() = runTest {
        val repo = FakePageRepository(overlapSecondPage = false, failProvider = true)
        val viewModel = MediaScanViewModel(repo)
        viewModel.startSession()
        advanceUntilIdle()

        assertEquals(ScanUiStatus.FAILED, viewModel.state.value.status)
        org.junit.Assert.assertFalse(viewModel.state.value.shouldShowReviewAccessAction)
    }

    @Test
    fun buildsExportReviewSummaryWhenSelectionIsNonEmpty() = runTest {
        val viewModel = MediaScanViewModel(FakePageRepository(overlapSecondPage = false))
        viewModel.startSession()
        advanceUntilIdle()

        org.junit.Assert.assertNull(viewModel.buildReviewSummaryExportText())
        val first = requireNotNull(viewModel.state.value.currentPage).records.single()
        viewModel.setSelected(first, true)
        val summary = requireNotNull(viewModel.buildReviewSummaryExportText())
        assertTrue(summary.contains("Midiastore"))
        assertTrue(summary.contains("same.jpg"))
    }

    @Test
    fun globalDuplicateMissionFindsMatchingFilesAcrossDifferentPages() = runTest {
        val repository = GlobalMissionFakeRepository()
        val viewModel = MediaScanViewModel(repository)

        viewModel.startSession()
        advanceUntilIdle()
        assertEquals(ScanUiStatus.READY, viewModel.state.value.status)

        viewModel.executeGlobalOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
        advanceUntilIdle()

        val progress = requireNotNull(viewModel.state.value.missionProgress)
        val plan = requireNotNull(progress.plan)
        assertTrue(progress.coverageComplete)
        assertEquals(4, progress.pagesProcessed)
        assertEquals(2L, progress.recordsExamined)
        assertEquals(1, plan.protectedReferencesKept)
        assertEquals(1, plan.autoSelectedRecords.size)
        assertEquals(1, viewModel.state.value.selection.size)
        assertTrue(plan.autoSelectedRecords.single().uri.endsWith("/second"))
    }

    @Test
    fun globalMissionLabelsCoveragePartialWhenAProviderPageFails() = runTest {
        val repository = GlobalMissionFakeRepository(failVideoSource = true)
        val viewModel = MediaScanViewModel(repository)

        viewModel.startSession()
        advanceUntilIdle()
        viewModel.executeGlobalOutcomeMission(DesiredOutcomeMission.FREE_MAX_SPACE_NOW)
        advanceUntilIdle()

        val progress = requireNotNull(viewModel.state.value.missionProgress)
        assertTrue(!progress.coverageComplete)
        assertTrue(progress.warningCount > 0)
        assertEquals(ScanUiStatus.PARTIAL, viewModel.state.value.status)
    }

    @Test
    fun globalMissionReportsUnverifiedPagingAndKeepsCoveragePartial() = runTest {
        val repository = GlobalMissionFakeRepository(unverifiedPagingArgs = true)
        val viewModel = MediaScanViewModel(repository)

        viewModel.startSession()
        advanceUntilIdle()
        viewModel.executeGlobalOutcomeMission(DesiredOutcomeMission.FREE_MAX_SPACE_NOW)
        advanceUntilIdle()

        val progress = requireNotNull(viewModel.state.value.missionProgress)
        assertTrue(!progress.coverageComplete)
        assertEquals("paging_args_unverified", progress.message)
        assertEquals(ScanUiStatus.PARTIAL, viewModel.state.value.status)
        assertTrue(progress.warningCount > 0)
    }

    @Test
    fun globalDuplicateMissionDoesNotPrepareDeletionWhenAnySourceIsUnverified() = runTest {
        val repository = GlobalMissionFakeRepository(failVideoSource = true)
        val viewModel = MediaScanViewModel(repository)

        viewModel.startSession()
        advanceUntilIdle()
        viewModel.executeGlobalOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
        advanceUntilIdle()

        val progress = requireNotNull(viewModel.state.value.missionProgress)
        val plan = requireNotNull(progress.plan)
        assertTrue(!progress.coverageComplete)
        assertTrue(plan.autoSelectedRecords.isEmpty())
        assertTrue(viewModel.state.value.selection.isEmpty)
    }

    @Test
    fun globalDuplicateMissionStopsAtHashBudgetAndDoesNotSelectAnyFiles() = runTest {
        val repository = GlobalMissionFakeRepository(
            imageRecordCount = 6,
            imageFileSizeBytes = 2L * 1024L * 1024L * 1024L,
        )
        val viewModel = MediaScanViewModel(repository)

        viewModel.startSession()
        advanceUntilIdle()
        viewModel.executeGlobalOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
        advanceUntilIdle()

        val progress = requireNotNull(viewModel.state.value.missionProgress)
        val plan = requireNotNull(progress.plan)
        assertTrue(!progress.coverageComplete)
        assertTrue(plan.autoSelectedRecords.isEmpty())
        assertTrue(viewModel.state.value.selection.isEmpty)
        assertTrue(progress.warningCount > 0)
    }

    @Test
    fun globalMissionCanRemoveASelectedRecordThatIsNotOnTheCurrentPage() = runTest {
        val repository = GlobalMissionFakeRepository()
        val viewModel = MediaScanViewModel(repository)

        viewModel.startSession()
        advanceUntilIdle()
        viewModel.executeGlobalOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
        advanceUntilIdle()

        val selected = requireNotNull(viewModel.state.value.selection.records.singleOrNull())
        viewModel.setSelected(selected, false)
        assertTrue(viewModel.state.value.selection.isEmpty)
    }

    @Test
    fun globalScreenshots30dMissionSelectsOldScreenshotsAndSetsFilter() = runTest {
        val repository = GlobalMissionFakeRepository(screenshotMode = true)
        val viewModel = MediaScanViewModel(repository)

        viewModel.startSession()
        advanceUntilIdle()
        viewModel.executeGlobalOutcomeMission(
            DesiredOutcomeMission.CLEAN_OLD_SCREENSHOTS_30D,
            nowEpochSeconds = 1_700_000_000L,
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(com.rdf112.sweep.domain.grouping.SmartFilterMode.SCREENSHOTS_30D, state.smartFilter)
        val plan = requireNotNull(state.missionProgress?.plan)
        assertEquals(2, plan.autoSelectedRecords.size)
        assertEquals(2, state.selection.records.size)
    }

    private class GlobalMissionFakeRepository(
        private val failVideoSource: Boolean = false,
        private val unverifiedPagingArgs: Boolean = false,
        private val imageRecordCount: Int = 2,
        private val imageFileSizeBytes: Long = 12L * 1024L * 1024L,
        private val screenshotMode: Boolean = false,
    ) : MediaPageRepository {
        private val volume = "external_primary"
        private val access = mapOf(
            MediaKind.IMAGE to AccessLevel.FULL,
            MediaKind.VIDEO to AccessLevel.FULL,
            MediaKind.AUDIO to AccessLevel.FULL,
        )
        private val session = MediaScanSession(
            id = "global-mission-session",
            startedAtEpochMillis = 1L,
            accessByKind = access,
            volumeNames = setOf(volume),
            revisionsByVolume = mapOf(volume to MediaSourceRevision("test-provider", 1L)),
        )

        var computeSha256CallCount: Int = 0
        val cachedShaByUriAndMeta = mutableMapOf<Triple<String, Long, Long>, String>()
        val persistedShaRecords = mutableListOf<MediaRecord>()

        override suspend fun createSession(): MediaScanSession = session
        override suspend fun assessSession(session: MediaScanSession): SessionFreshness =
            SessionFreshness.REVISION_UNCHANGED

        override suspend fun getCachedSha256Batch(records: Collection<MediaRecord>): Map<String, String> =
            buildMap {
                for (rec in records) {
                    val mod = rec.dateModifiedEpochSeconds ?: continue
                    val hit = cachedShaByUriAndMeta[Triple(rec.uri, rec.sizeBytes, mod)]
                    if (hit != null) put(rec.uri, hit)
                }
            }

        override suspend fun cacheVerifiedSha256Batch(recordsWithSha256: Collection<MediaRecord>) {
            for (rec in recordsWithSha256) {
                val mod = rec.dateModifiedEpochSeconds ?: continue
                val sha = rec.contentSha256 ?: continue
                persistedShaRecords += rec
                cachedShaByUriAndMeta[Triple(rec.uri, rec.sizeBytes, mod)] = sha
            }
        }

        override suspend fun computeSha256(uri: String): String? {
            computeSha256CallCount++
            return "f".repeat(64)
        }

        override suspend fun loadPage(
            session: MediaScanSession,
            request: MediaPageRequest,
        ): MediaPage {
            if (failVideoSource && request.kind == MediaKind.VIDEO) {
                return MediaPage(
                    request = request,
                    records = emptyList(),
                    rowsRead = 0,
                    providerTotalCount = null,
                    nextOffset = null,
                    outcome = MediaPageOutcome.FAILED,
                    issues = listOf(
                        com.rdf112.sweep.domain.model.PageIssue(
                            com.rdf112.sweep.domain.model.PageIssueCode.PROVIDER_FAILURE,
                            request.kind,
                            request.volumeName,
                        )
                    ),
                )
            }
            if (request.kind != MediaKind.IMAGE) {
                return MediaPage(
                    request = request,
                    records = emptyList(),
                    rowsRead = 0,
                    providerTotalCount = 0L,
                    nextOffset = null,
                    outcome = MediaPageOutcome.END_OF_SOURCE,
                )
            }

            if (request.offset !in 0 until imageRecordCount) {
                return MediaPage(
                    request = request,
                    records = emptyList(),
                    rowsRead = 0,
                    providerTotalCount = imageRecordCount.toLong(),
                    nextOffset = null,
                    outcome = MediaPageOutcome.END_OF_SOURCE,
                )
            }
            val id = when (request.offset) {
                0 -> "first"
                1 -> "second"
                else -> "image-${request.offset}"
            }
            val record = MediaRecord(
                uri = "content://media/$volume/images/$id",
                volumeName = volume,
                kind = MediaKind.IMAGE,
                displayName = if (screenshotMode) "Screenshot_$id.png" else "$id.jpg",
                mimeType = if (screenshotMode) "image/png" else "image/jpeg",
                sizeBytes = imageFileSizeBytes,
                dateModifiedEpochSeconds = 100L + request.offset,
                relativePath = if (screenshotMode) "Pictures/Screenshots/" else "DCIM/Camera/",
            )
            val more = request.offset < imageRecordCount - 1
            return MediaPage(
                request = request,
                records = listOf(record),
                rowsRead = 1,
                providerTotalCount = imageRecordCount.toLong(),
                nextOffset = if (more) request.offset + 1 else null,
                outcome = if (more) MediaPageOutcome.MORE_AVAILABLE else MediaPageOutcome.END_OF_SOURCE,
                issues = if (unverifiedPagingArgs) {
                    listOf(
                        com.rdf112.sweep.domain.model.PageIssue(
                            com.rdf112.sweep.domain.model.PageIssueCode.PAGING_ARGS_UNVERIFIED,
                            request.kind,
                            request.volumeName,
                        )
                    )
                } else emptyList(),
            )
        }
    }

    private class FakePageRepository(
        private val overlapSecondPage: Boolean,
        partialImageAccess: Boolean = false,
        private val failProvider: Boolean = false,
    ) : MediaPageRepository {
        var freshness: SessionFreshness = SessionFreshness.REVISION_UNCHANGED
        private val access = mapOf(
            MediaKind.IMAGE to if (partialImageAccess) AccessLevel.PARTIAL else AccessLevel.FULL,
            MediaKind.VIDEO to AccessLevel.FULL,
            MediaKind.AUDIO to AccessLevel.UNKNOWN,
        )
        private val volume = "external_primary"
        private val revision = mapOf(volume to MediaSourceRevision("test-provider", 1L))
        private val session = MediaScanSession("test-session", 1L, access, setOf(volume), revision)

        override suspend fun createSession(): MediaScanSession = session

        override suspend fun assessSession(session: MediaScanSession): SessionFreshness = freshness

        override suspend fun loadPage(session: MediaScanSession, request: MediaPageRequest): MediaPage {
            if (failProvider) {
                return MediaPage(
                    request = request,
                    records = emptyList(),
                    rowsRead = 0,
                    providerTotalCount = null,
                    nextOffset = null,
                    outcome = MediaPageOutcome.FAILED,
                    issues = listOf(com.rdf112.sweep.domain.model.PageIssue(com.rdf112.sweep.domain.model.PageIssueCode.PROVIDER_FAILURE, request.kind, request.volumeName)),
                )
            }
            val id = if (request.offset == 0 || overlapSecondPage) "same" else "next"
            val record = MediaRecord(
                uri = "content://media/$volume/images/$id",
                volumeName = volume,
                kind = request.kind,
                displayName = "$id.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 100L,
                dateModifiedEpochSeconds = 1L,
            )
            val more = request.offset == 0
            return MediaPage(
                request = request,
                records = listOf(record),
                rowsRead = 1,
                providerTotalCount = 2L,
                nextOffset = if (more) 1 else null,
                outcome = if (more) MediaPageOutcome.MORE_AVAILABLE else MediaPageOutcome.END_OF_SOURCE,
            )
        }
    }

    @Test
    fun prepareExternalReviewSelection_loadsSelectableCandidatesAndRejectsProtected() = runTest {
        val repository = FakePageRepository(overlapSecondPage = false)
        val viewModel = MediaScanViewModel(repository)
        val candidate1 = MediaRecord(
            uri = "content://media/external/video/media/101",
            volumeName = "external_primary",
            kind = MediaKind.VIDEO,
            displayName = "VID-20261114-WA0001.mp4",
            mimeType = "video/mp4",
            sizeBytes = 4_500_000L,
            dateModifiedEpochSeconds = 1_700_000_000L,
        )
        val protectedRecord = MediaRecord(
            uri = "content://media/external/images/media/102",
            volumeName = "external_primary",
            kind = MediaKind.IMAGE,
            displayName = "IMG-20261114-WA0002.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 500_000L,
            dateModifiedEpochSeconds = 1_700_000_000L,
            isProtected = true,
        )

        val count = viewModel.prepareExternalReviewSelection(listOf(candidate1, protectedRecord))
        assertEquals(1, count)
        assertEquals(1, viewModel.state.value.selection.records.size)
        assertEquals(candidate1.uri, viewModel.state.value.selection.records.first().uri)

        viewModel.setSelected(candidate1, false)
        assertEquals(0, viewModel.state.value.selection.records.size)
    }

    @Test
    fun globalDuplicateMissionUsesCachedSha256WithoutRehashingUnchangedFiles() = runTest {
        val repository = GlobalMissionFakeRepository()
        val viewModel = MediaScanViewModel(repository)

        viewModel.startSession()
        advanceUntilIdle()

        // Cold run: streams SHA-256 for both same-size candidates and persists to SQLite WAL cache
        viewModel.executeGlobalOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
        advanceUntilIdle()
        assertEquals(2, repository.computeSha256CallCount)
        assertEquals(2, repository.persistedShaRecords.size)

        // Warm run: 0 streaming I/O calls because both candidates match (uri, sizeBytes, dateModified)
        repository.computeSha256CallCount = 0
        repository.persistedShaRecords.clear()
        viewModel.executeGlobalOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
        advanceUntilIdle()

        assertEquals(0, repository.computeSha256CallCount)
        assertEquals(0, repository.persistedShaRecords.size)
        val plan = requireNotNull(viewModel.state.value.missionProgress?.plan)
        assertEquals(1, plan.protectedReferencesKept)
        assertEquals(1, plan.autoSelectedRecords.size)
    }

    @Test
    fun globalDuplicateMissionRehashesWhenMetadataChangesAndUpdatesCache() = runTest {
        val repository = GlobalMissionFakeRepository()
        // Pre-seed cache with valid entry for /first, but stale dateModified (999L != 101L) for /second
        val sizeBytes = 12L * 1024L * 1024L
        repository.cachedShaByUriAndMeta[Triple("content://media/external_primary/images/first", sizeBytes, 100L)] =
            "f".repeat(64)
        repository.cachedShaByUriAndMeta[Triple("content://media/external_primary/images/second", sizeBytes, 999L)] =
            "a".repeat(64)

        val viewModel = MediaScanViewModel(repository)
        viewModel.startSession()
        advanceUntilIdle()

        viewModel.executeGlobalOutcomeMission(DesiredOutcomeMission.ELIMINATE_DUPLICATE_COPIES)
        advanceUntilIdle()

        // Only /second missed the cache and triggered streaming SHA-256
        assertEquals(1, repository.computeSha256CallCount)
        assertEquals(1, repository.persistedShaRecords.size)
        assertTrue(repository.persistedShaRecords.single().uri.endsWith("/second"))
        val plan = requireNotNull(viewModel.state.value.missionProgress?.plan)
        assertEquals(1, plan.autoSelectedRecords.size)
    }

    @Test
    fun fingerprintStoreSha256HexValidatorRejectsInvalidDigests() {
        assertTrue(com.rdf112.sweep.data.media.AndroidMediaFingerprintStore.isValidSha256Hex("a0f9".repeat(16)))
        assertTrue(com.rdf112.sweep.data.media.AndroidMediaFingerprintStore.isValidSha256Hex("A0F9".repeat(16)))
        org.junit.Assert.assertFalse(com.rdf112.sweep.data.media.AndroidMediaFingerprintStore.isValidSha256Hex(null))
        org.junit.Assert.assertFalse(com.rdf112.sweep.data.media.AndroidMediaFingerprintStore.isValidSha256Hex(""))
        org.junit.Assert.assertFalse(com.rdf112.sweep.data.media.AndroidMediaFingerprintStore.isValidSha256Hex("abc"))
        org.junit.Assert.assertFalse(
            com.rdf112.sweep.data.media.AndroidMediaFingerprintStore.isValidSha256Hex("g".repeat(64)),
        )
    }

    class MainDispatcherRule(
        val dispatcher: TestDispatcher = StandardTestDispatcher(),
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }
}
