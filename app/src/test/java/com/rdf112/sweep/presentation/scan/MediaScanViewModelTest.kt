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

    private class GlobalMissionFakeRepository(
        private val failVideoSource: Boolean = false,
        private val imageRecordCount: Int = 2,
        private val imageFileSizeBytes: Long = 12L * 1024L * 1024L,
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

        override suspend fun createSession(): MediaScanSession = session
        override suspend fun assessSession(session: MediaScanSession): SessionFreshness =
            SessionFreshness.REVISION_UNCHANGED

        override suspend fun computeSha256(uri: String): String? = "f".repeat(64)

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
                displayName = "$id.jpg",
                mimeType = "image/jpeg",
                sizeBytes = imageFileSizeBytes,
                dateModifiedEpochSeconds = 100L + request.offset,
            )
            val more = request.offset < imageRecordCount - 1
            return MediaPage(
                request = request,
                records = listOf(record),
                rowsRead = 1,
                providerTotalCount = imageRecordCount.toLong(),
                nextOffset = if (more) request.offset + 1 else null,
                outcome = if (more) MediaPageOutcome.MORE_AVAILABLE else MediaPageOutcome.END_OF_SOURCE,
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
