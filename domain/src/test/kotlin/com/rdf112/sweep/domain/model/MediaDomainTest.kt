package com.rdf112.sweep.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MediaDomainTest {
    private fun record(
        id: String,
        bytes: Long = 10,
        selectable: Boolean = true,
        protected: Boolean = false,
        kind: MediaKind = MediaKind.IMAGE,
    ) = MediaRecord(
        uri = "content://media/external/$id",
        volumeName = "external",
        kind = kind,
        displayName = "$id.jpg",
        mimeType = "image/jpeg",
        sizeBytes = bytes,
        dateModifiedEpochSeconds = 1L,
        isProtected = protected,
        isSelectable = selectable,
    )

    @Test fun selectionStartsEmptyAndImmutableOperationsReturnNewLedger() {
        val empty = SelectionLedger.empty()
        assertTrue(empty.isEmpty)
        assertEquals(0, empty.size)
        val chosen = empty.setSelected(record("1"), true)
        assertFalse(chosen.isEmpty)
        assertEquals(1, chosen.size)
        assertTrue(empty.isEmpty)
    }

    @Test fun protectedAndNonSelectableRecordsCannotBeSelectedOrToggled() {
        val ledger = SelectionLedger.empty()
        val protected = record("p", protected = true)
        val disabled = record("n", selectable = false)
        assertTrue(ledger.setSelected(protected, true).isEmpty)
        assertTrue(ledger.toggle(disabled).isEmpty)
        assertTrue(ledger.toggle(protected).isEmpty)

        val selected = ledger.toggle(record("ok"))
        val current = listOf(record("ok", protected = true), protected, disabled)
        assertFailsWith<IllegalArgumentException> { selected.snapshot(current) }
        assertFalse(selected.setSelected(current.first(), true).isSelected(current.first().uri))
        assertFalse(selected.toggle(current.first()).isSelected(current.first().uri))
        assertTrue(selected.reconcile(current).isEmpty)
    }

    @Test fun snapshotIsStableAcrossScanOrderAndSortedByUri() {
        val a = record("a", 12)
        val b = record("b", 28)
        val ledger = SelectionLedger.empty().setSelected(b, true).setSelected(a, true)
        val first = ledger.snapshot(listOf(a, b))
        val reordered = ledger.snapshot(listOf(b, a))
        assertEquals(listOf(a.uri, b.uri), first.records.map { it.uri })
        assertEquals(first.records.map { it.uri }, reordered.records.map { it.uri })
        assertEquals(40L, first.totalBytes)
        assertEquals(40L, reordered.totalBytes)
    }

    @Test fun reconcileRemovesMissingAndIneligibleRecordsButKeepsOtherSelections() {
        val a = record("a")
        val b = record("b")
        val c = record("c")
        val ledger = SelectionLedger.empty().setSelected(a, true).setSelected(b, true).setSelected(c, true)
        val reconciled = ledger.reconcile(listOf(record("a"), record("b", selectable = false)))
        assertTrue(reconciled.isSelected(a.uri))
        assertFalse(reconciled.isSelected(b.uri))
        assertFalse(reconciled.isSelected(c.uri))
        assertTrue(ledger.isSelected(b.uri))
    }

    @Test fun snapshotSumsEachSelectedRecordExactlyOnce() {
        val a = record("a", 7)
        val b = record("b", 11)
        val ledger = SelectionLedger.empty().setSelected(a, true).setSelected(b, true)
        val draft = ledger.snapshot(listOf(a, a, b))
        assertEquals(2, draft.records.size)
        assertEquals(18L, draft.totalBytes)
    }

    @Test fun byteOverflowFailsExplicitly() {
        val a = record("a", Long.MAX_VALUE)
        val b = record("b", 1)
        val ledger = SelectionLedger.empty().setSelected(a, true).setSelected(b, true)
        assertFailsWith<ArithmeticException> { ledger.snapshot(listOf(a, b)) }
        assertFailsWith<ArithmeticException> { OperationDraft(listOf(a, b), 0) }
    }

    @Test fun reportDistinguishesCompleteEmptyFromFailedPartialOrDeniedScans() {
        val allKinds = MediaKind.entries.toSet()
        val fullAccess = MediaKind.entries.associateWith { AccessLevel.FULL }
        val completeEmpty = ScanReport(emptyList(), fullAccess, allKinds, emptyList(), 0)
        assertFalse(completeEmpty.isPartial)
        assertTrue(completeEmpty.isCompleteEmpty)

        val queryFailed = ScanReport(emptyList(), fullAccess, allKinds - MediaKind.VIDEO, emptyList(), 1)
        assertTrue(queryFailed.isPartial)
        assertFalse(queryFailed.isCompleteEmpty)

        val partialAccess = ScanReport(
            emptyList(), fullAccess + (MediaKind.IMAGE to AccessLevel.PARTIAL), allKinds, emptyList(), 1,
        )
        assertTrue(partialAccess.isPartial)
        assertFalse(partialAccess.isCompleteEmpty)

        val denied = ScanReport(
            emptyList(), fullAccess + (MediaKind.AUDIO to AccessLevel.DENIED), allKinds, emptyList(), 1,
        )
        assertTrue(denied.isPartial)
        assertFalse(denied.isCompleteEmpty)

        val unknownAccess = ScanReport(emptyList(), emptyMap(), allKinds, emptyList(), 0)
        assertTrue(unknownAccess.isPartial)
        assertFalse(unknownAccess.isCompleteEmpty)

        val safetyLimited = ScanReport(
            listOf(record("limit")),
            fullAccess,
            allKinds,
            listOf(ScanIssue(ScanIssueCode.RESULT_LIMIT_REACHED, MediaKind.IMAGE)),
            1,
        )
        assertTrue(safetyLimited.isPartial)
        assertFalse(safetyLimited.isCompleteEmpty)
    }

    @Test fun recordAndReportValidationsAreEnforced() {
        assertFailsWith<IllegalArgumentException> { record("x").copy(uri = "/not/content") }
        assertFailsWith<IllegalArgumentException> { record("x").copy(volumeName = " ") }
        assertFailsWith<IllegalArgumentException> { record("x").copy(sizeBytes = -1) }
        assertFailsWith<IllegalArgumentException> {
            ScanReport(listOf(record("x"), record("x")), emptyMap(), emptySet(), emptyList(), 0)
        }
        assertFailsWith<IllegalArgumentException> {
            ScanReport(emptyList(), emptyMap(), emptySet(), emptyList(), -1)
        }
    }

    @Test fun reportAndDraftDefensivelyCopyCollectionsAndDraftValidatesTotal() {
        val mutableRecords = mutableListOf(record("a", 3))
        val report = ScanReport(
            mutableRecords,
            MediaKind.entries.associateWith { AccessLevel.FULL },
            MediaKind.entries.toSet(),
            emptyList(),
            2,
        )
        mutableRecords.clear()
        assertEquals(1, report.records.size)
        assertFailsWith<UnsupportedOperationException> { (report.records as MutableList).clear() }

        val draft = OperationDraft(listOf(record("b", 4)), 4)
        assertEquals(4L, draft.totalBytes)
        assertFailsWith<IllegalArgumentException> { OperationDraft(listOf(record("c", 4)), 5) }
        assertFailsWith<IllegalArgumentException> { OperationDraft(emptyList(), 0) }
        assertFailsWith<IllegalArgumentException> { OperationDraft(listOf(record("d"), record("d")), 20) }
        assertFailsWith<IllegalArgumentException> {
            OperationDraft(listOf(record("protected", protected = true)), 10)
        }
    }

    @Test fun togglingChangesOnlyTargetAndSetSelectedCanDeselect() {
        val a = record("a")
        val b = record("b")
        val both = SelectionLedger.empty().setSelected(a, true).setSelected(b, true)
        val toggled = both.toggle(a)
        assertFalse(toggled.isSelected(a.uri))
        assertTrue(toggled.isSelected(b.uri))
        assertTrue(both.isSelected(a.uri))
        assertFalse(both.setSelected(b, false).isSelected(b.uri))
    }

    @Test fun reportValidatesRequestedKindsAndCalculatesPartialOverRequestedSubset() {
        val visualKinds = setOf(MediaKind.IMAGE, MediaKind.VIDEO)
        val visualAccess = mapOf(MediaKind.IMAGE to AccessLevel.FULL, MediaKind.VIDEO to AccessLevel.FULL, MediaKind.AUDIO to AccessLevel.DENIED)
        val visualOnlyReport = ScanReport(
            records = listOf(record("img1", kind = MediaKind.IMAGE)),
            accessByKind = visualAccess,
            querySucceededKinds = visualKinds,
            issues = emptyList(),
            elapsedMillis = 5,
            requestedKinds = visualKinds,
        )
        assertFalse(visualOnlyReport.isPartial)
        assertEquals(visualKinds, visualOnlyReport.requestedKinds)

        assertFailsWith<IllegalArgumentException> {
            ScanReport(listOf(record("aud1", kind = MediaKind.AUDIO)), visualAccess, visualKinds, emptyList(), 1, visualKinds)
        }
        assertFailsWith<IllegalArgumentException> {
            ScanReport(emptyList(), visualAccess, setOf(MediaKind.AUDIO), emptyList(), 1, visualKinds)
        }
        assertFailsWith<IllegalArgumentException> {
            ScanReport(emptyList(), visualAccess, visualKinds, listOf(ScanIssue(ScanIssueCode.PROVIDER_FAILURE, kind = MediaKind.AUDIO)), 1, visualKinds)
        }
    }
}
