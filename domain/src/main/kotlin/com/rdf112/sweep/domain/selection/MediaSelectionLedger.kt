package com.rdf112.sweep.domain.selection

import com.rdf112.sweep.domain.model.MediaRecord
import java.util.Collections
import java.util.LinkedHashMap

sealed interface SelectionUpdate {
    data class Applied(val ledger: MediaSelectionLedger) : SelectionUpdate
    data class LimitReached(val ledger: MediaSelectionLedger, val limit: Int) : SelectionUpdate
    data class NotSelectable(val ledger: MediaSelectionLedger) : SelectionUpdate
}

/**
 * Bounded, URI-keyed review selection. It deliberately has no delete/trash operation.
 * Selection survives page navigation but is cleared when the scan session becomes stale.
 */
class MediaSelectionLedger private constructor(
    selected: Map<String, MediaRecord>,
) {
    private val selectedByUri: Map<String, MediaRecord> =
        Collections.unmodifiableMap(LinkedHashMap(selected))

    val records: List<MediaRecord> =
        Collections.unmodifiableList(ArrayList(selectedByUri.values))
    val size: Int get() = selectedByUri.size
    val isEmpty: Boolean get() = selectedByUri.isEmpty()
    fun isSelected(uri: String): Boolean = uri in selectedByUri

    fun setSelected(record: MediaRecord, selected: Boolean, limit: Int = DEFAULT_LIMIT): SelectionUpdate {
        require(limit in 1..MAX_LIMIT) { "Selection limit must be between 1 and $MAX_LIMIT" }
        val next = LinkedHashMap(selectedByUri)
        if (!selected) {
            next.remove(record.uri)
            return SelectionUpdate.Applied(MediaSelectionLedger(next))
        }
        if (!record.isSelectable || record.isProtected) return SelectionUpdate.NotSelectable(this)
        if (record.uri in next) return SelectionUpdate.Applied(this)
        if (next.size >= limit) return SelectionUpdate.LimitReached(this, limit)
        next[record.uri] = record
        return SelectionUpdate.Applied(MediaSelectionLedger(next))
    }

    fun reconcile(validRecords: Collection<MediaRecord>): MediaSelectionLedger {
        val validByUri = validRecords.asSequence()
            .filter { it.isSelectable && !it.isProtected }
            .associateBy { it.uri }
        return MediaSelectionLedger(
            selectedByUri.keys.mapNotNull { uri -> validByUri[uri]?.let { uri to it } }.toMap(LinkedHashMap())
        )
    }

    fun totalBytesOrNull(): Long? = try {
        records.fold(0L) { total, record -> Math.addExact(total, record.sizeBytes) }
    } catch (_: ArithmeticException) {
        null
    }

    companion object {
        const val DEFAULT_LIMIT = 500
        const val MAX_LIMIT = 500
        fun empty(): MediaSelectionLedger = MediaSelectionLedger(emptyMap())
    }
}
