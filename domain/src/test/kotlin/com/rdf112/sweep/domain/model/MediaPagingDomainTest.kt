package com.rdf112.sweep.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaPagingDomainTest {
    private val fullAccess = MediaKind.entries.associateWith { AccessLevel.FULL }
    private val volumes = setOf("external")
    private val revision = MediaSourceRevision(version = "provider-v1", generation = 10)

    private fun record(
        id: String,
        kind: MediaKind = MediaKind.IMAGE,
        volume: String = "external",
        bytes: Long = 10,
    ) = MediaRecord(
        uri = "content://media/$volume/$id",
        volumeName = volume,
        kind = kind,
        displayName = "$id.bin",
        mimeType = "application/octet-stream",
        sizeBytes = bytes,
        dateModifiedEpochSeconds = 1,
    )

    private fun request(offset: Int = 0, pageSize: Int = 100) = MediaPageRequest(
        sessionId = "scan-1",
        kind = MediaKind.IMAGE,
        volumeName = "external",
        offset = offset,
        pageSize = pageSize,
    )

    private fun session(
        access: Map<MediaKind, AccessLevel> = fullAccess,
        capturedVolumes: Set<String> = volumes,
        revisions: Map<String, MediaSourceRevision> = mapOf("external" to revision),
    ) = MediaScanSession(
        id = "scan-1",
        startedAtEpochMillis = 100,
        accessByKind = access,
        volumeNames = capturedVolumes,
        revisionsByVolume = revisions,
    )

    @Test
    fun pageAdvancesByProviderRowsAndKeepsOnlyRequestedKindAndVolume() {
        val first = record("1")
        val second = record("2")
        val page = MediaPage(
            request = request(offset = 40, pageSize = 10),
            records = listOf(first, second),
            rowsRead = 3,
            providerTotalCount = 100,
            nextOffset = 43,
            outcome = MediaPageOutcome.MORE_AVAILABLE,
            issues = listOf(PageIssue(PageIssueCode.INVALID_ROW)),
        )

        assertEquals(listOf(first, second), page.records)
        assertEquals(3, page.rowsRead)
        assertEquals(43, page.nextOffset)
        assertEquals(100L, page.providerTotalCount)
        assertEquals(PageIssueCode.INVALID_ROW, page.issues.single().code)
    }

    @Test
    fun terminalPageCanBeEmptyAndHaveUnknownTotal() {
        val page = MediaPage(
            request = request(offset = 100),
            records = emptyList(),
            rowsRead = 0,
            providerTotalCount = null,
            nextOffset = null,
            outcome = MediaPageOutcome.END_OF_SOURCE,
        )
        assertTrue(page.records.isEmpty())
        assertEquals(null, page.providerTotalCount)
        assertEquals(null, page.nextOffset)
    }

    @Test
    fun pageDefensivelyCopiesRecordsAndIssues() {
        val records = mutableListOf(record("1"))
        val issues = mutableListOf(PageIssue(PageIssueCode.INVALID_ROW, detail = "row skipped"))
        val page = MediaPage(
            request = request(),
            records = records,
            rowsRead = 1,
            providerTotalCount = 1,
            nextOffset = null,
            outcome = MediaPageOutcome.END_OF_SOURCE,
            issues = issues,
        )
        records.clear()
        issues.clear()

        assertEquals(1, page.records.size)
        assertEquals(1, page.issues.size)
        assertFailsWith<UnsupportedOperationException> { (page.records as MutableList).clear() }
        assertFailsWith<UnsupportedOperationException> { (page.issues as MutableList).clear() }
    }

    @Test
    fun pageRejectsRowsOrRecordsAboveRequestedBound() {
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(pageSize = 2), listOf(record("1")), 3, null, null, MediaPageOutcome.END_OF_SOURCE)
        }
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(pageSize = 1), listOf(record("1"), record("2")), 1, null, null, MediaPageOutcome.END_OF_SOURCE)
        }
    }

    @Test
    fun pageRejectsDuplicateUrisAndMismatchedKindOrVolume() {
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(), listOf(record("1"), record("1")), 2, null, null, MediaPageOutcome.END_OF_SOURCE)
        }
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(), listOf(record("1", kind = MediaKind.VIDEO)), 1, null, null, MediaPageOutcome.END_OF_SOURCE)
        }
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(), listOf(record("1", volume = "removable")), 1, null, null, MediaPageOutcome.END_OF_SOURCE)
        }
    }

    @Test
    fun moreAvailableRequiresAnAdvancingOffsetExactlyMatchingRowsRead() {
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(offset = 5, pageSize = 10), listOf(record("1")), 1, null, 5, MediaPageOutcome.MORE_AVAILABLE)
        }
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(offset = 5, pageSize = 10), listOf(record("1")), 1, null, 7, MediaPageOutcome.MORE_AVAILABLE)
        }
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(), emptyList(), 0, null, 1, MediaPageOutcome.MORE_AVAILABLE)
        }
    }

    @Test
    fun terminalAndFailedPagesCannotExposeNextOffsetOrPartialProviderRows() {
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(), emptyList(), 0, null, 1, MediaPageOutcome.END_OF_SOURCE)
        }
        assertFailsWith<IllegalArgumentException> {
            MediaPage(
                request(), listOf(record("1")), 1, null, null, MediaPageOutcome.FAILED,
                listOf(PageIssue(PageIssueCode.PROVIDER_FAILURE)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(), emptyList(), 0, null, null, MediaPageOutcome.FAILED)
        }
    }

    @Test
    fun staleAndUnsupportedPagesRequireExplicitIssuesAndExposeNoRecords() {
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(), emptyList(), 0, null, null, MediaPageOutcome.SESSION_STALE)
        }
        assertFailsWith<IllegalArgumentException> {
            MediaPage(
                request(), listOf(record("1")), 1, null, null, MediaPageOutcome.SESSION_STALE,
                listOf(PageIssue(PageIssueCode.SESSION_STALE)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(), emptyList(), 0, null, null, MediaPageOutcome.PAGING_UNSUPPORTED)
        }

        val stale = MediaPage(
            request(), emptyList(), 0, null, null, MediaPageOutcome.SESSION_STALE,
            listOf(PageIssue(PageIssueCode.ACCESS_CHANGED)),
        )
        val unsupported = MediaPage(
            request(), emptyList(), 0, null, null, MediaPageOutcome.PAGING_UNSUPPORTED,
            listOf(PageIssue(PageIssueCode.PAGING_UNSUPPORTED)),
        )
        assertEquals(PageIssueCode.ACCESS_CHANGED, stale.issues.single().code)
        assertEquals(PageIssueCode.PAGING_UNSUPPORTED, unsupported.issues.single().code)
    }

    @Test
    fun cancelledPageMustDiscardIncompleteProviderRows() {
        assertFailsWith<IllegalArgumentException> {
            MediaPage(request(), listOf(record("1")), 1, null, null, MediaPageOutcome.CANCELLED)
        }
        val cancelled = MediaPage(request(), emptyList(), 0, null, null, MediaPageOutcome.CANCELLED)
        assertEquals(MediaPageOutcome.CANCELLED, cancelled.outcome)
    }

    @Test
    fun requestValidatesIdentityOffsetAndBoundedPageSize() {
        assertFailsWith<IllegalArgumentException> { request().copy(sessionId = " ") }
        assertFailsWith<IllegalArgumentException> { request().copy(volumeName = " ") }
        assertFailsWith<IllegalArgumentException> { request().copy(offset = -1) }
        assertFailsWith<IllegalArgumentException> { request(pageSize = 0) }
        assertFailsWith<IllegalArgumentException> { request(pageSize = MediaPageRequest.MAX_PAGE_SIZE + 1) }
        assertFailsWith<IllegalArgumentException> {
            MediaPageRequest("scan", MediaKind.IMAGE, "external", Int.MAX_VALUE - 5, 10)
        }
        assertEquals(100, MediaPageRequest.DEFAULT_PAGE_SIZE)
        assertEquals(500, MediaPageRequest.MAX_PAGE_SIZE)
    }

    @Test
    fun unchangedPermissionsVolumesAndGenerationsAreRevisionMatched() {
        val state = session()
        assertEquals(
            SessionFreshness.REVISION_UNCHANGED,
            state.assessFreshness(fullAccess, volumes, mapOf("external" to revision)),
        )
    }

    @Test
    fun sessionDetectsPermissionVolumeVersionAndGenerationChanges() {
        val state = session()
        assertEquals(
            SessionFreshness.ACCESS_CHANGED,
            state.assessFreshness(fullAccess + (MediaKind.IMAGE to AccessLevel.PARTIAL), volumes, mapOf("external" to revision)),
        )
        assertEquals(
            SessionFreshness.VOLUMES_CHANGED,
            state.assessFreshness(fullAccess, volumes + "removable", mapOf("external" to revision)),
        )
        assertEquals(
            SessionFreshness.SOURCE_CHANGED,
            state.assessFreshness(fullAccess, volumes, mapOf("external" to revision.copy(generation = 11))),
        )
        assertEquals(
            SessionFreshness.SOURCE_CHANGED,
            state.assessFreshness(fullAccess, volumes, mapOf("external" to revision.copy(version = "provider-v2"))),
        )
    }

    @Test
    fun unavailableProviderVersionIsUnverifiableEvenWhenGenerationIsPresent() {
        val unavailable = MediaSourceRevision(
            version = MediaSourceRevision.UNAVAILABLE_VERSION,
            generation = 10L,
        )
        val state = session(revisions = mapOf("external" to unavailable))
        assertEquals(
            SessionFreshness.UNVERIFIABLE,
            state.assessFreshness(fullAccess, volumes, mapOf("external" to unavailable)),
        )
    }

    @Test
    fun missingGenerationOrRevisionIsUnverifiableNotCurrent() {
        val api29Like = session(revisions = mapOf("external" to revision.copy(generation = null)))
        assertEquals(
            SessionFreshness.UNVERIFIABLE,
            api29Like.assessFreshness(fullAccess, volumes, mapOf("external" to revision.copy(generation = null))),
        )
        assertEquals(
            SessionFreshness.UNVERIFIABLE,
            session().assessFreshness(fullAccess, volumes, emptyMap()),
        )
    }

    @Test
    fun sessionDefensivelyCopiesInputsAndExposesUnmodifiableCollections() {
        val mutableAccess = fullAccess.toMutableMap()
        val mutableVolumes = volumes.toMutableSet()
        val mutableRevisions = mutableMapOf("external" to revision)
        val state = session(mutableAccess, mutableVolumes, mutableRevisions)
        mutableAccess.clear()
        mutableVolumes.clear()
        mutableRevisions.clear()

        assertEquals(fullAccess, state.accessByKind)
        assertEquals(volumes, state.volumeNames)
        assertEquals(mapOf("external" to revision), state.revisionsByVolume)
        assertFailsWith<UnsupportedOperationException> { (state.accessByKind as MutableMap).clear() }
        assertFailsWith<UnsupportedOperationException> { (state.volumeNames as MutableSet).clear() }
        assertFailsWith<UnsupportedOperationException> { (state.revisionsByVolume as MutableMap).clear() }
    }

    @Test
    fun sessionAndRevisionValidateTheirRequiredFields() {
        assertFailsWith<IllegalArgumentException> {
            MediaScanSession(" ", 100, fullAccess, volumes, mapOf("external" to revision))
        }
        assertFailsWith<IllegalArgumentException> {
            MediaScanSession("scan", -1, fullAccess, volumes, mapOf("external" to revision))
        }
        assertFailsWith<IllegalArgumentException> {
            MediaScanSession("scan", 0, fullAccess, emptySet(), emptyMap())
        }
        assertFailsWith<IllegalArgumentException> {
            MediaScanSession("scan", 0, fullAccess, volumes, mapOf("unknown" to revision))
        }
        assertFailsWith<IllegalArgumentException> { MediaSourceRevision(" ", null) }
        assertFailsWith<IllegalArgumentException> { MediaSourceRevision("v1", -1) }
        assertFalse(session().volumeNames.isEmpty())
        assertTrue(session().id == "scan-1")
    }
}
