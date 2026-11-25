package com.rdf112.sweep.work

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyDigestPolicyTest {

    private val baseNow = 1_764_000_000_000L
    private val eightDaysAgo = baseNow - (8L * 24L * 3600L * 1000L)
    private val threeDaysAgo = baseNow - (3L * 24L * 3600L * 1000L)
    private val twelveHoursAgo = baseNow - (12L * 3600L * 1000L)

    @Test
    fun `allows digest when recoverable is at least 300 MB, idle over 48h, and over 7d since last alert`() {
        assertTrue(
            WeeklyDigestPolicy.shouldSendWeeklyDigest(
                nowEpochMs = baseNow,
                lastDigestSentEpochMs = eightDaysAgo,
                lastAppOpenedEpochMs = threeDaysAgo,
                recoverableBytes = 420L * 1024L * 1024L,
                notificationsEnabled = true,
            ),
        )
    }

    @Test
    fun `silences digest when recoverable bytes are below 300 MB`() {
        assertFalse(
            WeeklyDigestPolicy.shouldSendWeeklyDigest(
                nowEpochMs = baseNow,
                lastDigestSentEpochMs = 0L,
                lastAppOpenedEpochMs = threeDaysAgo,
                recoverableBytes = 299L * 1024L * 1024L,
                notificationsEnabled = true,
            ),
        )
    }

    @Test
    fun `enforces max once per 7 days anti-spam cap`() {
        val fiveDaysAgo = baseNow - (5L * 24L * 3600L * 1000L)
        assertFalse(
            WeeklyDigestPolicy.shouldSendWeeklyDigest(
                nowEpochMs = baseNow,
                lastDigestSentEpochMs = fiveDaysAgo,
                lastAppOpenedEpochMs = threeDaysAgo,
                recoverableBytes = 900L * 1024L * 1024L,
                notificationsEnabled = true,
            ),
        )
    }

    @Test
    fun `silences digest when user opened app in the last 48 hours`() {
        assertFalse(
            WeeklyDigestPolicy.shouldSendWeeklyDigest(
                nowEpochMs = baseNow,
                lastDigestSentEpochMs = eightDaysAgo,
                lastAppOpenedEpochMs = twelveHoursAgo,
                recoverableBytes = 900L * 1024L * 1024L,
                notificationsEnabled = true,
            ),
        )
    }
}
