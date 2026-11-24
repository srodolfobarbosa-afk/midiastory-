package com.rdf112.sweep.domain.analysis

import com.rdf112.sweep.domain.model.MediaKind
import com.rdf112.sweep.domain.model.MediaRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerceptualImageHasherTest {

    private fun sampleRecord(
        id: Int,
        sizeBytes: Long = 2_000_000L,
        modifiedSec: Long? = 1_760_000_000L,
    ): MediaRecord = MediaRecord(
        uri = "content://media/external/images/media/$id",
        volumeName = "external_primary",
        kind = MediaKind.IMAGE,
        displayName = "IMG_$id.jpg",
        mimeType = "image/jpeg",
        sizeBytes = sizeBytes,
        dateModifiedEpochSeconds = modifiedSec,
    )

    @Test
    fun `exposure shifted gradient produces zero or near-zero hamming distance`() {
        val w = 36
        val h = 32
        val base = IntArray(w * h) { idx ->
            val x = idx % w
            val y = idx / w
            ((x * 5 + y * 3) % 200) + 10
        }
        val brighter = IntArray(w * h) { idx -> (base[idx] + 25).coerceAtMost(255) }

        val h1 = PerceptualImageHasher.computeDHash64FromLuma(base, w, h)
        val h2 = PerceptualImageHasher.computeDHash64FromLuma(brighter, w, h)

        assertNotNull(h1)
        assertNotNull(h2)
        assertTrue(PerceptualImageHasher.hammingDistance(h1!!, h2!!) <= 1)
    }

    @Test
    fun `distinct horizontal patterns produce large hamming distance`() {
        val w = 36
        val h = 32
        val leftToRight = IntArray(w * h) { idx -> ((idx % w) * 255) / (w - 1) }
        val rightToLeft = IntArray(w * h) { idx -> 255 - (((idx % w) * 255) / (w - 1)) }

        val h1 = PerceptualImageHasher.computeDHash64FromLuma(leftToRight, w, h)
        val h2 = PerceptualImageHasher.computeDHash64FromLuma(rightToLeft, w, h)

        assertNotNull(h1)
        assertNotNull(h2)
        assertEquals(64, PerceptualImageHasher.hammingDistance(h1!!, h2!!))
    }

    @Test
    fun `flat blank thumbnail fails closed to prevent false positive grouping`() {
        val w = 32
        val h = 32
        val blank = IntArray(w * h) { 128 }
        assertNull(PerceptualImageHasher.computeDHash64FromLuma(blank, w, h))
    }

    @Test
    fun `burst cluster protects sharpest photo and sums only review candidates`() {
        val baseHash = 0x0F0F_0F0F_0F0F_0F0FL
        val slightShift1 = baseHash xor 0b0011L // distance 2
        val slightShift2 = baseHash xor 0b0101L // distance 2

        val blurry1 = PhotoVisualFingerprint(
            record = sampleRecord(id = 101, sizeBytes = 3_100_000L, modifiedSec = 1_760_000_010L),
            dHash64 = slightShift1,
            sharpnessScore = 45.0,
        )
        val sharpBest = PhotoVisualFingerprint(
            record = sampleRecord(id = 102, sizeBytes = 4_500_000L, modifiedSec = 1_760_000_011L),
            dHash64 = baseHash,
            sharpnessScore = 410.0,
        )
        val blurry2 = PhotoVisualFingerprint(
            record = sampleRecord(id = 103, sizeBytes = 2_900_000L, modifiedSec = 1_760_000_012L),
            dHash64 = slightShift2,
            sharpnessScore = 82.0,
        )

        val groups = PerceptualImageHasher.detectSimilarGroups(listOf(blurry1, sharpBest, blurry2))
        assertEquals(1, groups.size)

        val group = groups.single()
        assertEquals("content://media/external/images/media/102", group.bestKeep.record.uri)
        assertTrue(group.bestKeep.record.isProtected)
        assertFalse(group.bestKeep.record.isSelectable)

        assertEquals(2, group.candidatesToReview.size)
        assertTrue(group.candidatesToReview.all { it.record.isSelectable && !it.record.isProtected })
        assertEquals(6_000_000L, group.recoverableBytes)
    }

    @Test
    fun `moderate similarity across distant dates does not falsely group while near-identical re-saves do`() {
        val hashA = 0x1234_5678_9ABC_DEF0L
        val hashModerate = hashA xor 0b0111_1111L // distance 7 (> NEAR_IDENTICAL 3, <= 8)
        val hashNearIdentical = hashA xor 0b0011L // distance 2 (<= NEAR_IDENTICAL 3)

        val oldPhoto = PhotoVisualFingerprint(
            record = sampleRecord(id = 1, sizeBytes = 2_000_000L, modifiedSec = 1_700_000_000L),
            dHash64 = hashA,
            sharpnessScore = 220.0,
        )
        val unrelatedTwoYearsLater = PhotoVisualFingerprint(
            record = sampleRecord(id = 2, sizeBytes = 2_100_000L, modifiedSec = 1_763_000_000L),
            dHash64 = hashModerate,
            sharpnessScore = 230.0,
        )
        assertTrue(PerceptualImageHasher.detectSimilarGroups(listOf(oldPhoto, unrelatedTwoYearsLater)).isEmpty())

        val resavedCopyLater = PhotoVisualFingerprint(
            record = sampleRecord(id = 3, sizeBytes = 1_500_000L, modifiedSec = 1_763_000_000L),
            dHash64 = hashNearIdentical,
            sharpnessScore = 180.0,
        )
        val groups = PerceptualImageHasher.detectSimilarGroups(listOf(oldPhoto, resavedCopyLater))
        assertEquals(1, groups.size)
        assertEquals("content://media/external/images/media/1", groups.single().bestKeep.record.uri)
    }
}
