package com.rdf112.sweep.domain.selection

import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaSelectionLedgerTest {
    private fun record(id: String, selectable: Boolean = true, protected: Boolean = false, size: Long = 10L) =
        MediaRecord(
            uri = "content://media/external/images/media/$id",
            volumeName = "external_primary",
            kind = MediaKind.IMAGE,
            displayName = "image-$id.jpg",
            mimeType = "image/jpeg",
            sizeBytes = size,
            dateModifiedEpochSeconds = null,
            isProtected = protected,
            isSelectable = selectable,
        )

    @Test
    fun selectionIsEmptyByDefaultAndStoredByUri() {
        val initial = MediaSelectionLedger.empty()
        assertTrue(initial.isEmpty)
        val selected = assertIs<SelectionUpdate.Applied>(initial.setSelected(record("1"), true)).ledger
        assertEquals(1, selected.size)
        assertTrue(selected.isSelected("content://media/external/images/media/1"))
    }

    @Test
    fun protectedOrIneligibleRecordsCannotBeSelected() {
        val initial = MediaSelectionLedger.empty()
        assertIs<SelectionUpdate.NotSelectable>(initial.setSelected(record("p", protected = true), true))
        assertIs<SelectionUpdate.NotSelectable>(initial.setSelected(record("n", selectable = false), true))
        assertTrue(initial.isEmpty)
    }

    @Test
    fun configuredLimitIsEnforcedAndDeselectStillWorks() {
        var ledger = MediaSelectionLedger.empty()
        ledger = assertIs<SelectionUpdate.Applied>(ledger.setSelected(record("1"), true, limit = 1)).ledger
        assertIs<SelectionUpdate.LimitReached>(ledger.setSelected(record("2"), true, limit = 1))
        ledger = assertIs<SelectionUpdate.Applied>(ledger.setSelected(record("1"), false, limit = 1)).ledger
        assertTrue(ledger.isEmpty)
    }

    @Test
    fun reconciliationDropsUrisNoLongerPresentOrEligible() {
        var ledger = MediaSelectionLedger.empty()
        ledger = assertIs<SelectionUpdate.Applied>(ledger.setSelected(record("1"), true)).ledger
        ledger = assertIs<SelectionUpdate.Applied>(ledger.setSelected(record("2"), true)).ledger
        val reconciled = ledger.reconcile(listOf(record("1"), record("2", protected = true)))
        assertEquals(listOf("content://media/external/images/media/1"), reconciled.records.map { it.uri })
    }

    @Test
    fun totalBytesUsesCheckedAddition() {
        var ledger = MediaSelectionLedger.empty()
        ledger = assertIs<SelectionUpdate.Applied>(ledger.setSelected(record("1", size = Long.MAX_VALUE), true)).ledger
        ledger = assertIs<SelectionUpdate.Applied>(ledger.setSelected(record("2", size = 1L), true)).ledger
        assertNull(ledger.totalBytesOrNull())
        assertFalse(ledger.isEmpty)
    }
}
