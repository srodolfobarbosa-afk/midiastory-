package com.rdf112.sweep.domain.storage

import kotlin.math.roundToInt

/**
 * Immutable snapshot of the primary shared-storage volume.
 * Android framework values are normalized here so the UI never displays impossible capacity values.
 */
data class StorageUsageSnapshot(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val usedFraction: Double,
    val usedPercent: Int,
)

object StorageUsageCalculator {
    /**
     * Returns null when the provider reports an invalid capacity.
     * Available bytes are clamped to [0, totalBytes] to protect the UI from inconsistent providers.
     */
    fun from(totalBytes: Long, availableBytes: Long): StorageUsageSnapshot? {
        if (totalBytes <= 0L || availableBytes < 0L) return null
        val safeAvailableBytes = availableBytes.coerceAtMost(totalBytes)
        val usedBytes = totalBytes - safeAvailableBytes
        val fraction = (usedBytes.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 1.0)
        val percent = (fraction * 100.0).roundToInt().coerceIn(0, 100)
        return StorageUsageSnapshot(totalBytes, usedBytes, safeAvailableBytes, fraction, percent)
    }
}
