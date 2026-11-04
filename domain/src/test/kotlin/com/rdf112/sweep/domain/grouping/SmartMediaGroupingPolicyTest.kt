package com.rdf112.sweep.domain.grouping

import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmartMediaGroupingPolicyTest {
    private fun rec(id: String, size: Long, modifiedSec: Long, sha256: String? = null) = MediaRecord(
        uri = "content://media/external_primary/images/media/$id",
        volumeName = "external_primary",
        kind = MediaKind.IMAGE,
        displayName = "file_$id.jpg",
        mimeType = "image/jpeg",
        sizeBytes = size,
        dateModifiedEpochSeconds = modifiedSec,
        contentSha256 = sha256,
    )

    @Test
    fun filtersLargeFilesAndProtectsOneReferenceInSameSizeGroups() {
        val r1 = rec("1", 15L * 1024L * 1024L, 100L, "aa11")
        val r2 = rec("2", 15L * 1024L * 1024L, 200L, "aa11")
        val r3 = rec("3", 500L, 300L, "bb22")

        val large = SmartMediaGroupingPolicy.applyFilter(listOf(r1, r2, r3), SmartFilterMode.LARGE_FILES_10MB, 1000L)
        assertEquals(2, large.size)

        val sameSize = SmartMediaGroupingPolicy.applyFilter(listOf(r1, r2, r3), SmartFilterMode.SAME_SIZE_CANDIDATES, 1000L)
        assertEquals(2, sameSize.size)
        val protectedItem = sameSize.first { it.uri == r1.uri }
        val selectableDuplicate = sameSize.first { it.uri == r2.uri }
        assertTrue(protectedItem.isProtected)
        assertFalse(protectedItem.isSelectable)
        assertFalse(selectableDuplicate.isProtected)
        assertTrue(selectableDuplicate.isSelectable)
    }

    @Test
    fun rejectsSameSizeFilesWithDifferentSha256HashesAsFalsePositives() {
        val sameSizeA = rec("10", 8L * 1024L * 1024L, 100L, "hash_alpha_1111")
        val sameSizeB = rec("11", 8L * 1024L * 1024L, 200L, "hash_beta_2222")

        val confirmed = SmartMediaGroupingPolicy.markProtectedReferencesForSameSizeGroups(listOf(sameSizeA, sameSizeB))
        assertTrue(confirmed.isEmpty(), "Arquivos de mesmo tamanho com SHA-256 diferente nunca devem ser marcados como duplicatas")

        val unconfirmed = SmartMediaGroupingPolicy.findUnconfirmedSameSizeRecords(listOf(sameSizeA, sameSizeB))
        assertEquals(2, unconfirmed.size)
    }
    @Test
    fun neverTreatsSameSizeFilesWithoutSha256AsConfirmedDuplicates() {
        val sameSizeA = rec("20", 8L * 1024L * 1024L, 100L)
        val sameSizeB = rec("21", 8L * 1024L * 1024L, 200L)

        val confirmed = SmartMediaGroupingPolicy.markProtectedReferencesForSameSizeGroups(
            listOf(sameSizeA, sameSizeB)
        )
        assertTrue(confirmed.isEmpty(), "Mesmo tamanho sem SHA-256 não confirma duplicidade")

        val unconfirmed = SmartMediaGroupingPolicy.findUnconfirmedSameSizeRecords(
            listOf(sameSizeA, sameSizeB)
        )
        assertEquals(2, unconfirmed.size)
    }

    @Test
    fun oneMissingHashCannotBeGroupedAsAnExactDuplicate() {
        val hashed = rec("30", 8L * 1024L * 1024L, 100L, "same_hash")
        val missingHash = rec("31", 8L * 1024L * 1024L, 200L)

        val confirmed = SmartMediaGroupingPolicy.markProtectedReferencesForSameSizeGroups(
            listOf(hashed, missingHash)
        )
        assertTrue(confirmed.isEmpty())
    }

}
