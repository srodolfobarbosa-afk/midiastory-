package com.rdf112.sweep.domain.analysis

import kotlin.math.sqrt

/**
 * Result of local discrete Laplacian sharpness estimation on a luma image sample.
 */
data class BlurAnalysisResult(
    val width: Int,
    val height: Int,
    val laplacianVariance: Double,
    val luminanceStdDev: Double,
    val isBlurryCandidate: Boolean,
)

/**
 * Pure Kotlin deterministic sharpness estimator (discrete 4-neighbor Laplacian variance).
 * Designed for downsampled grayscale thumbnails (~96px); fails closed (returns null) if
 * dimensions are invalid or smaller than [MIN_SAMPLE_SIDE].
 */
object BlurVarianceEstimator {
    const val MIN_SAMPLE_SIDE = 16
    const val DEFAULT_BLUR_VARIANCE_THRESHOLD = 110.0

    fun rgbPixelToLuma(argb: Int): Int {
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        // BT.601 integer approximation in [0..255]
        return (299 * r + 587 * g + 114 * b) / 1000
    }

    fun analyzeArgbPixels(
        argbPixels: IntArray,
        width: Int,
        height: Int,
        threshold: Double = DEFAULT_BLUR_VARIANCE_THRESHOLD,
    ): BlurAnalysisResult? {
        if (width < MIN_SAMPLE_SIDE || height < MIN_SAMPLE_SIDE) return null
        val pixelCount = width.toLong() * height.toLong()
        if (pixelCount <= 0L || argbPixels.size.toLong() < pixelCount) return null

        val luma = IntArray(pixelCount.toInt()) { idx -> rgbPixelToLuma(argbPixels[idx]) }
        return analyzeLumaSample(luma, width, height, threshold)
    }

    fun analyzeLumaSample(
        luma: IntArray,
        width: Int,
        height: Int,
        threshold: Double = DEFAULT_BLUR_VARIANCE_THRESHOLD,
    ): BlurAnalysisResult? {
        if (width < MIN_SAMPLE_SIDE || height < MIN_SAMPLE_SIDE) return null
        val pixelCount = width.toLong() * height.toLong()
        if (pixelCount <= 0L || luma.size.toLong() < pixelCount) return null

        val interiorWidth = width - 2
        val interiorHeight = height - 2
        val n = interiorWidth * interiorHeight
        if (n <= 0) return null

        var lapSum = 0.0
        var lapSqSum = 0.0
        var lumaSum = 0.0
        var lumaSqSum = 0.0

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val center = luma[idx].coerceIn(0, 255)
                val up = luma[idx - width].coerceIn(0, 255)
                val down = luma[idx + width].coerceIn(0, 255)
                val left = luma[idx - 1].coerceIn(0, 255)
                val right = luma[idx + 1].coerceIn(0, 255)

                val lap = (4 * center - up - down - left - right).toDouble()
                lapSum += lap
                lapSqSum += lap * lap

                val l = center.toDouble()
                lumaSum += l
                lumaSqSum += l * l
            }
        }

        val invN = 1.0 / n.toDouble()
        val lapMean = lapSum * invN
        val lapVar = ((lapSqSum * invN) - lapMean * lapMean).coerceAtLeast(0.0)

        val lumaMean = lumaSum * invN
        val lumaVar = ((lumaSqSum * invN) - lumaMean * lumaMean).coerceAtLeast(0.0)
        val lumaStdDev = sqrt(lumaVar)

        return BlurAnalysisResult(
            width = width,
            height = height,
            laplacianVariance = lapVar,
            luminanceStdDev = lumaStdDev,
            isBlurryCandidate = lapVar < threshold,
        )
    }
}
