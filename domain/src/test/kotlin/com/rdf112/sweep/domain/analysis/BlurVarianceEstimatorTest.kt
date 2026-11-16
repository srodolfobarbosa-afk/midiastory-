package com.rdf112.sweep.domain.analysis

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlurVarianceEstimatorTest {
    @Test
    fun rejectsSmallOrInvalidSamplesFailClosed() {
        val tiny = IntArray(8 * 8) { 128 }
        assertNull(BlurVarianceEstimator.analyzeLumaSample(tiny, 8, 8))

        val truncated = IntArray(100) { 128 }
        assertNull(BlurVarianceEstimator.analyzeArgbPixels(truncated, 32, 32))
    }

    @Test
    fun detectsSmoothBlurredImageAndRejectsSharpEdgeImage() {
        val width = 48
        val height = 48

        // Smooth gradual ramp -> very low Laplacian variance (blurry / out-of-focus)
        val smoothLuma = IntArray(width * height) { idx ->
            val x = idx % width
            val y = idx / width
            ((x * 2 + y * 2) / 2).coerceIn(0, 255)
        }
        val smoothResult = assertNotNull(BlurVarianceEstimator.analyzeLumaSample(smoothLuma, width, height))
        assertTrue(smoothResult.isBlurryCandidate, "Expected smooth ramp to classify as blurry, variance=${smoothResult.laplacianVariance}")
        assertTrue(smoothResult.laplacianVariance < BlurVarianceEstimator.DEFAULT_BLUR_VARIANCE_THRESHOLD)

        // Sharp alternating checkerboard/edges -> high Laplacian variance (crisp image)
        val sharpLuma = IntArray(width * height) { idx ->
            val x = idx % width
            val y = idx / width
            if (((x / 2) + (y / 2)) % 2 == 0) 240 else 15
        }
        val sharpResult = assertNotNull(BlurVarianceEstimator.analyzeLumaSample(sharpLuma, width, height))
        assertFalse(sharpResult.isBlurryCandidate, "Expected sharp edges NOT to classify as blurry, variance=${sharpResult.laplacianVariance}")
        assertTrue(sharpResult.laplacianVariance > 1000.0)
    }
}
