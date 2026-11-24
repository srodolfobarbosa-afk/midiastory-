package com.rdf112.sweep.domain.analysis

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

data class VideoCompressionPlan(
    val targetWidth: Int,
    val targetHeight: Int,
    val targetBitrateBps: Int,
    val targetFrameRate: Int,
    val rotationDegrees: Int,
    val estimatedOutputBytes: Long,
) {
    init {
        require(targetWidth >= 16 && targetWidth % 2 == 0) { "targetWidth must be even and >= 16" }
        require(targetHeight >= 16 && targetHeight % 2 == 0) { "targetHeight must be even and >= 16" }
        require(targetBitrateBps > 0) { "targetBitrateBps must be positive" }
        require(targetFrameRate in 15..60) { "targetFrameRate must be within 15..60" }
    }
}

/**
 * Pure Kotlin planner and post-transcode safety verifier for hardware H.265/HEVC video compression.
 * Guarantees deterministic sizing, macroblock-aligned dimensions, and fail-closed verification
 * so Midiastore never saves a larger, corrupted, or truncated video.
 */
object VideoCompressionPlanner {
    const val MIN_COMPRESSIBLE_VIDEO_BYTES: Long = 15L * 1024L * 1024L // 15 MB
    const val MAX_LONG_EDGE_PX: Int = 1280
    const val MIN_SAVINGS_RATIO: Double = 0.15 // Must save at least 15% to be worth keeping
    const val MIN_VALID_OUTPUT_BYTES: Long = 64L * 1024L

    fun buildPlan(
        sourceSizeBytes: Long,
        sourceWidth: Int,
        sourceHeight: Int,
        durationMs: Long,
        sourceBitrateBps: Int?,
        sourceFrameRate: Int?,
        rotationDegrees: Int = 0,
        preferHevc: Boolean = true,
    ): VideoCompressionPlan? {
        if (sourceSizeBytes < MIN_COMPRESSIBLE_VIDEO_BYTES) return null
        if (sourceWidth < 64 || sourceHeight < 64 || durationMs < 1_000L) return null

        val longEdge = maxOf(sourceWidth, sourceHeight)
        val scale = if (longEdge > MAX_LONG_EDGE_PX) {
            MAX_LONG_EDGE_PX.toDouble() / longEdge.toDouble()
        } else {
            1.0
        }

        val alignedW = alignMacroblock((sourceWidth * scale).roundToInt(), minPx = 160)
        val alignedH = alignMacroblock((sourceHeight * scale).roundToInt(), minPx = 160)
        val fps = (sourceFrameRate ?: 30).coerceIn(24, 30)

        val inferredSourceBitrate = sourceBitrateBps?.takeIf { it > 100_000 }
            ?: (((sourceSizeBytes * 8.0) / (durationMs / 1000.0)).roundToInt().coerceAtLeast(500_000))

        // Bits-per-pixel-per-frame target: 0.065 for HEVC, 0.090 for AVC fallback
        val bpp = if (preferHevc) 0.065 else 0.090
        val qualityBitrate = (alignedW.toDouble() * alignedH.toDouble() * fps.toDouble() * bpp).roundToInt()
        val maxAllowedBitrate = (inferredSourceBitrate * 0.55).roundToInt().coerceAtLeast(350_000)
        val targetBitrate = min(qualityBitrate, maxAllowedBitrate).coerceIn(350_000, 6_000_000)

        // If source bitrate is already near or below target, compression will not produce meaningful savings
        if (targetBitrate >= (inferredSourceBitrate * 0.82).roundToInt()) {
            return null
        }

        val audioAllowanceBps = 128_000L
        val estimatedBytes = (((targetBitrate.toLong() + audioAllowanceBps) * durationMs) / 8_000L)
            .coerceAtLeast(MIN_VALID_OUTPUT_BYTES)

        val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
        return VideoCompressionPlan(
            targetWidth = alignedW,
            targetHeight = alignedH,
            targetBitrateBps = targetBitrate,
            targetFrameRate = fps,
            rotationDegrees = if (normalizedRotation in setOf(0, 90, 180, 270)) normalizedRotation else 0,
            estimatedOutputBytes = estimatedBytes,
        )
    }

    /**
     * Fail-closed post-transcode verification:
     * 1. Output file must be non-empty (>= 64 KB).
     * 2. Output duration must match source duration within 8% (or 1.5s for short clips) so truncated encodes are rejected.
     * 3. Output must achieve at least [MIN_SAVINGS_RATIO] (15%) real byte reduction.
     */
    fun isCompressedOutputVerified(
        sourceSizeBytes: Long,
        sourceDurationMs: Long,
        outputSizeBytes: Long,
        outputDurationMs: Long,
    ): Boolean {
        if (sourceSizeBytes <= 0L || outputSizeBytes < MIN_VALID_OUTPUT_BYTES) return false
        if (sourceDurationMs <= 0L || outputDurationMs <= 0L) return false

        val maxDriftMs = maxOf(1_500L, (sourceDurationMs * 0.08).toLong())
        if (abs(sourceDurationMs - outputDurationMs) > maxDriftMs) return false

        val maxAcceptableBytes = (sourceSizeBytes * (1.0 - MIN_SAVINGS_RATIO)).toLong()
        return outputSizeBytes in MIN_VALID_OUTPUT_BYTES..maxAcceptableBytes
    }

    internal fun alignMacroblock(value: Int, minPx: Int = 16): Int {
        val clamped = value.coerceAtLeast(minPx)
        val rem = clamped % 16
        val aligned = if (rem == 0) clamped else clamped - rem
        return aligned.coerceAtLeast(16)
    }
}
