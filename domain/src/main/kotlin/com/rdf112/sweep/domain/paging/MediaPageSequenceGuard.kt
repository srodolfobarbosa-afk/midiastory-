package com.rdf112.sweep.domain.paging

import com.rdf112.sweep.domain.model.MediaPage
import com.rdf112.sweep.domain.model.MediaPageOutcome
import java.util.LinkedHashMap

/** Result of comparing a page against recently observed offsets for the same source. */
enum class PageSequenceDecision {
    ACCEPTED,
    SAME_OFFSET_SAME_CONTENT,
    OVERLAPPING_ITEMS,
    SAME_OFFSET_CHANGED_CONTENT,
}

/**
 * Bounded guard against providers that claim to honor offset but repeat or mutate pages.
 * It stores raw provider-row URI keys and visible-record fingerprints for only a small
 * recent window; it never retains the complete media library.
 */
class MediaPageSequenceGuard(maxRememberedPages: Int = DEFAULT_REMEMBERED_PAGES) {
    private val maxPages = maxRememberedPages.also {
        require(it in 1..MAX_REMEMBERED_PAGES) {
            "maxRememberedPages must be between 1 and $MAX_REMEMBERED_PAGES"
        }
    }
    private data class PageObservation(
        val rawRowKeys: List<String>,
        val visibleRecordFingerprints: List<String>,
    )

    private val observedByOffset = LinkedHashMap<Int, PageObservation>()

    @Synchronized
    fun observe(page: MediaPage): PageSequenceDecision {
        require(
            page.outcome == MediaPageOutcome.MORE_AVAILABLE ||
                page.outcome == MediaPageOutcome.END_OF_SOURCE
        ) { "Only successful pages can be observed by the sequence guard" }

        val offset = page.request.offset
        val observation = PageObservation(
            rawRowKeys = page.observedRowKeys.ifEmpty { page.records.map { it.uri } },
            visibleRecordFingerprints = page.records.map { record ->
                listOf(
                    record.uri,
                    record.displayName.orEmpty(),
                    record.mimeType.orEmpty(),
                    record.sizeBytes.toString(),
                    record.dateModifiedEpochSeconds?.toString().orEmpty(),
                ).joinToString("\u001f")
            },
        )
        val existingAtOffset = observedByOffset[offset]
        if (existingAtOffset != null) {
            observedByOffset.remove(offset)
            observedByOffset[offset] = existingAtOffset
            return if (existingAtOffset == observation) {
                PageSequenceDecision.SAME_OFFSET_SAME_CONTENT
            } else {
                PageSequenceDecision.SAME_OFFSET_CHANGED_CONTENT
            }
        }

        val currentKeys = observation.rawRowKeys.toHashSet()
        val overlaps = currentKeys.isNotEmpty() && observedByOffset.values.any { previous ->
            previous.rawRowKeys.any(currentKeys::contains)
        }
        if (overlaps) return PageSequenceDecision.OVERLAPPING_ITEMS

        observedByOffset[offset] = observation
        while (observedByOffset.size > maxPages) {
            val oldestOffset = observedByOffset.keys.first()
            observedByOffset.remove(oldestOffset)
        }
        return PageSequenceDecision.ACCEPTED
    }

    @Synchronized
    fun clear() = observedByOffset.clear()

    @Synchronized
    fun rememberedPageCount(): Int = observedByOffset.size

    companion object {
        const val DEFAULT_REMEMBERED_PAGES = 8
        const val MAX_REMEMBERED_PAGES = 16
    }
}
