package com.rdf112.sweep.domain.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StorageUsageCalculatorTest {
    @Test
    fun calculatesPrimarySharedStorageUsage() {
        val snapshot = StorageUsageCalculator.from(totalBytes = 1_000L, availableBytes = 250L)
        requireNotNull(snapshot)
        assertEquals(1_000L, snapshot.totalBytes)
        assertEquals(750L, snapshot.usedBytes)
        assertEquals(250L, snapshot.availableBytes)
        assertEquals(0.75, snapshot.usedFraction, 0.000001)
        assertEquals(75, snapshot.usedPercent)
    }

    @Test
    fun clampsAvailableBytesAboveCapacityToZeroUsage() {
        val snapshot = StorageUsageCalculator.from(totalBytes = 1_000L, availableBytes = 1_500L)
        requireNotNull(snapshot)
        assertEquals(0L, snapshot.usedBytes)
        assertEquals(0, snapshot.usedPercent)
    }

    @Test
    fun rejectsInvalidCapacityAndNegativeAvailability() {
        assertNull(StorageUsageCalculator.from(0L, 0L))
        assertNull(StorageUsageCalculator.from(-1L, 0L))
        assertNull(StorageUsageCalculator.from(1_000L, -1L))
    }

    @Test
    fun roundsPercentageWithinTheValidRange() {
        val almostFull = StorageUsageCalculator.from(100L, 1L)
        val almostEmpty = StorageUsageCalculator.from(100L, 99L)
        assertEquals(99, requireNotNull(almostFull).usedPercent)
        assertEquals(1, requireNotNull(almostEmpty).usedPercent)
    }
}
