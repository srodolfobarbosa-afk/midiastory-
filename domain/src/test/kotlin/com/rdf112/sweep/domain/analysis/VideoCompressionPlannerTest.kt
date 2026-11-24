package com.rdf112.sweep.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCompressionPlannerTest {
    @Test
    fun buildPlanDownscales4kToMacroblockAligned720pAndCutsBitrate() {
        val plan = VideoCompressionPlanner.buildPlan(
            sourceSizeBytes = 240L * 1024L * 1024L,
            sourceWidth = 3840,
            sourceHeight = 2160,
            durationMs = 45_000L,
            sourceBitrateBps = 42_000_000,
            sourceFrameRate = 60,
            rotationDegrees = 90,
            preferHevc = true,
        )
        assertNotNull(plan)
        requireNotNull(plan)
        assertEquals(1280, plan.targetWidth)
        assertEquals(720, plan.targetHeight)
        assertEquals(0, plan.targetWidth % 16)
        assertEquals(0, plan.targetHeight % 16)
        assertEquals(30, plan.targetFrameRate)
        assertEquals(90, plan.rotationDegrees)
        assertTrue(plan.targetBitrateBps in 350_000..6_000_000)
        assertTrue(plan.estimatedOutputBytes < 40L * 1024L * 1024L)
    }

    @Test
    fun buildPlanRejectsSmallOrAlreadyLowBitrateVideos() {
        assertNull(
            VideoCompressionPlanner.buildPlan(
                sourceSizeBytes = 5L * 1024L * 1024L,
                sourceWidth = 1920,
                sourceHeight = 1080,
                durationMs = 15_000L,
                sourceBitrateBps = 2_500_000,
                sourceFrameRate = 30,
            ),
        )

        // 20 MB over 8 minutes = ~330 kbps already ultra-compressed -> must return null
        assertNull(
            VideoCompressionPlanner.buildPlan(
                sourceSizeBytes = 20L * 1024L * 1024L,
                sourceWidth = 640,
                sourceHeight = 360,
                durationMs = 480_000L,
                sourceBitrateBps = 340_000,
                sourceFrameRate = 24,
            ),
        )
    }

    @Test
    fun postTranscodeVerificationRejectsTruncatedOrInsufficientSavings() {
        // Valid 75% reduction with matching duration
        assertTrue(
            VideoCompressionPlanner.isCompressedOutputVerified(
                sourceSizeBytes = 200L * 1024L * 1024L,
                sourceDurationMs = 60_000L,
                outputSizeBytes = 48L * 1024L * 1024L,
                outputDurationMs = 59_850L,
            ),
        )

        // Truncated video (stopped halfway at 30s instead of 60s) -> FAIL CLOSED
        assertFalse(
            VideoCompressionPlanner.isCompressedOutputVerified(
                sourceSizeBytes = 200L * 1024L * 1024L,
                sourceDurationMs = 60_000L,
                outputSizeBytes = 25L * 1024L * 1024L,
                outputDurationMs = 30_000L,
            ),
        )

        // Only 5% smaller -> FAIL CLOSED
        assertFalse(
            VideoCompressionPlanner.isCompressedOutputVerified(
                sourceSizeBytes = 100L * 1024L * 1024L,
                sourceDurationMs = 60_000L,
                outputSizeBytes = 95L * 1024L * 1024L,
                outputDurationMs = 60_000L,
            ),
        )
    }
}
