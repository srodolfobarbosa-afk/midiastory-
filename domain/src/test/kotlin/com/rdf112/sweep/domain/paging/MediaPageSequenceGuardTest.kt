package com.rdf112.sweep.domain.paging

import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaPage
import com.rdf112.sweep.domain.model.MediaPageOutcome
import com.rdf112.sweep.domain.model.MediaPageRequest
import com.rdf112.sweep.domain.model.MediaRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MediaPageSequenceGuardTest {
    private fun page(offset: Int, ids: List<String>, outcome: MediaPageOutcome = MediaPageOutcome.MORE_AVAILABLE): MediaPage {
        val request = MediaPageRequest("session", MediaKind.IMAGE, "external_primary", offset, 100)
        val records = ids.map { id ->
            MediaRecord(
                uri = "content://media/external_primary/images/$id",
                volumeName = "external_primary",
                kind = MediaKind.IMAGE,
                displayName = "$id.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 100L,
                dateModifiedEpochSeconds = 1L,
            )
        }
        val rowsRead = ids.size
        val hasMore = outcome == MediaPageOutcome.MORE_AVAILABLE
        return MediaPage(
            request = request,
            records = records,
            rowsRead = rowsRead,
            providerTotalCount = null,
            nextOffset = if (hasMore) offset + rowsRead else null,
            outcome = outcome,
        )
    }

    @Test
    fun acceptsDistinctPagesAndAllowsSameOffsetReplay() {
        val guard = MediaPageSequenceGuard()
        assertEquals(PageSequenceDecision.ACCEPTED, guard.observe(page(0, listOf("1", "2"))))
        assertEquals(PageSequenceDecision.ACCEPTED, guard.observe(page(2, listOf("3", "4"))))
        assertEquals(PageSequenceDecision.SAME_OFFSET_SAME_CONTENT, guard.observe(page(2, listOf("3", "4"))))
    }

    @Test
    fun rejectsOverlapAcrossDifferentOffsets() {
        val guard = MediaPageSequenceGuard()
        assertEquals(PageSequenceDecision.ACCEPTED, guard.observe(page(0, listOf("1", "2"))))
        assertEquals(PageSequenceDecision.OVERLAPPING_ITEMS, guard.observe(page(2, listOf("2", "3"))))
    }

    @Test
    fun detectsOverlapInRawProviderRowsEvenWhenSystemFilteredFromVisibleRecords() {
        val guard = MediaPageSequenceGuard()
        fun hiddenPage(offset: Int, keys: List<String>, more: Boolean): MediaPage {
            val request = MediaPageRequest("session", MediaKind.IMAGE, "external_primary", offset, 100)
            val rowsRead = keys.size
            return MediaPage(
                request = request,
                records = emptyList(),
                rowsRead = rowsRead,
                providerTotalCount = null,
                nextOffset = if (more) offset + rowsRead else null,
                outcome = if (more) MediaPageOutcome.MORE_AVAILABLE else MediaPageOutcome.END_OF_SOURCE,
                observedRowKeys = keys,
            )
        }
        assertEquals(
            PageSequenceDecision.ACCEPTED,
            guard.observe(hiddenPage(0, listOf("content://media/1", "content://media/2"), true)),
        )
        assertEquals(
            PageSequenceDecision.OVERLAPPING_ITEMS,
            guard.observe(hiddenPage(2, listOf("content://media/2", "content://media/3"), false)),
        )
    }

    @Test
    fun rejectsChangedContentForPreviouslyObservedOffset() {
        val guard = MediaPageSequenceGuard()
        assertEquals(PageSequenceDecision.ACCEPTED, guard.observe(page(0, listOf("1", "2"))))
        assertEquals(PageSequenceDecision.SAME_OFFSET_CHANGED_CONTENT, guard.observe(page(0, listOf("1", "3"))))
    }

    @Test
    fun boundsRememberedPagesAndCanClear() {
        val guard = MediaPageSequenceGuard(maxRememberedPages = 2)
        assertEquals(PageSequenceDecision.ACCEPTED, guard.observe(page(0, listOf("1"))))
        assertEquals(PageSequenceDecision.ACCEPTED, guard.observe(page(1, listOf("2"))))
        assertEquals(PageSequenceDecision.ACCEPTED, guard.observe(page(2, listOf("3"))))
        assertEquals(2, guard.rememberedPageCount())
        guard.clear()
        assertEquals(0, guard.rememberedPageCount())
        assertFailsWith<IllegalArgumentException> { MediaPageSequenceGuard(0) }
    }
}
