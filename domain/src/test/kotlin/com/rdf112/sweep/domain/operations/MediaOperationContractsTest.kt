package com.rdf112.sweep.domain.operations

import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaOperationContractsTest {
    private fun sampleRecord(id: String, size: Long = 100L, protected: Boolean = false) = MediaRecord(
        uri = "content://media/external_primary/images/media/$id",
        volumeName = "external_primary",
        kind = MediaKind.IMAGE,
        displayName = "img_$id.jpg",
        mimeType = "image/jpeg",
        sizeBytes = size,
        dateModifiedEpochSeconds = 1700000000L,
        isProtected = protected,
        isSelectable = !protected,
    )

    @Test
    fun snapshotValidationDetectsModifiedTrashedOrPendingItems() {
        assertEquals(
            ItemRevalidationReason.VALID_UNCHANGED,
            RevalidatedOperationPlan.isSnapshotUnchanged(
                expectedSizeBytes = 100L,
                expectedMimeType = "image/jpeg",
                expectedDateModifiedSec = 10L,
                actualSizeBytes = 100L,
                actualMimeType = "image/jpeg",
                actualDateModifiedSec = 10L,
                actualIsPending = 0,
                actualIsTrashed = 0,
            )
        )
        assertEquals(
            ItemRevalidationReason.ALREADY_TRASHED,
            RevalidatedOperationPlan.isSnapshotUnchanged(100L, "image/jpeg", 10L, 100L, "image/jpeg", 10L, 0, 1)
        )
        assertEquals(
            ItemRevalidationReason.MODIFIED_SINCE_SCAN,
            RevalidatedOperationPlan.isSnapshotUnchanged(100L, "image/jpeg", 10L, 200L, "image/jpeg", 10L, 0, 0)
        )
    }

    @Test
    fun contentHashRevalidationRequiresAnExactNonEmptySha256Match() {
        assertTrue(RevalidatedOperationPlan.contentHashMatches("ab12", "AB12"))
        assertFalse(RevalidatedOperationPlan.contentHashMatches("ab12", "ab13"))
        assertFalse(RevalidatedOperationPlan.contentHashMatches("ab12", null))
        assertFalse(RevalidatedOperationPlan.contentHashMatches(null, "ab12"))
    }

    @Test
    fun operationPlanSeparatesValidAndRejectedItemsAndChunksBatches() {
        val r1 = sampleRecord("1", 120L)
        val r2 = sampleRecord("2", 80L)
        val r3 = sampleRecord("3", 50L)
        val plan = RevalidatedOperationPlan(
            mode = MediaOperationMode.MOVE_TO_TRASH,
            evaluatedItems = listOf(
                RevalidatedMediaItem(r1, ItemRevalidationReason.VALID_UNCHANGED),
                RevalidatedMediaItem(r2, ItemRevalidationReason.VALID_UNCHANGED),
                RevalidatedMediaItem(r3, ItemRevalidationReason.MODIFIED_SINCE_SCAN),
            ),
            maxBatchSize = 1,
        )
        assertEquals(2, plan.validRecords.size)
        assertEquals(1, plan.rejectedItems.size)
        assertEquals(200L, plan.totalValidBytes)
        assertEquals(2, plan.uriBatches.size)
    }

    @Test
    fun trashRecoveryLedgerKeepsUnconfirmedAndStillTrashedReferencesOnPartialRestore() {
        val uri1 = "content://media/external_primary/images/media/101"
        val uri2 = "content://media/external_primary/images/media/102"
        val uri3 = "content://media/external_primary/images/media/103"

        val initial = TrashRecoveryLedger.empty().recordConfirmedTrashed(
            mapOf(
                uri1 to ObservedMediaPostState.IN_TRASH,
                uri2 to ObservedMediaPostState.IN_TRASH,
                uri3 to ObservedMediaPostState.IN_TRASH,
            )
        )
        assertEquals(3, initial.size)

        val reconciliation = initial.reconcileAfterRestoreAttempt(
            mapOf(
                uri1 to ObservedMediaPostState.RESTORED_ACTIVE,
                uri2 to ObservedMediaPostState.IN_TRASH,
                uri3 to ObservedMediaPostState.UNVERIFIED_ERROR,
            )
        )

        assertFalse(reconciliation.isCompleteSuccess)
        assertTrue(reconciliation.isPartialOrUnresolved)
        assertEquals(setOf(uri1), reconciliation.confirmedRestoredUris)
        assertEquals(setOf(uri2, uri3), reconciliation.unresolvedUris)
        assertEquals(setOf(uri2, uri3), reconciliation.updatedLedger.trackedUris)
    }
}
