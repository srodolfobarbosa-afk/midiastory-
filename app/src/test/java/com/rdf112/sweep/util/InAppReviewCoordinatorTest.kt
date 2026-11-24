package com.rdf112.sweep.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppReviewCoordinatorTest {
    @Test
    fun triggersWhenSingleCleanupMeets5MbOrLifetimeMeets25MbAndRespectsCooldown() {
        val now = 1_760_000_000_000L

        // Single cleanup >= 5 MB with no prior prompt -> triggers
        assertTrue(
            InAppReviewCoordinator.shouldTriggerReviewPrompt(
                confirmedFreedBytes = 6L * 1024L * 1024L,
                lifetimeConfirmedFreedBytes = 6L * 1024L * 1024L,
                alreadyRequestedInSession = false,
                lastPromptEpochMs = 0L,
                nowEpochMs = now,
            ),
        )

        // Small single cleanup (2 MB), but lifetime >= 25 MB -> triggers
        assertTrue(
            InAppReviewCoordinator.shouldTriggerReviewPrompt(
                confirmedFreedBytes = 2L * 1024L * 1024L,
                lifetimeConfirmedFreedBytes = 26L * 1024L * 1024L,
                alreadyRequestedInSession = false,
                lastPromptEpochMs = 0L,
                nowEpochMs = now,
            ),
        )

        // Below both thresholds -> does not spam user
        assertFalse(
            InAppReviewCoordinator.shouldTriggerReviewPrompt(
                confirmedFreedBytes = 2L * 1024L * 1024L,
                lifetimeConfirmedFreedBytes = 10L * 1024L * 1024L,
                alreadyRequestedInSession = false,
                lastPromptEpochMs = 0L,
                nowEpochMs = now,
            ),
        )

        // Prompted 2 days ago (< 7-day cooldown) -> suppressed
        assertFalse(
            InAppReviewCoordinator.shouldTriggerReviewPrompt(
                confirmedFreedBytes = 100L * 1024L * 1024L,
                lifetimeConfirmedFreedBytes = 500L * 1024L * 1024L,
                alreadyRequestedInSession = false,
                lastPromptEpochMs = now - (2L * 24L * 60L * 60L * 1_000L),
                nowEpochMs = now,
            ),
        )

        // Prompted 8 days ago (> 7-day cooldown) -> allowed again
        assertTrue(
            InAppReviewCoordinator.shouldTriggerReviewPrompt(
                confirmedFreedBytes = 100L * 1024L * 1024L,
                lifetimeConfirmedFreedBytes = 500L * 1024L * 1024L,
                alreadyRequestedInSession = false,
                lastPromptEpochMs = now - (8L * 24L * 60L * 60L * 1_000L),
                nowEpochMs = now,
            ),
        )
    }
}
