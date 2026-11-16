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
        val r1 = rec("1", 15L * 1024L * 1024L, 100L, "a".repeat(64))
        val r2 = rec("2", 15L * 1024L * 1024L, 200L, "a".repeat(64))
        val r3 = rec("3", 500L, 300L, "b".repeat(64))

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
        val originalSnapshot = requireNotNull(selectableDuplicate.duplicateReferenceSnapshot)
        assertEquals(r1.uri, originalSnapshot.uri)
        assertEquals("a".repeat(64), originalSnapshot.sha256)
        assertTrue(protectedItem.duplicateReferenceSnapshot == null)
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
        val hashed = rec("30", 8L * 1024L * 1024L, 100L, "c".repeat(64))
        val missingHash = rec("31", 8L * 1024L * 1024L, 200L)

        val confirmed = SmartMediaGroupingPolicy.markProtectedReferencesForSameSizeGroups(
            listOf(hashed, missingHash)
        )
        assertTrue(confirmed.isEmpty())
    }

    @Test
    fun filtersOldScreenshotsOlderThan30DaysOnlyForImages() {
        val now = 40L * 24L * 3600L
        val oldShotByPath = MediaRecord(
            uri = "content://media/external_primary/images/media/101",
            volumeName = "external_primary",
            kind = MediaKind.IMAGE,
            displayName = "IMG_20260101.png",
            mimeType = "image/png",
            sizeBytes = 2L * 1024L * 1024L,
            dateModifiedEpochSeconds = 5L * 24L * 3600L,
            relativePath = "Pictures/Screenshots/",
        )
        val recentShotByName = MediaRecord(
            uri = "content://media/external_primary/images/media/102",
            volumeName = "external_primary",
            kind = MediaKind.IMAGE,
            displayName = "Screenshot_20261115.png",
            mimeType = "image/png",
            sizeBytes = 1L * 1024L * 1024L,
            dateModifiedEpochSeconds = now - 3600L,
            relativePath = "DCIM/Screenshots/",
        )
        val oldCameraPhoto = MediaRecord(
            uri = "content://media/external_primary/images/media/103",
            volumeName = "external_primary",
            kind = MediaKind.IMAGE,
            displayName = "IMG_CAMERA_001.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 4L * 1024L * 1024L,
            dateModifiedEpochSeconds = 5L * 24L * 3600L,
            relativePath = "DCIM/Camera/",
        )
        val oldVideoNamedScreenshot = MediaRecord(
            uri = "content://media/external_primary/video/media/104",
            volumeName = "external_primary",
            kind = MediaKind.VIDEO,
            displayName = "Screenshot_screen_record.mp4",
            mimeType = "video/mp4",
            sizeBytes = 9L * 1024L * 1024L,
            dateModifiedEpochSeconds = 5L * 24L * 3600L,
            relativePath = "Movies/Screenshots/",
        )

        val filtered = SmartMediaGroupingPolicy.applyFilter(
            listOf(oldShotByPath, recentShotByName, oldCameraPhoto, oldVideoNamedScreenshot),
            SmartFilterMode.SCREENSHOTS_30D,
            now,
        )
        assertEquals(listOf(oldShotByPath.uri), filtered.map { it.uri })
    }
}
